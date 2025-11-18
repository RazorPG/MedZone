package com.example.medzone.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.medzone.api.ApiClient;
import com.example.medzone.api.ApiService;
import com.example.medzone.api.PredictionResponse;
import com.example.medzone.model.Recommendation;
import com.example.medzone.viewmodel.HistoryViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.example.medzone.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private EditText inputKeluhan;
    private ChipGroup chipGroupKeluhan;
    private MaterialButton btnSearchObat;
    private MaterialCardView resultCard;
    private TextView resultObatName, resultObatDosis;
    private HistoryViewModel historyViewModel;
    private String selectedQuickChip = null; // track which quick chip was clicked

    public HomeFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        historyViewModel = new ViewModelProvider(requireActivity()).get(HistoryViewModel.class);

        inputKeluhan = view.findViewById(R.id.inputKeluhan);
        chipGroupKeluhan = view.findViewById(R.id.chipGroupKeluhan);
        btnSearchObat = view.findViewById(R.id.btnSearchObat);
        resultCard = view.findViewById(R.id.resultCard);
        resultObatName = view.findViewById(R.id.result_obat_name);
        resultObatDosis = view.findViewById(R.id.result_obat_dosis);

        // Chip click behavior: set inputKeluhan and track selected quick chip
        if (chipGroupKeluhan != null && inputKeluhan != null) {
            for (int i = 0; i < chipGroupKeluhan.getChildCount(); i++) {
                View child = chipGroupKeluhan.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    chip.setCheckable(true);
                    chip.setOnClickListener(v -> {
                        String current = inputKeluhan.getText() == null ? "" : inputKeluhan.getText().toString();
                        String chipText = chip.getText() == null ? "" : chip.getText().toString();

                        if (current.equals(chipText) && chip.isChecked()) {
                            // If clicking the same checked chip, deselect
                            chip.setChecked(false);
                            inputKeluhan.setText("");
                            selectedQuickChip = null;
                        } else {
                            // Uncheck other chips
                            for (int j = 0; j < chipGroupKeluhan.getChildCount(); j++) {
                                View other = chipGroupKeluhan.getChildAt(j);
                                if (other instanceof Chip) ((Chip) other).setChecked(false);
                            }
                            chip.setChecked(true);
                            inputKeluhan.setText(chipText);
                            inputKeluhan.setSelection(inputKeluhan.getText().length());
                            selectedQuickChip = chipText;
                        }
                    });
                }
            }
        }

        if (inputKeluhan != null && btnSearchObat != null) {
            inputKeluhan.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) { }

                @Override
                public void afterTextChanged(Editable s) {
                    String text = s == null ? "" : s.toString().trim();
                    btnSearchObat.setEnabled(!text.isEmpty());
                    if (!text.isEmpty() && resultCard != null) {
                        resultCard.setVisibility(View.GONE);
                    }
                    // If user types manually, clear selected quick chip
                    if (selectedQuickChip != null && !text.equals(selectedQuickChip)) {
                        selectedQuickChip = null;
                        // uncheck all chips
                        if (chipGroupKeluhan != null) {
                            for (int j = 0; j < chipGroupKeluhan.getChildCount(); j++) {
                                View other = chipGroupKeluhan.getChildAt(j);
                                if (other instanceof Chip) ((Chip) other).setChecked(false);
                            }
                        }
                    }
                }
            });

            btnSearchObat.setOnClickListener(v -> {
                String keluhan = inputKeluhan.getText() == null ? "" : inputKeluhan.getText().toString().trim();
                if (keluhan.isEmpty()) return;

                btnSearchObat.setEnabled(false);
                if (resultCard != null) resultCard.setVisibility(View.VISIBLE);
                if (resultObatName != null) resultObatName.setText("Memuat rekomendasi...");
                if (resultObatDosis != null) resultObatDosis.setText("");

                // Save keluhan for later
                final String finalKeluhan = keluhan;

                // Collect selected quick chips (we only add if text equals keluhan)
                List<String> quickChips = new ArrayList<>();
                if (chipGroupKeluhan != null) {
                    for (int i = 0; i < chipGroupKeluhan.getChildCount(); i++) {
                        View child = chipGroupKeluhan.getChildAt(i);
                        if (child instanceof Chip) {
                            Chip chip = (Chip) child;
                            if (chip.getText() != null && chip.getText().toString().equals(keluhan) && chip.isChecked()) {
                                quickChips.add(chip.getText().toString());
                            }
                        }
                    }
                }
                final List<String> finalQuickChips = quickChips;

                // Clear input and hide keyboard
                inputKeluhan.setText("");
                inputKeluhan.clearFocus();
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(inputKeluhan.getWindowToken(), 0);

                try {
                    ApiService apiService = ApiClient.getClient().create(ApiService.class);
                    JSONObject json = new JSONObject();
                    json.put("keluhan", finalKeluhan);
                    RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), json.toString());
                    Call<PredictionResponse> call = apiService.getPrediction(body);
                    call.enqueue(new Callback<PredictionResponse>() {
                        @Override
                        public void onResponse(Call<PredictionResponse> call, Response<PredictionResponse> response) {
                            btnSearchObat.setEnabled(true);
                            if (response.isSuccessful() && response.body() != null) {
                                PredictionResponse resp = response.body();

                                // Log raw response
                                android.util.Log.d("HomeFragment", "API Response - Obat: '" + resp.getObat() + "' Dosis: '" + resp.getDosis() + "' Diagnosis: '" + resp.getDiagnosis() + "'");

                                String obat = resp.getObat() == null ? "-" : resp.getObat();
                                String dosis = resp.getDosis() == null ? "-" : resp.getDosis();

                                android.util.Log.d("HomeFragment", "After fallback - Obat: '" + obat + "' Dosis: '" + dosis + "'");

                                if (resultObatName != null) resultObatName.setText(obat);
                                if (resultObatDosis != null) resultObatDosis.setText(dosis);
                                if (resultCard != null) resultCard.setVisibility(View.VISIBLE);

                                // Determine diagnosis to save: prefer selected quick chip over API diagnosis
                                String apiDiagnosis = resp.getDiagnosis() == null ? "" : resp.getDiagnosis();
                                String diagnosisToSave = (selectedQuickChip != null && !selectedQuickChip.isEmpty()) ? selectedQuickChip : apiDiagnosis;

                                // ✅ Simpan ke local database (akan otomatis sync ke Firebase di background)
                                List<Recommendation> rekomendasi = new ArrayList<>();
                                Recommendation rec = new Recommendation(obat, dosis, "");
                                rekomendasi.add(rec);

                                android.util.Log.d("HomeFragment", "Saving recommendation - name: '" + rec.name + "' dosis: '" + rec.dosis + "'");

                                historyViewModel.saveHistory(finalKeluhan, finalQuickChips, rekomendasi, diagnosisToSave);

                                // reset selection after saving
                                selectedQuickChip = null;
                                if (chipGroupKeluhan != null) {
                                    for (int j = 0; j < chipGroupKeluhan.getChildCount(); j++) {
                                        View other = chipGroupKeluhan.getChildAt(j);
                                        if (other instanceof Chip) ((Chip) other).setChecked(false);
                                    }
                                }
                            } else {
                                if (resultObatName != null) resultObatName.setText("Gagal: respons tidak valid dari server");
                            }
                        }

                        @Override
                        public void onFailure(Call<PredictionResponse> call, Throwable t) {
                            btnSearchObat.setEnabled(true);
                            if (resultObatName != null) resultObatName.setText("Gagal memanggil API: " + t.getMessage());
                        }
                    });
                } catch (Exception e) {
                    btnSearchObat.setEnabled(true);
                    if (resultCard != null) resultCard.setVisibility(View.VISIBLE);
                    if (resultObatName != null) resultObatName.setText("Exception: " + e.getMessage());
                }
            });
        }
    }
}
