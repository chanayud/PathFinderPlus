package com.example.pathfinderplus;

import android.content.Context;
import android.location.Geocoder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class DistanceCalculator {
    private Context context;

    public DistanceCalculator(Context context) {
        this.context = context;
    }

    public void calculateDistance(LatLng origin, LatLng destination, int totalApiCalls, GetDistanceTask.DistanceCallback callback) throws IOException {
        GetDistanceTask getDistanceTask = new GetDistanceTask(origin, destination, totalApiCalls);
        getDistanceTask.setDistanceCallback(callback);
        String originAddress = convertLatLngToAddress(context, origin.latitude, origin.longitude);
        String destinationAddress = convertLatLngToAddress(context, destination.latitude, destination.longitude);

        getDistanceTask.execute(originAddress, destinationAddress);
    }

    public static String convertLatLngToAddress(Context context, double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        Log.d("mylog", "convertLatLngToAddress: latitude: "+latitude+" longitude: "+longitude);

        try {
            List<android.location.Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && addresses.size() > 0) {
                String address = addresses.get(0).getAddressLine(0);
                return address;
            }

        } catch (IOException e) {
            Log.d("mylog", "convertLatLngToAddress exception: ", e);
        }
        return null;
    }

}
