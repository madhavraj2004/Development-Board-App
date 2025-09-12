package com.example.whatapp.ui.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;


import com.example.whatapp.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.cachemanager.CacheManager;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MapFragment extends Fragment {

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private Marker receivedMarker;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_map, container, false);

        // osmdroid config
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView = rootView.findViewById(R.id.mapView);

        // Always use the online MAPNIK source
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        // my location overlay
        GpsMyLocationProvider locProvider = new GpsMyLocationProvider(requireContext());
        myLocationOverlay = new MyLocationNewOverlay(locProvider, mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        });

        // Download button prefetches currently visible tiles for offline
        Button btnDownloadTiles = rootView.findViewById(R.id.btn_download_tiles);
        btnDownloadTiles.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                prefetchTilesForCurrentView();
            } else {
                Toast.makeText(requireContext(), "No internet connection. Cannot download tiles.", Toast.LENGTH_SHORT).show();
            }
        });

        // handle manual taps (optional)
        mapView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                GeoPoint tapped = (GeoPoint) mapView.getProjection()
                        .fromPixels((int) event.getX(), (int) event.getY());
                if (tapped != null) {
                    // displayLocationInfo(tapped);
                }
                return true;
            }
            return false;
        });

        // incoming location args from ChatFragment
        Bundle args = getArguments();
        if (args != null && args.containsKey("lat") && args.containsKey("lon")) {
            float lat = args.getFloat("lat");
            float lon = args.getFloat("lon");
            GeoPoint fromChat = new GeoPoint(lat, lon);
            showReceivedLocationMarker(fromChat);
        } else {
            GeoPoint defaultPoint = new GeoPoint(48.8583, 2.2944);
            mapView.getController().setCenter(defaultPoint);
            addDefaultMarker(defaultPoint, "Eiffel Tower");
        }

        return rootView;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
        return false;
    }

    private void prefetchTilesForCurrentView() {
        BoundingBox bbox = mapView.getBoundingBox();
        int minZoom = 12, maxZoom = 19;

        CacheManager cacheManager = new CacheManager(mapView);
        Toast.makeText(requireContext(), "Downloading tiles...", Toast.LENGTH_SHORT).show();
        cacheManager.downloadAreaAsync(
                requireContext(),
                bbox,
                minZoom,
                maxZoom,
                new CacheManager.CacheManagerCallback() {
                    @Override
                    public void downloadStarted() {
                        Toast.makeText(requireContext(), "Tile download started!", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onTaskComplete() {
                        Toast.makeText(requireContext(), "Tiles downloaded for offline use!", Toast.LENGTH_LONG).show();
                    }
                    @Override
                    public void onTaskFailed(int errors) {
                        Toast.makeText(requireContext(), "Some tiles failed to download: " + errors, Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void updateProgress(int progress, int currentZoomLevel, int zoomMin, int zoomMax) {}
                    @Override
                    public void setPossibleTilesInArea(int total) {}
                }
        );
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), p)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        permissions,
                        REQUEST_PERMISSIONS_REQUEST_CODE);
                break;
            }
        }
    }

    private void showReceivedLocationMarker(GeoPoint point) {
        if (receivedMarker != null) {
            mapView.getOverlays().remove(receivedMarker);
        }
        receivedMarker = new Marker(mapView);
        receivedMarker.setPosition(point);
        receivedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        receivedMarker.setTitle("Message Location");
        double latitude = point.getLatitude();
        double longitude = point.getLongitude();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String info = "Lat: " + latitude + "\nLon: " + longitude + "\nTime: " + time;
        receivedMarker.setSnippet(info);
        mapView.getOverlays().add(receivedMarker);
        mapView.getController().setCenter(point);
        receivedMarker.showInfoWindow();
        mapView.invalidate();
        Toast.makeText(requireContext(), info, Toast.LENGTH_LONG).show();
    }

    private void addDefaultMarker(GeoPoint point, String title) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title);
        mapView.getOverlays().add(marker);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDetach();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            for (int res : grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED) return;
            }
            if (myLocationOverlay != null) {
                myLocationOverlay.enableMyLocation();
                myLocationOverlay.enableFollowLocation();
            }
        }
    }
}