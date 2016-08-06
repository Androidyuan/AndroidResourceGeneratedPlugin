package com.eddyyuan.android;

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
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.ElementType;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.*;
import org.apache.http.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import static com.intellij.xml.util.documentation.XmlDocumentationProvider.findPreviousComment;

/**
 * Created by yuancong on 16/6/9.
 */
public class AndroidResourceGeneratedAction extends AnAction {

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
            prefix = Messages.showInputDialog(project, "Configure Replace Prefix: \r\n\r\nExample : if you set \"getApplication()\"\r\nBefore : .getResources().getString(R.string.xxx)\r\nAfter : getApplication().getResources().getString(R.string.xxx)", "Configurateion", null);
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

        SelectionModel selectionModel = editor.getSelectionModel();

        String selectedText = selectionModel.getSelectedText();

        int start = selectionModel.getSelectionStart();
        int end = selectionModel.getSelectionEnd();

        String string = selectedText;

        PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());

        element = adjustPsiElement(psiFile, element);

        if(element == null){
            return;
        }

        String elementText = element.getText();

        if (!TextUtils.isEmpty(elementText)) {
            if (TextUtils.isEmpty(selectedText) || (elementText.length() > selectedText.length() && elementText.contains(selectedText))) {
                start = element.getTextOffset();
                if(element instanceof XmlAttributeValue){
                    start -= 1;
                }
                end = start + elementText.length();
                string = elementText;
            }
        }

        if(TextUtils.isEmpty(string)){
            return;
        }

        if(string.startsWith("\"")){
            if(string.endsWith("\"")){
                if(string.length() == 1){
                    return;
                }
                string = string.substring(1, string.length() - 1);
                if(psiFile instanceof XmlFile){
                    start += 1;
                    end -= 1;
                }
            }else{
                string = string.substring(1);
                if(psiFile instanceof XmlFile){
                    start += 1;
                }else{
                    end += 1;
                }
            }
        }else{
            if(string.endsWith("\"")){
                string = string.substring(0, string.length() - 1);
                if(psiFile instanceof XmlFile){
                    end -= 1;
                }else{
                    start -= 1;
                }
            }else{
                if(psiFile instanceof PsiJavaFile){
                    start -= 1;
                    end += 1;
                }
            }
        }

        final int realStart = start;
        final int realEnd = end;

        String suffix = "";


        final String[] stringArr = string.split("/");
        final StringBuilder replace = new StringBuilder();
        final StringBuilder stringBuilder = new StringBuilder();
        final String[] resFileArr = stringArr[0].split("-");
        if(resFileArr.length > 1){
            int i = 1;
            while (i < resFileArr.length){
                suffix += resFileArr[i];
                i++;
                if(i < resFileArr.length){
                    suffix +="-";
                }
            }
        }
        if(stringArr.length > 2 && resFileArr[0].startsWith("@")){
            if(psiFile instanceof PsiJavaFile) {
                replace.append(prefix);
                replace.append(".getResources().get");
                replace.append(resFileArr[0].substring(1, 2).toUpperCase());
                replace.append(resFileArr[0].substring(2).toLowerCase());
                if(resFileArr[0].contentEquals("@dimen")){
                    replace.append("sion");
                }
                replace.append("(R.");
                replace.append(resFileArr[0].substring(1).toLowerCase());
                replace.append(".");
                replace.append(stringArr[1]);
                replace.append(")");
            }else if(psiFile instanceof XmlFile){
                replace.append(resFileArr[0].toLowerCase());
                replace.append("/");
                replace.append(stringArr[1]);
            }
            int i = 2;
            while(i < stringArr.length){
                stringBuilder.append(stringArr[i]);
                i++;
                if(i < stringArr.length){
                    stringBuilder.append("/");
                }
            }
            String fileName = resFileArr[0].substring(1) + "s.xml";
            PsiFile[] files  = FilenameIndex.getFilesByName(project, fileName, GlobalSearchScope.projectScope(project));
            if(files == null || files.length == 0){
                Messages.showErrorDialog(project, "Can not find " + fileName + " in project", "Error");
                return;
            }

            PsiFile valueFile = null;

            for(PsiFile file : files){
                if(file.getParent().getName().contentEquals((TextUtils.isEmpty(suffix) ? "values" : ("values-" + suffix)))){
                    valueFile = file;
                    break;
                }
            }

            if(valueFile == null){
                return;
            }

            final PsiFile file = valueFile;

            if(file instanceof XmlFile){
                if(checkName(((XmlFile) file).getRootTag(), stringArr[1])){
                    Messages.showInfoMessage(project, "name already exists", "Information");
                    return;
                }
            }

            Object action = new WriteCommandAction(project, "Auto Add Resource", file) {
                @Override
                protected void run(@NotNull Result result) throws Throwable {

                    if(file instanceof XmlFile) {

                        String tag = resFileArr[0].substring(1);
                        StringBuilder builder = new StringBuilder();
                        builder.append("<");
                        builder.append(tag);
                        builder.append(" name=\"");
                        builder.append(stringArr[1]);
                        builder.append("\">");
                        builder.append(stringBuilder.toString());
                        builder.append("</");
                        builder.append(tag);
                        builder.append(">");
                        XmlTag tagFromText = XmlElementFactory.getInstance(project).createTagFromText(builder);

                        XmlTag rootTag = ((XmlFile) file).getRootTag();
                        if(rootTag == null){
                            return;
                        }
                        PsiElement whiteSpace = PsiTreeUtil.getChildOfType(XmlElementFactory.getInstance(project).createTagFromText("<eddyyuan>\n</eddyyuan>"), XmlText.class);
                        PsiElement[] children = rootTag.getChildren();
                        PsiElement comment = null;
                        if(children != null && children.length >= 6) {
                            comment = findCommentByText(rootTag.getChildren()[rootTag.getChildren().length - 4], psiFile.getName());
                        }
                        if(comment == null){
                            StringBuilder commentBuilder = new StringBuilder();
                            commentBuilder.append("<eddyyuan><!-- ");
                            commentBuilder.append(psiFile.getName());
                            commentBuilder.append(" --></eddyyuan>");

                            PsiElement commentElement = PsiTreeUtil.getChildOfType(XmlElementFactory.getInstance(project).createTagFromText(commentBuilder), XmlComment.class);

                            rootTag.add(whiteSpace);
                            rootTag.add(commentElement);
                            rootTag.add(whiteSpace);
                            rootTag.add(tagFromText);
                            rootTag.add(whiteSpace);
                            rootTag.add(commentElement);
                            rootTag.add(whiteSpace);
                        }else{
                            if(comment.getPrevSibling() instanceof XmlText) {
                                rootTag.addBefore(tagFromText, comment.getPrevSibling());
                            }else{
                                rootTag.addBefore(whiteSpace, comment);
                                rootTag.addBefore(tagFromText, comment.getPrevSibling());
                            }
                        }

                        document.replaceString(realStart, realEnd, replace);

                        UndoUtil.markPsiFileForUndo(psiFile);
                        UndoUtil.markPsiFileForUndo(file);
                    }
                }
            };
            ((WriteCommandAction)action).execute();

            selectionModel.removeSelection();
        }
    }

    PsiElement findCommentByText(PsiElement lastElement, String string){
        PsiElement comment = findPreviousComment(lastElement);
        if(comment != null){
            PsiElement[] elements = comment.getChildren();
            if(elements != null){
                if(elements.length > 2) {
                    if (elements[1].getText().trim().contentEquals(string)) {
                        return comment;
                    }
                }
            }
        }
        return null;
    }

    boolean checkName(XmlTag rootTag, String name){
        if(rootTag == null){
            return false;
        }
        XmlTag[] xmlTags = rootTag.getSubTags();
        for(XmlTag xmlTag : xmlTags){
            if(xmlTag.getAttribute("name").getValue().contentEquals(name)){
                return true;
            }
        }
        return false;
    }

    public PsiElement adjustPsiElement(PsiFile file, PsiElement element){
        PsiElement psiElement = element;
        String elementText = null;
        if(file instanceof PsiJavaFile) {
            if (psiElement instanceof PsiJavaToken) {
                if (((PsiJavaToken) psiElement).getTokenType() == ElementType.SEMICOLON) {
                    psiElement = psiElement.getPrevSibling();
                } else if (((PsiJavaToken) psiElement).getTokenType() == ElementType.STRING_LITERAL) {
                    psiElement = psiElement.getParent();
                }
            }
            if (psiElement instanceof PsiLiteralExpression) {
                return psiElement;
            }
        }else if(file instanceof XmlFile) {
            if(psiElement instanceof XmlToken){
                psiElement = psiElement.getParent();
                if(psiElement instanceof XmlAttributeValue) {
                    return psiElement;
                }
            }
        }
        return null;
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

        PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());

        element = adjustPsiElement(psiFile, element);

        if(element == null){
            e.getPresentation().setVisible(false);
            return;
        }

        String elementText = element.getText();

        if (!TextUtils.isEmpty(elementText)) {
            String[] stringArr = elementText.split("/");
            if (stringArr != null && stringArr.length > 2) {
                if ((stringArr[0].indexOf("@") == 0 || stringArr[0].indexOf("\"@") == 0) && !psiFile.getName().contentEquals(stringArr[0].substring(1) + "s.xml")) {
                    e.getPresentation().setVisible(true);
                    return;
                }
            }
        }
        e.getPresentation().setVisible(false);
    }
}
