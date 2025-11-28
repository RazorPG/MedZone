package com.example.medzone.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.medzone.R;

public class HelpSupportActivity extends AppCompatActivity {

    private ImageView btnBack;
    private LinearLayout layoutWhatsapp, layoutEmail, layoutPhone;
    private LinearLayout faqItem1, faqItem2, faqItem3, faqItem4;
    private TextView faqAnswer1, faqAnswer2, faqAnswer3, faqAnswer4;
    private ImageView faqArrow1, faqArrow2, faqArrow3, faqArrow4;

    private boolean isFaq1Expanded = false;
    private boolean isFaq2Expanded = false;
    private boolean isFaq3Expanded = false;
    private boolean isFaq4Expanded = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        layoutWhatsapp = findViewById(R.id.layoutWhatsapp);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPhone = findViewById(R.id.layoutPhone);

        // FAQ items
        faqItem1 = findViewById(R.id.faqItem1);
        faqItem2 = findViewById(R.id.faqItem2);
        faqItem3 = findViewById(R.id.faqItem3);
        faqItem4 = findViewById(R.id.faqItem4);

        faqAnswer1 = findViewById(R.id.faqAnswer1);
        faqAnswer2 = findViewById(R.id.faqAnswer2);
        faqAnswer3 = findViewById(R.id.faqAnswer3);
        faqAnswer4 = findViewById(R.id.faqAnswer4);

        faqArrow1 = findViewById(R.id.faqArrow1);
        faqArrow2 = findViewById(R.id.faqArrow2);
        faqArrow3 = findViewById(R.id.faqArrow3);
        faqArrow4 = findViewById(R.id.faqArrow4);
    }

    private void setupListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Contact methods
        layoutWhatsapp.setOnClickListener(v -> openWhatsApp());
        layoutEmail.setOnClickListener(v -> openEmail());
        layoutPhone.setOnClickListener(v -> openPhone());

        // FAQ items
        faqItem1.setOnClickListener(v -> toggleFaq(1));
        faqItem2.setOnClickListener(v -> toggleFaq(2));
        faqItem3.setOnClickListener(v -> toggleFaq(3));
        faqItem4.setOnClickListener(v -> toggleFaq(4));
    }

    private void openWhatsApp() {
        try {
            String phoneNumber = getString(R.string.whatsapp_number_clean); // without formatting
            String message = "Halo, saya membutuhkan bantuan terkait MedZone.";

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" + Uri.encode(message)));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp tidak terinstall", Toast.LENGTH_SHORT).show();
        }
    }

    private void openEmail() {
        try {
            String email = getString(R.string.support_email);
            String subject = "Bantuan MedZone";

            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            startActivity(Intent.createChooser(intent, "Kirim Email"));
        } catch (Exception e) {
            Toast.makeText(this, "Tidak ada aplikasi email yang tersedia", Toast.LENGTH_SHORT).show();
        }
    }

    private void openPhone() {
        try {
            String phoneNumber = getString(R.string.phone_number_clean); // without formatting

            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Tidak dapat membuka dialer", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFaq(int faqNumber) {
        switch (faqNumber) {
            case 1:
                isFaq1Expanded = !isFaq1Expanded;
                faqAnswer1.setVisibility(isFaq1Expanded ? View.VISIBLE : View.GONE);
                rotateFaqArrow(faqArrow1, isFaq1Expanded);
                break;
            case 2:
                isFaq2Expanded = !isFaq2Expanded;
                faqAnswer2.setVisibility(isFaq2Expanded ? View.VISIBLE : View.GONE);
                rotateFaqArrow(faqArrow2, isFaq2Expanded);
                break;
            case 3:
                isFaq3Expanded = !isFaq3Expanded;
                faqAnswer3.setVisibility(isFaq3Expanded ? View.VISIBLE : View.GONE);
                rotateFaqArrow(faqArrow3, isFaq3Expanded);
                break;
            case 4:
                isFaq4Expanded = !isFaq4Expanded;
                faqAnswer4.setVisibility(isFaq4Expanded ? View.VISIBLE : View.GONE);
                rotateFaqArrow(faqArrow4, isFaq4Expanded);
                break;
        }
    }

    private void rotateFaqArrow(ImageView arrow, boolean isExpanded) {
        float rotation = isExpanded ? 180f : 0f;
        arrow.animate()
                .rotation(rotation)
                .setDuration(200)
                .start();
    }
}

