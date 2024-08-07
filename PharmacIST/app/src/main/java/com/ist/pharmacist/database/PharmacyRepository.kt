package com.ist.pharmacist.database

class PharmacyRepository(private val pharmacyDao: PharmacyDao) {

        suspend fun insertPharmacy(pharmacyCacheData: PharmacyCacheData) {
            pharmacyDao.insertPharmacy(pharmacyCacheData)
        }

        suspend fun getPharmacy(id: String): PharmacyCacheData? {
            return pharmacyDao.getPharmacy(id)
        }

        suspend fun getAllPharmacies(): List<PharmacyCacheData> {
            return pharmacyDao.getAllPharmacies()
        }
}