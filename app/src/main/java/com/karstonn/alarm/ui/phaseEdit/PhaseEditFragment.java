package com.karstonn.alarm.ui.phaseEdit;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.karstonn.alarm.R;
import com.karstonn.alarm.ui.actionEdit.ActionEditFragment;
import com.karstonn.alarm.ui.scheduleEdit.ScheduleEditViewModel;
import com.karstonn.alarmsystem.proto.Action;
import com.karstonn.alarmsystem.proto.ActionId;
import com.karstonn.alarmsystem.proto.Alarm;
import com.karstonn.alarmsystem.proto.AlarmPhase;
import com.karstonn.alarmsystem.proto.AlarmPhaseId;
import com.karstonn.alarmsystem.proto.TimeOfDay;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PhaseEditFragment extends Fragment {
    private static final String ARG_PHASE_ID_BYTES = "phase_id_bytes";

    private RecyclerView actionRecyclerView;
    private ActionListAdapter actionListAdapter;
    private int selectedHour = 0;
    private int selectedMinute = 0;
    private ScheduleEditViewModel scheduleEditVm;
    private PhaseEditViewModel phaseEditVm;

    public PhaseEditFragment() {
        // Required empty public constructor.
    }

    //TODO - link actions

    public static PhaseEditFragment newInstance(AlarmPhaseId phase) {
        PhaseEditFragment fragment = new PhaseEditFragment();

        Bundle args = new Bundle();
        args.putByteArray(ARG_PHASE_ID_BYTES, phase.toByteArray());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        scheduleEditVm = new ViewModelProvider(requireActivity())
                .get(ScheduleEditViewModel.class);

        phaseEditVm = new ViewModelProvider(requireActivity())
                .get(PhaseEditViewModel.class);

        boolean openingNewPhase = savedInstanceState == null;
        boolean phaseVmWasRecreated = !phaseEditVm.hasDraftPhase();

        if (openingNewPhase || phaseVmWasRecreated) {
            String phaseId;

            try {
                byte[] bytes = requireArguments()
                        .getByteArray(ARG_PHASE_ID_BYTES);

                phaseId = AlarmPhaseId.parseFrom(bytes).getPhaseId();
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to parse phase ID",
                        e
                );
            }

            int index = findPhaseIndex(phaseId);
            phaseEditVm.loadPhase(
                    scheduleEditVm.getDraftAlarm().getAlarmPhases(index)
            );
        }
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

        scheduleEditVm = new ViewModelProvider(requireActivity())
                .get(ScheduleEditViewModel.class);
        phaseEditVm = new ViewModelProvider(requireActivity())
                .get(PhaseEditViewModel.class);

        actionRecyclerView = view.findViewById(R.id.actionRecyclerView);
        List<Action> actions = phaseEditVm.getDraftPhase().getActionsList();

        //TODO - Link to action edit
        actionListAdapter = new ActionListAdapter(
                actions,
                actionIndex -> getParentFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragmentContainer,
                                ActionEditFragment.newInstance(actionIndex)
                        )
                        .addToBackStack(null)
                        .commit()
        );

        actionRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        actionRecyclerView.setAdapter(actionListAdapter);

        // Setup TimePicker
        TextView phaseTimeButton = view.findViewById(R.id.phaseTimeButton);

        TimeOfDay initialTime = phaseEditVm.getDraftPhase().getTriggerTime();
        selectedHour = initialTime.getHour();
        selectedMinute = initialTime.getMin();

        updateTimeText(phaseTimeButton);

        phaseTimeButton.setOnClickListener(v ->
                showTimePicker(phaseTimeButton)
        );

        // Confirm Button
        view.findViewById(R.id.confirmPhaseButton).setOnClickListener(v-> {
            Toast.makeText(
                    requireContext(),
                    "Confirm clicked",
                    Toast.LENGTH_SHORT
            ).show();
            scheduleEditVm.updateAlarm(builder ->
                    builder.setAlarmPhases(findPhaseIndex(phaseEditVm.getDraftPhase().getPhaseId().getPhaseId()),phaseEditVm.getDraftPhase())
            );
            leavePhaseEditor();
        });

        // Cancel Button
        view.findViewById(R.id.cancelPhaseButton).setOnClickListener(v -> showCancelConfirmation());

        // Delete Button
        view.findViewById(R.id.deletePhaseButton).setOnClickListener(v -> showDeleteConfirmation());

        //TODO
        //Add ActionButton
        view.findViewById(R.id.addActionButton).setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, ActionEditFragment.newInstance(onNewAction()))
                    .addToBackStack(null)
                    .commit();
        });

        //Setup name field
        EditText nameInput = view.findViewById(R.id.phaseNameInput);
        nameInput.setText(phaseEditVm.getDraftPhase().getLabel());
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text,int start,int count,int after) {}
            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count
            ) {
                phaseEditVm.updatePhase(builder ->
                        builder.setLabel(text.toString())
                );
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

    }

    private int onNewAction(){
        String newActionId;
        do {
            newActionId = UUID.randomUUID().toString();
        } while (actionIdExists(newActionId));

        //Create new action
        Action newAction = Action.newBuilder()
                .setLabel("New Action")
                .setId(ActionId.newBuilder().setActionId(newActionId).build())
                .build();

        phaseEditVm.updatePhase(builder ->
                builder.addActions(newAction));
        return findActionIndex(newActionId);
    }
    private int findActionIndex(String actionId) {
        AlarmPhase draftPhase = phaseEditVm.getDraftPhase();
        for (int i = 0; i < draftPhase.getActionsCount(); i++) {
            Action action = draftPhase.getActions(i);

            if (action.getId().getActionId().equals(actionId)) {
                return i;
            }
        }

        throw new IllegalArgumentException(
                "No action found with ID: " + actionId
        );
    }

    private boolean actionIdExists(String id) {
        for (Action action : phaseEditVm.getDraftPhase().getActionsList()) {
            if (action.getId().getActionId().equals(id)) {
                return true;
            }
        }

        return false;
    }

    private int findPhaseIndex(String phaseId) {
        Alarm draftAlarm = scheduleEditVm.getDraftAlarm();
        for (int i = 0; i < draftAlarm.getAlarmPhasesCount(); i++) {
            AlarmPhase phase = draftAlarm.getAlarmPhases(i);

            if (phase.getPhaseId().getPhaseId().equals(phaseId)) {
                return i;
            }
        }

        throw new IllegalArgumentException(
                "No phase found with ID: " + phaseId
        );
    }
    private void showTimePicker(TextView phaseTimeButton) {
        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (timePicker, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;

                    TimeOfDay selectedTime = TimeOfDay.newBuilder()
                            .setHour(selectedHour)
                            .setMin(selectedMinute)
                            .build();

                    phaseEditVm.updatePhase(builder ->
                            builder.setTriggerTime(selectedTime)
                    );

                    updateTimeText(phaseTimeButton);
                },
                selectedHour,
                selectedMinute,
                true
        );

        dialog.show();
    }

    private void updateTimeText(TextView phaseTimeButton) {
        String timeText = String.format(
                Locale.UK,
                "%02d:%02d",
                selectedHour,
                selectedMinute
        );

        phaseTimeButton.setText(timeText);
    }

    private void showCancelConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Discard changes?")
                .setMessage("Any changes you have made will be lost.")
                .setNegativeButton("Keep editing", null)
                .setPositiveButton("Discard", (dialog, which) ->
                        leavePhaseEditor()
                )
                .show();
    }

    private void showDeleteConfirmation () {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete schedule?")
                .setMessage("The phase will be permanently lost.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    //Sorry
                            scheduleEditVm.updateAlarm(builder ->
                                    builder.removeAlarmPhases(
                                            findPhaseIndex(
                                                    phaseEditVm.getDraftPhase().getPhaseId().getPhaseId())));
                    leavePhaseEditor();
                        }
                )
                .show();
    }
    private void leavePhaseEditor() {
        getParentFragmentManager().popBackStack();
    }
}