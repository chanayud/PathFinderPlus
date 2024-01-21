package com.example.pathfinderplus;

import com.google.android.gms.maps.model.LatLng;

public class Constraint {
    LatLng address;
    long timeConstraint;
    LatLng addressConstraint;

    public LatLng getAddress() {
        return address;
    }

    public void setAddress(LatLng address) {
        this.address = address;
    }

    public long getTimeConstraint() {
        return timeConstraint;
    }

    public void setTimeConstraint(long timeConstraint) {
        this.timeConstraint = timeConstraint;
    }

    public LatLng getAddressConstraint() {
        return addressConstraint;
    }

    public void setAddressConstraint(LatLng addressConstraint) {
        this.addressConstraint = addressConstraint;
    }

}

