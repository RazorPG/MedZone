package com.example.medzone.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.medzone.model.HistoryItem;
import com.example.medzone.model.Recommendation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;

public class HistoryViewModel extends ViewModel {

    private final MutableLiveData<List<HistoryItem>> histories = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<HistoryItem>> getHistories() {
        return histories;
    }

    public void loadHistoriesForCurrentUser() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) {
            histories.postValue(new ArrayList<>());
            return;
        }

        db.collection("users").document(uid)
                .collection("histories")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            histories.postValue(new ArrayList<>());
                            return;
                        }

                        List<HistoryItem> list = new ArrayList<>();
                        if (value != null) {
                            for (DocumentSnapshot ds : value.getDocuments()) {
                                HistoryItem item = new HistoryItem();
                                item.id = ds.getId();
                                Object ts = ds.get("timestamp");
                                if (ts instanceof com.google.firebase.Timestamp) {
                                    item.timestamp = ((com.google.firebase.Timestamp) ts).toDate();
                                } else {
                                    item.timestamp = new Date();
                                }
                                item.keluhan = ds.getString("keluhan");

                                List<String> chips = (List<String>) ds.get("quickChips");
                                item.quickChips = chips;

                                List<Recommendation> recs = new ArrayList<>();
                                List<Map<String, Object>> rawRecs = (List<Map<String, Object>>) ds.get("rekomendasi");
                                if (rawRecs != null) {
                                    for (Map<String, Object> m : rawRecs) {
                                        Recommendation r = new Recommendation();
                                        r.name = m.get("name") == null ? "" : m.get("name").toString();
                                        r.dosis = m.get("dosis") == null ? "" : m.get("dosis").toString();
                                        r.note = m.get("note") == null ? "" : m.get("note").toString();
                                        recs.add(r);
                                    }
                                }
                                item.rekomendasi = recs;
                                list.add(item);
                            }
                        }
                        histories.postValue(list);
                    }
                });
    }
}
