package com.eddyyuan.android.entity;

import com.eddyyuan.android.util.TextUtils;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.xml.XmlFile;

/**
 * Created by eddyyuan on 2017/4/16.
 */
public class WriteObj {
    private String resourceWriteString;
    private String replaceWriteString;
    private String resourceName;
    private String resourceNameSuffix;
    private String valueName;

    public WriteObj(PsiFile psiFile, String prefix, Selection selection) {
        if(TextUtils.isEmpty(selection.getText())){
            return;
        }
        int first = selection.getText().indexOf("/");
        int second = selection.getText().indexOf("/", first + 1);
        if(first == -1 || second == -1){
            return;
        }
        String type = selection.getText().substring(1, first);
        valueName = selection.getText().substring(first + 1, second);
        String vaule = selection.getText().substring(second + 1);
        StringBuilder replace = new StringBuilder();
        int index;
        if ((index = type.indexOf("-")) == -1) {
            resourceName = type;
        } else {
            resourceName = type.substring(0, index);
            resourceNameSuffix = type.substring(index + 1);
        }
        if (psiFile instanceof PsiJavaFile) {
            replace.append(prefix)
                    .append(".getResources().get")
                    .append(resourceName.substring(0, 1).toUpperCase())
                    .append(resourceName.substring(1).toLowerCase());
            if (resourceName.contentEquals("dimen")) {
                replace.append("sion");
            }
            replace.append("(R.")
                    .append(resourceName.toLowerCase()).append(".")
                    .append(valueName)
                    .append(")");
        } else if (psiFile instanceof XmlFile) {
            replace.append("@")
                    .append(resourceName.toLowerCase())
                    .append("/")
                    .append(valueName);
        }
        replaceWriteString = replace.toString();
        resourceWriteString = new StringBuilder().append("<")
                .append(resourceName)
                .append(" name=\"")
                .append(valueName)
                .append("\">")
                .append(vaule)
                .append("</")
                .append(resourceName)
                .append(">").toString();
    }

    public String getResourceWriteString() {
        return resourceWriteString;
    }

    public String getReplaceWriteString() {
        return replaceWriteString;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getResourceNameSuffix() {
        return resourceNameSuffix;
    }

    public String getValueName() {
        return valueName;
    }
}