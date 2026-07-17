package com.karstonn.alarm.ui.scheduleList;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.karstonn.alarm.AlarmRepo;
import com.karstonn.alarm.InMemoryAlarmRepo;
import com.karstonn.alarm.R;
import com.karstonn.alarm.ui.scheduleEdit.ScheduleEditFragment;
import com.karstonn.alarmsystem.proto.Alarm;
import com.karstonn.alarmsystem.proto.AlarmListing;

import java.util.ArrayList;
import java.util.List;

public class ScheduleListFragment extends Fragment {
    //Current Repo hardcoded - TODO: Make dynamic repo choice
    private static AlarmRepo repo = new InMemoryAlarmRepo();
    private ScheduleListViewModel scheduleListVm;
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

        scheduleListVm = new ViewModelProvider(this)
                .get(ScheduleListViewModel.class);

        scheduleListVm.setAlarmRepo(repo);
        scheduleListVm.fetchSchedulesFromRepo();

        setUpScheduleRecyclerView(view);

        //setup addAlarm
        view.findViewById(R.id.addScheduleButton).setOnClickListener(v ->
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new ScheduleEditFragment().newEmptyAlarm())
                        .addToBackStack(null)
                        .commit());

        //setup bottom box
        view.findViewById(R.id.serviceConfigButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Service config clicked", Toast.LENGTH_SHORT).show()
        );
    }

    private void setUpScheduleRecyclerView (
            @NonNull View view
    ){
        List<AlarmListing> schedules = scheduleListVm.getScheduleDisplayInfos();
        scheduleListAdapter = new ScheduleListAdapter(
                schedules,
                new ScheduleListAdapter.OnScheduleClickListener() {
                    @Override
                    public void onNameClick(AlarmListing schedule) {
                        getParentFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragmentContainer, ScheduleEditFragment.newInstance(repo.getAlarm(schedule.getId())))
                                .addToBackStack(null)
                                .commit();
                    }

                    @Override
                    public void onStatusClick(AlarmListing schedule) {
                        Toast.makeText(
                                requireContext(),
                                "Status clicked: " + schedule.getLabel(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        scheduleRecyclerView.setAdapter(scheduleListAdapter);
    }
}