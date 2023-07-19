package com.example.pathfinderplus;

import android.location.Location;

public interface LocationCallback {
    void onLocationReceived(Location location);
    void onLocationFailed();
}
