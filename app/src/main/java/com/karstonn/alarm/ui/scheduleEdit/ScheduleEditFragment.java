package com.karstonn.alarm.ui.scheduleEdit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.google.android.material.button.MaterialButton;
import com.karstonn.alarm.AlarmApplication;
import com.karstonn.alarm.R;
import com.karstonn.alarm.ui.phaseEdit.PhaseEditFragment;
import com.karstonn.alarmsystem.proto.Alarm;
import com.karstonn.alarmsystem.proto.AlarmPhase;
import com.karstonn.alarmsystem.proto.AlarmPhaseId;
import com.karstonn.alarmsystem.proto.DayOfWeek;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScheduleEditFragment extends Fragment {

    private static final String ARG_ALARM_BYTES = "alarm_bytes";
    private ScheduleEditViewModel scheduleEditVm;
    private RecyclerView phaseRecyclerView;
    private PhaseListAdapter phaseListAdapter;

    public ScheduleEditFragment(){
        // Required empty public constructor
    }

    public static ScheduleEditFragment newInstance(Alarm alarm) {
        ScheduleEditFragment fragment = new ScheduleEditFragment();

        Bundle args = new Bundle();
        args.putByteArray(ARG_ALARM_BYTES, alarm.toByteArray());
        fragment.setArguments(args);
        return fragment;
    }

    public static ScheduleEditFragment newEmptyAlarm() {
        Alarm newAlarm = Alarm.newBuilder()
                .setLabel("New alarm")
                .build();

        return newInstance(newAlarm);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        scheduleEditVm = new ViewModelProvider(requireActivity())
                .get(ScheduleEditViewModel.class);

        AlarmApplication application =
                (AlarmApplication) requireActivity().getApplication();
        scheduleEditVm.setAlarmRepo(application.getAlarmRepo());

        boolean openingNewEditor = savedInstanceState == null;
        boolean viewModelWasRecreated = !scheduleEditVm.hasDraftAlarm();

        if (openingNewEditor || viewModelWasRecreated) {
            loadAlarmFromArguments();
        }
    }

    private void loadAlarmFromArguments() {
        Bundle args = requireArguments();

        try {
            Alarm alarm = Alarm.parseFrom(
                    args.getByteArray(ARG_ALARM_BYTES)
            );

            // This still deliberately overrides the current draft.
            scheduleEditVm.loadAlarm(alarm);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to parse Alarm from arguments",
                    e
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
        return inflater.inflate(R.layout.fragment_schedule_edit, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        phaseRecyclerView = view.findViewById(R.id.phaseRecyclerView);

        // Setup Phase list
        List<AlarmPhase> phases =
                scheduleEditVm.getDraftAlarm().getAlarmPhasesList();

        phaseListAdapter = new PhaseListAdapter(
                phases,
                phase -> getParentFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragmentContainer,
                                PhaseEditFragment.newInstance(phase.getPhaseId())
                        )
                        .addToBackStack(null)
                        .commit()
        );

        phaseRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
        phaseRecyclerView.setAdapter(phaseListAdapter);

        // Setup Day Selection
        setupDayButtons(view, scheduleEditVm);

        // Setup Looping Toggle
        ((MaterialButton)view.findViewById(R.id.repeatButton)).setChecked(scheduleEditVm.getDraftAlarm().getIsRecurring());
        view.findViewById(R.id.repeatButton).setOnClickListener(v -> scheduleEditVm.updateAlarm(builder ->
                builder.setIsRecurring(!builder.getIsRecurring())
        ));

        // Setup Alarm Toggle
        ((MaterialButton)view.findViewById(R.id.toggleScheduleButton)).setChecked(scheduleEditVm.getDraftAlarm().getIsEnabled());
        view.findViewById(R.id.toggleScheduleButton).setOnClickListener(v -> scheduleEditVm.updateAlarm(builder ->
                builder.setIsEnabled(!builder.getIsEnabled())
        ));

        // Setup Add Phase Button
        view.findViewById(R.id.addPhaseButton).setOnClickListener(v ->
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, PhaseEditFragment.newInstance(onNewPhase()))
                    .addToBackStack(null)
                    .commit()
        );

        // Setup Name field
        EditText nameInput = view.findViewById(R.id.scheduleNameInput);
        nameInput.setText(scheduleEditVm.getDraftAlarm().getLabel());
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text,int start,int count,int after) {}
            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count
            ) {
                scheduleEditVm.updateAlarm(builder ->
                        builder.setLabel(text.toString())
                );
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        //Confirm Alarm Changes
        view.findViewById(R.id.confirmButton).setOnClickListener(v-> {
            Toast.makeText(
                    requireContext(),
                    "Confirm clicked",
                    Toast.LENGTH_SHORT
            ).show();
            scheduleEditVm.saveAlarm();
            leaveScheduleEditor();
        });

        //Cancel Alarm Changes
        view.findViewById(R.id.cancelButton).setOnClickListener(v-> showCancelConfirmation());

        //Delete Alarm
        view.findViewById(R.id.deleteButton).setOnClickListener(v -> showDeleteConfirmation());

        // Handle Android system Back button / gesture
        OnBackPressedCallback backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showCancelConfirmation();
            }
        };
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), backCallback);
    }

    private void setupDayButtons(View view, ScheduleEditViewModel scheduleEditVm) {
        int[] dayButtonIds = {
                R.id.mondayButton,
                R.id.tuesdayButton,
                R.id.wednesdayButton,
                R.id.thursdayButton,
                R.id.fridayButton,
                R.id.saturdayButton,
                R.id.sundayButton
        };

        DayOfWeek[] days = {
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
        };

        for (int i = 0; i < dayButtonIds.length; i++) {
            ToggleButton button = view.findViewById(dayButtonIds[i]);
            DayOfWeek day = days[i];

            //Setup Loaded buttons
            button.setChecked(
                    scheduleEditVm.getDraftAlarm()
                            .getDaysList()
                            .contains(day)
            );

            //Setup Listener
            button.setOnClickListener(v ->
                    scheduleEditVm.updateAlarm(builder -> {
                        List<DayOfWeek> selectedDays =
                                new ArrayList<>(builder.getDaysList());

                        if (button.isChecked()) {
                            if (!selectedDays.contains(day)) {
                                selectedDays.add(day);
                            }
                        } else {
                            selectedDays.remove(day);
                        }

                        builder.clearDays();
                        builder.addAllDays(selectedDays);
                    })
            );
        }
    }

    private void showCancelConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Discard changes?")
                .setMessage("Any changes you have made will be lost.")
                .setNegativeButton("Keep editing", null)
                .setPositiveButton("Discard", (dialog, which) ->
                        leaveScheduleEditor()
                )
                .show();
    }

    private void showDeleteConfirmation () {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete schedule?")
                .setMessage("The schedule will be permanently lost.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                        scheduleEditVm.deleteAlarm();
                        leaveScheduleEditor();
                        }
                )
                .show();
    }

    private AlarmPhaseId onNewPhase() {
        String newPhaseId;
        do {
            newPhaseId = UUID.randomUUID().toString();
        } while (phaseIdExists(newPhaseId));

        AlarmPhase newPhase = AlarmPhase.newBuilder()
                .setLabel("new phase")
                .setPhaseId(AlarmPhaseId.newBuilder()
                        .setPhaseId(newPhaseId)
                        .build())
                .build();
        scheduleEditVm.updateAlarm(builder ->
            builder.addAlarmPhases(newPhase));
        return newPhase.getPhaseId();
    }

    private boolean phaseIdExists(String phaseId) {
        for (AlarmPhase phase : scheduleEditVm.getDraftAlarm().getAlarmPhasesList()) {
            if (phase.getPhaseId().getPhaseId().equals(phaseId)) {
                return true;
            }
        }

        return false;
    }
    private void leaveScheduleEditor() {
        getParentFragmentManager().popBackStack();
    }
}