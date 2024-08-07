package com.ist.pharmacist.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MedicineDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMedicine(medicineCacheData: MedicineCacheData)

    @Query("SELECT * FROM medicines WHERE barcode = :barcode")
    suspend fun getMedicine(barcode: String): MedicineCacheData?

    @Query("SELECT * FROM medicines")
    suspend fun getAllMedicines(): List<MedicineCacheData>
}