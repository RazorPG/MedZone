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

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        repository = new HistoryRepository(application);
        // ✅ Inisialisasi dengan empty LiveData untuk mencegah null
        histories = new MutableLiveData<>(new ArrayList<>());
    }

    public LiveData<List<HistoryItem>> getHistories() {
        return histories;
    }

    public void loadHistoriesForCurrentUser() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid != null) {
            histories = repository.getHistories(uid);
        }
    }

    // Method untuk menyimpan history (dipanggil dari HomeFragment)
    public void saveHistory(String keluhan, List<String> quickChips, List<Recommendation> rekomendasi) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid != null) {
            repository.saveHistoryLocal(uid, keluhan, quickChips, rekomendasi);
        }
    }

    // Sync unsynced histories ke Firebase
    public void syncUnsyncedHistories() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid != null) {
            repository.syncUnsyncedHistories(uid);
        }
    }
}
