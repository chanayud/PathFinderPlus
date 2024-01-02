package com.example.pathfinderplus;

import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;

public class Route {
    private String title;
    private ArrayList<LatLng> addresses;

    public Route() {
        // Default constructor required for Firestore
    }

    public Route(String title, ArrayList<LatLng> addresses) {
        this.title = title;
        this.addresses = addresses;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ArrayList<LatLng> getAddresses() {
        return addresses;
    }

    public void setAddresses(ArrayList<LatLng> addresses) {
        this.addresses = addresses;
    }
}
