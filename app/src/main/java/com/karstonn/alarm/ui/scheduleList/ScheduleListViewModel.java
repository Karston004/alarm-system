package com.karstonn.alarm.ui.scheduleList;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.karstonn.alarm.AlarmRepo;
import com.karstonn.alarmsystem.proto.AlarmListResponse;
import com.karstonn.alarmsystem.proto.AlarmListing;

import java.util.ArrayList;
import java.util.List;

public class ScheduleListViewModel extends ViewModel {
    private final List<AlarmListing> scheduleDisplayInfos = new ArrayList<>();
    private AlarmRepo repo;

    public void setAlarmRepo(
            @NonNull AlarmRepo repo
    ) {
        this.repo = repo;
    }

    public List<AlarmListing> getScheduleDisplayInfos() {
        return scheduleDisplayInfos;
    }

    public void fetchSchedulesFromRepo() {
        if (repo == null) {
            throw new IllegalStateException(
                    "AlarmRepo must be set before fetching schedules"
            );
        }

        AlarmListResponse response = repo.listAlarms();
        //TODO - keep local cache for optimistic updates
        scheduleDisplayInfos.clear();
        scheduleDisplayInfos.addAll(response.getAlarmsList());
    }
}


