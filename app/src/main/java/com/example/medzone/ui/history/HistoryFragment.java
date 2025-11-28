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
    private View emptyStateView;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rvHistory);
        emptyStateView = view.findViewById(R.id.emptyStateView);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter(requireContext());
        recyclerView.setAdapter(adapter);

        // Use activity-scoped ViewModel so HomeFragment and HistoryFragment share the same instance
        viewModel = new ViewModelProvider(requireActivity()).get(HistoryViewModel.class);

        // ✅ Load histories terlebih dahulu untuk inisialisasi LiveData
        viewModel.loadHistoriesForCurrentUser();

        // ✅ Observe LiveData dari Room
        viewModel.getHistories().observe(getViewLifecycleOwner(), list -> {
            if (list == null) list = new ArrayList<>();
            adapter.submitList(list);

            // Show empty state if list is empty, otherwise show recycler view
            if (list.isEmpty()) {
                emptyStateView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyStateView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });

        // ✅ Auto-fetch dari Firebase dan merge ke lokal apabila berbeda
        viewModel.refreshFromCloudIfChanged();
    }
}
