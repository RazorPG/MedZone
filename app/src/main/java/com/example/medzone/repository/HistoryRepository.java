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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
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

    // Simpan history ke local database
    public void saveHistoryLocal(String userId, String keluhan, List<String> quickChips, List<Recommendation> rekomendasi) {
        executor.execute(() -> {
            String rekomendasiJson = gson.toJson(rekomendasi);
            HistoryEntity entity = new HistoryEntity(userId, new Date(), keluhan, quickChips, rekomendasiJson);
            long id = historyDao.insert(entity);
            Log.d(TAG, "History saved to local DB with ID: " + id);

            // Sync ke Firebase di background
            syncToFirebase(id, entity);
        });
    }

    // Sync ke Firebase
    private void syncToFirebase(long localId, HistoryEntity entity) {
        if (entity.userId == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", entity.timestamp);
        data.put("keluhan", entity.keluhan);
        data.put("quickChips", entity.quickChips);

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
                for (HistoryEntity entity : entities) {
                    HistoryItem item = new HistoryItem();
                    item.id = String.valueOf(entity.id);
                    item.timestamp = entity.timestamp;
                    item.keluhan = entity.keluhan;
                    item.quickChips = entity.quickChips;

                    // Convert JSON string back to List<Recommendation>
                    Type type = new TypeToken<List<Recommendation>>(){}.getType();
                    item.rekomendasi = gson.fromJson(entity.rekomendasiJson, type);

                    items.add(item);
                }
            }
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
}

