package com.ist.pharmacist.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [MedicineCacheData::class], version = 1)
@TypeConverters(Converters::class)
abstract class MedicineDatabase: RoomDatabase(){

    abstract fun medicineDao(): MedicineDao


    companion object{
        @Volatile
        private var instance: MedicineDatabase? = null


        fun getDatabase(context: Context): MedicineDatabase{
            return instance ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedicineDatabase::class.java,
                    "medicine_database"
                ).build()
                this.instance = instance
                instance
            }
        }
    }

}