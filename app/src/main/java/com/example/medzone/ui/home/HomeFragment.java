package com.example.medzone.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.medzone.api.ApiClient;
import com.example.medzone.api.ApiService;
import com.example.medzone.api.PredictionResponse;
import com.example.medzone.model.Recommendation;
import com.example.medzone.viewmodel.HistoryViewModel;
import com.example.medzone.utils.UserPreferences;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.example.medzone.R;
import com.example.medzone.activities.AccountSettingsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private EditText inputKeluhan;
    private ChipGroup chipGroupKeluhan;
    private MaterialButton btnSearchObat;
    private MaterialCardView resultCard;
    private TextView resultObatName, resultObatDosis;
    private HistoryViewModel historyViewModel;
    private String selectedQuickChip = null; // track which quick chip was clicked
    private ImageView profileIcon;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private UserPreferences userPrefs;

    public HomeFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        historyViewModel = new ViewModelProvider(requireActivity()).get(HistoryViewModel.class);

        // Initialize UserPreferences
        userPrefs = new UserPreferences(requireContext());

        inputKeluhan = view.findViewById(R.id.inputKeluhan);
        chipGroupKeluhan = view.findViewById(R.id.chipGroupKeluhan);
        btnSearchObat = view.findViewById(R.id.btnSearchObat);
        resultCard = view.findViewById(R.id.resultCard);
        resultObatName = view.findViewById(R.id.result_obat_name);
        resultObatDosis = view.findViewById(R.id.result_obat_dosis);

        // Profile icon and Firebase
        profileIcon = view.findViewById(R.id.profileIcon);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Load profile photo from cache or Firebase
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) loadProfilePhoto(user);

        // Open account settings when profile icon clicked
        if (profileIcon != null) {
            profileIcon.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AccountSettingsActivity.class);
                startActivity(intent);
            });
        }

        // Chip click behavior: set inputKeluhan and track selected quick chip
        if (chipGroupKeluhan != null && inputKeluhan != null) {
            for (int i = 0; i < chipGroupKeluhan.getChildCount(); i++) {
                View child = chipGroupKeluhan.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    chip.setCheckable(true);
                    chip.setOnClickListener(v -> {
                        String current = inputKeluhan.getText() == null ? "" : inputKeluhan.getText().toString();
                        String chipText = chip.getText() == null ? "" : chip.getText().toString();

                        if (current.equals(chipText) && chip.isChecked()) {
                            // If clicking the same checked chip, deselect
                            chip.setChecked(false);
                            inputKeluhan.setText("");
                            selectedQuickChip = null;
                        } else {
                            // Uncheck other chips
                            for (int j = 0; j < chipGroupKeluhan.getChildCount(); j++) {
                                View other = chipGroupKeluhan.getChildAt(j);
                                if (other instanceof Chip) ((Chip) other).setChecked(false);
                            }
                            chip.setChecked(true);
                            inputKeluhan.setText(chipText);
                            inputKeluhan.setSelection(inputKeluhan.getText().length());
                            selectedQuickChip = chipText;
                        }
                    });
                }
            }
        }

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
                    if (!text.isEmpty() && resultCard != null) {
                        resultCard.setVisibility(View.GONE);
                    }
                    // If user types manually, clear selected quick chip
                    if (selectedQuickChip != null && !text.equals(selectedQuickChip)) {
                        selectedQuickChip = null;
                        // uncheck all chips
                        if (chipGroupKeluhan != null) {
                            for (int j = 0; j < chipGroupKeluhan.getChildCount(); j++) {
                                View other = chipGroupKeluhan.getChildAt(j);
                                if (other instanceof Chip) ((Chip) other).setChecked(false);
                            }
                        }
                    }
                }
            });

            btnSearchObat.setOnClickListener(v -> {
                String keluhan = inputKeluhan.getText() == null ? "" : inputKeluhan.getText().toString().trim();
                if (keluhan.isEmpty()) return;

                btnSearchObat.setEnabled(false);
                if (resultCard != null) resultCard.setVisibility(View.VISIBLE);
                if (resultObatName != null) resultObatName.setText("Memuat rekomendasi...");
                if (resultObatDosis != null) resultObatDosis.setText("");

                // Save keluhan for later
                final String finalKeluhan = keluhan;

                // Collect selected quick chips (we only add if text equals keluhan)
                List<String> quickChips = new ArrayList<>();
                if (chipGroupKeluhan != null) {
                    for (int i = 0; i < chipGroupKeluhan.getChildCount(); i++) {
                        View child = chipGroupKeluhan.getChildAt(i);
                        if (child instanceof Chip) {
                            Chip chip = (Chip) child;
                            if (chip.getText() != null && chip.getText().toString().equals(keluhan) && chip.isChecked()) {
                                quickChips.add(chip.getText().toString());
                            }
                        }
                    }
                }
                final List<String> finalQuickChips = quickChips;

                // Clear input and hide keyboard
                inputKeluhan.setText("");
                inputKeluhan.clearFocus();
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(inputKeluhan.getWindowToken(), 0);

                try {
                    ApiService apiService = ApiClient.getClient().create(ApiService.class);
                    JSONObject json = new JSONObject();
                    json.put("keluhan", finalKeluhan);
                    RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), json.toString());
                    Call<PredictionResponse> call = apiService.getPrediction(body);
                    call.enqueue(new Callback<PredictionResponse>() {
                        @Override
                        public void onResponse(Call<PredictionResponse> call, Response<PredictionResponse> response) {
                            btnSearchObat.setEnabled(true);
                            if (response.isSuccessful() && response.body() != null) {
                                PredictionResponse resp = response.body();

                                // Log raw response
                                android.util.Log.d("HomeFragment", "API Response - Obat: '" + resp.getObat() + "' Dosis: '" + resp.getDosis() + "' Diagnosis: '" + resp.getDiagnosis() + "'");

                                String obat = resp.getObat() == null ? "-" : resp.getObat();
                                String dosis = resp.getDosis() == null ? "-" : resp.getDosis();

                                android.util.Log.d("HomeFragment", "After fallback - Obat: '" + obat + "' Dosis: '" + dosis + "'");

                                if (resultObatName != null) resultObatName.setText(obat);
                                if (resultObatDosis != null) resultObatDosis.setText(dosis);
                                if (resultCard != null) resultCard.setVisibility(View.VISIBLE);

                                // Determine diagnosis to save: prefer selected quick chip over API diagnosis
                                String apiDiagnosis = resp.getDiagnosis() == null ? "" : resp.getDiagnosis();
                                String diagnosisToSave = (selectedQuickChip != null && !selectedQuickChip.isEmpty()) ? selectedQuickChip : apiDiagnosis;

                                // ✅ Simpan ke local database (akan otomatis sync ke Firebase di background)
                                List<Recommendation> rekomendasi = new ArrayList<>();
                                Recommendation rec = new Recommendation(obat, dosis, "");
                                rekomendasi.add(rec);

                                android.util.Log.d("HomeFragment", "Saving recommendation - name: '" + rec.name + "' dosis: '" + rec.dosis + "'");

                                historyViewModel.saveHistory(finalKeluhan, finalQuickChips, rekomendasi, diagnosisToSave);

                                // reset selection after saving
                                selectedQuickChip = null;
                                if (chipGroupKeluhan != null) {
                                    for (int j = 0; j < chipGroupKeluhan.getChildCount(); j++) {
                                        View other = chipGroupKeluhan.getChildAt(j);
                                        if (other instanceof Chip) ((Chip) other).setChecked(false);
                                    }
                                }
                            } else {
                                if (resultObatName != null) resultObatName.setText("Gagal: respons tidak valid dari server");
                            }
                        }

                        @Override
                        public void onFailure(Call<PredictionResponse> call, Throwable t) {
                            btnSearchObat.setEnabled(true);
                            if (resultObatName != null) resultObatName.setText("Gagal memanggil API: " + t.getMessage());
                        }
                    });
                } catch (Exception e) {
                    btnSearchObat.setEnabled(true);
                    if (resultCard != null) resultCard.setVisibility(View.VISIBLE);
                    if (resultObatName != null) resultObatName.setText("Exception: " + e.getMessage());
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Only refresh from Firebase if data is stale
            if (userPrefs.needsRefresh()) {
                loadProfilePhotoFromFirebase(user);
            } else {
                loadProfilePhotoFromCache();
            }
        }
    }

    private void loadProfilePhoto(FirebaseUser user) {
        // Try cache first
        if (userPrefs.hasUserData() && !userPrefs.needsRefresh()) {
            loadProfilePhotoFromCache();
        } else {
            loadProfilePhotoFromFirebase(user);
        }
    }

    /**
     * Load profile photo from cache
     */
    private void loadProfilePhotoFromCache() {
        String photoUrl = userPrefs.getUserPhotoUrl();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            loadPhotoUrl(photoUrl, profileIcon);
        } else {
            profileIcon.setImageResource(R.drawable.ic_profil);
            androidx.core.widget.ImageViewCompat.setImageTintList(profileIcon,
                android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)));
        }
    }

    /**
     * Load profile photo from Firebase
     */
    private void loadProfilePhotoFromFirebase(FirebaseUser user) {
        String uid = user.getUid();
        // Try Firestore users collection first
        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            String photoUrl = null;

            if (documentSnapshot.exists()) {
                photoUrl = documentSnapshot.getString("photoUrl");
            }

            // Fallback to FirebaseUser photo
            if ((photoUrl == null || photoUrl.isEmpty()) && user.getPhotoUrl() != null) {
                photoUrl = user.getPhotoUrl().toString();
            }

            // Update cache if different from current cache
            String cachedPhotoUrl = userPrefs.getUserPhotoUrl();
            if (photoUrl != null && !photoUrl.equals(cachedPhotoUrl)) {
                // Photo changed, update cache
                String name = userPrefs.getUserName();
                String email = user.getEmail();
                String phone = userPrefs.getUserPhone();
                long joinDate = userPrefs.getJoinDate();

                if (documentSnapshot.exists()) {
                    String firestoreName = documentSnapshot.getString("name");
                    if (firestoreName != null && !firestoreName.isEmpty()) {
                        name = firestoreName;
                    }
                    String firestorePhone = documentSnapshot.getString("phone");
                    if (firestorePhone != null && !firestorePhone.isEmpty()) {
                        phone = firestorePhone;
                    }
                }

                if (name == null || name.isEmpty()) {
                    name = user.getDisplayName();
                }
                if (joinDate == 0 && user.getMetadata() != null) {
                    joinDate = user.getMetadata().getCreationTimestamp();
                }

                userPrefs.saveUserData(uid, name, email, phone, photoUrl, joinDate);
            }

            // Load photo
            if (photoUrl != null && !photoUrl.isEmpty()) {
                loadPhotoUrl(photoUrl, profileIcon);
            } else {
                profileIcon.setImageResource(R.drawable.ic_profil);
                androidx.core.widget.ImageViewCompat.setImageTintList(profileIcon,
                    android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)));
            }
        }).addOnFailureListener(e -> {
            // If Firestore fails, try to load from cache
            loadProfilePhotoFromCache();
        });
    }

    /**
     * Load photo from URL or Base64 data URI
     */
    private void loadPhotoUrl(String photoUrl, ImageView imageView) {
        if (photoUrl == null || photoUrl.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_profil);
            // Set tint putih untuk icon default
            androidx.core.widget.ImageViewCompat.setImageTintList(imageView,
                android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)));
            return;
        }

        // Check if it's a Base64 data URI
        if (photoUrl.startsWith("data:image/")) {
            try {
                // Extract Base64 part
                String base64String = photoUrl.substring(photoUrl.indexOf(",") + 1);
                byte[] decodedBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                if (bitmap != null) {
                    // Apply circular crop to bitmap
                    android.graphics.Bitmap circularBitmap = getCircularBitmap(bitmap);

                    // PENTING: Hapus tint sebelum set foto profil asli
                    androidx.core.widget.ImageViewCompat.setImageTintList(imageView, null);

                    imageView.setImageBitmap(circularBitmap);
                } else {
                    imageView.setImageResource(R.drawable.ic_profil);
                    // Set tint putih untuk icon default
                    androidx.core.widget.ImageViewCompat.setImageTintList(imageView,
                        android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)));
                }
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Failed to decode Base64 image: " + e.getMessage());
                imageView.setImageResource(R.drawable.ic_profil);
                // Set tint putih untuk icon default
                androidx.core.widget.ImageViewCompat.setImageTintList(imageView,
                    android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)));
            }
        } else {
            // Regular URL, use Glide
            try {
                // Hapus tint sebelum load dengan Glide
                androidx.core.widget.ImageViewCompat.setImageTintList(imageView, null);

                Glide.with(requireContext())
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_profil)
                    .circleCrop()
                    .into(imageView);
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Failed to load image with Glide: " + e.getMessage());
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
}
