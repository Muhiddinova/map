package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.directions.route.Route
import com.directions.route.RouteException
import com.directions.route.Routing
import com.directions.route.RoutingListener
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener,
    GoogleMap.InfoWindowAdapter, GoogleMap.OnMyLocationButtonClickListener, RoutingListener {

    private val TAG = "MapsActivity"

    private lateinit var mMap: GoogleMap
    private var sIsPermissionGranted = false
    private var mLocation: Location? = null

    private lateinit var placesClient: PlacesClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        Places.initialize(applicationContext, resources.getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        sIsPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

//        setSearch()

    }

//    private fun setSearch() {
//
//        val placeFields = listOf(Place.Field.NAME)
//
//        val search =
//            supportFragmentManager.findFragmentById(R.id.search) as AutocompleteSupportFragment
//        search.setCountries("UZ")
//        search.setHint("Qidirish")
//        search.setPlaceFields(placeFields)
//        search.setTypeFilter(TypeFilter.ADDRESS)
//        search.setOnPlaceSelectedListener(object : PlaceSelectionListener {
//            override fun onPlaceSelected(place: Place) {
//                Toast.makeText(
//                    this@MapsActivity,
//                    "selected place: ${place.name.orEmpty()}",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//
//            override fun onError(status: Status) {
//                Log.e(TAG, "onError: $status")
//            }
//
//        })
//    }

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

        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isMapToolbarEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.isTrafficEnabled = true
        mMap.setOnMapClickListener(this)
        mMap.setInfoWindowAdapter(this)
        mMap.setOnMyLocationButtonClickListener(this)
        enableLocation()
    }

    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        if (sIsPermissionGranted) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.setOnMyLocationClickListener {
                mLocation = it
            }
        } else {
            requestPermission()
        }

    }

    private fun requestPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            this.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                        sIsPermissionGranted = true
                        enableLocation()
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    override fun onMapClick(latLng: LatLng?) {
        mMap.clear()
        mMap.addMarker(latLng?.let {
            MarkerOptions().position(it)
                .title("marker")
        })
        mMap.animateCamera(
            CameraUpdateFactory.newLatLng(latLng),
            1000,
            object : GoogleMap.CancelableCallback {
                override fun onFinish() {
                    Toast.makeText(
                        this@MapsActivity,
                        "move camera finish",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }

                override fun onCancel() {
                    Toast.makeText(
                        this@MapsActivity,
                        "move camera cancelled",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        drawRoute(latLng)
    }

    private fun drawRoute(latLng: LatLng?) {
        if (mLocation != null && latLng != null) {

            val routing = Routing.Builder()
                .alternativeRoutes(true)
                .language("en")
                .withListener(this)
                .waypoints(LatLng(mLocation!!.latitude, mLocation!!.longitude),latLng)
                .key(resources.getString(R.string.google_maps_key))
                .build()
            routing.execute()
        }
    }

    override fun getInfoWindow(marker: Marker?): View? {
        return null
    }

    override fun getInfoContents(marker: Marker?): View {
        val infoWindow =
            layoutInflater.inflate(R.layout.marker_details, findViewById(R.id.map), false)
        infoWindow.findViewById<TextView>(R.id.text_title).text = marker?.title.orEmpty()
        return infoWindow
    }

    override fun onMyLocationButtonClick(): Boolean {

        getPlacesInfo()
        return false
    }

    @SuppressLint("MissingPermission")
    private fun getPlacesInfo() {
        val list = arrayListOf<String>()

        val placeFiles =
            arrayListOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.USER_RATINGS_TOTAL)

        val request = FindCurrentPlaceRequest.newInstance(placeFiles)

        val placeResult = placesClient.findCurrentPlace(request)

        placeResult.addOnCompleteListener { response ->
            Log.d(TAG, "getPlacesInfo: ${response.isSuccessful}")
            if (response.isSuccessful) {
                val result = response.result
                if (result != null) {
                    Log.d(TAG, "getPlacesInfo: ${result.placeLikelihoods.size}")

                    repeat(result.placeLikelihoods.size) {

                        result.placeLikelihoods[it]
                            .place
                            .address?.let { it1 -> list.add(it1) }
                    }
                }
                Log.d("MapsActivity", "getPlacesInfo: ${list.toString()}")
            } else {
                Log.e("MapsActivity", "getPlacesInfo: ${response.exception}")
            }
        }

    }

    override fun onRoutingFailure(exception: RouteException?) {
        Log.e(TAG, "onRoutingFailure: $exception")
    }

    override fun onRoutingStart() {
        Log.d(TAG, "onRoutingStart: start")
    }

    override fun onRoutingSuccess(list: ArrayList<Route>?, p1: Int) {
        Log.d(TAG, "onRoutingSuccess: $p1")
        val options = PolylineOptions()

        list?.let {
            repeat(list.size) { i ->
                options.color(Color.BLUE)
                options.addAll(list[i].points)
                options.width((10 + 3 * i).toFloat())

                list
                val polyline = mMap.addPolyline(options)
            }
        }
    }

    override fun onRoutingCancelled() {
        Log.d(TAG, "onRoutingCancelled: routing cancelled")
    }

}