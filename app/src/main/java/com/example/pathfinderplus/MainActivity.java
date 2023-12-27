package com.example.pathfinderplus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements GetDistanceTask.DistanceCallback {

    public static LinearLayout addressListLayout;
    public static ArrayList<LatLng> addressesArray = new ArrayList<>();
    public static ArrayList<Distance> distanceArray = new ArrayList<>();
    private DistanceCalculator distanceCalculator;
    private
    String chosenAddress;
    Button addAddressButton;
    Button giveRouteButton;
    LatLng chosenAddressCoordinates = null;
    private static final String API_KEY = "AIzaSyCyh9ja_vpEOIxKFpNIt9EVf3miQjRV2EU";
    int expectedApiCalls;
    int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    int MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 2;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private ProgressBar progressBar;
    FusedLocationProviderClient fusedLocationClient;
    public static Intent serviceIntent;
    public double shortestDistance = Double.MAX_VALUE;
    public ArrayList<LatLng> shortestRoute = new ArrayList<>();


    com.google.android.gms.location.LocationCallback locationCallback;


    private BroadcastReceiver myReceiver;
    private static final String ACTION_TRIGGERED = "com.example.pathfinderplus.ACTION_TRIGGERED";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setContentView(R.layout.activity_main);
        progressBar = findViewById(R.id.progressBar);
        Intent intent = getIntent();
        if (intent != null && "START_NAVIGATION".equals(intent.getAction())) {
            Log.d("MainActivity", "stop service here");
            stopService(serviceIntent);
            if(addressesArray.size()<5){
                routeCalculateByNaiveAlgorithm();
            }
            else {
                routeCalculateBySimulatedAnealing();
            }
        } else {
            addressesArray.clear();
            addressListLayout = findViewById(R.id.addressListLayoutID);
            addAddressButton = findViewById(R.id.saveAddressButtonID);
            giveRouteButton = findViewById(R.id.giveMeRouteButtonID);
            distanceCalculator = new DistanceCalculator(this);

            if (!Places.isInitialized()) {
                Places.initialize(getApplicationContext(), "AIzaSyB2wY2x6ZthLJ0XsvsdVahEY-Iap6ryi6M");
            }
            PlacesClient placesClient = Places.createClient(this);

            AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                    getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
            assert autocompleteFragment != null;
            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

            // Set up a PlaceSelectionListener to handle the response.
            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    String placeId = place.getId();

                    // make a Place Details request using the placeId
                    List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
                    FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, fields);

                    placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
                        Place detailedPlace = response.getPlace();
                        chosenAddress = detailedPlace.getAddress();
                        chosenAddressCoordinates = detailedPlace.getLatLng();

                        // Use formattedAddress and coordinates as needed
                    }).addOnFailureListener((exception) -> {
                        Log.e("MyLog", "Place details request failed: " + exception.getMessage());
                    });
                }

                @Override
                public void onError(@NonNull Status status) {
                    Log.i("MyLog", "An error occurred: " + status);
                }
            });

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
                    if (!Objects.equals(chosenAddress, "") && chosenAddress != null) {
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
                            if (addressListLayout.getChildCount() == 0) {
                                giveRouteButton.setBackgroundColor(0xCCCCCC);
                                giveRouteButton.setEnabled(false);
                            }
                        }
                    });

                    addressLayout.addView(deleteButton);
                    addressLayout.addView(textView);
                    addressesArray.add(chosenAddressCoordinates);


                    addressListLayout.addView(addressLayout);

                    if (addressListLayout.getChildCount() != 0) {
                        giveRouteButton.setBackgroundColor(0xFFFF0000);
                        giveRouteButton.setEnabled(true);
                    }
                }
            });


            giveRouteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Create a dialog builder
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("שמירה בהיסטוריה");
                    builder.setMessage("האם אתה רוצה לשמור את המסלול בהיסטורית המסלולים שלך?");

                    // Add buttons and their actions
                    builder.setPositiveButton("כן", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // User clicked "Yes," so save the address list to Firebase history
                            saveToFirebaseHistory(addressesArray);
                        }
                    });

                    builder.setNegativeButton("לא", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    // Create and show the dialog
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    requestPermissions();
                }
            });
        }
    }

    @Override

    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver when the activity is destroyed
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);


        // Stop the location update service
        stopService(new Intent(this, GeofenceForegroundService.class));
    }

    @Override
    public void onDistanceCalculated(Distance distance) {
        distanceArray.add(distance);
        if (distanceArray.size() == expectedApiCalls) {
            Log.d("mylog", "expectedApiCalls: "+expectedApiCalls);
            // The distanceArray should contain all the responses now
            for (int i = 0; i < distanceArray.size(); i++) {
                // Do something with distanceArray.get(i) to process each response
                Log.d("MYLOG", "Origin Address: " + distanceArray.get(i).getOriginAddress());
                Log.d("MYLOG", "Destination Address: " + distanceArray.get(i).getDestinationAddress());
                Log.d("MYLOG", "Distance: " + distanceArray.get(i).getDistance());

            }
            if(addressesArray.size()<5)
                routeCalculateByNaiveAlgorithm();
            else
                routeCalculateBySimulatedAnealing();
        }
    }

    public void requestPermissions() {
        Log.d("MainActivity", "requestPermissions: ");
        int locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        } else {
            Log.d("MainActivity", "before addCurrentLocation");
            addCurrentLocation();
        }
        Log.d("MainActivity", "requestPermissions: end");

    }
    public void addCurrentLocation() {
        if (!isLocationEnabled()) {
            Log.d("MainActivity", "location disabled");
            promptEnableLocation();
        } else {
            Log.d("MainActivity", "location enabled");
            Log.d("MainActivity", "in addCurrentLocation");
            // Check for permission and request if needed
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                // Permission already granted, proceed to get the current location
                // Define location request parameters
                LocationRequest locationRequest = LocationRequest.create();
                locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
                locationRequest.setInterval(10000); // Interval in milliseconds

                locationCallback = new com.google.android.gms.location.LocationCallback() {


                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult == null) {
                            Log.d("MainActivity", "locationResult is null");
                            return;
                        }
                        Location location = locationResult.getLastLocation();
                        if(location == null){
                            Log.d("MainActivity", "location is null");
                        }
                        if (location != null) {
                            stopLocationUpdates();
                            // Got the current location
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            // Add the current location to the addressesArray at the first position
                            addressesArray.add(0, currentLatLng);
                            Log.d("MainActivity", "in addCurrentLocation - add to addressArray - current location: "+addressesArray.get(0));

                            // Rest of your code here (distance calculation)
                            distanceArray = new ArrayList<>();
                            int totalAddresses = addressesArray.size();
                            expectedApiCalls = factorial(totalAddresses) / factorial(totalAddresses - 2);
                            Log.d("mylog", "expectedApiCalls: "+expectedApiCalls);

                            for (int i = 0; i < addressesArray.size(); i++) {
                                LatLng address1 = addressesArray.get(i);

                                for (int j = 0; j < addressesArray.size(); j++) {
                                    if(j==i)
                                        continue;
                                    LatLng address2 = addressesArray.get(j);

                                    try {
                                        distanceCalculator.calculateDistance(address1, address2, expectedApiCalls, MainActivity.this);
                                    } catch (IOException e) {
                                        Log.d("mylog", "exception: ", e);
                                    }
                                }
                            }

                        }
                    }


                };
                startLocationUpdates(locationRequest, locationCallback);

            }
        }

    }

    private void startLocationUpdates(LocationRequest locationRequest, LocationCallback locationCallback) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "missed required permission");
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    public void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates((com.google.android.gms.location.LocationCallback) locationCallback);
        }
    }

    // Method to check if location services are enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // Method to prompt user to enable location services
    private void promptEnableLocation() {
        Log.d("MainActivity", "promptEnableLocation");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Location Services Not Enabled")
                .setMessage("Please enable location services to use this feature")
                .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Redirect user to location settings
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Handle cancel action
                    }
                });
        builder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if(requestCode == 1) {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_BACKGROUND_LOCATION"}, MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION);
                Log.d("MainActivity", "onRequestPermissionsResult: " + requestCode);
            }
            else if(requestCode == 2) {
                addCurrentLocation();
            }

            }

    public void routeCalculateBySimulatedAnealing(){
        progressBar.setVisibility(View.VISIBLE); // Show the spinner

        new Thread(new Runnable() {
            @Override
            public void run() {
                // Execute the code in a new thread
                Log.d("MainActivity", "addressesArray.size:"+addressesArray.size());

                ArrayList<LatLng> solution = TSPSolver.solveTSP(addressesArray, distanceArray);
             //   addressesArray.clear();
                addressesArray = new ArrayList<>(solution);
                Log.d("mylog", "addressesArray: "+ addressesArray.get(0).latitude + " "+ addressesArray.get(0).longitude);
                if (addressesArray.size() > 1) {
                    // Remove the first address since you've already reached it
                    addressesArray.remove(0);
                }
             else {
                // Display a dialog message indicating arrival at all destinations
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("You have arrived at all of your destinations!")
                        .setCancelable(false)
                        .setPositiveButton("OK", (dialog, id) -> {
                            // Close the dialog and stop the app or handle further actions
                            dialog.dismiss();
                          //  stopApp();
                        });

                AlertDialog alert = builder.create();
                alert.show();

            }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE); // Dismiss the spinner
                    }
                });

                startNavigation(addressesArray.get(0));


                // Print the solution
                for (LatLng address : solution) {
                    String stringAddress = convertLatLngToAddress(MainActivity.this, address.latitude, address.longitude);
                    Log.d("mylog", "anealing-address: "+ stringAddress);
                }
            }
        }).start();
    };

//    public void routeCalculateByNaiveAlgorithm() {
//        progressBar.setVisibility(View.VISIBLE); // Show the spinner
//        List<LatLng> shortestRoute = findShortestRoute(addressesArray, distanceArray);
//        addressesArray = new ArrayList<>(shortestRoute);
//        Log.d("mylog", "addressesArray: "+ addressesArray.get(0).latitude + " "+ addressesArray.get(0).longitude);
//        if (addressesArray.size() > 1) {
//            // Remove the first address since you've already reached it
//            addressesArray.remove(0);
//        }
//            runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                progressBar.setVisibility(View.GONE); // Dismiss the spinner
//            }
//        });
//        // Print the solution
//        for (LatLng address : shortestRoute) {
//            String stringAddress = convertLatLngToAddress(MainActivity.this, address.latitude, address.longitude);
//            Log.d("mylog", "naive-address: "+ stringAddress);
//        }
//
//        startNavigation(addressesArray.get(0));
//
//
//    }
public void routeCalculateByNaiveAlgorithm() {
    progressBar.setVisibility(View.VISIBLE); // Show the spinner
    Log.d("mylog", "addressesArray: "+ addressesArray.get(0).latitude + " "+ addressesArray.get(0).longitude);
    addressesArray = solveTSP();
    // Print the solution
    for (LatLng address : addressesArray) {
        String stringAddress = convertLatLngToAddress(MainActivity.this, address.latitude, address.longitude);
        Log.d("mylog", "naive-address: "+ stringAddress);
    }
    if (addressesArray.size() > 1) {
        // Remove the first address since you've already reached it
        addressesArray.remove(0);
    }
    runOnUiThread(new Runnable() {
        @Override
        public void run() {
            progressBar.setVisibility(View.GONE); // Dismiss the spinner
        }
    });

    startNavigation(addressesArray.get(0));


}
//    public ArrayList<LatLng> solveTSP() {
//        ArrayList<LatLng> currentRoute = new ArrayList<>(addressesArray);
//        //  currentRoute.add(addressesArray.get(0)); // Returning to the starting point
//
//        permute(1, currentRoute);
//
//        for (LatLng address : shortestRoute) {
//            String stringAddress = convertLatLngToAddress(MainActivity.this, address.latitude, address.longitude);
//            Log.d("mylog", "shortest-route-naive-address: "+ stringAddress);
//        }
//
//        return shortestRoute;
//    }

//    private void permute(int start, ArrayList<LatLng> route) {
//        Log.d("mylog", "start= "+start+" route.size= "+route.size());
//
//        if (start == route.size() - 1) {
//            double currentDistance = calculateRouteDistance(route);
//            Log.d("mylog", "permute: current distance: "+currentDistance+" shortest distance: "+shortestDistance);
//            if (currentDistance < shortestDistance) {
//                Log.d("mylog", "currentDistance < shortestDistance ");
//                shortestDistance = currentDistance;
//                shortestRoute = new ArrayList<>(route);
//            }
//            return;
//        }
//
//        for (int i = start; i < route.size() - 1; i++) {
//            Collections.swap(route, start, i);
//            Log.d("mylog", "permute: start= "+start+"i= "+i);
//            permute(start + 1, route);
//            Collections.swap(route, start, i);
//            Log.d("mylog", "permute: start= "+start+"i= "+i);
//
//        }
//    }


//    private double calculateRouteDistance(ArrayList<LatLng> route) {
//        double totalDistance = 0.0;
//        for (int i = 0; i < route.size() - 1; i++) {
//            LatLng current = route.get(i);
//            Log.d("mylog", "calculateRouteDistance: current address "+convertLatLngToAddress(this, route.get(i).latitude, route.get(i).longitude));
//            LatLng next = route.get(i + 1);
//            Log.d("mylog", "calculateRouteDistance: next address "+convertLatLngToAddress(this, route.get(i+1).latitude, route.get(i+1).longitude));
//
//            double distance = getDistanceBetween(current, next);
//            Log.d("mylog", "calculateRouteDistance: distance "+distance);
//            totalDistance += distance;
//        }
//        Log.d("mylog", "calculateRouteDistance: totalDistance "+totalDistance);
//
//        return totalDistance;
//    }

//    private double getDistanceBetween(LatLng source, LatLng destination) {
//        for (Distance d : distanceArray) {
//            if (d.getOrigin().equals(source) && d.getDestination().equals(destination)) {
//                return d.getDistance();
//            }
//        }
//        return Double.MAX_VALUE; // Handle case when distance is not found
//    }


    public ArrayList<LatLng> solveTSP() {
        int n = addressesArray.size();
        ArrayList<LatLng> currentRoute = new ArrayList<>(addressesArray);

        int[] indices = new int[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }

        while (true) {
            double currentDistance = calculateRouteDistance(currentRoute);

            if (currentDistance < shortestDistance) {
                shortestDistance = currentDistance;
                shortestRoute = new ArrayList<>(currentRoute);
            }

            int i = n - 1;
            while (i > 0 && indices[i - 1] >= indices[i]) {
                i--;
            }

            if (i <= 0) {
                break;
            }

            int j = n - 1;
            while (indices[j] <= indices[i - 1]) {
                j--;
            }

            swap(indices, i - 1, j);

            j = n - 1;
            while (i < j) {
                swap(indices, i, j);
                i++;
                j--;
            }

            // Rebuild currentRoute using indices
            ArrayList<LatLng> tempRoute = new ArrayList<>();
            for (int index : indices) {
                tempRoute.add(addressesArray.get(index));
            }
            currentRoute = tempRoute;
        }

        // Ensure the first destination remains at the start of the route
        ArrayList<LatLng> finalRoute = new ArrayList<>(shortestRoute);
        int indexOfFirstDestination = finalRoute.indexOf(addressesArray.get(0));
        Collections.rotate(finalRoute, -indexOfFirstDestination);

        Log.d("mylog", "Shortest Route: " + finalRoute);
        Log.d("mylog", "Shortest Distance: " + shortestDistance);
        return finalRoute;
    }

    private void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    private double calculateRouteDistance(ArrayList<LatLng> route) {
        double totalDistance = 0.0;
        for (int i = 0; i < route.size() - 1; i++) {
            LatLng current = route.get(i);
            Log.d("mylog", "current: " + convertLatLngToAddress(this,current.latitude, current.longitude));

            LatLng next = route.get(i + 1);
            Log.d("mylog", "next: " + convertLatLngToAddress(this,next.latitude, next.longitude));

            int distance = getDistanceBetween(current, next);
            Log.d("mylog", "distance: " + distance);

            totalDistance += distance;
        }
        Log.d("mylog", "totalDistance: " + totalDistance);

        return totalDistance;
    }

    private int getDistanceBetween(LatLng source, LatLng destination) {
        String sourceAddress = convertLatLngToAddress(this, source.latitude, source.longitude);
        String destinationAddress = convertLatLngToAddress(this, destination.latitude, destination.longitude);
        Log.d("mylog", "getDistanceBetween: source: "+sourceAddress+" destination: "+destinationAddress);

        for (Distance d : distanceArray) {
            String tempSource = convertLatLngToAddress(this, d.getOrigin().latitude, d.getOrigin().longitude);
            String tempDestination = convertLatLngToAddress(this, d.getDestination().latitude, d.getDestination().longitude);
            Log.d("mylog", "d.getDistance: "+d.getDistance() + " tempSource: "+tempSource+" d.tempDestination: "+tempDestination);
          //  if (d.getOrigin().latitude == source.latitude && d.getOrigin().longitude == source.longitude  && d.getDestination().latitude == destination.latitude && d.getDestination().longitude == destination.longitude) {
            assert tempSource != null;
            if(tempSource.equals(sourceAddress)) {
                assert tempDestination != null;
                if (tempDestination.equals(destinationAddress)) {
                    Log.d("mylog", "getDistanceBetween: " + d.getDistance());
                    return d.getDistance();
                }
            }
        }
        return Integer.MAX_VALUE; // Handle case when distance is not found
    }



//    public static double findShortestRoute(List<LatLng> destinations, double[][] distances) {
//
//        public static double getDistanceBetween(LatLng origin, LatLng destination, List<Distance> distances) {
//            for (Distance distance : distances) {
//                if (distance.getOrigin().equals(origin) && distance.getDestination().equals(destination)) {
//                    return distance.getDistance();
//                }
//            }
//            return Double.MAX_VALUE; // Return a large value if distance not found
//        }
//
//        // Helper method to generate all permutations of a list of LatLng addresses
//        public static List<List<LatLng>> permute(List<LatLng> addresses) {
//            List<List<LatLng>> permutations = new ArrayList<>();
//            permuteHelper(addresses, 0, permutations);
//            return permutations;
//        }
//
//        private static void permuteHelper(List<LatLng> addresses, int start, List<List<LatLng>> result) {
//            if (start >= addresses.size()) {
//                result.add(new ArrayList<>(addresses));
//                return;
//            }
//
//            for (int i = start; i < addresses.size(); i++) {
//                swap(addresses, start, i);
//                permuteHelper(addresses, start + 1, result);
//                swap(addresses, start, i);
//            }
//        }
//
//        private static void swap(List<LatLng> addresses, int i, int j) {
//            LatLng temp = addresses.get(i);
//            addresses.set(i, addresses.get(j));
//            addresses.set(j, temp);
//        }
//
//
//
//


    public void startNavigation(LatLng destination) {
        Log.d("MainActivity", "start navigation");

        double destinationLatitude = destination.latitude;
        double destinationLongitude = destination.longitude;
        String jobId = UUID.randomUUID().toString();


        serviceIntent = new Intent(this, LocationMonitoringForegroundService.class);
        serviceIntent.putExtra("DESTINATION_LATITUDE", destinationLatitude);
        serviceIntent.putExtra("DESTINATION_LONGITUDE", destinationLongitude);
        serviceIntent.putExtra("JOB_ID", jobId);
        Log.d("MainActivity", "DESTINATION_LATITUDE: "+destinationLatitude+" DESTINATION_LONGITUDE: "+destinationLongitude+" JOB_ID: "+jobId);

        startService(serviceIntent);

        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destinationLatitude + "," + destinationLongitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            Log.d("MainACtivity", "navigation started: ");
            // Open Google Maps with navigation
            startActivity(mapIntent);
        }
    }

    private int factorial(int n) {
        if (n <= 1) {
            return 1;
        }
        return n * factorial(n - 1);
    }


    @Override
    public void onDistanceCalculationFailed(String errorMessage) {
        Log.d("mylog", "onDistanceCalculationFailed: " + errorMessage);
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

    public void saveToFirebaseHistory(ArrayList<LatLng> addressesArray) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference historyCollection = db.collection("routeHistory"); // Replace "routeHistory" with the desired collection name

        // Create a new document to store the addresses
        Map<String, Object> addressData = new HashMap<>();
        addressData.put("addresses", addressesArray); // You can save the addresses as an array or any other suitable format

        historyCollection.add(addressData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Document was successfully added
                        Log.d("Firestore", "DocumentSnapshot written with ID: " + documentReference.getId());
                        // You can perform any further actions here if needed
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle any errors that occur
                        Log.e("Firestore", "Error adding document", e);
                    }
                });
    }


}
