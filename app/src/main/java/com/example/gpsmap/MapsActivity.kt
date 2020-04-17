package com.example.gpsmap

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback : MyLocationCallBack
    private val REQUEST_ACCESS_FINE_LOCATION = 1000
    private val polyLineOptions = PolylineOptions().width(5f).color(Color.RED)

    private fun permissionCheck(cancel: () -> Unit, ok:()->Unit ) {
        //위치 권한이 있는지 검사
        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION )!= PackageManager.PERMISSION_GRANTED ){
                //권한이 허용되지 않을때
                if( ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION )) {
                    //이전 권한을 한번 거부한적 있는 경우
                    cancel()
                } else {
                    //권한 요청
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_ACCESS_FINE_LOCATION)
                }
            }
        else {
            ok()
        }

    }

    private fun showPermissionInfoDialog() {
        alert("현재 위치 정보를 얻으려면 위치 권한이 필요합니다", "권한이 필요한 이유") {
            yesButton {
                ActivityCompat.requestPermissions(this@MapsActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_ACCESS_FINE_LOCATION)
            }
            noButton {  }
        }.show()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //화면이 꺼지지 않게 하기
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        //세로 모드로 화면 고정
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationInit()
    }

    override fun onPause() {
        super.onPause()
        removeLocationListener()
    }

    private fun removeLocationListener() {
        //현재 위치 요청을 삭제
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    //위치정볼르 얻기 위한  각종 초기화
    private fun locationInit() {
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        locationCallback = MyLocationCallBack()

        locationRequest = LocationRequest()

        //GPS 우선
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        //업데이트 인터벌
        // 위치 정보가 없을때는 업데이트 안함
        // 상황에 따라 짧아질 수 있음, 정확하지 않음
        // 다른 앱에서 짧은 인터벌로 위치 정보를 요청하면 짧아질 수 있음.
        locationRequest.interval = 10000
        //정확함. 이것보다 짧은 업뎉이트는 하지 않음.
        locationRequest.fastestInterval = 5000
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    override fun onResume() {
        super.onResume()
        permissionCheck( cancel = {
            //위치 정보가 필요한 이유 다이얼로그 표시
            showPermissionInfoDialog()
        } , ok = {
            //현재 위치를 주기적으로 요청 ( 권한 필요 )
            addLocationListener()
        } )

    }

    private fun addLocationListener() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
            locationCallback, null)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when( requestCode ) {
            REQUEST_ACCESS_FINE_LOCATION-> {
                if((grantResults.isNotEmpty()&& grantResults[0] == PackageManager.PERMISSION_GRANTED )) {
                    addLocationListener()
                } else {
                    toast("권한거부됨")
                }
                return
            }
        }
    }

    inner class MyLocationCallBack : LocationCallback() {
        override fun onLocationResult(p0: LocationResult?) {
            super.onLocationResult(p0)

            val location = p0?.lastLocation

            location?.run {
                val latlng = LatLng( latitude , longitude )
                mMap.animateCamera( CameraUpdateFactory.newLatLngZoom(latlng, 17f))
                Log.d("MapsActivity", "위도 : $latitude, 경도 : $longitude")

                polyLineOptions.add(latlng)
                mMap.addPolyline(polyLineOptions)
            }
        }
    }
}
