package com.example.pathfinderplus;

import com.google.android.gms.maps.model.LatLng;

public class Address {
    private LatLng addressCoordinates;
    private String addressName;


    public LatLng getAddressCoordinates() {
        return addressCoordinates;
    }

    public void setAddressCoordinates(LatLng addressCoordinates) {
        this.addressCoordinates = addressCoordinates;
    }

    public String getAddressName() {
        return addressName;
    }

    public void setAddressName(String addressName) {
        this.addressName = addressName;
    }

}

