package com.example.dailyplanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PlanItem::class, PlanTemplate::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v1 → v2: 去掉 isCompleted，新增 title
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plan_items ADD COLUMN title TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE plan_items DROP COLUMN isCompleted")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daily_planner.db"
                ).addMigrations(MIGRATION_1_2)
                 .fallbackToDestructiveMigrationOnDowngrade()
                 .build().also { INSTANCE = it }
            }
        }
    }
}
