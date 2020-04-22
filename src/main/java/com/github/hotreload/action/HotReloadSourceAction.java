package com.github.hotreload.action;

import static com.github.hotreload.model.Result.SUCCESS_CODE;
import static com.github.hotreload.utils.Constants.NEED_SELECT_JVM_PROCESS;
import static com.github.hotreload.utils.ReloadUtil.filterProcess;
import static com.github.hotreload.utils.ReloadUtil.getProcessList;
import static com.github.hotreload.utils.ReloadUtil.notifyFailed;
import static com.github.hotreload.utils.ReloadUtil.notifySuccess;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.github.hotreload.component.SettingStorage;
import com.github.hotreload.config.ApplicationConfig;
import com.github.hotreload.config.PluginConfig;
import com.github.hotreload.http.HttpService;
import com.github.hotreload.http.HttpServiceFactory;
import com.github.hotreload.model.HotfixResult;
import com.github.hotreload.model.JvmProcess;
import com.github.hotreload.model.Result;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;

/**
 * @author liuzhengyang
 */
public class HotReloadSourceAction extends AnAction {

    private static final String NEED_SELECT_PROCESS = "Need select process";
    private Logger logger = Logger.getInstance(HotReloadSourceAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        logger.info("Reload action performed");
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null || virtualFile == null) {
            return;
        }

        ApplicationConfig applicationConfig = SettingStorage.getApplicationConfig();
        if (StringUtils.isEmpty(applicationConfig.getServer())) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, PluginConfig.class);
            return;
        }

        Application application = ApplicationManager.getApplication();
        application.runWriteAction(() -> FileDocumentManager.getInstance().saveAllDocuments());
        Document currentDoc = FileEditorManager.getInstance(project).getSelectedTextEditor().getDocument();
        VirtualFile currentFile = FileDocumentManager.getInstance().getFile(currentDoc);
        reloadClassFile(project, currentFile);
    }

    private void reloadClassFile(final Project project, final VirtualFile file) {
        log("Sync " + project + ' ' + file);
        if (file == null) {
            return;
        }
        Application application = ApplicationManager.getApplication();
        application.invokeLater(() -> file.refresh(false, false,
                () -> startReloadTask(project, file)));
    }

    private void startReloadTask(Project project, VirtualFile file) {
        ProgressManager.getInstance().run(new Backgroundable(project, "Uploading Class") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    uploadAndReloadClass(project, file);
                    notifySuccess();
                } catch (Exception e) {
                    notifyFailed(e.getMessage());
                    log(e.getMessage(), e);
                }
            }
        });
    }

    private void uploadAndReloadClass(final Project project, final VirtualFile file) throws Exception {
        ApplicationConfig applicationConfig = SettingStorage.getApplicationConfig();

        if (applicationConfig.getSelectedProcess() == null) {
            tryRefreshProcessList();
        }
        if (applicationConfig.getSelectedProcess() == null) {
            showDialogWithError(project, NEED_SELECT_PROCESS);
        }
        if (applicationConfig.getSelectedProcess().equals(NEED_SELECT_JVM_PROCESS)) {
            showDialogWithError(project, NEED_SELECT_PROCESS);
        }

        String server = applicationConfig.getServer();
        HttpServiceFactory.trySetServer(server);
        String pid = applicationConfig.getSelectedProcess().getPid();
        String hostName = applicationConfig.getSelectedHostName();
        log("Server " + server + " pid " + pid);
        Result<HotfixResult> result = doHotfix(pid, file, hostName);
        checkNotNull(result);
        if (result.getCode() == SUCCESS_CODE) {
            return;
        }
        boolean success = tryRefreshProcessList();
        if (!success) {
            showDialogWithError(project, NEED_SELECT_PROCESS);
        }
        pid = applicationConfig.getSelectedProcess().getPid();
        result = doHotfix(pid, file, hostName);
        checkNotNull(result);
        if (result.getCode() == SUCCESS_CODE) {
            return;
        }
        String msg = result.getMsg();
        if ("Non-numeric value found - int expected".equals(msg)) {
            return;
        }
        throw new RuntimeException(result.getMsg());
    }

    private Result<HotfixResult> doHotfix(String pid, VirtualFile file, String hostName) throws IOException {
        HttpService httpService = HttpServiceFactory.getInstance();
        RequestBody requestBody = RequestBody.create(MediaType.get("multipart/form-data"), file.contentsToByteArray());
        MultipartBody.Part classFile = MultipartBody.Part.createFormData("file", file.getName(), requestBody);
        RequestBody targetPid = RequestBody.create(MediaType.parse("multipart/form-data"), pid);
        String proxyServer = HttpServiceFactory.normalization(hostName);
        RequestBody proxyServerPart = RequestBody.create(MediaType.parse("multipart/form-data"), proxyServer);
        Call<Result<HotfixResult>> hotReloadResultCall = httpService.reloadClass(classFile,
                targetPid, proxyServerPart);
        return hotReloadResultCall.execute().body();
    }

    private boolean tryRefreshProcessList() {
        ApplicationConfig applicationConfig = SettingStorage.getApplicationConfig();
        List<JvmProcess> processList = getProcessList(applicationConfig.getSelectedHostName());
        List<JvmProcess> filteredProcessList = filterProcess(processList, applicationConfig.getKeywords());
        if (filteredProcessList.size() != 1) {
            return false;
        }
        applicationConfig.setSelectedProcess(filteredProcessList.get(0));
        return true;
    }

    private void showDialogWithError(Project project, String errorMsg) {
        ApplicationManager.getApplication().invokeLater(
                () -> ShowSettingsUtil.getInstance().showSettingsDialog(project, PluginConfig.class));
        throw new RuntimeException(errorMsg);
    }

    private void log(String message) {
        logger.info(message);
    }

    private void log(String message, Throwable throwable) {
        logger.info(message, throwable);
    }
}
