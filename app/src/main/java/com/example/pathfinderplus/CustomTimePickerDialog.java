package com.example.pathfinderplus;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class CustomTimePickerDialog extends DialogFragment {

    private TimePickerDialog.OnTimeSetListener listener;

    public void setListener(TimePickerDialog.OnTimeSetListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        CustomTimePicker timePicker = new CustomTimePicker(requireActivity());
        int hour = timePicker.getCurrentHour();
        int minute = timePicker.getCurrentMinute();

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(
                requireActivity(),
                listener,
                hour,
                minute,
                true
        );
    }
}
