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
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.medzone.R;
import com.example.medzone.activities.AccountSettingsActivity;
import com.example.medzone.activities.LoginActivity;
import com.example.medzone.viewmodel.HistoryViewModel;
import com.example.medzone.utils.UserPreferences;
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
    private HistoryViewModel historyViewModel;
    private UserPreferences userPrefs;
    private TextView tvName, tvJoin, tvEmail, tvPhone;
    private TextView tvConsultationCount, tvMedicineCount;
    private ImageView imgAvatar;

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

        // Initialize UserPreferences for local caching
        userPrefs = new UserPreferences(requireContext());

        // Initialize ViewModel
        historyViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        // If not logged in, redirect to login
        if (user == null) {
            redirectToLogin();
            return;
        }

        // Initialize views
        initializeViews(view);

        // Load user data (from cache first, then Firebase if needed)
        loadUserData(user, false);

        // Load statistics from database
        loadStatistics();

        // Setup menu items
        setupMenuItems(view);

        // Setup logout button
        setupLogoutButton(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh user data (in case it was changed in settings)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Only refresh from Firebase if data is stale
            loadUserData(user, userPrefs.needsRefresh());
        }
    }

    private void initializeViews(View view) {
        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvName = view.findViewById(R.id.tvName);
        tvJoin = view.findViewById(R.id.tvJoin);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvConsultationCount = view.findViewById(R.id.tvConsultationCount);
        tvMedicineCount = view.findViewById(R.id.tvMedicineCount);

    }

    /**
     * Load user data from local cache or Firebase
     * @param user FirebaseUser instance
     * @param forceRefresh true to force fetch from Firebase, false to use cache if available
     */
    private void loadUserData(FirebaseUser user, boolean forceRefresh) {
        // Try to load from cache first
        if (!forceRefresh && userPrefs.hasUserData() && !userPrefs.needsRefresh()) {
            android.util.Log.d("ProfileFragment", "Loading user data from cache");
            loadUserDataFromCache();
            return;
        }

        // Fetch from Firebase
        android.util.Log.d("ProfileFragment", "Fetching user data from Firebase");
        loadUserDataFromFirestore(user);
    }

    /**
     * Load user data from local SharedPreferences cache
     */
    private void loadUserDataFromCache() {
        String name = userPrefs.getUserName();
        String email = userPrefs.getUserEmail();
        String phone = userPrefs.getUserPhone();
        String photoUrl = userPrefs.getUserPhotoUrl();
        long joinDate = userPrefs.getJoinDate();

        // Display cached data
        if (!TextUtils.isEmpty(name)) {
            tvName.setText(name);
        } else {
            tvName.setText(getString(R.string.default_user));
        }

        if (!TextUtils.isEmpty(email)) {
            tvEmail.setText(email);
        } else {
            tvEmail.setText(getString(R.string.label_email));
        }

        if (!TextUtils.isEmpty(phone)) {
            tvPhone.setText(phone);
        } else {
            tvPhone.setText(getString(R.string.phone_not_available));
        }

        if (!TextUtils.isEmpty(photoUrl)) {
            loadPhotoUrl(photoUrl, imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.ic_profil);
            androidx.core.widget.ImageViewCompat.setImageTintList(imgAvatar,
                android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)));
        }

        if (joinDate > 0) {
            Date date = new Date(joinDate);
            DateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID"));
            String formattedDate = dateFormat.format(date);
            tvJoin.setText(getString(R.string.joined_since, formattedDate));
        }
    }

    private void loadUserDataFromFirestore(FirebaseUser user) {
        String userId = user.getUid();

        // Fetch user data from Firestore
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = null;
                    String phone = null;
                    String photoUrl = null;

                    if (documentSnapshot.exists()) {
                        // Get data from Firestore
                        name = documentSnapshot.getString("name");
                        phone = documentSnapshot.getString("phone");
                        photoUrl = documentSnapshot.getString("photoUrl");
                    }

                    // Fallback to FirebaseAuth data
                    if (TextUtils.isEmpty(name)) {
                        name = user.getDisplayName();
                    }
                    if (TextUtils.isEmpty(name)) {
                        name = getString(R.string.default_user);
                    }

                    String email = user.getEmail();
                    if (TextUtils.isEmpty(email)) {
                        email = getString(R.string.label_email);
                    }

                    if (TextUtils.isEmpty(phone)) {
                        phone = user.getPhoneNumber();
                    }
                    if (TextUtils.isEmpty(phone)) {
                        phone = getString(R.string.phone_not_available);
                    }

                    if (TextUtils.isEmpty(photoUrl) && user.getPhotoUrl() != null) {
                        photoUrl = user.getPhotoUrl().toString();
                    }

                    long joinDate = 0;
                    if (user.getMetadata() != null) {
                        joinDate = user.getMetadata().getCreationTimestamp();
                    }

                    // Save to local cache
                    userPrefs.saveUserData(userId, name, email, phone, photoUrl, joinDate);

                    // Display data
                    tvName.setText(name);
                    tvEmail.setText(email);
                    tvPhone.setText(phone);

                    if (!TextUtils.isEmpty(photoUrl)) {
                        loadPhotoUrl(photoUrl, imgAvatar);
                    } else {
                        imgAvatar.setImageResource(R.drawable.ic_profil);
                        androidx.core.widget.ImageViewCompat.setImageTintList(imgAvatar,
                            android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)));
                    }

                    if (joinDate > 0) {
                        Date date = new Date(joinDate);
                        DateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID"));
                        String formattedDate = dateFormat.format(date);
                        tvJoin.setText(getString(R.string.joined_since, formattedDate));
                    } else {
                        tvJoin.setText(getString(R.string.joined_since, "-"));
                    }
                })
                .addOnFailureListener(e -> {

                    // If Firebase fails, try to load from cache
                    if (userPrefs.hasUserData()) {
                        loadUserDataFromCache();
                    } else {
                        Toast.makeText(requireContext(), "Gagal memuat data profil", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadStatistics() {
        // Load histories for current user first
        historyViewModel.loadHistoriesForCurrentUser();

        // Observe history data from ViewModel
        historyViewModel.getHistories().observe(getViewLifecycleOwner(), historyList -> {
            if (historyList != null) {
                int count = historyList.size();
                // Update consultation count
                tvConsultationCount.setText(String.valueOf(count));
                // Update medicine count (same as consultation count for now)
                tvMedicineCount.setText(String.valueOf(count));
            } else {
                tvConsultationCount.setText("0");
                tvMedicineCount.setText("0");
            }
        });
    }

    private void loadProfilePhoto(FirebaseUser user) {
        try {
            if (user.getPhotoUrl() != null) {
                loadPhotoUrl(user.getPhotoUrl().toString(), imgAvatar);
            } else {
                imgAvatar.setImageResource(R.drawable.ic_profil);
            }
        } catch (Exception e) {
            imgAvatar.setImageResource(R.drawable.ic_profil);
        }
    }

    /**
     * Load photo from URL or Base64 data URI
     */
    private void loadPhotoUrl(String photoUrl, ImageView imageView) {
        if (TextUtils.isEmpty(photoUrl)) {
            android.util.Log.d("ProfileFragment", "photoUrl is empty, using default icon");
            imageView.setImageResource(R.drawable.ic_profil);
            // Set tint putih untuk icon default
            androidx.core.widget.ImageViewCompat.setImageTintList(imageView,
                android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)));
            return;
        }

        android.util.Log.d("ProfileFragment", "Loading photo, starts with data:image/: " + photoUrl.startsWith("data:image/"));

        // Check if it's a Base64 data URI
        if (photoUrl.startsWith("data:image/")) {
            try {
                // Extract Base64 part
                String base64String = photoUrl.substring(photoUrl.indexOf(",") + 1);
                android.util.Log.d("ProfileFragment", "Base64 string length: " + base64String.length());

                byte[] decodedBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);
                android.util.Log.d("ProfileFragment", "Decoded bytes length: " + decodedBytes.length);

                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                if (bitmap != null) {
                    android.util.Log.d("ProfileFragment", "Bitmap created successfully: " + bitmap.getWidth() + "x" + bitmap.getHeight());

                    // Apply circular crop to bitmap
                    android.graphics.Bitmap circularBitmap = getCircularBitmap(bitmap);
                    android.util.Log.d("ProfileFragment", "Circular bitmap created: " + circularBitmap.getWidth() + "x" + circularBitmap.getHeight());

                    // PENTING: Hapus tint sebelum set foto profil asli
                    androidx.core.widget.ImageViewCompat.setImageTintList(imageView, null);

                    // Set bitmap to imageView
                    imageView.setImageBitmap(circularBitmap);
                    android.util.Log.d("ProfileFragment", "Bitmap set to ImageView successfully");

                    // Don't recycle immediately, let Android manage it
                } else {
                    android.util.Log.e("ProfileFragment", "Bitmap is null after decoding");
                    imageView.setImageResource(R.drawable.ic_profil);
                    // Set tint putih untuk icon default
                    androidx.core.widget.ImageViewCompat.setImageTintList(imageView,
                        android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)));
                }
            } catch (Exception e) {
                android.util.Log.e("ProfileFragment", "Failed to decode Base64 image: " + e.getMessage(), e);
                imageView.setImageResource(R.drawable.ic_profil);
                // Set tint putih untuk icon default
                androidx.core.widget.ImageViewCompat.setImageTintList(imageView,
                    android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)));
            }
        } else {
            // Regular URL, use Glide
            android.util.Log.d("ProfileFragment", "Loading with Glide: " + photoUrl);
            try {
                // Hapus tint sebelum load dengan Glide
                androidx.core.widget.ImageViewCompat.setImageTintList(imageView, null);

                Glide.with(requireContext())
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_profil)
                        .circleCrop()
                        .into(imageView);
            } catch (Exception e) {
                android.util.Log.e("ProfileFragment", "Failed to load image with Glide: " + e.getMessage());
                imageView.setImageResource(R.drawable.ic_profil);
                // Set tint putih untuk icon default
                androidx.core.widget.ImageViewCompat.setImageTintList(imageView,
                    android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)));
            }
        }
    }

    /**
     * Convert bitmap to circular shape
     */
    private android.graphics.Bitmap getCircularBitmap(android.graphics.Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = Math.min(width, height);

        android.util.Log.d("ProfileFragment", "Creating circular bitmap from " + width + "x" + height + " to " + size + "x" + size);

        android.graphics.Bitmap output = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(output);

        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        // Draw circle
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        // Use SRC_IN to keep only the circle part
        paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));

        // Calculate crop rect to center the image
        int left = (width - size) / 2;
        int top = (height - size) / 2;
        android.graphics.Rect srcRect = new android.graphics.Rect(left, top, left + size, top + size);
        android.graphics.Rect dstRect = new android.graphics.Rect(0, 0, size, size);

        canvas.drawBitmap(bitmap, srcRect, dstRect, paint);

        return output;
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
                tvName.setText(getString(R.string.default_user));
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
                tvJoin.setText(getString(R.string.joined_since, formattedDate));
            } else {
                tvJoin.setText(getString(R.string.joined_since, "-"));
            }
        } catch (Exception e) {
            tvJoin.setText(getString(R.string.joined_since, "-"));
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
            menuAccount.setOnClickListener(v -> {
                // Open AccountSettingsActivity
                Intent intent = new Intent(requireContext(), AccountSettingsActivity.class);
                startActivity(intent);
            });
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
            // Clear local cache
            userPrefs.clearUserData();

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
