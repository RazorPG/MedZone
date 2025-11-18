package com.example.medzone.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medzone.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class HistoryAdapter extends ListAdapter<com.example.medzone.model.HistoryItem, HistoryAdapter.HistoryViewHolder> {

    private static final String TAG = "HistoryAdapter";
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

        Log.d(TAG, "Binding item at position " + position + " ID=" + item.id + " keluhan='" + item.keluhan + "'");

        if (item.timestamp != null) {
            holder.tvDate.setText(dateFormat.format(item.timestamp));
            holder.tvTime.setText(timeFormat.format(item.timestamp));
        } else {
            holder.tvDate.setText("");
            holder.tvTime.setText("");
        }
        // show keluhan text in the TextView (user requested keluhan be shown here)
        holder.tvKeluhanLabel.setText(item.keluhan != null ? item.keluhan : "");

        // chips: prefer diagnosis from API; fallback to quickChips
        holder.chipGroupKeluhanItem.removeAllViews();
        if (item.diagnosis != null && !item.diagnosis.trim().isEmpty()) {
            // Inflate reusable chip layout so XML attributes (colors, stroke) are applied
            Chip chip = (Chip) LayoutInflater.from(context).inflate(R.layout.chip_keluhan, holder.chipGroupKeluhanItem, false);
            chip.setText(item.diagnosis);
            chip.setClickable(false);
            chip.setCheckable(false);
            // Ensure colors/stroke are applied programmatically as fallback
            applyChipStyle(chip);
            holder.chipGroupKeluhanItem.addView(chip);
        } else if (item.quickChips != null) {
            for (String chipText : item.quickChips) {
                Chip chip = (Chip) LayoutInflater.from(context).inflate(R.layout.chip_keluhan, holder.chipGroupKeluhanItem, false);
                chip.setText(chipText);
                chip.setClickable(false);
                chip.setCheckable(false);
                applyChipStyle(chip);
                holder.chipGroupKeluhanItem.addView(chip);
            }
        }

        // rekomendasi
        holder.recommendationList.removeAllViews();
        if (item.rekomendasi != null) {
            Log.d(TAG, "Item has " + item.rekomendasi.size() + " recommendations");
            for (int i = 0; i < item.rekomendasi.size(); i++) {
                com.example.medzone.model.Recommendation r = item.rekomendasi.get(i);
                Log.d(TAG, "  Binding rec[" + i + "]: name='" + r.name + "' dosis='" + r.dosis + "'");

                View card = LayoutInflater.from(context).inflate(R.layout.item_history_rekomendation, holder.recommendationList, false);
                TextView name = card.findViewById(R.id.tvRecName);
                TextView dosis = card.findViewById(R.id.tvRecDosis);

                String nameText = r.name != null ? r.name : "-";
                String dosisText = r.dosis != null ? r.dosis : "-";

                Log.d(TAG, "  Setting name='" + nameText + "' dosis='" + dosisText + "'");
                name.setText(nameText);
                dosis.setText(dosisText);
                holder.recommendationList.addView(card);
            }
        } else {
            Log.w(TAG, "Item rekomendasi is null!");
        }
    }

    // Helper to explicitly apply chip style values programmatically (fallback)
    private void applyChipStyle(Chip chip) {
        int textColor = ContextCompat.getColor(context, R.color.primary_blue);
        int bgColor = ContextCompat.getColor(context, R.color.chip_bg_light);
        int strokeColor = ContextCompat.getColor(context, R.color.primary_blue);
        float strokeWidth = context.getResources().getDimension(R.dimen.chip_stroke_width);

        chip.setTextColor(textColor);
        chip.setChipBackgroundColor(ColorStateList.valueOf(bgColor));
        chip.setChipStrokeColor(ColorStateList.valueOf(strokeColor));
        chip.setChipStrokeWidth(strokeWidth);
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
