package com.example.android.prm391x_googlemap_anhnlfx00594;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    GoogleMap mMap;
    private boolean isLocationPermissionGranted = false;
    private GeoApiContext geoApiContext = null;
    private LatLng userPosition;

    private static final float DEFAULT_ZOOM = 15f;
    private static final String PERMISSION_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 3232;
    private static final int DEFAULT_PADDING = -1;

    public static final String PATH_DISTANCE = "pathDistance";
    public static final String PATH_DURATION = "pathDuration";

    private static final float MARKER_COLOR_BLUE = BitmapDescriptorFactory.HUE_RED;
    private static final float MARKER_COLOR_RED = BitmapDescriptorFactory.HUE_BLUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getLocationPermission();

        if (isLocationPermissionGranted) {
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            supportMapFragment.getMapAsync(this);
        }

        Button button = findViewById(R.id.find_path_button);
        button.setOnClickListener((View v) -> {
            EditText editText1 = findViewById(R.id.origin_address);
            String originAddress = editText1.getText().toString();
            EditText editText2 = findViewById(R.id.destination_address);
            String destinationAddress = editText2.getText().toString();

            geoLocate(originAddress, destinationAddress);
            hideSoftKeyboard();
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (geoApiContext == null) {
            geoApiContext = new GeoApiContext.Builder()
                    .apiKey(getString(R.string.google_directions_key))
                    .queryRateLimit(10)
                    .disableRetries()
                    .build();
        }

        hideSoftKeyboard();
        getCurrentPosition();
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            isLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{PERMISSION_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            isLocationPermissionGranted = true;
        }
    }

    private void getCurrentPosition() {
        // Construct a FusedLocationProviderClient.
        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{PERMISSION_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener((@NonNull Task<Location> task) -> {
            Location currentLocation = task.getResult();
            if (task.isSuccessful() && currentLocation != null) {
                userPosition = toLatLng(currentLocation);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(toLatLng(currentLocation), DEFAULT_ZOOM));
                mMap.setMyLocationEnabled(true);
            } else {
                Toast.makeText(MapsActivity.this, "Unable to find your location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void geoLocate(String originAddress, String destinationAddress) {
        Geocoder geocoder = new Geocoder(this);
        LatLng startPoint = userPosition;
        LatLng endPoint;

        if (!originAddress.isEmpty()) {
            List<Address> originAddressList = new ArrayList<>();
            try {
                originAddressList = geocoder.getFromLocationName(originAddress, 1);
            } catch (IOException e) {
                Log.e("MyDebug", "geoLocate originAddress", e);
            }

            if (originAddressList.size() > 0) {
                startPoint = toLatLng(originAddressList.get(0));

                placeMarker(startPoint, "Origin Address", MARKER_COLOR_RED);
            }
        }

        List<Address> destinationAddressList = new ArrayList<>();
        try {
            destinationAddressList = geocoder.getFromLocationName(destinationAddress, 1);
        } catch (IOException e) {
            Log.e("MyDebug", "geoLocate destinationAddress", e);
        }

        if (destinationAddressList.size() > 0) {
            endPoint = toLatLng(destinationAddressList.get(0));

            placeMarker(endPoint, "Destination Address", MARKER_COLOR_BLUE);
            calculateDirection(startPoint, endPoint);
            moveCameraToPath(startPoint, endPoint);
        }
    }

    private void placeMarker(LatLng latLng, String title, float color) {
        MarkerOptions markerOptions = new MarkerOptions()
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(color))
                .position(latLng);
        mMap.addMarker(markerOptions);
    }

    private void calculateDirection(LatLng originAddress, LatLng destinationAddress) {
        DirectionsApiRequest directionsApiRequest = new DirectionsApiRequest(geoApiContext);
        directionsApiRequest.alternatives(false);
        directionsApiRequest.origin(toGoogleMapsLatLng(originAddress));
        directionsApiRequest.destination(toGoogleMapsLatLng(destinationAddress));
        directionsApiRequest.setCallback(new com.google.maps.PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                Log.v("MyDebug", "Distance: " + result.routes[0].legs[0].distance.toString());
                Log.v("MyDebug", "Duration: " + result.routes[0].legs[0].duration.toString());

                Intent intent = new Intent(MapsActivity.this, MainActivity.class);
                intent.putExtra(PATH_DISTANCE, result.routes[0].legs[0].distance.toString());
                intent.putExtra(PATH_DURATION, result.routes[0].legs[0].duration.toString());
                startActivity(intent);

                DirectionsRoute[] directionsRoute = result.routes;
                List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(directionsRoute[0].overviewPolyline.getEncodedPath());
                List<LatLng> newDecodedPath = new ArrayList<>();

                for (com.google.maps.model.LatLng latLng : decodedPath) {
                    newDecodedPath.add(toLatLng(latLng));
                }

                runOnUiThread(() -> {
                    Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                    polyline.setColor(R.color.colorAccent);
                });
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e("MyDebug", "calculateDirection Failed!", e);
            }
        });
    }

    /*private void drawPath(DirectionsResult result) {
        new Handler(Looper.getMainLooper()).post(() -> {
            List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(result.routes[0].overviewPolyline.getEncodedPath());
            List<LatLng> newDecodedPath = new ArrayList<>();

            for (com.google.maps.model.LatLng latLng : decodedPath) {
                newDecodedPath.add(toLatLng(latLng));
            }
            Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
            polyline.setColor(R.color.colorAccent);
        });
    }*/

    private void moveCameraToPath(LatLng startPoint, LatLng endPoint) {
        double topBound;
        double bottomBound;
        double leftBound;
        double rightBound;

        if (startPoint.latitude > endPoint.latitude) {
            topBound = startPoint.latitude;
            bottomBound = endPoint.latitude;
        } else {
            topBound = endPoint.latitude;
            bottomBound = startPoint.latitude;
        }

        if (startPoint.longitude > endPoint.longitude) {
            rightBound = startPoint.longitude;
            leftBound = endPoint.longitude;
        } else {
            rightBound = endPoint.longitude;
            leftBound = startPoint.longitude;
        }

        LatLngBounds latLngBounds = new LatLngBounds(toLatLng(bottomBound, leftBound), toLatLng(topBound, rightBound));
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, DEFAULT_PADDING));
    }

    private LatLng toLatLng(double latitude, double longitude) {
        return new LatLng(latitude, longitude);
    }

    private LatLng toLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    private LatLng toLatLng(Address address) {
        return new LatLng(address.getLatitude(), address.getLongitude());
    }

    private static LatLng toLatLng(com.google.maps.model.LatLng latLng) {
        return new LatLng(latLng.lat, latLng.lng);
    }

    private com.google.maps.model.LatLng toGoogleMapsLatLng(LatLng latLng) {
        return new com.google.maps.model.LatLng(latLng.latitude, latLng.longitude);
    }

    private void hideSoftKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
