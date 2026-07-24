package com.karstonn.alarm.ui.scheduleEdit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.karstonn.alarm.R;
import com.karstonn.alarm.ui.util.TimeFormatUtils;
import com.karstonn.alarmsystem.proto.AlarmPhase;

import java.util.List;

public class PhaseListAdapter extends RecyclerView.Adapter<PhaseListAdapter.PhaseViewHolder> {

    public interface OnPhaseClickListener {
        void onPhaseClick(AlarmPhase phase);
    }

    private final List<AlarmPhase> phases;
    private final OnPhaseClickListener clickListener;

    public PhaseListAdapter(List<AlarmPhase> phases, OnPhaseClickListener clickListener) {
        this.phases = phases;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public PhaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_phase, parent, false);

        return new PhaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhaseViewHolder holder, int position) {
        AlarmPhase phase = phases.get(position);

        holder.phaseTimeText.setText(TimeFormatUtils.formatTimeOfDay(phase.getTriggerTime()));
        holder.phaseNameText.setText(phase.getLabel());

        holder.itemView.setOnClickListener(v -> clickListener.onPhaseClick(phase));
    }

    @Override
    public int getItemCount() {
        return phases.size();
    }

    static public class PhaseViewHolder extends RecyclerView.ViewHolder {
        private final TextView phaseTimeText;
        private final TextView phaseNameText;

        public PhaseViewHolder(@NonNull View itemView) {
            super(itemView);

            phaseTimeText = itemView.findViewById(R.id.phaseTimeText);
            phaseNameText = itemView.findViewById(R.id.phaseNameText);
        }
    }
}