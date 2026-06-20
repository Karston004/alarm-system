package com.karstonn.alarm.ui.phaseEdit;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.karstonn.alarm.R;
import com.karstonn.alarm.ui.actionEdit.ActionEditFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PhaseEditFragment extends Fragment {

    private RecyclerView actionRecyclerView;
    private ActionListAdapter actionListAdapter;

    private int selectedHour = 0;
    private int selectedMinute = 0;

    public PhaseEditFragment() {
        // Required empty public constructor.
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_phase_edit, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        actionRecyclerView = view.findViewById(R.id.actionRecyclerView);

        List<DebugActionItem> actions = createDebugActions();

        actionListAdapter = new ActionListAdapter(
                actions,
                action -> getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new ActionEditFragment())
                        .addToBackStack(null)
                        .commit()
        );

        actionRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        actionRecyclerView.setAdapter(actionListAdapter);

        TextView phaseTimeButton = view.findViewById(R.id.phaseTimeButton);
        phaseTimeButton.setOnClickListener(v -> showTimePicker(phaseTimeButton));

        view.findViewById(R.id.addActionButton).setOnClickListener(v ->
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new ActionEditFragment())
                        .addToBackStack(null)
                        .commit()
        );
    }

    private List<DebugActionItem> createDebugActions() {
        List<DebugActionItem> actions = new ArrayList<>();

        actions.add(new DebugActionItem("Action Name 1"));
        actions.add(new DebugActionItem("Action Name 2"));
        actions.add(new DebugActionItem("Actions Name 3"));

        return actions;
    }

    private void showTimePicker(TextView phaseTimeButton) {
        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;

                    String timeText = String.format(
                            Locale.UK,
                            "%02d:%02d",
                            selectedHour,
                            selectedMinute
                    );

                    phaseTimeButton.setText(timeText);
                },
                selectedHour,
                selectedMinute,
                true
        );

        dialog.show();
    }
}