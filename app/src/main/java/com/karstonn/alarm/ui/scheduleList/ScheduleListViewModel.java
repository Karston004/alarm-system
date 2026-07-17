package com.karstonn.alarm.ui.scheduleList;

import androidx.lifecycle.ViewModel;

import com.karstonn.alarm.AlarmRepo;
import com.karstonn.alarmsystem.proto.AlarmListResponse;
import com.karstonn.alarmsystem.proto.AlarmListing;

import java.util.List;

public class ScheduleListViewModel extends ViewModel {
    public List<AlarmListing> scheduleDisplayInfos;
    private AlarmRepo repo;

    public void setAlarmRepo(AlarmRepo repo) {
        if (repo == null) {
            throw new IllegalStateException("Invalid Repo passed to ViewModel");
        }
        this.repo = repo;
    }
    public boolean fetchSchedulesFromRepo (){
        AlarmListResponse response = repo.listAlarms();
        for (AlarmListing listing : response.getAlarmsList())
            scheduleDisplayInfos.add(listing);

        return true;
    }

}


