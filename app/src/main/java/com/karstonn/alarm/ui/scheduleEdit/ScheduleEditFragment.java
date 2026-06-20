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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.karstonn.alarm.R;
import com.karstonn.alarm.ui.phaseEdit.PhaseEditFragment;

import java.util.ArrayList;
import java.util.List;

public class ScheduleEditFragment extends Fragment {

    private RecyclerView phaseRecyclerView;
    private PhaseListAdapter phaseListAdapter;

    public ScheduleEditFragment() {
        // Required empty public constructor.
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

        List<DebugPhaseItem> phases = createDebugPhases();

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

        setupDebugDayButtons(view);

        view.findViewById(R.id.repeatButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Repeat pressed", Toast.LENGTH_SHORT).show()
        );

        view.findViewById(R.id.toggleScheduleButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Schedule toggled", Toast.LENGTH_SHORT).show()
        );

        view.findViewById(R.id.addPhaseButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Add phase clicked", Toast.LENGTH_SHORT).show()
        );
    }

    private List<DebugPhaseItem> createDebugPhases() {
        List<DebugPhaseItem> phases = new ArrayList<>();

        phases.add(new DebugPhaseItem("07:00", "Phase Name 1"));
        phases.add(new DebugPhaseItem("07:10", "Phase Name 2"));
        phases.add(new DebugPhaseItem("07:30", "Phase Name 3"));

        return phases;
    }

    private void setupDebugDayButtons(View view) {
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