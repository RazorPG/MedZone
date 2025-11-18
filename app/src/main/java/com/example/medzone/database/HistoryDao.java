package com.example.medzone.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface HistoryDao {

    @Insert
    long insert(HistoryEntity history);

    @Update
    void update(HistoryEntity history);

    // Support null userId: if :userId is null, match rows where userId IS NULL
    @Query("SELECT * FROM history WHERE ((:userId IS NULL AND userId IS NULL) OR userId = :userId) ORDER BY timestamp DESC")
    LiveData<List<HistoryEntity>> getAllHistoriesByUser(String userId);

    @Query("SELECT * FROM history WHERE syncedToFirebase = 0 AND ((:userId IS NULL AND userId IS NULL) OR userId = :userId)")
    List<HistoryEntity> getUnsyncedHistories(String userId);

    @Query("UPDATE history SET syncedToFirebase = 1 WHERE id = :id")
    void markAsSynced(long id);

    @Query("DELETE FROM history WHERE id = :id")
    void delete(long id);

    @Query("DELETE FROM history WHERE ((:userId IS NULL AND userId IS NULL) OR userId = :userId)")
    void deleteAllByUser(String userId);

    @Query("SELECT id FROM history WHERE ((:userId IS NULL AND userId IS NULL) OR userId = :userId) AND timestamp = :timestampMs AND keluhan = :keluhan LIMIT 1")
    Long findIdByUserTimestampKeluhan(String userId, long timestampMs, String keluhan);

    @Query("SELECT * FROM history WHERE id = :id LIMIT 1")
    HistoryEntity getById(long id);
}
