package com.karstonn.alarm.ui.util;

import android.text.TextWatcher;

public abstract class SimpleTextWatcher implements TextWatcher {
    @Override
    public void beforeTextChanged(
            CharSequence text,
            int start,
            int count,
            int after
    ) {
    }

    @Override
    public void onTextChanged(
            CharSequence text,
            int start,
            int before,
            int count
    ) {
    }
}
