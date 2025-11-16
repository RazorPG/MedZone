package com.example.medzone;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageView;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.medzone.activities.LoginActivity;
import com.example.medzone.api.ApiClient;
import com.example.medzone.api.ApiService;
import com.example.medzone.api.PredictionResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView welcomeText, predictionText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Button logoutBtn;
    private EditText inputKeluhan;
    private ChipGroup chipGroupKeluhan;
    private MaterialButton btnSearchObat;
    // Result card views
    private MaterialCardView resultCard;
    private TextView resultObatName;
    private TextView resultObatDosis;
    private ImageView resultIcon;
    private TextView resultTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🔹 Inisialisasi Firebase Auth & Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🔹 Bind view
//        welcomeText = findViewById(R.id.textUserName);
//        logoutBtn = findViewById(R.id.buttonLogout);
        // Bind input and chip group from activity_main.xml
        inputKeluhan = findViewById(R.id.inputKeluhan);
        chipGroupKeluhan = findViewById(R.id.chipGroupKeluhan);
        btnSearchObat = findViewById(R.id.btnSearchObat);
        // Bind result card and inner views
        resultCard = findViewById(R.id.resultCard);
        resultObatName = findViewById(R.id.result_obat_name);
        resultObatDosis = findViewById(R.id.result_obat_dosis);
        resultIcon = findViewById(R.id.resultIcon);
        resultTitle = findViewById(R.id.resultTitle);

        // Pasang listener untuk setiap Chip: klik -> isi EditText (ganti teks sebelumnya)
        if (chipGroupKeluhan != null && inputKeluhan != null) {
            for (int i = 0; i < chipGroupKeluhan.getChildCount(); i++) {
                View child = chipGroupKeluhan.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    chip.setOnClickListener(v -> {
                        // Toggle behavior: jika teks saat ini sama dengan teks chip -> clear,
                        // kalau tidak -> ganti ke teks chip
                        String current = inputKeluhan.getText() == null ? "" : inputKeluhan.getText().toString();
                        String chipText = chip.getText() == null ? "" : chip.getText().toString();
                        if (current.equals(chipText)) {
                            inputKeluhan.setText("");
                        } else {
                            inputKeluhan.setText(chipText);
                            // Posisikan kursor di akhir teks
                            inputKeluhan.setSelection(inputKeluhan.getText().length());
                        }
                    });
                }
            }
        }

        // TextWatcher: aktifkan tombol cari jika input tidak kosong
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
                    // sembunyikan hasil lama saat user mulai mengetik
                    if (!text.isEmpty() && resultCard != null) {
                        resultCard.setVisibility(View.GONE);
                    }
                }
            });

            // OnClick: kirim keluhan ke API saat tombol diklik
            btnSearchObat.setOnClickListener(v -> {
                String keluhan = inputKeluhan.getText() == null ? "" : inputKeluhan.getText().toString().trim();
                if (keluhan.isEmpty()) return;

                // Disable button while request
                btnSearchObat.setEnabled(false);
                if (resultCard != null) resultCard.setVisibility(View.VISIBLE);
                if (resultObatName != null) resultObatName.setText("Memuat rekomendasi...");
                if (resultObatDosis != null) resultObatDosis.setText("");

                // Clear input immediately after button click (better UX) and hide keyboard
                inputKeluhan.setText("");
                inputKeluhan.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(inputKeluhan.getWindowToken(), 0);

                try {
                    ApiService apiService = ApiClient.getClient().create(ApiService.class);
                    JSONObject json = new JSONObject();
                    json.put("keluhan", keluhan);

                    RequestBody body = RequestBody.create(
                            okhttp3.MediaType.parse("application/json; charset=utf-8"),
                            json.toString()
                    );
                    Call<PredictionResponse> call = apiService.getPrediction(body);
                    call.enqueue(new Callback<PredictionResponse>() {
                        @Override
                        public void onResponse(Call<PredictionResponse> call, Response<PredictionResponse> response) {
                            btnSearchObat.setEnabled(true);
                            if (response.isSuccessful() && response.body() != null) {
                                PredictionResponse resp = response.body();
                                String obat = resp.getObat() == null ? "-" : resp.getObat();
                                String dosis = resp.getDosis() == null ? "-" : resp.getDosis();
                                String kel = resp.getKeluhan() == null ? keluhan : resp.getKeluhan();
                                if (resultObatName != null) resultObatName.setText(obat);
                                if (resultObatDosis != null) resultObatDosis.setText(dosis);
                                if (resultCard != null) resultCard.setVisibility(View.VISIBLE);
                            } else {
                                if (resultObatName != null) resultObatName.setText("Gagal: respons tidak valid dari server");
                                if (resultObatDosis != null) resultObatDosis.setText("");
                                 Log.e("API", "Response error: " + response.code());
                            }
                        }

                        @Override
                        public void onFailure(Call<PredictionResponse> call, Throwable t) {
                            btnSearchObat.setEnabled(true);
                            if (resultObatName != null) resultObatName.setText("Gagal memanggil API: " + t.getMessage());
                            if (resultObatDosis != null) resultObatDosis.setText("");
                             Log.e("API", "onFailure: ", t);
                        }
                    });

                } catch (Exception e) {
                    btnSearchObat.setEnabled(true);
                    if (resultCard != null) resultCard.setVisibility(View.VISIBLE);
                    if (resultObatName != null) resultObatName.setText("Exception: " + e.getMessage());
                    if (resultObatDosis != null) resultObatDosis.setText("");
                     Log.e("API", "Exception building request", e);
                }
            });
        }

        // Test API
//        predictionText = findViewById(R.id.predictResult);

        // 🔹 Set logout listener sekali saja di onCreate
//        logoutBtn.setOnClickListener(v -> {
//            mAuth.signOut();
//            Intent intent = new Intent(this, LoginActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(intent);
//            finish();
//        });
//        testApi();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // 🔹 Dapatkan user setelah Activity aktif
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // 🔹 Ambil nama user dari Firestore
//            String userId = currentUser.getUid();
//
//            db.collection("users").document(userId)
//                    .get()
//                    .addOnSuccessListener(documentSnapshot -> {
//                        if (documentSnapshot.exists()) {
//                            // 🔹 Ambil nama dari Firestore
//                            String name = documentSnapshot.getString("name");
//
//                            if (name != null && !name.isEmpty()) {
//                                String welcomeMessage = "Selamat datang di MedZone, " + name + "!";
//                                welcomeText.setText(welcomeMessage);
//                                Log.d("MainActivity", "Welcome message set: " + welcomeMessage);
//                            } else {
//                                // Fallback ke email jika nama tidak ada
//                                String email = currentUser.getEmail();
//                                String welcomeMessage = "Selamat datang di MedZone, " + email + "!";
//                                welcomeText.setText(welcomeMessage);
//                                Log.d("MainActivity", "Name not found, using email: " + email);
//                            }
//                        } else {
//                            // Dokumen user tidak ditemukan di Firestore
//                            Log.w("MainActivity", "User document not found in Firestore");
//                            String email = currentUser.getEmail();
//                            String welcomeMessage = "Selamat datang di MedZone, " + email + "!";
//                            welcomeText.setText(welcomeMessage);
//                        }
//                    })
//                    .addOnFailureListener(e -> {
//                        Log.e("MainActivity", "Failed to fetch user data from Firestore", e);
//                        // Fallback ke email jika gagal mengambil data
//                        String email = currentUser.getEmail();
//                        String welcomeMessage = "Selamat datang di MedZone, " + email + "!";
//                        welcomeText.setText(welcomeMessage);
//                    });
        } else {
            // 🔹 Kalau belum login, arahkan ke halaman Login
            Log.w("MainActivity", "User belum login, redirect ke LoginActivity");
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void testApi() {
        try {
            ApiService apiService = ApiClient.getClient().create(ApiService.class);

            JSONObject json = new JSONObject();
            json.put("keluhan", "sakit kepala dan demam tinggi");

            RequestBody body = RequestBody.create(
                    okhttp3.MediaType.parse("application/json; charset=utf-8"),
                    json.toString()
            );
            Call<PredictionResponse> call = apiService.getPrediction(body);
            call.enqueue(new Callback<PredictionResponse>() {
                @Override
                public void onResponse(Call<PredictionResponse> call, Response<PredictionResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String keluhan = response.body().getKeluhan();
                        String obat = response.body().getObat();
                        String dosis = response.body().getDosis();
                        predictionText.setText("Keluhan: " + keluhan + "\nObat: " + obat + "\nDosis: " + dosis);
                    } else {
                        Log.e("API", "Response gagal atau body null");
                    }
                }

                @Override
                public void onFailure(Call<PredictionResponse> call, Throwable t) {
                    predictionText.setText("Gagal memanggil API: " + t.getMessage());
                    Log.e("API", "onFailure: ", t);
                }
            });

        } catch (Exception e) {
            predictionText.setText("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
