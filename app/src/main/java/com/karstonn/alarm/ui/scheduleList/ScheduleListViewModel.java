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
        //TODO allow for async
        try {
            AlarmListResponse response = repo.listAlarms().get();
            scheduleDisplayInfos.clear();
            scheduleDisplayInfos.addAll(response.getAlarmsList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Alarm getAlarm(AlarmId alarmId){
        requireRepo();
        //TODO allow for async
        try {
            return repo.getAlarm(alarmId).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void requireRepo(){
        if (repo == null) {
            throw new IllegalStateException(
                    "AlarmRepo must be set before fetching schedules"
            );
        }
    }
}


