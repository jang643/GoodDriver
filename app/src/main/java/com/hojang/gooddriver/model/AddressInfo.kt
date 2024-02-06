package com.hojang.gooddriver.model

class AddressInfo {
    val fullAddress: String? = null
    private val addressType: String? = null
    private val city_do: String? = null
    private val gu_gun: String? = null
    private val eup_myun: String? = null
    private val adminDong: String? = null
    private val adminDongCode: String? = null
    private val legalDong: String? = null
    private val legalDongCode: String? = null
    private val ri: String? = null
    private val bunji: String? = null
    private val roadName: String? = null
    private val buildingIndex: String? = null
    private val buildingName: String? = null
    private val mappingDistance: String? = null
    private val roadCode: String? = null

    override fun toString(): String {
        return "AddressInfo{" +
                "fullAddress='" + fullAddress + '\'' +
                ", addressType='" + addressType + '\'' +
                ", city_do='" + city_do + '\'' +
                ", gu_gun='" + gu_gun + '\'' +
                ", eup_myun='" + eup_myun + '\'' +
                ", adminDong='" + adminDong + '\'' +
                ", adminDongCode='" + adminDongCode + '\'' +
                ", legalDong='" + legalDong + '\'' +
                ", legalDongCode='" + legalDongCode + '\'' +
                ", ri='" + ri + '\'' +
                ", bunji='" + bunji + '\'' +
                ", roadName='" + roadName + '\'' +
                ", buildingIndex='" + buildingIndex + '\'' +
                ", buildingName='" + buildingName + '\'' +
                ", mappingDistance='" + mappingDistance + '\'' +
                ", roadCode='" + roadCode + '\'' +
                '}'
    }
}
