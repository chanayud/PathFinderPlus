package com.example.pathfinderplus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LinearLayout addressListLayout;
   private double longitude;
   private double latitude;
   private LatLng latLng;
   private ArrayList<ArrayList<Double>> coordinatesArray;
   private
   String chosenAddress;
    Button addAddressButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "Im here ");
        addressListLayout = findViewById(R.id.addressListLayoutID);
        addAddressButton = findViewById(R.id.saveAddressButtonID);
        coordinatesArray = new ArrayList<>();



        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyA_oc0Ffut9TNjwgZUUqANMRvQZDbnERPM");
        }
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        AutocompleteFilter filter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
                .build();
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS);
        autocompleteFragment.setPlaceFields(fields);

        addAddressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textView = new TextView(MainActivity.this);
                if(chosenAddress!=null) {
                    textView.setText(chosenAddress);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                    textView.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.black));
                    textView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, android.R.color.white));
                    textView.setPadding(16, 16, 16, 16);
                    textView.setTextDirection(textView.TEXT_DIRECTION_RTL);

                }



                addressListLayout.addView(textView);

            }
        });

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {


            @Override
            public void onError(@NonNull Status status) {

            }

            public void onPlaceSelected(@NonNull Place place) {
                chosenAddress = place.getAddress();
                saveTheCoordinates(chosenAddress);


            }

        });



    }

    public void saveTheCoordinates(String address){
        // Create a Geocoder object
        Geocoder geocoder = new Geocoder(this);


        try {
            // Get the first result from the Geocoder
            List<Address> addresses = geocoder.getFromLocationName(address, 1);

            // Check if the result is valid
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);

                // Get the latitude and longitude
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                ArrayList<Double> addressCoordinates = new ArrayList<>();
                addressCoordinates.add(latitude);
                addressCoordinates.add(longitude);
                coordinatesArray.add(addressCoordinates);


                // Do something with the coordinates
                Log.d("coordinates", "Latitude: " + latitude + ", Longitude: " + longitude);
            } else {
                Log.d("TAG", "No results found");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}