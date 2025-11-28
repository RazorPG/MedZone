package com.example.medzone.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.medzone.R;
import com.example.medzone.utils.UserPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AccountSettingsActivity extends AppCompatActivity {

    private ImageView imgProfile;
    private Button btnChangePhoto;
    private EditText etFullName;
    private TextView tvEmail;
    private EditText etPhone;
    private Button btnSave;
    private ProgressBar progressBar;

    private Uri selectedImageUri;

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private UserPreferences userPrefs;

    // make temp upload file a field so lambdas can reference it
    private java.io.File tempUploadFile;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    private String currentPhotoUrlFromServer; // track latest known photoUrl from Firestore/userPrefs

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        // Initialize views
        ImageView btnBack = findViewById(R.id.btnBack);
        imgProfile = findViewById(R.id.imgProfile);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        etFullName = findViewById(R.id.etFullName);
        tvEmail = findViewById(R.id.tvEmail);
        etPhone = findViewById(R.id.etPhone);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        // Setup back button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        userPrefs = new UserPreferences(this);

        // Load cached photoUrl first so we don't overwrite with null later
        currentPhotoUrlFromServer = userPrefs.getUserPhotoUrl();

        if (user == null) {
            finish();
            return;
        }

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openImagePickerInternal();
            } else {
                Toast.makeText(this, "Izin akses foto ditolak. Tidak bisa memilih gambar.", Toast.LENGTH_LONG).show();
            }
        });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result != null && result.getData() != null && result.getData().getData() != null) {
                        selectedImageUri = result.getData().getData();
                        try {
                            Glide.with(this).load(selectedImageUri).placeholder(R.drawable.ic_profil).circleCrop().into(imgProfile);
                        } catch (Exception glideEx) {
                            android.util.Log.w("AccountSettings", "Glide preview failed: " + glideEx.getMessage());
                            try {
                                imgProfile.setImageURI(selectedImageUri);
                            } catch (Exception uriEx) {
                                android.util.Log.e("AccountSettings", "URI preview failed: " + uriEx.getMessage());
                                imgProfile.setImageResource(R.drawable.ic_profil);
                            }
                        }
                    }
                }
        );

        loadUserData();

        btnChangePhoto.setOnClickListener(v -> openImagePicker());
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadUserData() {
        if (user.getPhotoUrl() != null) {
            loadPhotoUrl(user.getPhotoUrl().toString(), imgProfile);
        } else {
            imgProfile.setImageResource(R.drawable.ic_profil);
        }

        String email = user.getEmail();
        tvEmail.setText(!TextUtils.isEmpty(email) ? email : getString(R.string.label_email));

        if (!TextUtils.isEmpty(user.getDisplayName())) {
            etFullName.setText(user.getDisplayName());
        }

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String phone = documentSnapshot.getString("phone");
                        String name = documentSnapshot.getString("name");
                        String photoUrl = documentSnapshot.getString("photoUrl");

                        if (!TextUtils.isEmpty(phone)) etPhone.setText(phone);
                        if (!TextUtils.isEmpty(name) && TextUtils.isEmpty(etFullName.getText())) etFullName.setText(name);

                        // Kalau Firestore punya photoUrl, pakai itu dan update state lokal
                        if (!TextUtils.isEmpty(photoUrl)) {
                            currentPhotoUrlFromServer = photoUrl;
                            loadPhotoUrl(photoUrl, imgProfile);
                        } else {
                            // Kalau Firestore belum punya photoUrl, tapi cache punya, tetap pakai yang di cache
                            String cachedPhoto = userPrefs.getUserPhotoUrl();
                            if (!TextUtils.isEmpty(cachedPhoto)) {
                                currentPhotoUrlFromServer = cachedPhoto;
                                loadPhotoUrl(cachedPhoto, imgProfile);
                            }
                        }
                    }
                });
    }

    /**
     * Load photo from URL or Base64 data URI
     */
    private void loadPhotoUrl(String photoUrl, ImageView imageView) {
        if (TextUtils.isEmpty(photoUrl)) {
            android.util.Log.d("AccountSettings", "photoUrl is empty, using default icon");
            imageView.setImageResource(R.drawable.ic_profil);
            // Set tint putih untuk icon default
            androidx.core.widget.ImageViewCompat.setImageTintList(imageView,
                android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.white)));
            return;
        }

        android.util.Log.d("AccountSettings", "Loading photo, starts with data:image/: " + photoUrl.startsWith("data:image/"));

        // Check if it's a Base64 data URI
        if (photoUrl.startsWith("data:image/")) {
            try {
                // Extract Base64 part
                String base64String = photoUrl.substring(photoUrl.indexOf(",") + 1);
                android.util.Log.d("AccountSettings", "Base64 string length: " + base64String.length());

                byte[] decodedBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);
                android.util.Log.d("AccountSettings", "Decoded bytes length: " + decodedBytes.length);

                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                if (bitmap != null) {
                    android.util.Log.d("AccountSettings", "Bitmap created successfully: " + bitmap.getWidth() + "x" + bitmap.getHeight());

                    // Apply circular crop to bitmap
                    android.graphics.Bitmap circularBitmap = getCircularBitmap(bitmap);
                    android.util.Log.d("AccountSettings", "Circular bitmap created: " + circularBitmap.getWidth() + "x" + circularBitmap.getHeight());

                    // PENTING: Hapus tint sebelum set foto profil asli
                    androidx.core.widget.ImageViewCompat.setImageTintList(imageView, null);

                    // Set bitmap to imageView
                    imageView.setImageBitmap(circularBitmap);
                    android.util.Log.d("AccountSettings", "Bitmap set to ImageView successfully");

                    // Don't recycle immediately, let Android manage it
                } else {
                    android.util.Log.e("AccountSettings", "Bitmap is null after decoding");
                    imageView.setImageResource(R.drawable.ic_profil);
                    // Set tint putih untuk icon default
                    androidx.core.widget.ImageViewCompat.setImageTintList(imageView,
                        android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.white)));
                }
            } catch (Exception e) {
                android.util.Log.e("AccountSettings", "Failed to decode Base64 image: " + e.getMessage(), e);
                imageView.setImageResource(R.drawable.ic_profil);
                // Set tint putih untuk icon default
                androidx.core.widget.ImageViewCompat.setImageTintList(imageView,
                    android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.white)));
            }
        } else {
            // Regular URL, use Glide
            android.util.Log.d("AccountSettings", "Loading with Glide: " + photoUrl);
            try {
                // Hapus tint sebelum load dengan Glide
                androidx.core.widget.ImageViewCompat.setImageTintList(imageView, null);

                Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_profil)
                        .circleCrop()
                        .into(imageView);
            } catch (Exception e) {
                android.util.Log.e("AccountSettings", "Failed to load image with Glide: " + e.getMessage());
                imageView.setImageResource(R.drawable.ic_profil);
                // Set tint putih untuk icon default
                androidx.core.widget.ImageViewCompat.setImageTintList(imageView,
                    android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.white)));
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

    private void openImagePicker() {
        String permission;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openImagePickerInternal();
        } else {
            permissionLauncher.launch(permission);
        }
    }

    private void openImagePickerInternal() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, getString(R.string.choose_photo_title)));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void saveProfile() {
        final String name = etFullName.getText().toString().trim();
        final String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etFullName.setError(getString(R.string.name_required));
            etFullName.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        if (selectedImageUri != null) {
            uploadImageAndSave(name, phone);
        } else {
            // Tidak ubah foto → gunakan photoUrl yang sudah ada (kalau ada)
            String photoToKeep = currentPhotoUrlFromServer;
            updateUserProfile(name, phone, photoToKeep);
        }
    }

    private void uploadImageAndSave(String name, String phone) {
        if (selectedImageUri == null) {
            updateUserProfile(name, phone, null);
            return;
        }

        android.content.ContentResolver resolver = getContentResolver();

        java.io.InputStream is = null;
        try {
            is = resolver.openInputStream(selectedImageUri);
            if (is == null) throw new java.io.FileNotFoundException("Cannot open input stream for URI: " + selectedImageUri);

            // Read and compress image
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
            if (bitmap == null) {
                throw new Exception("Failed to decode image");
            }

            // Resize bitmap to max 512x512 to reduce size
            int maxSize = 512;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
            int newWidth = Math.round(width * ratio);
            int newHeight = Math.round(height * ratio);

            android.graphics.Bitmap resizedBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

            // Convert to Base64
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);

            // Add data URI prefix
            String photoDataUrl = "data:image/jpeg;base64," + base64Image;

            android.util.Log.d("AccountSettings", "Image converted to Base64, size: " + base64Image.length() + " chars");

            // Simpan photoUrl baru ke state supaya konsisten dipakai
            currentPhotoUrlFromServer = photoDataUrl;

            // Save to Firestore via updateUserProfile
            updateUserProfile(name, phone, photoDataUrl);

            // Clean up
            bitmap.recycle();
            resizedBitmap.recycle();

        } catch (java.io.FileNotFoundException fnf) {
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            android.util.Log.e("AccountSettings", "Selected image not found", fnf);
            Toast.makeText(this, "File tidak ditemukan untuk diunggah", Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            // Log detail tanpa menampilkan pesan mentah ke pengguna
            android.util.Log.e("AccountSettings", "Unexpected error reading image", ex);
            showUploadFailedDialog(null, name, phone);
        } finally {
            try { if (is != null) is.close(); } catch (Exception ignore) {}
        }
    }

    private void updateUserProfile(String name, String phone, @Nullable String photoUrl) {
        UserProfileChangeRequest.Builder profileBuilder = new UserProfileChangeRequest.Builder();
        profileBuilder.setDisplayName(name);

        // JANGAN set photoUri ke FirebaseAuth jika Base64 data URI
        // Firebase Auth hanya terima URL HTTP/HTTPS, bukan data URI
        // Photo Base64 hanya disimpan ke Firestore & cache lokal
        if (!TextUtils.isEmpty(photoUrl) && !photoUrl.startsWith("data:image/")) {
            // Hanya set jika URL valid (bukan Base64)
            profileBuilder.setPhotoUri(Uri.parse(photoUrl));
        }

        user.updateProfile(profileBuilder.build())
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);

                        Exception e = task.getException();
                        android.util.Log.e("AccountSettings", "Failed to update Firebase Auth profile", e);
                        Toast.makeText(
                                AccountSettingsActivity.this,
                                getString(R.string.profile_update_failed),
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    // Update Firestore dengan photoUrl (bisa Base64 atau URL biasa)
                    Map<String, Object> updateMap = new HashMap<>();
                    updateMap.put("name", name);
                    updateMap.put("phone", phone);

                    // Selalu simpan photoUrl ke Firestore jika ada
                    if (!TextUtils.isEmpty(photoUrl)) {
                        updateMap.put("photoUrl", photoUrl);
                    } else if (!TextUtils.isEmpty(currentPhotoUrlFromServer)) {
                        // Jika tidak ada photoUrl baru, pertahankan yang lama
                        updateMap.put("photoUrl", currentPhotoUrlFromServer);
                    }

                    db.collection("users")
                            .document(user.getUid())
                            .set(updateMap, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(View.GONE);
                                btnSave.setEnabled(true);

                                // Ambil data terbaru dari FirebaseUser
                                String email = user.getEmail();
                                long joinDate = 0;
                                if (user.getMetadata() != null) {
                                    joinDate = user.getMetadata().getCreationTimestamp();
                                }

                                // Tentukan photo final: pakai argumen jika ada, kalau tidak pakai state currentPhotoUrlFromServer
                                String finalPhoto = !TextUtils.isEmpty(photoUrl) ? photoUrl : currentPhotoUrlFromServer;

                                // Simpan ke cache lokal
                                userPrefs.saveUserData(user.getUid(), name, email, phone, finalPhoto, joinDate);

                                android.util.Log.d("AccountSettings", "Profile saved successfully with photo: " + (finalPhoto != null ? finalPhoto.substring(0, Math.min(50, finalPhoto.length())) : "null"));

                                Toast.makeText(
                                        AccountSettingsActivity.this,
                                        getString(R.string.profile_saved),
                                        Toast.LENGTH_SHORT
                                ).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                btnSave.setEnabled(true);

                                android.util.Log.e("AccountSettings", "Failed to save to Firestore", e);
                                Toast.makeText(
                                        AccountSettingsActivity.this,
                                        getString(R.string.profile_sync_failed),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);

                    android.util.Log.e("AccountSettings", "Failed to update profile", e);
                    Toast.makeText(
                            AccountSettingsActivity.this,
                            getString(R.string.profile_update_failed),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void showUploadFailedDialog(String errorMessage, String name, String phone) {
        // Don't expose the raw errorMessage to users; use a generic message. Log already contains detail.
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Gagal Mengunggah Foto")
                .setMessage(getString(R.string.upload_photo_failed_generic))
                .setPositiveButton("Simpan tanpa foto", (dialog, which) -> updateUserProfile(name, phone, null))
                .setNegativeButton("Coba lagi", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    btnSave.setEnabled(false);
                    uploadImageAndSave(name, phone);
                })
                .setNeutralButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
