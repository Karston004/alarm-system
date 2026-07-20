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

import com.karstonn.alarm.R;
import com.karstonn.alarm.ui.phaseEdit.PhaseEditFragment;
import com.karstonn.alarmsystem.proto.Alarm;
import com.karstonn.alarmsystem.proto.AlarmPhase;

import java.util.List;

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

        Bundle args = getArguments();

        scheduleEditVm = new ViewModelProvider(this)
                .get(ScheduleEditViewModel.class);

        if (args != null && args.containsKey(ARG_ALARM_BYTES)) {
            try {
                scheduleEditVm.loadAlarm(Alarm.parseFrom(args.getByteArray(ARG_ALARM_BYTES)));
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse Alarm from arguments", e);
            }
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

        //Setup Phase list
        List<AlarmPhase> phases = scheduleEditVm.getDraftAlarm().getAlarmPhasesList();
        phaseListAdapter = new PhaseListAdapter(
                phases,
                phase -> getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new PhaseEditFragment())
                        .addToBackStack(null)
                        .commit()
        );
        phaseRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        phaseRecyclerView.setAdapter(phaseListAdapter);

        //Setup Day Selection
        setupDayButtons(view);

        //Setup Looping Toggle
        view.findViewById(R.id.repeatButton).setOnClickListener(v ->{
                Toast.makeText(requireContext(), "Repeat pressed", Toast.LENGTH_SHORT).show();
                scheduleEditVm.updateAlarm(builder ->
                        builder.setIsRecurring(!builder.getIsRecurring()));

                }
        );

        //Setup Alarm Toggle
        view.findViewById(R.id.toggleScheduleButton).setOnClickListener(v ->{
                Toast.makeText(requireContext(), "Schedule toggled", Toast.LENGTH_SHORT).show();
                scheduleEditVm.updateAlarm(builder ->
                        builder.setIsEnabled(!builder.getIsEnabled()));
                }
        );

        //Setup Add Phase Button
        view.findViewById(R.id.addPhaseButton).setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Add phase clicked", Toast.LENGTH_SHORT).show();

                }
        );
    }

    //TODO link to builder
    private void setupDayButtons(View view) {
        int[] dayButtonIds = {
                R.id.mondayButton,
                R.id.tuesdayButton,
                R.id.wednesdayButton,
                R.id.thursdayButton,
                R.id.fridayButton,
                R.id.saturdayButton,
                R.id.sundayButton
        };

        for (int id : dayButtonIds) {
            ToggleButton button = view.findViewById(id);
            button.setChecked(true);
        }
    }
}