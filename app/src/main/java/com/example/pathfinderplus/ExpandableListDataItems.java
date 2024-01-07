package com.example.pathfinderplus;

import android.content.Context;
import android.location.Geocoder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ExpandableListDataItems {
    static HashMap<String, List<String>> expandableDetailList = new HashMap<>();

    public static HashMap<String, List<String>> getData(ArrayList<Route> routeList, Context context) {
        // As we are populating List of fruits, vegetables and nuts, using them here
        // We can modify them as per our choice.
        // And also choice of fruits/vegetables/nuts can be changed
        for (Route iterator : routeList) {
            List<String> route = new ArrayList<String>();
            for (LatLng address : iterator.getAddresses()) {
                String addressStr = convertLatLngToAddress(context, address.latitude, address.longitude);
                route.add(addressStr);
            }
            String title = iterator.getTitle();
            expandableDetailList.put(title, route);


        }
        return expandableDetailList;
    }


    public static String convertLatLngToAddress(Context context, double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

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

