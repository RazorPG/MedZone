package com.example.medzone.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.medzone.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText inputName, inputEmail, inputPassword, inputConfirmPassword;
    private Button btnRegister;
    private TextView txtLoginHere;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        inputName = findViewById(R.id.inputName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        txtLoginHere = findViewById(R.id.txtLoginHere);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(v -> registerUser());
        txtLoginHere.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String name = inputName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Isi semua field!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Password dan konfirmasi tidak sesuai!", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Registrasi berhasil, simpan ke Firestore
                        String userId = auth.getCurrentUser().getUid();
                        Map<String, Object> user = new HashMap<>();
                        user.put("name", name);
                        user.put("email", email);

                        db.collection("users").document(userId)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("REGISTER", "Firestore write success");
                                    Toast.makeText(this, "Registrasi berhasil!", Toast.LENGTH_SHORT).show();

                                    // Beri delay biar toast kebaca dulu
                                    new Handler().postDelayed(() -> {
                                        startActivity(new Intent(this, LoginActivity.class));
                                        finish();
                                    }, 1000);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("REGISTER", "Firestore write failed", e);
                                    Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // Registrasi gagal — tangkap semua jenis error
                        Exception e = task.getException();
                        if (e != null) {
                            if (e instanceof FirebaseAuthUserCollisionException) {
                                Toast.makeText(this, "Email sudah digunakan. Silakan login!", Toast.LENGTH_LONG).show();
                            } else if (e instanceof FirebaseAuthWeakPasswordException) {
                                Toast.makeText(this, "Password terlalu lemah (minimal 6 karakter).", Toast.LENGTH_LONG).show();
                            } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(this, "Format email tidak valid!", Toast.LENGTH_LONG).show();
                            } else {
                                android.util.Log.e("REGISTER", "Registration failed", e);
                                Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_LONG).show();
                            }
                            Log.e("REGISTER", "FirebaseAuth error", e);
                        } else {
                            Toast.makeText(this, "Registrasi gagal tanpa exception!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
