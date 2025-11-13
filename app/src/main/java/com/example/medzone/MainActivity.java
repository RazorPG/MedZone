package com.example.medzone;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.medzone.activities.LoginActivity;
import com.example.medzone.api.ApiClient;
import com.example.medzone.api.ApiService;
import com.example.medzone.api.PredictionResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🔹 Inisialisasi Firebase Auth & Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🔹 Bind view
        welcomeText = findViewById(R.id.textUserName);
        logoutBtn = findViewById(R.id.buttonLogout);

        // Test API
        predictionText = findViewById(R.id.predictResult);

        // 🔹 Set logout listener sekali saja di onCreate
        logoutBtn.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
        testApi();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // 🔹 Dapatkan user setelah Activity aktif
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // 🔹 Ambil nama user dari Firestore
            String userId = currentUser.getUid();

            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // 🔹 Ambil nama dari Firestore
                            String name = documentSnapshot.getString("name");

                            if (name != null && !name.isEmpty()) {
                                String welcomeMessage = "Selamat datang di MedZone, " + name + "!";
                                welcomeText.setText(welcomeMessage);
                                Log.d("MainActivity", "Welcome message set: " + welcomeMessage);
                            } else {
                                // Fallback ke email jika nama tidak ada
                                String email = currentUser.getEmail();
                                String welcomeMessage = "Selamat datang di MedZone, " + email + "!";
                                welcomeText.setText(welcomeMessage);
                                Log.d("MainActivity", "Name not found, using email: " + email);
                            }
                        } else {
                            // Dokumen user tidak ditemukan di Firestore
                            Log.w("MainActivity", "User document not found in Firestore");
                            String email = currentUser.getEmail();
                            String welcomeMessage = "Selamat datang di MedZone, " + email + "!";
                            welcomeText.setText(welcomeMessage);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("MainActivity", "Failed to fetch user data from Firestore", e);
                        // Fallback ke email jika gagal mengambil data
                        String email = currentUser.getEmail();
                        String welcomeMessage = "Selamat datang di MedZone, " + email + "!";
                        welcomeText.setText(welcomeMessage);
                    });
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
