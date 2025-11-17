package com.example.medzone.model;

import java.util.Date;
import java.util.List;

public class HistoryItem {
    public String id;
    public Date timestamp;
    public String keluhan;
    public List<String> quickChips;
    public List<Recommendation> rekomendasi;

    public HistoryItem() { }

    public HistoryItem(String id, Date timestamp, String keluhan, List<String> quickChips, List<Recommendation> rekomendasi) {
        this.id = id;
        this.timestamp = timestamp;
        this.keluhan = keluhan;
        this.quickChips = quickChips;
        this.rekomendasi = rekomendasi;
    }
}
