package com.ist.pharmacist.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PharmacyDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPharmacy(pharmacyCacheData: PharmacyCacheData)

    @Query("SELECT * FROM pharmacies WHERE id = :id")
    suspend fun getPharmacy(id: String): PharmacyCacheData?

    @Query("SELECT * FROM pharmacies")
    suspend fun getAllPharmacies(): List<PharmacyCacheData>
}