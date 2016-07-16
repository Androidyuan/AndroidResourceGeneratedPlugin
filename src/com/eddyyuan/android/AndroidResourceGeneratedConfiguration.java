package com.eddyyuan.android;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Created by yuancong on 16/6/13.
 */
public class AndroidResourceGeneratedConfiguration {
    private JPanel mPanel;
    private JLabel mLable;
    private JTextField mPrefix;
    private JTextPane mBefore;
    private JTextPane mAfter;

    private String stringContext = ".getResources().getString(R.string.xxx);\r\n";
    private String dimenContext = ".getResources().getDemin(R.dimen.xxx);\r\n";
    private String colorContext = ".getResources().getColor(R.color.xxx);\r\n";

    private String mPrefixString = "";

    public AndroidResourceGeneratedConfiguration(String prefix){

        mPrefixString = prefix;

        mPrefix.setText(prefix);

        mAfter.setText(mPrefixString + stringContext + mPrefixString + dimenContext + mPrefixString + colorContext);

        mPrefix.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                mPrefixString = mPrefix.getText();
                mAfter.setText(mPrefixString + stringContext + mPrefixString + dimenContext + mPrefixString + colorContext);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                mPrefixString = mPrefix.getText();
                mAfter.setText(mPrefixString + stringContext + mPrefixString + dimenContext + mPrefixString + colorContext);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                mPrefixString = mPrefix.getText();
                mAfter.setText(mPrefixString + stringContext + mPrefixString + dimenContext + mPrefixString + colorContext);
            }
        });

    }

    public JComponent getComponent(){
        return mPanel;
    }

    public String getPrefix(){
        return mPrefixString;
    }

    public void reset(){
        mPrefixString = "";
        mPrefix.setText(mPrefixString);
    }
}
