package com.android.szparag.gpslocationcontroller.views;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.szparag.gpslocationcontroller.GpsLocationControllerApplication;
import com.android.szparag.gpslocationcontroller.R;
import com.android.szparag.gpslocationcontroller.adapters.RecyclerOnPosClickListener;
import com.android.szparag.gpslocationcontroller.adapters.RegionRecyclerViewAdapter;
import com.android.szparag.gpslocationcontroller.backend.models.Policy;
import com.android.szparag.gpslocationcontroller.backend.models.Region;
import com.github.rubensousa.floatingtoolbar.FloatingToolbar;
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;

import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmResults;

import static com.google.android.gms.common.api.GoogleApiClient.*;


public class MainMapFragment extends Fragment implements OnMapReadyCallback, ConnectionCallbacks, OnConnectionFailedListener {


    @BindView(R.id.fab)
    FloatingActionButton fab;

    @BindView(R.id.morphingFabToolbar)
    FloatingToolbar morphingFabToolbar;

    @BindView(R.id.region_recycler_quickview)
    RecyclerView regionQuickView;
    RegionRecyclerViewAdapter regionQuickViewAdapter;

    MapView mapView;
    Bundle bundle;
    Location lastKnownLocation;
    GoogleMap map;
    GoogleApiClient googleApiClient;

    RealmResults<Region> regions;

    private static final String MAPVIEW_BUNDLE_KEY = "mapviewbundlekey";


    public static MainMapFragment newInstance() {
        MainMapFragment fragment = new MainMapFragment();
        return fragment;
    }

    private void buildRegionQuickView() {
        RecyclerView.LayoutManager manag = new LinearLayoutManager(
                getContext(),
                LinearLayoutManager.HORIZONTAL,
                false);
        regionQuickView.setLayoutManager(manag);
        SnapHelper snapHelper = new GravitySnapHelper(Gravity.START);
        snapHelper.attachToRecyclerView(regionQuickView);
        ((GravitySnapHelper) snapHelper).enableLastItemSnap(false);

        regionQuickViewAdapter = new RegionRecyclerViewAdapter(new RecyclerOnPosClickListener() {
            @Override
            public void OnPosClick(View v, int position) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(regionQuickViewAdapter.getItem(position).getCenter(), 15));
            }
        });
        regionQuickView.setAdapter(regionQuickViewAdapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        ButterKnife.bind(this, layoutView);
        buildRegionQuickView();
        mapView = (MapView) layoutView.findViewById(R.id.main_map_mapview);
        mapView.onCreate(bundle);

        morphingFabToolbar.attachFab(fab);

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

        updateLocations();
    }

    private void updateLocations() {
        regions = Realm.getDefaultInstance().where(Region.class).findAll();
        regionQuickViewAdapter.updateItems(regions);
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

        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.googlemaps_style_2));

        if (lastKnownLocation != null) {
            moveCameraLocation(lastKnownLocation, 18L);
        }

        for (int i = 0; i < regions.size(); ++i) {
            addCircle(regions.get(i).getCenter());
        }
    }


    private void moveCameraLocation(Location location, float zoomValue) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomValue));
        addCircle(latLng, R.color.app_primary_orange_lighter_alpha, R.color.white);
    }

    private void moveCameraLocation(LatLng latLng, float zoomValue) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomValue));
    }

    private void addCircle(LatLng latLng) {
        addCircle(latLng, R.color.app_triadic_purple, R.color.app_triadic_purple_lighter);
    }

    private void addCircle(LatLng latLng, int strokeColorResId, int fillColorResId) {
        map.addCircle(new CircleOptions()
                .center(latLng)
                .radius(100) //in meters
                .strokeColor(ContextCompat.getColor(getContext(), strokeColorResId))
                .fillColor(ContextCompat.getColor(getContext(), fillColorResId))
        );
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
