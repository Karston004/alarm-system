package com.karstonn.alarm.ui.actionEdit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.karstonn.alarm.R;

public class ActionEditFragment extends Fragment {

    public ActionEditFragment() {
        // Required empty public constructor.
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_action_edit, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupDeviceSpinner(view);
        setupActionTypeSpinner(view);

        view.findViewById(R.id.finalColorPreview).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Colour picker later", Toast.LENGTH_SHORT).show()
        );
    }

    private void setupDeviceSpinner(View view) {
        Spinner deviceSpinner = view.findViewById(R.id.deviceSpinner);

        String[] devices = {
                "Shelly Lightbulb",
                "Phone Speaker",
                "ESP32 Bedside Unit"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                devices
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceSpinner.setAdapter(adapter);
    }

    private void setupActionTypeSpinner(View view) {
        Spinner actionTypeSpinner = view.findViewById(R.id.actionTypeSpinner);

        String[] actionTypes = {
                "Turn on",
                "Turn off",
                "Set colour",
                "Play sound"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                actionTypes
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionTypeSpinner.setAdapter(adapter);
    }
}