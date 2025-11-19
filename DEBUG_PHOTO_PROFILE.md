# Debug Guide - Foto Profil Blank di Profile Fragment & Settings

## Masalah
- ✅ Foto profil tampil dan ter-crop dengan benar di **Home**
- ❌ Foto profil blank putih di **ProfileFragment**
- ❌ Foto profil blank putih di **AccountSettingsActivity**

## Langkah Debugging

### 1. Clean & Rebuild Project
```cmd
gradlew clean
gradlew assembleDebug
```

### 2. Uninstall & Reinstall App
- Uninstall aplikasi dari device/emulator
- Install ulang APK yang baru di-build

### 3. Cek Logcat saat buka Profile Tab
Filter logcat dengan tag: `ProfileFragment`

**Log yang diharapkan:**
```
D/ProfileFragment: Loading photo, starts with data:image/: true
D/ProfileFragment: Base64 string length: [angka]
D/ProfileFragment: Decoded bytes length: [angka]
D/ProfileFragment: Bitmap created successfully: 512x512
D/ProfileFragment: Creating circular bitmap from 512x512 to 512x512
D/ProfileFragment: Circular bitmap created: 512x512
D/ProfileFragment: Bitmap set to ImageView successfully
```

**Jika ada error:**
- Cari baris yang mengandung "Failed to decode" atau "Bitmap is null"
- Screenshot log error lengkap

### 4. Cek Logcat saat buka Account Settings
Filter logcat dengan tag: `AccountSettings`

**Log yang diharapkan sama seperti di atas.**

## Kemungkinan Penyebab

### A. ImageView Tidak Memiliki Ukuran yang Valid
Cek XML layout untuk ImageView di:
- `fragment_profile.xml` → `imgAvatar`
- `activity_account_settings.xml` → `imgProfile`

Pastikan:
```xml
<ImageView
    android:id="@+id/imgAvatar"
    android:layout_width="80dp"
    android:layout_height="80dp"
    android:scaleType="centerCrop" />
```

### B. Base64 String Corrupted
Jika log menunjukkan "Bitmap is null after decoding", kemungkinan:
- Base64 string rusak saat penyimpanan
- Format data URI tidak benar

### C. Memory Issues
Jika log menunjukkan OutOfMemoryError:
- Ukuran gambar terlalu besar
- Perlu resize lebih kecil

### D. ImageView di-Override oleh Layout Lain
Jika ImageView tertutup oleh View lain yang background putih.

## Solusi Alternatif

Jika masalah tetap berlanjut, gunakan Glide untuk load Base64:

```java
private void loadPhotoUrl(String photoUrl, ImageView imageView) {
    if (TextUtils.isEmpty(photoUrl)) {
        imageView.setImageResource(R.drawable.ic_profil);
        return;
    }

    // Gunakan Glide untuk semua jenis URL
    try {
        Glide.with(requireContext())
            .load(photoUrl.startsWith("data:image/") ? 
                  Uri.parse(photoUrl) : photoUrl)
            .placeholder(R.drawable.ic_profil)
            .error(R.drawable.ic_profil)
            .circleCrop()
            .into(imageView);
    } catch (Exception e) {
        android.util.Log.e("ProfileFragment", "Failed to load: " + e.getMessage());
        imageView.setImageResource(R.drawable.ic_profil);
    }
}
```

## Testing Steps

1. **Upload foto baru** di Account Settings
2. **Cek Home** → Foto harus tampil bulat
3. **Cek Profile Tab** → Foto harus tampil bulat
4. **Buka Settings lagi** → Foto harus tampil bulat
5. **Screenshot logcat** jika masih blank

## Informasi yang Diperlukan

Jika masalah masih berlanjut, kirim:
1. Screenshot logcat filtered "ProfileFragment"
2. Screenshot logcat filtered "AccountSettings"
3. Screenshot layout (foto blank)
4. Informasi device: Android version, RAM

