package com.hojang.gooddriver.network

import android.util.Log
import com.hojang.gooddriver.model.LocationDAO
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ReverseGeocodingTask {

    interface ReverseGeocodingListener {
        fun onReverseGeocodingResult(locationDAO: LocationDAO?)
        fun onReverseGeocodingError(message: String)
    }

    companion object {
        private const val API_URL = "https://apis.openapi.sk.com/tmap/geo/reversegeocoding"
        private const val API_KEY = "RMc5Qg1uEg21fq92oS0Lq8iabyxPq6kiMdgOjdh9"
    }

    suspend fun execute(latitude: Double, longitude: Double): LocationDAO? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(
                    "$API_URL?" +
                            "version=1" +
                            "&format=json" +
                            "&coordType=WGS84GEO" +
                            "&addressType=A10" +
                            "&lat=$latitude" +
                            "&lon=$longitude" +
                            "&appkey=$API_KEY"
                )

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

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
                    val gson = Gson()
                    val jsonResponse = JSONObject(response.toString())
                    val addressInfoJson = jsonResponse.optJSONObject("addressInfo")

                    // JSON 파싱 오류 처리
                    if (addressInfoJson != null) {
                        // 변경된 Gson 매핑 부분
                        val locationDAO = gson.fromJson(response.toString(), LocationDAO::class.java)
                        locationDAO
                    } else {
                        Log.e("com.example.gooddriver.network.ReverseGeocodingTask", "AddressInfo not found in JSON response")
                        null
                    }
                } else {
                    Log.e("com.example.gooddriver.network.ReverseGeocodingTask", "HTTP error code: $responseCode")
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
        listener: ReverseGeocodingListener
    ) {
        val result = execute(latitude, longitude)
        if (result != null) {
            listener.onReverseGeocodingResult(result)
        } else {
            listener.onReverseGeocodingError("Failed to retrieve location information.")
            Log.e("com.example.gooddriver.network.ReverseGeocodingTask", "Failed to retrieve location information.")
        }
    }
}
