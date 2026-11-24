package com.kamarajlaw.mactmedlegal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CaseEntity::class,
        InjuryPinEntity::class,
        LegalHeirEntity::class,
        SceneItemEntity::class,
        CaseDocumentEntity::class,
        CaseExpenseEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mcopDao(): McopDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mact_medlegal_pro_v3.db"
                ).build().also { INSTANCE = it }
            }
    }
}
