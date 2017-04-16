package com.eddyyuan.android.util;

import com.eddyyuan.android.entity.Selection;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.ElementType;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;

/**
 * Created by eddyyuan on 2017/4/16.
 */
public class TextUtils {

    public static Selection getSelectionByCaret(Editor editor, PsiFile psiFile){

        PsiElement sectionElement = psiFile.findElementAt(editor.getCaretModel().getOffset());
        sectionElement = adjustSectionElement(psiFile, sectionElement);

        if(sectionElement == null){
            return null;
        }

        String elementText = sectionElement.getText();

        int start = 0, end = 0;
        if (!isEmpty(elementText)) {
            start = sectionElement.getTextOffset();
            if(sectionElement instanceof XmlAttributeValue){
                start -= 1;
            }
            end = start + elementText.length();
        }else{
            return null;
        }

        if(elementText.startsWith("\"")){
            if(elementText.endsWith("\"")){
                if(elementText.length() <= 1){
                    return null;
                }
                elementText = elementText.substring(1, elementText.length() - 1);
                if(psiFile instanceof XmlFile){
                    start += 1;
                    end -= 1;
                }
            }else{
                elementText = elementText.substring(1);
                if(psiFile instanceof XmlFile){
                    start += 1;
                }else{
                    end += 1;
                }
            }
        }else{
            if(elementText.endsWith("\"")){
                elementText = elementText.substring(0, elementText.length() - 1);
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

        Selection selection = new Selection();
        selection.setStart(start);
        selection.setEnd(end);
        selection.setText(elementText);

        return selection;
    }

    public static boolean isEmpty(String str){
        return str == null || str.length() == 0;
    }

    public static boolean isNameInXml(XmlTag rootTag, String name){
        if(rootTag == null){
            return false;
        }
        XmlTag[] xmlTags = rootTag.getSubTags();
        for(XmlTag xmlTag : xmlTags){
            if(xmlTag.getAttribute("name").getValue().equals(name)){
                return true;
            }
        }
        return false;
    }


    private static PsiElement adjustSectionElement(PsiFile file, PsiElement element){
        PsiElement psiElement = element;
        if(file instanceof PsiJavaFile) {
            if (psiElement instanceof PsiJavaToken) {
                if (((PsiJavaToken) psiElement).getTokenType() == ElementType.SEMICOLON) {
                    psiElement = psiElement.getPrevSibling();
                    if (psiElement instanceof PsiLiteralExpression) {
                        return psiElement;
                    }
                } else if (((PsiJavaToken) psiElement).getTokenType() == ElementType.STRING_LITERAL) {
                    return psiElement;
                }
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
}
