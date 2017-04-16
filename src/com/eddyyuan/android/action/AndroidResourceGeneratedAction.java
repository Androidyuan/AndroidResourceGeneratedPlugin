package com.eddyyuan.android.action;

import com.eddyyuan.android.AndroidResourceGeneratedConfigure;
import com.eddyyuan.android.entity.Selection;
import com.eddyyuan.android.entity.WriteObj;
import com.eddyyuan.android.util.TextUtils;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import org.jetbrains.annotations.NotNull;

/**
 * Created by eddyyuan on 16/6/9.
 */
public class AndroidResourceGeneratedAction extends AnAction {

    private Selection mSelection;

    @Override
    public void actionPerformed(AnActionEvent e) {

        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        final PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);

        if(editor == null || project == null || psiFile == null){
            Messages.showErrorDialog(project, "Sorry, Unexcepted error occured", "Error");
            return;
        }

        String prefix = PropertiesComponent.getInstance().getValue(AndroidResourceGeneratedConfigure.KEY_PREFIX, "");
        if(TextUtils.isEmpty(prefix)) {
            prefix = Messages.showInputDialog(project, "Configure Replace Prefix: \r\nExample:\rif you set \"getApplication()\"\rBefore:\r.getResources().getString(R.string.xxx)\rAfter:\rgetApplication().getResources().getString(R.string.xxx)", "Configurateion", null);
            if(!TextUtils.isEmpty(prefix)) {
                PropertiesComponent.getInstance().setValue(AndroidResourceGeneratedConfigure.KEY_PREFIX, prefix);
            }
        }

        if(!(psiFile instanceof PsiJavaFile) && !(psiFile instanceof XmlFile)){
            return;
        }

        final Document document = editor.getDocument();

        if(document == null){
            Messages.showErrorDialog(project, "Sorry, Unexcepted error occured", "Error");
            return;
        }

        if(mSelection == null) {
            mSelection = TextUtils.getSelectionByCaret(editor, psiFile);
        }

        if(TextUtils.isEmpty(mSelection.getText())) {
            return;
        }
        WriteObj writeObj = new WriteObj(psiFile, prefix, mSelection);

        String fileName = writeObj.getResourceName() + "s.xml";
        PsiFile[] files  = FilenameIndex.getFilesByName(project, fileName, GlobalSearchScope.moduleScope(ModuleUtil.findModuleForPsiElement(psiFile)));
        if(files == null || files.length == 0){
            Messages.showErrorDialog(project, "Can not find " + fileName + " in project", "Error");
            return;
        }

        PsiFile valueFile = null;

        for(PsiFile file : files){
            if(file.getParent().getName().equals((TextUtils.isEmpty(writeObj.getResourceNameSuffix()) ? "values" : ("values-" + writeObj.getResourceNameSuffix())))){
                valueFile = file;
                break;
            }
        }

        if(valueFile == null){
            return;
        }

        final PsiFile file = valueFile;

        if(file instanceof XmlFile){
            if(TextUtils.isNameInXml(((XmlFile) file).getRootTag(), writeObj.getValueName())){
                Messages.showInfoMessage(project, "name already exists", "Information");
                return;
            }
        }

        Object action = new WriteCommandAction(project, "Auto Add Resource", file) {
            @Override
            protected void run(@NotNull Result result) throws Throwable {

                if(file instanceof XmlFile) {

                    XmlTag tagFromText = XmlElementFactory.getInstance(project).createTagFromText(writeObj.getResourceWriteString());

                    XmlTag rootTag = ((XmlFile) file).getRootTag();
                    if(rootTag == null){
                        return;
                    }
                    PsiElement whiteSpace = PsiTreeUtil.getChildOfType(XmlElementFactory.getInstance(project).createTagFromText("<eddyyuan>\r\n</eddyyuan>"), XmlText.class);

                    rootTag.add(whiteSpace);
                    rootTag.add(tagFromText);

                    document.replaceString(mSelection.getStart(), mSelection.getEnd(), writeObj.getReplaceWriteString());

                    UndoUtil.markPsiFileForUndo(psiFile);
                    UndoUtil.markPsiFileForUndo(file);
                }
            }
        };
        ((WriteCommandAction)action).execute();

        editor.getSelectionModel().removeSelection();
    }



    @Override
    public void update(AnActionEvent e) {

        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);

        if(!(psiFile instanceof PsiJavaFile) && !(psiFile instanceof XmlFile)){
            e.getPresentation().setVisible(false);
            return;
        }

        if (editor == null || project == null || psiFile == null) {
            e.getPresentation().setVisible(false);
            return;
        }

        mSelection = TextUtils.getSelectionByCaret(editor, psiFile);

        if(mSelection == null || TextUtils.isEmpty(mSelection.getText())){
            e.getPresentation().setVisible(false);
            return;
        }

        String[] stringArr = mSelection.getText().split("/");
        if (stringArr != null && stringArr.length > 2) {
            if ((stringArr[0].indexOf("@") == 0 || stringArr[0].indexOf("\"@") == 0) && psiFile.getParent().getName().indexOf("values") != 0 ) {
                e.getPresentation().setVisible(true);
                return;
            }
        }
        e.getPresentation().setVisible(false);
    }

}
