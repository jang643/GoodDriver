package com.hojang.gooddriver

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hojang.gooddriver.databinding.ActivityMainBinding
import com.hojang.gooddriver.model.LocationDAO
import com.hojang.gooddriver.model.LocationHospital
import com.hojang.gooddriver.model.UserSetting
import com.hojang.gooddriver.network.PoiSearchingTask
import com.hojang.gooddriver.network.ReverseGeocodingTask
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapCircle
import com.skt.tmap.overlay.TMapMarkerItem
import com.skt.tmap.overlay.TMapMarkerItem2
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 1
    private var previousLatitude: Double = 0.0
    private var previousLongitude: Double = 0.0
    private lateinit var tMapView: TMapView

    private val bottomSheetLayout by lazy { findViewById<LinearLayout>(R.id.bottom_sheet_layout) }
    private val bottomSheetUserButton by lazy { findViewById<Button>(R.id.btnUser) }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        //setResultSignUp()

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        } else {
            initializeTMapView()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        with(binding) {
            btnCurr.setOnClickListener {
                lifecycleScope.launch {
                    setCurrLocationAsync()
                }
            }
            btnZoomIn.setOnClickListener {
                lifecycleScope.launch {
                    tMapView.mapZoomIn()
                }
            }
            btnZoomOut.setOnClickListener {
                lifecycleScope.launch {
                    tMapView.mapZoomOut()
                }
            }
            /*btnSignIn.setOnClickListener {
                signIn()
            }

            btnSignOut.setOnClickListener {
                signOut()
            }*/
        }

        setContentView(binding.root)
        initializePersistentBottomSheet()
        persistentBottomSheetEvent()
    }

    private fun requestLocationUpdates() {
        // 위치 업데이트를 받기 위한 LocationListener 설정
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val newLatitude = location.latitude
                val newLongitude = location.longitude
                if (newLatitude != previousLatitude || newLongitude != previousLongitude) {
                    previousLatitude = newLatitude
                    previousLongitude = newLongitude
                    UserSetting.userLatitude = newLatitude
                    UserSetting.userLongitude = newLongitude
                    Log.d("LocationUpdate", "Latitude: $previousLatitude, Longitude: $previousLongitude")
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                // 위치 공급자의 상태가 변경될 때 호출되는 메서드
            }
            override fun onProviderEnabled(provider: String) {
                // 위치 공급자가 사용 가능해질 때 호출되는 메서드
            }
            override fun onProviderDisabled(provider: String) {
                // 위치 공급자가 사용 불가능해질 때 호출되는 메서드
            }
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0,
                0f,
                locationListener
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }


    private fun initializePersistentBottomSheet() {
        // null 체크
        bottomSheetLayout?.let { layout ->
            bottomSheetBehavior = BottomSheetBehavior.from(layout)
        }
    }
    private fun persistentBottomSheetEvent() {
        bottomSheetUserButton.setOnClickListener {
            getCurrentUserProfile()
        }
    }

    private suspend fun reverseGeocodingAsync(latitude: Double, longitude: Double): LocationDAO? {
        val reverseGeocodingTask = ReverseGeocodingTask()
        return reverseGeocodingTask.execute(latitude, longitude)
    }


    private suspend fun setCurrLocationAsync() {
        val locationDAO = reverseGeocodingAsync(previousLatitude, previousLongitude)
        // 마커 생성
        tMapView.zoomLevel = 15
        markPOI()

        //위치 연동
        locationDAO?.let {
            val reverseGeocodingTask = ReverseGeocodingTask()
            reverseGeocodingTask.executeAsync(previousLatitude, previousLongitude, object : ReverseGeocodingTask.ReverseGeocodingListener {
                override fun onReverseGeocodingResult(locationDAO: LocationDAO?) {
                    val fullAddress = locationDAO?.addressInfo?.fullAddress
                    if (!fullAddress.isNullOrBlank()) {
                        val currLocationFormat = getString(R.string.curr_location)
                        val extractedPart = extractLastPart(fullAddress)
                        val formattedCurrLocation = String.format(currLocationFormat, extractedPart)
                        findViewById<TextView>(R.id.currLocation).text = formattedCurrLocation
                    }
                }
                override fun onReverseGeocodingError(message: String) {
                    // 에러를 처리하는 코드
                }
            })
        }
    }

    private suspend fun markPOI() {
        tMapView.removeTMapMarkerItem("current_location")
        for(i in 0..UserSetting.markercount){
            tMapView.removeTMapMarkerItem("hospital_${i}")
        }
        val marker = TMapMarkerItem()
        marker.id = "current_location"
        val iconBitmap = BitmapFactory.decodeResource(resources, R.drawable.poi)
        if (iconBitmap != null) {
            marker.icon = iconBitmap
            marker.setTMapPoint(previousLatitude, previousLongitude)
            tMapView.addTMapMarkerItem(marker)
        } else {
            Log.e("Bitmap Error", "Failed to load bitmap from resources")
        }

        val poiSearchingTask = PoiSearchingTask()

        val poiSearchingListener = object : PoiSearchingTask.PoiSearchingListener {
            override fun onPoiSearchingResult(hospitals: List<LocationHospital>) {
                var i = 0
                for (poi in hospitals) {
                    val hospitalMarker = TMapMarkerItem()
                    hospitalMarker.id = "hospital_${i}"
                    hospitalMarker.canShowCallout = true
                    hospitalMarker.calloutTitle = poi.name
                    hospitalMarker.calloutSubTitle = poi.address
                    Log.d("PoiSearchingTask", "POI Name: ${poi.name}, id: ${i}, address: ${poi.address}")


                    val hospitalIconBitmap = BitmapFactory.decodeResource(resources, R.drawable.hospital_dot)

                    if (hospitalIconBitmap != null) {
                        hospitalMarker.icon = hospitalIconBitmap
                        hospitalMarker.setTMapPoint(poi.latitude, poi.longitude)
                        tMapView.addTMapMarkerItem(hospitalMarker)

                    } else {
                        Log.e("Bitmap Error", "Failed to load hospital icon bitmap from resources")
                    }

                    i += 1
                }
                UserSetting.markercount = i
            }

            override fun onPoiSearchingError(message: String) {
                Log.e("PoiSearchingTask", "Error: $message")
            }
        }

        // 코루틴 스코프 내에서 async 사용
        coroutineScope {
            async {
                poiSearchingTask.executeAsync(previousLatitude, previousLongitude, poiSearchingListener)
            }

            tMapView.setCenterPoint(previousLatitude, previousLongitude)

            tMapView.removeTMapCircle("circle")
            val circle = TMapCircle("circle", previousLatitude, previousLongitude)
            circle.lineColor = Color.BLACK
            circle.circleWidth = 1F
            circle.radiusVisible = false;
            circle.radius = 500.0
            circle.areaColor = Color.GRAY
            circle.areaAlpha = 10
            tMapView.addTMapCircle(circle)


        }


    }


    fun extractLastPart(fullAddress: String): String {
        val parts = fullAddress.split(",")
        return parts.last().trim()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeTMapView()
            } else {
                showToast("위치 권한이 거부되었습니다.")
            }
        }
    }

    private fun initializeTMapView() {
        // TMapView 초기화
        tMapView = TMapView(this)
        val container = binding.tmapViewContainer
        container.addView(tMapView)
        tMapView.setSKTMapApiKey("RMc5Qg1uEg21fq92oS0Lq8iabyxPq6kiMdgOjdh9")

        requestLocationUpdates()
    }



    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setResultSignUp() {
        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val task: Task<GoogleSignInAccount> =
                        GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    handleSignInResult(task)
                }
            }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val email = account?.email.toString()

            val displayName = account?.displayName.toString()
            val photoUrl = account?.photoUrl.toString()

            Log.d("로그인한 유저의 이메일", email)
            Log.d("로그인한 유저의 전체이름", displayName)
            Log.d("로그인한 유저의 프로필 사진의 주소", photoUrl)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("failed", "signInResult:failed code=" + e.statusCode)
        }
    }

    private fun signIn() {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        resultLauncher.launch(signInIntent)
    }

    private fun signOut() {
        mGoogleSignInClient.signOut()
            .addOnCompleteListener(this) {
                // ...
            }
    }

    private fun revokeAccess() {
        mGoogleSignInClient.revokeAccess()
            .addOnCompleteListener(this) {
                // ...
            }
    }

    private fun getCurrentUserProfile() {
        val curUser = GoogleSignIn.getLastSignedInAccount(this)
        curUser?.let {
            val email = curUser.email.toString()
            val displayName = curUser.displayName.toString()
            val photoUrl = curUser.photoUrl.toString()

            Log.d("현재 로그인 되어있는 유저의 이메일", email)
            Log.d("현재 로그인 되어있는 유저의 전체이름", displayName)
        }
    }
}