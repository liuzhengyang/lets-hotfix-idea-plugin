package com.github.hotreload.action;

import static com.github.hotreload.model.Result.SUCCESS_CODE;
import static com.github.hotreload.utils.Constants.NEED_SELECT_JVM_PROCESS;
import static com.github.hotreload.utils.ReloadUtil.filterProcess;
import static com.github.hotreload.utils.ReloadUtil.getProcessList;
import static com.github.hotreload.utils.ReloadUtil.notifyFailed;
import static com.github.hotreload.utils.ReloadUtil.notifySuccess;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Arrays;
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
import com.intellij.ide.util.JavaAnonymousClassesHelper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;

/**
 * @author liuzhengyang
 */
public class HotReloadAction extends AnAction {

    private static final String NEED_SELECT_PROCESS = "Need select process";
    private Logger logger = Logger.getInstance(HotReloadAction.class);

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

        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile instanceof PsiClassOwner) {
            Module module = ModuleUtilCore.findModuleForPsiElement(psiFile);
            CompilerModuleExtension compilerModuleExtension = CompilerModuleExtension.getInstance(module);
            CompilerManager compilerManager = CompilerManager.getInstance(project);
            VirtualFile[] files = {virtualFile};
            if ("class".equals(virtualFile.getExtension())) {
                reloadClassFile(project, virtualFile);
            } else if (!virtualFile.isInLocalFileSystem() && !virtualFile.isWritable()) {
                // source file in a library
            } else {
                Application application = ApplicationManager.getApplication();
                application.runWriteAction(() -> FileDocumentManager.getInstance().saveAllDocuments());

                CompileScope compileScope = compilerManager.createFilesCompileScope(files);
                final VirtualFile[] result = {null};
                VirtualFile[] outputDirectories = compilerModuleExtension == null ? null
                        : compilerModuleExtension.getOutputRoots(true);

                if (outputDirectories != null && compilerManager.isUpToDate(compileScope)) {
                    result[0] = findClassFile(outputDirectories, psiFile);
                }
                log("Files " + Arrays.toString(files));
                compilerManager.compile(files, (aborted, errors, warnings, compileContext) -> {
                    if (errors == 0) {
                        VirtualFile[] outputRoots = compilerModuleExtension.getOutputRoots(true);
                        log("Output Directories " + Arrays.toString(outputRoots));
                        result[0] = findClassFile(outputRoots, psiFile);
                        reloadClassFile(project, result[0]);
                    } else {
                        log("Compile error " + errors);
                    }
                });
            }
        }
    }

    private VirtualFile findClassFile(final VirtualFile[] outputDirectories, final PsiFile psiFile) {
        return ApplicationManager.getApplication().runReadAction(new Computable<VirtualFile>() {

            @Override
            public VirtualFile compute() {
                if (outputDirectories != null && psiFile instanceof PsiClassOwner) {
                    FileEditor editor = FileEditorManager.getInstance(psiFile.getProject())
                            .getSelectedEditor(psiFile.getVirtualFile());
                    int caretOffset = editor == null ? -1 : ((TextEditor) editor).getEditor().getCaretModel()
                            .getOffset();
                    if (caretOffset >= 0) {
                        PsiElement psiElement = psiFile.findElementAt(caretOffset);
                        PsiClass classAtCaret = findClassAtCaret(psiElement);
                        if (classAtCaret != null) {
                            return getClassFile(classAtCaret);
                        }
                    }
                    PsiClassOwner psiJavaFile = (PsiClassOwner) psiFile;
                    for (PsiClass psiClass : psiJavaFile.getClasses()) {
                        final VirtualFile file = getClassFile(psiClass);
                        if (file != null) {
                            return file;
                        }
                    }
                }
                return null;
            }

            private VirtualFile getClassFile(PsiClass psiClass) {
                String className = psiClass.getQualifiedName();
                if (className == null) {
                    if (psiClass instanceof PsiAnonymousClass) {
                        PsiClass parentClass = PsiTreeUtil.getParentOfType(psiClass, PsiClass.class);
                        if (parentClass != null) {
                            className = parentClass.getQualifiedName() + JavaAnonymousClassesHelper
                                    .getName((PsiAnonymousClass) psiClass);
                        }
                    } else {
                        className = PsiTreeUtil.getParentOfType(psiClass, PsiClass.class).getQualifiedName();
                    }
                }
                StringBuilder sb = new StringBuilder(className);
                while (psiClass.getContainingClass() != null) {
                    sb.setCharAt(sb.lastIndexOf("."), '$');
                    psiClass = psiClass.getContainingClass();
                }
                String classFileName = sb.toString().replace('.', '/') + ".class";
                for (VirtualFile outputDirectory : outputDirectories) {
                    final VirtualFile file = outputDirectory.findFileByRelativePath(classFileName);
                    if (file != null && file.exists()) {
                        return file;
                    }
                }
                return null;
            }

            private PsiClass findClassAtCaret(PsiElement psiElement) {
                while (psiElement != null) {
                    if (psiElement instanceof PsiClass) {
                        return (PsiClass) psiElement;
                    }
                    psiElement = psiElement.getParent();
                    findClassAtCaret(psiElement);
                }
                return null;
            }
        });
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
