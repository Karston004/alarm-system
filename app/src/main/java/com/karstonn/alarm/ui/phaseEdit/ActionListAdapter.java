package com.karstonn.alarm.ui.phaseEdit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.karstonn.alarm.R;
import com.karstonn.alarmsystem.proto.Action;

import java.util.List;

public class ActionListAdapter
        extends RecyclerView.Adapter<ActionListAdapter.ActionViewHolder> {

    public interface OnActionClickListener {
        void onActionClick(int actionIndex);
    }

    private final List<Action> actions;
    private final OnActionClickListener clickListener;

    public ActionListAdapter(
            List<Action> actions,
            OnActionClickListener clickListener
    ) {
        this.actions = actions;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ActionViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_action, parent, false);

        return new ActionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ActionViewHolder holder,
            int position
    ) {
        Action action = actions.get(position);

        holder.actionNameText.setText(action.getLabel());

        holder.itemView.setOnClickListener(v -> {
            int clickedPosition = holder.getBindingAdapterPosition();

            if (clickedPosition != RecyclerView.NO_POSITION) {
                clickListener.onActionClick(clickedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return actions.size();
    }

    public static class ActionViewHolder extends RecyclerView.ViewHolder {
        private final TextView actionNameText;

        public ActionViewHolder(@NonNull View itemView) {
            super(itemView);

            actionNameText = itemView.findViewById(
                    R.id.actionNameText
            );
        }
    }
}