package com.github.hotreload.action;

import java.io.IOException;
import java.util.Arrays;

import org.jetbrains.annotations.NotNull;

import com.github.hotreload.config.ApplicationConfig;
import com.github.hotreload.config.HotReloadPluginComponent;
import com.github.hotreload.http.HotReloadHttpService;
import com.github.hotreload.http.model.HotReloadResult;
import com.intellij.ide.util.JavaAnonymousClassesHelper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
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
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * @author liuzhengyang
 */
public class HotReloadAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null || virtualFile == null) {
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
                        VirtualFile[] outputRoots =
                                compilerModuleExtension.getOutputRoots(true);
                        log("Output Directories " + Arrays.toString(outputRoots));
                        if (outputRoots != null) {
                            result[0] = findClassFile(outputRoots, psiFile);
                        }
                        reloadClassFile(project, result[0]);
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
                    FileEditor editor = FileEditorManager.getInstance(psiFile.getProject()).getSelectedEditor(psiFile.getVirtualFile());
                    int caretOffset = editor == null ? -1 : ((TextEditor) editor).getEditor().getCaretModel().getOffset();
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
                            className = parentClass.getQualifiedName() + JavaAnonymousClassesHelper.getName((PsiAnonymousClass) psiClass);
                        }
                    } else if (psiClass instanceof PsiClass) {
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
        application.invokeLater(() -> {
            file.refresh(false, false, () -> { //
                uploadAndReloadClass(file);
            });
        });
    }

    private void uploadAndReloadClass(final VirtualFile file) {
        ApplicationConfig applicationConfig = HotReloadPluginComponent.getApplicationConfig();
        String server = applicationConfig.getServer();
        String pid = applicationConfig.getPid();

        log("Server " + server + " pid " + pid);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();


        HotReloadHttpService hotReloadHttpService = retrofit.create(HotReloadHttpService.class);
        try {
            RequestBody requestBody = RequestBody.create(MediaType.get("multipart/form-data"), file.contentsToByteArray());

            MultipartBody.Part classFile =
                    MultipartBody.Part.createFormData("file", file.getName(), requestBody);

            RequestBody targetPid =
                    RequestBody.create(MediaType.parse("multipart/form-data"), pid);

            Call<HotReloadResult> hotReloadResultCall =
                    hotReloadHttpService.reloadClass(classFile, targetPid);
            HotReloadResult hotReloadResult = hotReloadResultCall.execute().body();
            log("Result " + hotReloadResult);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void log(String message) {
        System.out.println(message);
    }
}
