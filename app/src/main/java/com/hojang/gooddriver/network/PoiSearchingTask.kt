package com.hojang.gooddriver.network

import android.util.Log
import com.hojang.gooddriver.MyApplication
import com.hojang.gooddriver.model.LocationHospital
import com.hojang.gooddriver.model.UserSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class PoiSearchingTask {
    private val apiResponsesStorage: ApiResponsesStorage = ApiResponsesStorage(MyApplication.applicationContext())

    companion object {
        private const val API_URL = "https://apis.openapi.sk.com/tmap/pois?"
        private const val API_KEY = "RMc5Qg1uEg21fq92oS0Lq8iabyxPq6kiMdgOjdh9"
        private const val CIRCLE_SIZE = UserSetting.circle_size
    }

    interface PoiSearchingListener {
        fun onPoiSearchingResult(hospitals: List<LocationHospital>)
        fun onPoiSearchingError(message: String)
    }

    private suspend fun execute(latitude: Double, longitude: Double): List<LocationHospital>? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(
                    API_URL +
                            "version=1" +
                            "&page=1" +
                            "&count=30" +
                            "&searchKeyword=%EB%B3%91%EC%9B%90" +
                            "&resCoordType=WGS84GEO" +
                            "&searchType=all" +
                            "&searchtypCd=A" +
                            "&radius=$CIRCLE_SIZE" +
                            "&reqCoordType=WGS84GEO" +
                            "&centerLon=$longitude" +
                            "&centerLat=$latitude" +
                            "&multiPoint=Y" +
                            "&callback=result"+
                            "&appKey=$API_KEY"  // 본인의 API 키로 교체
                )

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("appKey", API_KEY)
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }

                    reader.close()
                    connection.disconnect()

                    // JSON 파싱
                    val jsonResponse = JSONObject(response.toString())
                    val pois = jsonResponse.optJSONObject("searchPoiInfo")?.optJSONObject("pois")?.optJSONArray("poi")

                    if (pois != null) {
                        val hospitalList = mutableListOf<LocationHospital>()

                        for (i in 0 until pois.length()) {
                            val poiJson = pois.optJSONObject(i)
                            val lowerBizName = poiJson.optString("lowerBizName")

                            // "lowerBizName"이 "의원"인 경우에만 저장
                            if (lowerBizName == "의원") {
                                // 주소 정보 가져오기
                                val newAddressArray = poiJson.optJSONObject("newAddressList")?.optJSONArray("newAddress")
                                val fullAddressRoad = newAddressArray?.optJSONObject(0)?.optString("fullAddressRoad")

                                val hospital = fullAddressRoad?.let {
                                    LocationHospital(
                                        id = poiJson.optString("id"),
                                        name = poiJson.optString("name"),
                                        latitude = poiJson.optDouble("noorLat"),
                                        longitude = poiJson.optDouble("noorLon"),
                                        address = it,
                                        phoneNumber = poiJson.optString("telNo")
                                        // 다른 필요한 정보들 추가
                                    )
                                }
                                if (hospital != null) {
                                    hospitalList.add(hospital)
                                }
                            }
                        }

                        // ApiResponsesStorage를 사용하여 API 응답 저장
                        apiResponsesStorage.insertResponse(response.toString())
                        hospitalList
                    } else {
                        Log.e("com.example.gooddriver.network.PoiSearchingTask", "Pois not found in JSON response")
                        null
                    }

                } else {
                    Log.e("com.example.gooddriver.network.PoiSearchingTask", "HTTP error code: $responseCode")
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun executeAsync(
        latitude: Double,
        longitude: Double,
        listener: PoiSearchingListener
    ) {
        val result = execute(latitude, longitude)
        if (result != null) {
            listener.onPoiSearchingResult(result)

            // 추가: close 호출
            apiResponsesStorage.close()
        } else {
            listener.onPoiSearchingError("Failed to retrieve hospital information.")
        }
    }
}
