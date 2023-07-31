package com.example.pathfinderplus;

import static com.example.pathfinderplus.R.id.addressListLayoutID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
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

public class MainActivity extends AppCompatActivity  implements GetDistanceTask.DistanceCallback
{

    private LinearLayout addressListLayout;
   private double longitude;
   private double latitude;
   private LatLng latLng;
   private ArrayList<String> addressesArray;
   private ArrayList<Distance> distanceArray;
   private  DistanceCalculator distanceCalculator;
   private
   String chosenAddress;
    Button addAddressButton;
    Button giveRouteButton;
    private static final String API_KEY = "AIzaSyAXFwrx6mtQFOfHyTy6umAPjf5GJrAIY0A";
    int expectedApiCalls;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "Im here ");
        addressListLayout = findViewById(R.id.addressListLayoutID);
        addAddressButton = findViewById(R.id.saveAddressButtonID);
        giveRouteButton = findViewById(R.id.giveMeRouteButtonID);
        addressesArray = new ArrayList<>();
        distanceArray = new ArrayList<>();
        distanceCalculator = new DistanceCalculator();

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
                LinearLayout addressLayout = new LinearLayout(MainActivity.this);
                addressLayout.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                addressLayout.setLayoutParams(layoutParams);

                TextView textView = new TextView(MainActivity.this);
                if (chosenAddress != "" && chosenAddress != null) {
                    textView.setText(chosenAddress);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                    textView.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.black));
                    textView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, android.R.color.white));
                    textView.setPadding(16, 16, 16, 16);
                    textView.setTextDirection(View.TEXT_DIRECTION_RTL);
                }

                Button deleteButton = new Button(MainActivity.this);
                LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                deleteButton.setLayoutParams(buttonLayoutParams);
                deleteButton.setBackgroundResource(R.drawable.ic_trash_can);

                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addressListLayout.removeView(addressLayout);
                        if(addressListLayout.getChildCount() == 0){
                            giveRouteButton.setBackgroundColor(0xCCCCCC);
                            giveRouteButton.setEnabled(false);
                        }
                    }
                });

                addressLayout.addView(deleteButton);
                addressLayout.addView(textView);
                addressesArray.add(chosenAddress);


                addressListLayout.addView(addressLayout);

                if (addressListLayout.getChildCount() != 0) {
                    giveRouteButton.setBackgroundColor(0xFFFF0000);
                    giveRouteButton.setEnabled(true);
                    Log.d("TAG", "tehilaaaaaaaaa");
                }
            }
        });


        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {


            @Override
            public void onError(@NonNull Status status) {

            }

            public void onPlaceSelected(@NonNull Place place) {
                chosenAddress = place.getAddress();

              //  saveTheCoordinates(chosenAddress);


            }

        });
        giveRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                distanceArray.clear();
                int totalAddresses = addressesArray.size();
                expectedApiCalls = factorial(totalAddresses) / (factorial(2) * factorial(totalAddresses - 2));

                // Iterate over each element in the coordinatesArray
                for (int i = 0; i < addressesArray.size(); i++) {
                //    LatLng coordinate1 = coordinatesArray.get(i);
                        String address1 = addressesArray.get(i);


                    // Iterate over the remaining elements starting from the next index
                    for (int j = i + 1; j < addressesArray.size(); j++) {
                        String address2 = addressesArray.get(j);
                        Log.d("MYLOG", "onClick: pppppppppppppp");

                      //  DistanceCalculator distanceCalculator = new DistanceCalculator();
                        // GetDistanceTask getDistanceTask = new GetDistanceTask();
                      //  getDistanceTask.setDistanceCallback(distanceCalculator); // Assuming DistanceCalculator implements the DistanceCallback interface
                        distanceCalculator.calculateDistance(address1, address2,expectedApiCalls, MainActivity.this);

                    }
                }
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
                LatLng coordinates = new LatLng(location.getLatitude(),location.getLongitude());
                //double latitude = location.getLatitude();
                //double longitude = location.getLongitude();
                //ArrayList<Double> addressCoordinates = new ArrayList<>();
                //addressCoordinates.add(latitude);
               // addressCoordinates.add(longitude);
                addressesArray.add(chosenAddress);



                // Do something with the coordinates
                Log.d("coordinates", "Latitude: " + latitude + ", Longitude: " + longitude);
            } else {
                Log.d("TAG", "No results found");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDistanceCalculated(Distance distance) {
            distanceArray.add(distance);
        if (distanceArray.size() == expectedApiCalls) {
            // Process the responses here
            // The distanceArray should contain all the responses now
            for (int i = 0; i < distanceArray.size(); i++) {
                // Do something with distanceArray.get(i) to process each response
                Log.d("MYLOG", "Origin Address: " + distanceArray.get(i).getOriginAddress());
                Log.d("MYLOG", "Destination Address: " + distanceArray.get(i).getDestinationAddress());
                Log.d("MYLOG", "Distance: " + distanceArray.get(i).getDistance());

            }
            startNavigation(distanceArray.get(0).getOrigin().toString(), distanceArray.get(0).getDestination().toString());

        }
    }

    public void startNavigation(String origin, String destination) {
        // if (currentDestinationIndex < DESTINATIONS.length) {
        // String origin = (currentDestinationIndex == 0) ? "current_location" : DESTINATIONS[currentDestinationIndex - 1];
        //    String destination = DESTINATIONS[currentDestinationIndex];
        //   currentDestinationIndex++;

        String navigationUrl = "https://www.google.com/maps/dir/?api=1&origin=" + origin + "&destination=" + destination + "&travelmode=driving&key=" + API_KEY;

        // Open Google Maps with the navigation URL
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(navigationUrl));
        startActivity(intent);
    }

            private int factorial(int n) {
        if (n <= 1) {
            return 1;
        }
        return n * factorial(n - 1);
    }


    @Override
    public void onDistanceCalculationFailed(String errorMessage) {

    }
}