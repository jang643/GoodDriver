package com.hojang.gooddriver.model

data class LocationHospital(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val phoneNumber: String
    // 다른 필요한 정보들 추가
)