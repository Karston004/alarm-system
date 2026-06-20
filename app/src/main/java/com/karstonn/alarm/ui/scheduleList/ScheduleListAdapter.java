package com.karstonn.alarm.ui.scheduleList;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.karstonn.alarm.R;

import java.util.List;

public class ScheduleListAdapter extends RecyclerView.Adapter<ScheduleListAdapter.ScheduleViewHolder> {

    public interface OnScheduleClickListener {
        void onNameClick (DebugScheduleItem schedule);
        void onStatusClick(DebugScheduleItem schedule);

    }

    private final List<DebugScheduleItem> schedules;
    private final OnScheduleClickListener clickListener;

    public ScheduleListAdapter(
            List<DebugScheduleItem> schedules,
            OnScheduleClickListener clickListener
    ) {
        this.schedules = schedules;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);

        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ScheduleViewHolder holder,
            int position
    ) {
        DebugScheduleItem schedule = schedules.get(position);

        holder.scheduleNameText.setText(schedule.getName());

        int statusDrawable = schedule.isEnabled()
                ? R.drawable.status_green
                : R.drawable.status_red;

        holder.scheduleStatusCircle.setBackgroundResource(statusDrawable);

        holder.scheduleNameText.setOnClickListener(v ->
                clickListener.onNameClick(schedule)
        );

        holder.scheduleStatusCircle.setOnClickListener(v ->
                clickListener.onStatusClick(schedule)
        );
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    static public class ScheduleViewHolder extends RecyclerView.ViewHolder {
        private final TextView scheduleNameText;
        private final View scheduleStatusCircle;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);

            scheduleNameText = itemView.findViewById(R.id.scheduleNameText);
            scheduleStatusCircle = itemView.findViewById(R.id.scheduleStatusCircle);
        }
    }
}