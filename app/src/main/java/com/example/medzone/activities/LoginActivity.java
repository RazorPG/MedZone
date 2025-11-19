package com.example.medzone.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.medzone.MainActivity;
import com.example.medzone.R;
import com.example.medzone.utils.UserPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private EditText inputEmail, inputPassword;
    private Button btnLogin;
    private TextView txtRegisterHere;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private UserPreferences userPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔹 Inisialisasi Firebase Auth dan Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userPrefs = new UserPreferences(this);

        // 🔹 Cek apakah user sudah login
        if (auth.getCurrentUser() != null) {
            // User sudah login, langsung ke MainActivity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        // 🔹 Jika belum login, tampilkan halaman login
        setContentView(R.layout.activity_login);

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtRegisterHere = findViewById(R.id.txtRegisterHere);

        btnLogin.setOnClickListener(v -> loginUser());
        txtRegisterHere.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    private void loginUser() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Isi semua field!", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show();

                    // 🔹 Simpan data user ke cache lokal setelah login berhasil
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        saveUserDataToCache(user);
                    }

                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Login gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Simpan data user ke cache lokal setelah login
     */
    private void saveUserDataToCache(FirebaseUser user) {
        String userId = user.getUid();
        String email = user.getEmail();
        long joinDate = 0;
        if (user.getMetadata() != null) {
            joinDate = user.getMetadata().getCreationTimestamp();
        }
        final long finalJoinDate = joinDate;

        // 🔹 Ambil data dari Firestore
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = user.getDisplayName();
                    String phone = null;
                    String photoUrl = null;

                    if (documentSnapshot.exists()) {
                        String firestoreName = documentSnapshot.getString("name");
                        if (firestoreName != null && !firestoreName.isEmpty()) {
                            name = firestoreName;
                        }
                        phone = documentSnapshot.getString("phone");
                        photoUrl = documentSnapshot.getString("photoUrl");
                    }

                    // 🔹 Fallback jika masih null
                    if (name == null || name.isEmpty()) {
                        name = email != null && email.contains("@") ? email.substring(0, email.indexOf("@")) : "User";
                    }
                    if (phone == null || phone.isEmpty()) {
                        phone = user.getPhoneNumber();
                    }
                    if ((photoUrl == null || photoUrl.isEmpty()) && user.getPhotoUrl() != null) {
                        photoUrl = user.getPhotoUrl().toString();
                    }

                    // 🔹 Simpan ke cache lokal
                    userPrefs.saveUserData(userId, name, email, phone, photoUrl, finalJoinDate);
                    android.util.Log.d("LoginActivity", "User data saved to cache: " + name);
                })
                .addOnFailureListener(e -> {
                    // 🔹 Jika gagal ambil dari Firestore, simpan data basic dari FirebaseAuth
                    String name = user.getDisplayName();
                    if (name == null || name.isEmpty()) {
                        name = email != null && email.contains("@") ? email.substring(0, email.indexOf("@")) : "User";
                    }
                    String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;
                    userPrefs.saveUserData(userId, name, email, user.getPhoneNumber(), photoUrl, finalJoinDate);
                    android.util.Log.w("LoginActivity", "Failed to fetch Firestore data, using FirebaseAuth data");
                });
    }
}