package com.example.medzone.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.medzone.model.HistoryItem;
import com.example.medzone.model.Recommendation;
import com.example.medzone.repository.HistoryRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class HistoryViewModel extends AndroidViewModel {

    private final HistoryRepository repository;
    private LiveData<List<HistoryItem>> histories;

    // Event to notify the UI when a save is initiated (or finished)
    private final MutableLiveData<Boolean> saveEvent = new MutableLiveData<>();

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        repository = new HistoryRepository(application);
        // ✅ Inisialisasi dengan empty LiveData untuk mencegah null
        histories = new MutableLiveData<>(new ArrayList<>());
    }

    public LiveData<List<HistoryItem>> getHistories() {
        return histories;
    }

    public LiveData<Boolean> getSaveEvent() {
        return saveEvent;
    }

    public void loadHistoriesForCurrentUser() {
        // Pass nullable userId to repository so local histories are available even when user is not signed in
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        histories = repository.getHistories(uid);
    }

    // Method untuk menyimpan history (dipanggil dari HomeFragment)
    public void saveHistory(String keluhan, List<String> quickChips, List<Recommendation> rekomendasi, String diagnosis) {
        // Pass nullable userId; repository will skip syncing to Firebase if userId is null
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        repository.saveHistoryLocal(uid, keluhan, quickChips, rekomendasi, diagnosis);
        // Notify UI that a save was initiated (UI can show immediate feedback). This is not a guarantee of remote sync.
        saveEvent.setValue(true);
    }

    // Sync unsynced histories ke Firebase
    public void syncUnsyncedHistories() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid != null) {
            repository.syncUnsyncedHistories(uid);
        }
    }

    // Fetch dari Firebase dan merge ke lokal jika berbeda
    public void refreshFromCloudIfChanged() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid != null) {
            repository.refreshFromCloudIfChanged(uid, null);
        }
    }
}
