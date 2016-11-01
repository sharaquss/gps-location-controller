package com.android.szparag.gpslocationcontroller;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.google.android.gms.common.api.GoogleApiClient.*;


public class MainMapFragment extends Fragment implements OnMapReadyCallback, ConnectionCallbacks, OnConnectionFailedListener {


    MapView mapView;
    Bundle bundle;

    Location lastKnownLocation;

    GoogleMap map;

    GoogleApiClient googleApiClient;

    private static final String MAPVIEW_BUNDLE_KEY = "mapviewbundlekey";


    public static MainMapFragment newInstance() {
        MainMapFragment fragment = new MainMapFragment();
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        ButterKnife.bind(this, getView());

        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        bundle = null;
        if (savedInstanceState != null) {
            bundle  = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layoutView = inflater.inflate(R.layout.fragment_main_map, container, false);
        mapView = (MapView) layoutView.findViewById(R.id.main_map_mapview);
        mapView.onCreate(bundle);

        mapView.getMapAsync(this);
        return layoutView;
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
        googleApiClient = ((GpsLocationControllerApplication) getActivity().getApplication()).getGoogleApiClient();
        googleApiClient.registerConnectionCallbacks(this);
        googleApiClient.registerConnectionFailedListener(this);
        googleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
        googleApiClient.disconnect();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }



    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        if (lastKnownLocation != null) {
            moveCameraLocation(lastKnownLocation, 18L);
        }
    }


    private void moveCameraLocation(Location location, float zoomValue) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomValue));
        map.addCircle(new CircleOptions()
                .center(latLng)
                .radius(10) //in meters
                .strokeColor(ContextCompat.getColor(getContext(), R.color.app_triadic_purple_lighter))
                .fillColor(ContextCompat.getColor(getContext(), R.color.app_triadic_purple_lighter_alpha))
        );
//        map.addMarker(new MarkerOptions().position(latLng).title("pos"));
//                this.map.addMarker(new MarkerOptions().position(new LatLng(50, 50)).title("Marker"));
    }

    //google play location service callbacks:
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

//            return ;
        }
        lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(((GpsLocationControllerApplication) getActivity().getApplication()).getGoogleApiClient());
        moveCameraLocation(lastKnownLocation, 18L);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Snackbar.make(getView(), "onConnectionSuspended", Snackbar.LENGTH_LONG);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Snackbar.make(getView(), "onConnectionFailed", Snackbar.LENGTH_LONG);
    }
}
