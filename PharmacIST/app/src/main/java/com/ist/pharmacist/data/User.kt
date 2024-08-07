package com.ist.pharmacist.data

data class User(
    val username: String,
    val email: String,
    val password: String,
    val pharmaciesCreatedByUser: MutableList<String> = mutableListOf(),
    val favoritePharmacies: MutableList<String> = mutableListOf(),
    val pharmaciesFlagged: MutableList<String> = mutableListOf(),
    var suspended: Boolean = false
) {
    fun addFavoritePharmacy(pharmacyId: String) {
        favoritePharmacies.add(pharmacyId)
    }

    fun removeFavoritePharmacy(pharmacyId: String) {
        favoritePharmacies.remove(pharmacyId)
    }

    fun isFavoritePharmacy(pharmacyId: String): Boolean {
        return favoritePharmacies.contains(pharmacyId)
    }

    fun addFlaggedPharmacy(pharmacyId: String) {
        pharmaciesFlagged.add(pharmacyId)
    }

    fun getFlaggedPharmacies(): MutableList<String> {
        return pharmaciesFlagged
    }

    fun addPharmacyCreatedByUser(pharmacyId: String) {
        pharmaciesCreatedByUser.add(pharmacyId)
    }

    fun suspendUser() {
        suspended = true
    }
}