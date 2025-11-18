package com.example.medzone.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.medzone.database.AppDatabase;
import com.example.medzone.database.HistoryDao;
import com.example.medzone.database.HistoryEntity;
import com.example.medzone.model.HistoryItem;
import com.example.medzone.model.Recommendation;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HistoryRepository {
    private static final String TAG = "HistoryRepository";
    private final HistoryDao historyDao;
    private final FirebaseFirestore db;
    private final Executor executor;
    private final Handler mainHandler;
    private final Gson gson;

    public HistoryRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.historyDao = database.historyDao();
        this.db = FirebaseFirestore.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
    }

    // Simpan history ke local database (menyertakan diagnosis)
    public void saveHistoryLocal(String userId, String keluhan, List<String> quickChips, List<Recommendation> rekomendasi, String diagnosis) {
        executor.execute(() -> {
            try {
                String rekomendasiJson = gson.toJson(rekomendasi);
                Log.d(TAG, "Saving rekomendasi JSON: " + rekomendasiJson);
                HistoryEntity entity = new HistoryEntity(userId, new Date(), keluhan, quickChips, rekomendasiJson, diagnosis);
                Log.d(TAG, "Inserting history locally for userId=" + userId + " keluhan='" + keluhan + "'");
                long id = historyDao.insert(entity);
                Log.d(TAG, "History saved to local DB with ID: " + id);

                // Sync ke Firebase di background
                syncToFirebase(id, entity);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save history locally: " + e.getMessage(), e);
            }
        });
    }

    // Sync ke Firebase
    private void syncToFirebase(long localId, HistoryEntity entity) {
        if (entity.userId == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", entity.timestamp);
        data.put("keluhan", entity.keluhan);
        data.put("quickChips", entity.quickChips);
        data.put("diagnosis", entity.diagnosis);

        // Convert JSON string back to List<Recommendation>
        Type type = new TypeToken<List<Recommendation>>(){}.getType();
        List<Recommendation> rekomendasi = gson.fromJson(entity.rekomendasiJson, type);

        List<Map<String, Object>> rekomendasiMaps = new ArrayList<>();
        if (rekomendasi != null) {
            for (Recommendation r : rekomendasi) {
                Map<String, Object> recMap = new HashMap<>();
                recMap.put("name", r.name);
                recMap.put("dosis", r.dosis);
                recMap.put("note", r.note);
                rekomendasiMaps.add(recMap);
            }
        }
        data.put("rekomendasi", rekomendasiMaps);

        db.collection("users").document(entity.userId)
                .collection("histories")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "History synced to Firebase: " + documentReference.getId());
                    executor.execute(() -> historyDao.markAsSynced(localId));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync to Firebase: " + e.getMessage());
                });
    }

    // Ambil history dari local database
    public LiveData<List<HistoryItem>> getHistories(String userId) {
        LiveData<List<HistoryEntity>> localData = historyDao.getAllHistoriesByUser(userId);

        MediatorLiveData<List<HistoryItem>> result = new MediatorLiveData<>();
        result.addSource(localData, entities -> {
            List<HistoryItem> items = new ArrayList<>();
            if (entities != null) {
                Log.d(TAG, "Loaded " + entities.size() + " history rows for userId=" + userId);
                for (HistoryEntity entity : entities) {
                    HistoryItem item = new HistoryItem();
                    item.id = String.valueOf(entity.id);
                    item.timestamp = entity.timestamp;
                    item.keluhan = entity.keluhan;
                    item.quickChips = entity.quickChips;
                    item.diagnosis = entity.diagnosis; // map diagnosis from entity

                    // Log the raw JSON from database
                    Log.d(TAG, "Entity ID=" + entity.id + " rekomendasiJson: " + entity.rekomendasiJson);

                    // Convert JSON string back to List<Recommendation>
                    Type type = new TypeToken<List<Recommendation>>(){}.getType();
                    try {
                        item.rekomendasi = gson.fromJson(entity.rekomendasiJson, type);
                        if (item.rekomendasi != null) {
                            Log.d(TAG, "Parsed " + item.rekomendasi.size() + " recommendations for entity " + entity.id);
                            for (int i = 0; i < item.rekomendasi.size(); i++) {
                                Recommendation r = item.rekomendasi.get(i);
                                Log.d(TAG, "  Rec[" + i + "]: name='" + r.name + "' dosis='" + r.dosis + "' note='" + r.note + "'");
                            }
                        } else {
                            Log.w(TAG, "Parsed rekomendasi is null for entity " + entity.id);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse rekomendasiJson for entity " + entity.id + ": " + e.getMessage(), e);
                        item.rekomendasi = new ArrayList<>();
                    }

                    items.add(item);
                }
            } else {
                Log.d(TAG, "Loaded null entities for userId=" + userId);
            }
            Log.d(TAG, "Mapped to " + items.size() + " HistoryItem objects");
            result.setValue(items);
        });

        return result;
    }

    // Sync unsynced histories (untuk dipanggil saat ada koneksi internet)
    public void syncUnsyncedHistories(String userId) {
        executor.execute(() -> {
            List<HistoryEntity> unsyncedList = historyDao.getUnsyncedHistories(userId);
            Log.d(TAG, "Found " + unsyncedList.size() + " unsynced histories");

            for (HistoryEntity entity : unsyncedList) {
                syncToFirebase(entity.id, entity);
            }
        });
    }

    // Load dari Firebase (optional, untuk sync dari cloud ke local)
    public void loadFromFirebase(String userId) {
        db.collection("users").document(userId)
                .collection("histories")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Loaded " + queryDocumentSnapshots.size() + " histories from Firebase");
                    // Bisa ditambahkan logic untuk merge dengan local data jika diperlukan
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load from Firebase: " + e.getMessage());
                });
    }

    /**
     * Fetch histories from Firestore and merge into local DB.
     * Matching rule: (userId, timestamp(ms), keluhan). If exists and fields differ, update local.
     * Otherwise, insert new row and mark as synced.
     */
    public void refreshFromCloudIfChanged(String userId, Runnable onComplete) {
        if (userId == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        db.collection("users").document(userId)
                .collection("histories")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    Log.d(TAG, "Cloud returned " + query.size() + " histories to merge");
                    for (var doc : query.getDocuments()) {
                        try {
                            Date ts = doc.getDate("timestamp");
                            String keluhan = doc.getString("keluhan");
                            String diagnosis = doc.getString("diagnosis");
                            @SuppressWarnings("unchecked")
                            List<String> quickChips = (List<String>) doc.get("quickChips");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> recMaps = (List<Map<String, Object>>) doc.get("rekomendasi");

                            List<Recommendation> rekomendasi = new ArrayList<>();
                            if (recMaps != null) {
                                for (Map<String, Object> m : recMaps) {
                                    String name = m == null ? null : Objects.toString(m.get("name"), null);
                                    String dosis = m == null ? null : Objects.toString(m.get("dosis"), null);
                                    String note = m == null ? null : Objects.toString(m.get("note"), null);
                                    rekomendasi.add(new Recommendation(name, dosis, note));
                                }
                            }
                            String rekomendasiJson = gson.toJson(rekomendasi);

                            long tsMs = ts != null ? ts.getTime() : 0L;
                            executor.execute(() -> {
                                Long localId = historyDao.findIdByUserTimestampKeluhan(userId, tsMs, keluhan);
                                if (localId != null) {
                                    HistoryEntity local = historyDao.getById(localId);
                                    boolean needUpdate = false;

                                    if (!Objects.equals(local.diagnosis, diagnosis)) needUpdate = true;
                                    if (!Objects.equals(local.rekomendasiJson, rekomendasiJson)) needUpdate = true;
                                    if (!Objects.equals(local.quickChips, quickChips)) needUpdate = true;

                                    if (needUpdate) {
                                        Log.d(TAG, "Updating local history id=" + localId + " from cloud changes");
                                        local.diagnosis = diagnosis;
                                        local.rekomendasiJson = rekomendasiJson;
                                        local.quickChips = quickChips;
                                        local.syncedToFirebase = true;
                                        historyDao.update(local);
                                    }
                                } else {
                                    Log.d(TAG, "Inserting new local history from cloud");
                                    HistoryEntity entity = new HistoryEntity();
                                    entity.userId = userId;
                                    entity.timestamp = ts != null ? ts : new Date();
                                    entity.keluhan = keluhan;
                                    entity.quickChips = quickChips;
                                    entity.rekomendasiJson = rekomendasiJson;
                                    entity.diagnosis = diagnosis;
                                    entity.syncedToFirebase = true;
                                    historyDao.insert(entity);
                                }
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to merge a cloud history: " + e.getMessage(), e);
                        }
                    }
                    if (onComplete != null) mainHandler.post(onComplete);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch histories from cloud: " + e.getMessage());
                    if (onComplete != null) mainHandler.post(onComplete);
                });
    }
}
