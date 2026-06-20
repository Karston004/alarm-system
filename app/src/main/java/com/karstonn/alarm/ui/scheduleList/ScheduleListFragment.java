package com.karstonn.alarm.ui.scheduleList;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.karstonn.alarm.R;

import java.util.ArrayList;
import java.util.List;

public class ScheduleListFragment extends Fragment {

    private RecyclerView scheduleRecyclerView;
    private ScheduleListAdapter scheduleListAdapter;

    public ScheduleListFragment() {
        // Required empty public constructor.
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_schedule_list, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        scheduleRecyclerView = view.findViewById(R.id.scheduleRecyclerView);

        List<DebugScheduleItem> schedules = createDebugSchedules();

        scheduleListAdapter = new ScheduleListAdapter(
                schedules,
                new ScheduleListAdapter.OnScheduleClickListener() {
                    @Override
                    public void onNameClick(DebugScheduleItem schedule) {
                        Toast.makeText(
                                requireContext(),
                                "Name clicked: " + schedule.getName(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onStatusClick(DebugScheduleItem schedule) {
                        Toast.makeText(
                                requireContext(),
                                "Status clicked: " + schedule.getName(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        scheduleRecyclerView.setAdapter(scheduleListAdapter);

        view.findViewById(R.id.addScheduleButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Add schedule clicked", Toast.LENGTH_SHORT).show()
        );

        view.findViewById(R.id.serviceConfigButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Service config clicked", Toast.LENGTH_SHORT).show()
        );
    }

    private List<DebugScheduleItem> createDebugSchedules() {
        List<DebugScheduleItem> schedules = new ArrayList<>();

        schedules.add(new DebugScheduleItem("Schedule Name 1", true));
        schedules.add(new DebugScheduleItem("Morning Alarm 2", true));
        schedules.add(new DebugScheduleItem("Schedule 3", false));
        schedules.add(new DebugScheduleItem("Schedule Name 4", true));
        schedules.add(new DebugScheduleItem("Schedule Name 5", true));
        schedules.add(new DebugScheduleItem("Schedule Name 6", true));
        schedules.add(new DebugScheduleItem("Schedule Name 7", true));
        schedules.add(new DebugScheduleItem("Schedule Name 8", true));
        schedules.add(new DebugScheduleItem("Schedule Name 9", true));
        schedules.add(new DebugScheduleItem("Schedule Name 10", true));
        schedules.add(new DebugScheduleItem("Schedule Name 11", true));
        schedules.add(new DebugScheduleItem("Schedule Name 12", true));
        schedules.add(new DebugScheduleItem("Schedule Name 13", true));


        return schedules;
    }
}