package com.example.medzone.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.medzone.R;
import com.example.medzone.activities.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextView tvName, tvJoin, tvEmail, tvPhone;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        // If not logged in, redirect to login
        if (user == null) {
            redirectToLogin();
            return;
        }

        // Initialize views
        initializeViews(view);

        // Load user data from Firestore
        loadUserDataFromFirestore(user);

        // Setup menu items
        setupMenuItems(view);

        // Setup logout button
        setupLogoutButton(view);
    }

    private void initializeViews(View view) {
        tvName = view.findViewById(R.id.tvName);
        tvJoin = view.findViewById(R.id.tvJoin);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
    }

    private void loadUserDataFromFirestore(FirebaseUser user) {
        String userId = user.getUid();

        // Fetch user data from Firestore
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get name from Firestore
                        String name = documentSnapshot.getString("name");
                        if (!TextUtils.isEmpty(name)) {
                            tvName.setText(name);
                        } else {
                            tvName.setText("Pengguna");
                        }
                    } else {
                        // Document doesn't exist, use email username as fallback
                        setFallbackName(user);
                    }
                })
                .addOnFailureListener(e -> {
                    // Error fetching from Firestore, use fallback
                    setFallbackName(user);
                    Toast.makeText(requireContext(), "Gagal memuat data profil", Toast.LENGTH_SHORT).show();
                });

        // Load email
        String email = user.getEmail();
        tvEmail.setText(!TextUtils.isEmpty(email) ? email : "Email tidak tersedia");

        // Load phone number
        String phone = user.getPhoneNumber();
        if (TextUtils.isEmpty(phone)) {
            tvPhone.setText("Nomor tidak tersedia");
        } else {
            tvPhone.setText(phone);
        }

        // Load join date from Firebase metadata
        loadJoinDate(user);
    }

    private void setFallbackName(FirebaseUser user) {
        String displayName = user.getDisplayName();
        if (!TextUtils.isEmpty(displayName)) {
            tvName.setText(displayName);
        } else {
            String email = user.getEmail();
            if (!TextUtils.isEmpty(email) && email.contains("@")) {
                tvName.setText(email.substring(0, email.indexOf("@")));
            } else {
                tvName.setText("Pengguna");
            }
        }
    }

    private void loadJoinDate(FirebaseUser user) {
        try {
            long createdMillis = 0;
            if (user.getMetadata() != null) {
                createdMillis = user.getMetadata().getCreationTimestamp();
            }

            if (createdMillis > 0) {
                Date date = new Date(createdMillis);
                DateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID"));
                String formattedDate = dateFormat.format(date);
                tvJoin.setText("Bergabung sejak\n" + formattedDate);
            } else {
                tvJoin.setText("Bergabung sejak\n-");
            }
        } catch (Exception e) {
            tvJoin.setText("Bergabung sejak\n-");
        }
    }

    private void setupMenuItems(View view) {
        // Configure Account Settings menu
        View menuAccount = view.findViewById(R.id.menuAccount);
        if (menuAccount != null) {
            ImageView icon = menuAccount.findViewById(R.id.menuIcon);
            TextView title = menuAccount.findViewById(R.id.menuTitle);
            if (icon != null) icon.setImageResource(R.drawable.ic_setting);
            if (title != null) title.setText(R.string.settings_account);
            menuAccount.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Pengaturan Akun - Coming Soon", Toast.LENGTH_SHORT).show()
            );
        }

        // Configure Notifications menu
        View menuNotifications = view.findViewById(R.id.menuNotifications);
        if (menuNotifications != null) {
            ImageView icon = menuNotifications.findViewById(R.id.menuIcon);
            TextView title = menuNotifications.findViewById(R.id.menuTitle);
            if (icon != null) icon.setImageResource(R.drawable.ic_notification);
            if (title != null) title.setText(R.string.settings_notifications);
            menuNotifications.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Notifikasi - Coming Soon", Toast.LENGTH_SHORT).show()
            );
        }

        // Configure Help menu
        View menuHelp = view.findViewById(R.id.menuHelp);
        if (menuHelp != null) {
            ImageView icon = menuHelp.findViewById(R.id.menuIcon);
            TextView title = menuHelp.findViewById(R.id.menuTitle);
            if (icon != null) icon.setImageResource(R.drawable.ic_help);
            if (title != null) title.setText(R.string.settings_help);
            menuHelp.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Bantuan - Coming Soon", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void setupLogoutButton(View view) {
        View btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Keluar")
                .setMessage("Apakah Anda yakin ingin keluar dari akun?")
                .setPositiveButton("Ya", (dialog, which) -> performLogout())
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void performLogout() {
        try {
            // Sign out from Firebase
            auth.signOut();

            // Show success message
            Toast.makeText(requireContext(), "Berhasil keluar", Toast.LENGTH_SHORT).show();

            // Redirect to login and clear activity stack
            redirectToLogin();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Gagal keluar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
