package com.eddyyuan.android;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Created by yuancong on 16/6/9.
 */
public class AndroidResourceGeneratedConfigure implements Configurable {

    public final static String KEY_PREFIX = "KEY_PREFIX";

    private String mPrefix = "";

    private AndroidResourceGeneratedConfiguration configuration = null;

    private PropertiesComponent mPersistent = null;

    private boolean mIsModified = false;

    public AndroidResourceGeneratedConfigure() {
        mPersistent = PropertiesComponent.getInstance();
        if(mPersistent != null){
            mPrefix = mPersistent.getValue(KEY_PREFIX, "");
        }
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Generate Android Resource";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if(configuration == null){
            configuration = new AndroidResourceGeneratedConfiguration(mPrefix);
        }
        return configuration.getComponent();
    }

    @Override
    public boolean isModified() {
        if(configuration == null){
            return false;
        }
        mIsModified = !mPrefix.contentEquals(configuration.getPrefix());
        return mIsModified;
    }

    @Override
    public void apply() throws ConfigurationException {
        if(configuration != null){
            mPrefix = configuration.getPrefix();
            if(mPersistent != null) {
                mPersistent.setValue(KEY_PREFIX, mPrefix);
            }
        }
    }

    @Override
    public void reset() {
        if(mIsModified) {
            if (configuration != null) {
                mPrefix = "";
                configuration.reset();
            }
        }
    }

    @Override
    public void disposeUIResources() {
        configuration = null;
    }

}
