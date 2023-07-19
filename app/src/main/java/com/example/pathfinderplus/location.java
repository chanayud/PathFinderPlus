package com.example.pathfinderplus;

import android.location.Location;
import android.location.LocationListener;

import androidx.annotation.NonNull;

public class location implements LocationCallback {
    public void onLocationReceived(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
    }

    @Override
    public void onLocationFailed() {

    }
}
