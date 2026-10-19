package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [CustomCommand::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

  abstract fun customCommandDao(): CustomCommandDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "durefishan_database"
        )
          .addCallback(object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
              super.onCreate(db)
              // Seed default custom command as specified in prompt
              CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.customCommandDao()?.insertCommand(
                  CustomCommand(
                    commandPhrase = "Open my favourite reels",
                    alternativePhrases = "Meri pasandeeda reels kholo, favourite reels",
                    actionType = "OPEN_DESTINATION",
                    actionConfig = "instagram_reels",
                    requiresConfirmation = false,
                    isEnabled = true
                  )
                )
              }
            }
          })
          .fallbackToDestructiveMigration(true)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
