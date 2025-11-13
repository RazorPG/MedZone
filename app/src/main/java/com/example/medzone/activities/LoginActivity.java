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
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private EditText inputEmail, inputPassword;
    private Button btnLogin;
    private TextView txtRegisterHere;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔹 Inisialisasi Firebase Auth terlebih dahulu
        auth = FirebaseAuth.getInstance();

        // 🔹 Cek apakah user sudah login
        if (auth.getCurrentUser() != null) {
            // User sudah login, langsung ke MainActivity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return; // Stop eksekusi lebih lanjut
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
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Login gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}