package com.example.medzone.database;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Date;
import java.util.List;

@Entity(tableName = "history")
@TypeConverters(Converters.class)
public class HistoryEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String userId;
    public Date timestamp;
    public String keluhan;
    public List<String> quickChips;
    public String rekomendasiJson; // Store as JSON string
    public boolean syncedToFirebase;

    public HistoryEntity() {
    }

    @Ignore
    public HistoryEntity(String userId, Date timestamp, String keluhan, List<String> quickChips, String rekomendasiJson) {
        this.userId = userId;
        this.timestamp = timestamp;
        this.keluhan = keluhan;
        this.quickChips = quickChips;
        this.rekomendasiJson = rekomendasiJson;
        this.syncedToFirebase = false;
    }
}
