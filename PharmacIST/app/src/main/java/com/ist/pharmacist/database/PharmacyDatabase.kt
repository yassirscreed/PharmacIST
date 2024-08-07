package com.ist.pharmacist.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [PharmacyCacheData::class], version = 1)
@TypeConverters(Converters::class)
abstract class PharmacyDatabase : RoomDatabase() {

    abstract fun pharmacyDao(): PharmacyDao

    companion object {
        @Volatile
        private var instance: PharmacyDatabase? = null

        fun getDatabase(context: Context): PharmacyDatabase {
            return instance ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PharmacyDatabase::class.java,
                    "pharmacy_database"
                ).build()
                this.instance = instance
                instance
            }
        }
    }

}