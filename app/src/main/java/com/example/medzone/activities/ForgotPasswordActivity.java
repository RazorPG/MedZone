package com.example.medzone.activities;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.medzone.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText inputEmailStep1;
    private MaterialButton btnSendCode;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        auth = FirebaseAuth.getInstance();

        // Back button handling using OnBackPressedDispatcher
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        inputEmailStep1 = findViewById(R.id.inputEmailStep1);
        btnSendCode = findViewById(R.id.btnSendCode);

        btnSendCode.setOnClickListener(v -> sendResetLink());
    }

    private void sendResetLink() {
        String email = inputEmailStep1.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Masukkan email Anda!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Format email tidak valid!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendCode.setEnabled(false);
        btnSendCode.setText(R.string.sending);

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, R.string.reset_sent, Toast.LENGTH_LONG).show();
                    btnSendCode.setEnabled(true);
                    btnSendCode.setText(R.string.send_reset_link);

                    Intent intent = new Intent(this, com.example.medzone.activities.LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ForgotPasswordActivity", "Failed to send reset email", e);
                    Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_LONG).show();
                    btnSendCode.setEnabled(true);
                    btnSendCode.setText(R.string.send_reset_link);
                });
    }
}
