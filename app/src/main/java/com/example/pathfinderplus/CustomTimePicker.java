package com.example.pathfinderplus;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TimePicker;

import java.lang.reflect.Field;

public class CustomTimePicker extends TimePicker {

    public CustomTimePicker(Context context) {
        super(context);
        hideTitle();
    }

    public CustomTimePicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        hideTitle();
    }

    public CustomTimePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        hideTitle();
    }

    private void hideTitle() {
        try {
            Class<?> ll = TimePicker.class;
            Field llImpl = ll.getDeclaredField("mDelegate");
            llImpl.setAccessible(false);
            Class<?> delegateClass = llImpl.get(this).getClass();
            Field textInputPicker = delegateClass.getDeclaredField("mTextInputPicker");
            textInputPicker.setAccessible(false);
            Object textInputPickerObj = textInputPicker.get(llImpl.get(this));
            Class<?> textInputPickerClass = textInputPicker.getType();
            Field title = textInputPickerClass.getDeclaredField("mTimeHeaderTextDisplay");
            title.setAccessible(false);
            Object titleObj = title.get(textInputPickerObj);
            titleObj.getClass().getMethod("setHeight", int.class).invoke(titleObj, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
