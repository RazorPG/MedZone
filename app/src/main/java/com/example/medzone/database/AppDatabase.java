package com.example.medzone.database;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {HistoryEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract HistoryDao historyDao();

    // Migration from version 1 to 2: add any missing columns to preserve existing data
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Query existing columns using try-with-resources to ensure cursor is closed
            try (Cursor cursor = database.query("PRAGMA table_info('history')")) {
                boolean hasDiagnosis = false;
                boolean hasSynced = false;
                boolean hasQuickChips = false;
                boolean hasRekomendasiJson = false;
                boolean hasTimestamp = false;

                int nameIndex = -1;
                while (cursor != null && cursor.moveToNext()) {
                    if (nameIndex == -1) {
                        nameIndex = cursor.getColumnIndex("name");
                    }
                    if (nameIndex >= 0) {
                        String name = cursor.getString(nameIndex);
                        if ("diagnosis".equals(name)) hasDiagnosis = true;
                        if ("syncedToFirebase".equals(name)) hasSynced = true;
                        if ("quickChips".equals(name)) hasQuickChips = true;
                        if ("rekomendasiJson".equals(name)) hasRekomendasiJson = true;
                        if ("timestamp".equals(name)) hasTimestamp = true;
                    }
                }

                if (!hasDiagnosis) {
                    database.execSQL("ALTER TABLE history ADD COLUMN diagnosis TEXT");
                }
                if (!hasSynced) {
                    // INTEGER used for boolean values: 0 = false, 1 = true
                    database.execSQL("ALTER TABLE history ADD COLUMN syncedToFirebase INTEGER NOT NULL DEFAULT 0");
                }
                if (!hasQuickChips) {
                    database.execSQL("ALTER TABLE history ADD COLUMN quickChips TEXT");
                }
                if (!hasRekomendasiJson) {
                    database.execSQL("ALTER TABLE history ADD COLUMN rekomendasiJson TEXT");
                }
                if (!hasTimestamp) {
                    database.execSQL("ALTER TABLE history ADD COLUMN timestamp INTEGER");
                }

            }
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "medzone_database"
                    )
                    .addMigrations(MIGRATION_1_2)
                    // If schema changed between app versions and no explicit migration covers it,
                    // this will clear and recreate the database to avoid RoomIllegalStateException.
                    // For production apps where preserving user data is important, implement proper
                    // Migration objects and remove this fallback.
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
