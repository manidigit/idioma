package com.manidigit.flashlearn.database

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver

object DatabaseProvider {
    @Volatile
    private var instance: FlashLearnDatabase? = null

    fun get(context: Context): FlashLearnDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder<FlashLearnDatabase>(
                context.applicationContext,
                "flashlearn.db"
            )
                .setDriver(AndroidSQLiteDriver())
                .addMigrations(
                    DatabaseMigrations.V1_TO_V2,
                    DatabaseMigrations.V2_TO_V3,
                    DatabaseMigrations.V3_TO_V4,
                    DatabaseMigrations.V4_TO_V5
                )
                .build()
                .also { instance = it }
        }
}
