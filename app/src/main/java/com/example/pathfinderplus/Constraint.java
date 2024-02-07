package com.example.pathfinderplus;

import com.google.android.gms.maps.model.LatLng;

public class Constraint {
    LatLng address;
    int timeConstraint;
    LatLng addressConstraint;
    String addressString;


    public String getAddressString() {
        return addressString;
    }

    public void setAddressString(String addressString) {
        this.addressString = addressString;
    }


    public LatLng getAddress() {
        return address;
    }

    public void setAddress(LatLng address) {
        this.address = address;
    }

    public long getTimeConstraint() {
        return timeConstraint;
    }

    public void setTimeConstraint(int timeConstraint) {
        this.timeConstraint = timeConstraint;
    }

    public LatLng getAddressConstraint() {
        return addressConstraint;
    }

    public void setAddressConstraint(LatLng addressConstraint) {
        this.addressConstraint = addressConstraint;
    }

}

