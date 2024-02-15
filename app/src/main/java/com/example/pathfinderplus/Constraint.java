package com.example.pathfinderplus;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class Constraint {
    LatLng address;
    int timeConstraint;
    LatLng addressConstraint;
    String addressString;

    public static LatLng getAddressConstraintByAddress(ArrayList<Constraint> constraints, LatLng address) {
        Log.d("mylog", "address: "+ address.longitude+","+address.latitude);
        for (Constraint constraint : constraints) {
            Log.d("mylog", "constraint: "+ constraint.getAddress().longitude+","+constraint.getAddress().latitude);

            if (constraint.getAddress().latitude == address.latitude && constraint.getAddress().longitude == address.longitude) {
                return constraint.getAddressConstraint();
            }
        }
        return null; // Address not found
    }
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

