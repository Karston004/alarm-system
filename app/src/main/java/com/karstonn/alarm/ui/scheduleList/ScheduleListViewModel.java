package com.karstonn.alarm.ui.scheduleList;

import androidx.lifecycle.ViewModel;

import com.karstonn.alarm.AlarmRepo;
import com.karstonn.alarmsystem.proto.AlarmListResponse;
import com.karstonn.alarmsystem.proto.AlarmListing;

import java.util.ArrayList;
import java.util.List;

public class ScheduleListViewModel extends ViewModel {
    private final List<AlarmListing> scheduleDisplayInfos = new ArrayList<>();
    private AlarmRepo repo;

    public void setAlarmRepo(AlarmRepo repo) {
        if (repo == null) {
            throw new IllegalArgumentException("AlarmRepo cannot be null");
        }

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

        scheduleDisplayInfos.clear();
        scheduleDisplayInfos.addAll(response.getAlarmsList());
    }
}


