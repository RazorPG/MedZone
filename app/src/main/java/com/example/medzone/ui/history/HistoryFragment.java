package com.example.medzone.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medzone.R;
import com.example.medzone.adapters.HistoryAdapter;
import com.example.medzone.viewmodel.HistoryViewModel;

import java.util.ArrayList;

public class HistoryFragment extends Fragment {

    private HistoryViewModel viewModel;
    private HistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rvHistory);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter(requireContext());
        rv.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        // ✅ Load histories terlebih dahulu untuk inisialisasi LiveData
        viewModel.loadHistoriesForCurrentUser();

        // ✅ Kemudian observe LiveData yang sudah diinisialisasi
        viewModel.getHistories().observe(getViewLifecycleOwner(), list -> {
            if (list == null) list = new ArrayList<>();
            adapter.submitList(list);
        });
    }
}
