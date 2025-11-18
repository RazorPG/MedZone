package com.example.medzone.api;

import com.google.gson.annotations.SerializedName;

public class PredictionResponse {
    // Backend returns 'rekomendasi' for medicine name and 'dosis_rekomendasi' for dosage.
    // Keep getters getObat()/getDosis() used by the UI by mapping these JSON fields here.
    @SerializedName(value = "rekomendasi", alternate = {"obat"})
    private String obat;

    @SerializedName(value = "dosis_rekomendasi", alternate = {"dosis"})
    private String dosis;

    @SerializedName("keluhan")
    private String keluhan;

    @SerializedName("diagnosis")
    private String diagnosis;

    // Optional extra fields (not used yet)
    @SerializedName("keyakinan")
    private String keyakinan;

    @SerializedName("tingkat_keyakinan")
    private String tingkatKeyakinan;

    public String getObat() {
        return obat;
    }
    public String getDosis() {
        return dosis;
    }
    public String getKeluhan() {
        return keluhan;
    }
    public String getDiagnosis() { return diagnosis; }

    // Extra getters if needed in future
    public String getKeyakinan() { return keyakinan; }
    public String getTingkatKeyakinan() { return tingkatKeyakinan; }
}
