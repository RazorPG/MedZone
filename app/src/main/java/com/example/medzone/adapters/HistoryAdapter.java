package com.example.medzone.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medzone.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class HistoryAdapter extends ListAdapter<com.example.medzone.model.HistoryItem, HistoryAdapter.HistoryViewHolder> {

    private final Context context;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public HistoryAdapter(Context context) {
        super(DIFF_CALLBACK);
        this.context = context;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        com.example.medzone.model.HistoryItem item = getItem(position);
        if (item == null) return;
        if (item.timestamp != null) {
            holder.tvDate.setText(dateFormat.format(item.timestamp));
            holder.tvTime.setText(timeFormat.format(item.timestamp));
        } else {
            holder.tvDate.setText("");
            holder.tvTime.setText("");
        }
        holder.tvKeluhanLabel.setText("Keluhan:");

        // chips
        holder.chipGroupKeluhanItem.removeAllViews();
        if (item.quickChips != null) {
            for (String chipText : item.quickChips) {
                Chip chip = new Chip(context);
                chip.setText(chipText);
                chip.setClickable(false);
                chip.setCheckable(false);
                chip.setChipBackgroundColorResource(R.color.chip_bg_light);
                holder.chipGroupKeluhanItem.addView(chip);
            }
        }

        // rekomendasi
        holder.recommendationList.removeAllViews();
        if (item.rekomendasi != null) {
            for (com.example.medzone.model.Recommendation r : item.rekomendasi) {
                View card = LayoutInflater.from(context).inflate(R.layout.item_history_rekomendation, holder.recommendationList, false);
                TextView name = card.findViewById(R.id.tvRecName);
                TextView dosis = card.findViewById(R.id.tvRecDosis);
                name.setText(r.name != null ? r.name : "");
                dosis.setText(r.dosis != null ? r.dosis : "");
                holder.recommendationList.addView(card);
            }
        }
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime, tvKeluhanLabel;
        ChipGroup chipGroupKeluhanItem;
        LinearLayout recommendationList;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvKeluhanLabel = itemView.findViewById(R.id.tvKeluhanLabel);
            chipGroupKeluhanItem = itemView.findViewById(R.id.chipGroupKeluhanItem);
            recommendationList = itemView.findViewById(R.id.recommendationList);
        }
    }

    public static final DiffUtil.ItemCallback<com.example.medzone.model.HistoryItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<com.example.medzone.model.HistoryItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull com.example.medzone.model.HistoryItem oldItem, @NonNull com.example.medzone.model.HistoryItem newItem) {
            return oldItem.id != null && oldItem.id.equals(newItem.id);
        }

        @SuppressLint("DiffUtilEquals")
        @Override
        public boolean areContentsTheSame(@NonNull com.example.medzone.model.HistoryItem oldItem, @NonNull com.example.medzone.model.HistoryItem newItem) {
            return oldItem == newItem || (oldItem.timestamp == newItem.timestamp && (oldItem.keluhan == null ? newItem.keluhan == null : oldItem.keluhan.equals(newItem.keluhan)));
        }
    };
}
