package com.ist.pharmacist.database

class MedicineRepository(private val medicineDao: MedicineDao) {

        suspend fun insertMedicine(medicineCacheData: MedicineCacheData) {
            medicineDao.insertMedicine(medicineCacheData)
        }

        suspend fun getMedicine(barcode: String): MedicineCacheData? {
            return medicineDao.getMedicine(barcode)
        }

        suspend fun getAllMedicines(): List<MedicineCacheData> {
            return medicineDao.getAllMedicines()
        }
}