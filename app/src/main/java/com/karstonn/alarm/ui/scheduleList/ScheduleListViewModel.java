package com.karstonn.alarm.ui.scheduleList;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.karstonn.alarm.AlarmRepo;
import com.karstonn.alarmsystem.proto.Alarm;
import com.karstonn.alarmsystem.proto.AlarmId;
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
        requireRepo(); //Can Throw
        AlarmListResponse response = repo.listAlarms();
        scheduleDisplayInfos.clear();
        scheduleDisplayInfos.addAll(response.getAlarmsList());
    }

    public Alarm getAlarm(AlarmId alarmId){
        return repo.getAlarm(alarmId);
    }

    private void requireRepo(){
        if (repo == null) {
            throw new IllegalStateException(
                    "AlarmRepo must be set before fetching schedules"
            );
        }
    }
}


