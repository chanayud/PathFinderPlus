package com.example.pathfinderplus;

import static com.example.pathfinderplus.R.id.addressListLayoutID;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
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
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements GetDistanceTask.DistanceCallback {

    public static LinearLayout addressListLayout;
    private double longitude;
    private double latitude;
    private LatLng latLng;
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
    int radiusInMeters = 100;
    int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    int MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 2;
    int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    private static final int MY_PERMISSIONS_REQUEST_COMBINED = 123;
   // public static int counter = 0;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private ProgressBar progressBar;




    private BroadcastReceiver myReceiver;
    private static final String ACTION_TRIGGERED = "com.example.pathfinderplus.ACTION_TRIGGERED";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = findViewById(R.id.progressBar);
        Intent intent = getIntent();
        if (intent != null && "START_NAVIGATION".equals(intent.getAction())) {
//            Log.d("MainActivity", "onCreate, counter: "+counter);
            routeCalculateBySimulatedAnealing();  // Call your method to start navigation
        }

            //  boolean geofenceTransition = getIntent().getBooleanExtra("geofence_transition", false);
      /*  if (geofenceTransition) {
            String geofenceId = getIntent().getStringExtra("geofence_id");
            // Now you have the geofence ID and can use it to identify the geofence
            Log.d("MainActivity", "Geofence ID: " + geofenceId);
            routeCalculateBySimulatedAnealing(distanceArray, addressesArray);
        }*/
        // startForegroundService(new Intent(this, GeofenceForegroundService.class));
        // int locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        // int backgroundLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        // Log.d("MainActivity", "locationPermission: " + locationPermission + " backgroundLocationPermission: " + backgroundLocationPermission);
        // if (locationPermission != PackageManager.PERMISSION_GRANTED || backgroundLocationPermission != PackageManager.PERMISSION_GRANTED) {
        //     Log.d("MainActivity", "I have no permissions");
        //   ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        // }
        //  Log.d("MainActivity", "Im here ");
        //  if (getIntent().getStringExtra("JOB_ID") != null) {
        //      Log.d("MainActivity", "JOB_ID");
        // If triggered by the service, navigate to the next destination
        //      navigateToNextDestination();
        //  }
        Log.d("MainActivity", "onCreate: I'm here again");
    /*    myReceiver = new BroadcastReceiver() {
            @Override
                public void onReceive(Context context, Intent intent) {
                    String jobId = intent.getStringExtra("JOB_ID");
                    if (jobId != null) {
                        Log.d("MainActivity", "Received broadcast with JOB_ID: " + jobId);
                        navigateToNextDestination();
                    }
                }
            };


        // Register the receiver
        IntentFilter intentFilter = new IntentFilter("com.example.pathfinderplus.ACTION_TRIGGERED");
        LocalBroadcastManager.getInstance(this).

                registerReceiver(myReceiver, intentFilter);

     */


    addressListLayout = findViewById(R.id.addressListLayoutID);
        addAddressButton = findViewById(R.id.saveAddressButtonID);
        giveRouteButton = findViewById(R.id.giveMeRouteButtonID);
//        addressesArray = new ArrayList<>();

//        addressesArray.add(new LatLng(31.80323045037454, 35.09754359568119));
//        addressesArray.add(new LatLng(31.786021048079505, 35.212575962283914));
//        addressesArray.add(new LatLng(31.82959148757307, 35.244116841162196));

//        distanceArray = new ArrayList<>();
        distanceCalculator = new DistanceCalculator(this);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyB2wY2x6ZthLJ0XsvsdVahEY-Iap6ryi6M");
            // Create a new PlacesClient instance
        }
        PlacesClient placesClient = Places.createClient(this);


        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
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
    @Override

    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver when the activity is destroyed
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);


        // Stop the location update service
        stopService(new Intent(this, GeofenceForegroundService.class));
    }


//    public void saveTheCoordinates(String address) {
//        // Create a Geocoder object
//        Geocoder geocoder = new Geocoder(this);
//
//
//        try {
//
//            // Get the first result from the Geocoder
//            List<Address> addresses = geocoder.getFromLocationName(address, 1);
//
//            // Check if the result is valid
//            if (addresses != null && !addresses.isEmpty()) {
//                Address location = addresses.get(0);
//
//                // Get the latitude and longitude
//                LatLng coordinates = new LatLng(location.getLatitude(), location.getLongitude());
//                //double latitude = location.getLatitude();
//                //double longitude = location.getLongitude();
//                //ArrayList<Double> addressCoordinates = new ArrayList<>();
//                //addressCoordinates.add(latitude);
//                // addressCoordinates.add(longitude);
//                addressesArray.add(chosenAddressCoordinates);
//
//
//                // Do something with the coordinates
//                Log.d("coordinates", "Latitude: " + latitude + ", Longitude: " + longitude);
//            } else {
//                Log.d("TAG", "No results found");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }

    @Override
    public void onDistanceCalculated(Distance distance) {
        distanceArray.add(distance);
        if (distanceArray.size() == expectedApiCalls) {
            // Process the responses here
            // The distanceArray should contain all the responses now
            for (int i = 0; i < distanceArray.size(); i++) {
                // Do something with distanceArray.get(i) to process each response
                Log.d("MYLOG", "Origin Address: " + distanceArray.get(i).getOrigin());
                Log.d("MYLOG", "Destination Address: " + distanceArray.get(i).getDestination());
                Log.d("MYLOG", "Distance: " + distanceArray.get(i).getDistance());

            }
            routeCalculateBySimulatedAnealing();
        }
    }

    //    public void createGeofence(LatLng address) {
//        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//        String geofenceAddress = "";
//        try {
//            List<Address> addresses = geocoder.getFromLocation(address.latitude, address.longitude, 1);
//            if (addresses != null && !addresses.isEmpty()) {
//                Address address1 = addresses.get(0);
//                geofenceAddress = address1.getAddressLine(0);
//                Log.d("MainActivity", "createGeofence: geofenceAddress:" + geofenceAddress);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        Log.d("MainActivity", "createGeofence: latitude: " + address.latitude + "longitude: " + address.longitude);
//        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(this);
//
//        Geofence geofence = new Geofence.Builder()
//                .setRequestId(geofenceAddress)
//                .setCircularRegion(address.latitude, address.longitude, radiusInMeters)
//                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
//                .setExpirationDuration(Geofence.NEVER_EXPIRE)
//                .build();
//
//        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
//                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
//                .addGeofence(geofence)
//                .build();
//        Log.d("Geofence", "geofenceAddress: " + geofenceAddress);
//        PendingIntent geofencePendingIntent = PendingIntent.getBroadcast(
//                this,
//                0,
//                new Intent(this, GeofenceBroadcastReceiver.class),
//                PendingIntent.FLAG_UPDATE_CURRENT
//        );
//
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // Request location permission here if needed.
//            return;
//        }
//
//        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
//                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        Log.d("Geofence", "Geofences added successfully. " + geofencingRequest);
//
//                        // After successfully adding geofences, send a broadcast to trigger GeofenceBroadcastReceiver
//                        Intent geofenceIntent = new Intent("com.example.pathfinderplus.GEOFENCE_ACTION");
//                        // Optionally, add extra data to the intent if needed
//                        sendBroadcast(geofenceIntent);
//                    }
//                })
//                .addOnFailureListener(this, new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.e("Geofence", "Geofences not added. Error: " + e.getMessage());
//                    }
//                });
//    }
    public void requestPermissions()  {
        Log.d("MainActivity", "requestPermissions: ");
        //    int backgroundPermission = ContextCompat.checkSelfPermission(this,
        //            Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        int locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        //   List<String> listPermissionsNeeded = new ArrayList<>();
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);

            //    listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            // }
        //  if (backgroundPermission != PackageManager.PERMISSION_GRANTED) {
        //          listPermissionsNeeded.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
    //}
    //  for(int i=0; i<listPermissionsNeeded.size(); i++) {
    //      Log.d("MainActivity", "listPermissionsNeeded: " + listPermissionsNeeded.get(i));
    //  }
    //  if (!listPermissionsNeeded.isEmpty()) {
    //      Log.d("MainActivity", "listPermissionsNeeded: ");
    //      ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"},MY_PERMISSIONS_REQUEST_FINE_LOCATION);
  }
        else{
            addCurrentLocation();
//            counter++;
        }
        Log.d("MainActivity", "requestPermissions: end");

    }

    public void addCurrentLocation() {
        // Initialize the Fused Location Provider client
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check for permission and request if needed
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, proceed to get the current location
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                // Got the current location
                                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                // Add the current location to the addressesArray at the first position
                                addressesArray.add(0, currentLatLng);

                                // Rest of your code here (distance calculation)
                                // ...
                                distanceArray = new ArrayList<>();
                                int totalAddresses = addressesArray.size();
                                expectedApiCalls = factorial(totalAddresses) / (factorial(2) * factorial(totalAddresses - 2));

                                for (int i = 0; i < addressesArray.size(); i++) {
                                    LatLng address1 = addressesArray.get(i);

                                    for (int j = i + 1; j < addressesArray.size(); j++) {
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
                    });
        }
    }


//    public void requestPermissions() {
//        Log.d("MainActivity", "requestPermissions: ");
//        int backgroundPermission = ContextCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_BACKGROUND_LOCATION);
//        int locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
//        List<String> listPermissionsNeeded = new ArrayList<>();
//        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
//            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
//        }
//        if (backgroundPermission != PackageManager.PERMISSION_GRANTED) {
//            listPermissionsNeeded.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
//        }
//        for(int i=0; i<listPermissionsNeeded.size(); i++) {
//            Log.d("MainActivity", "listPermissionsNeeded: " + listPermissionsNeeded.get(i));
//        }
//        if (!listPermissionsNeeded.isEmpty()) {
//            Log.d("MainActivity", "listPermissionsNeeded: ");
//            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"},MY_PERMISSIONS_REQUEST_FINE_LOCATION);
//        }
//        Log.d("MainActivity", "requestPermissions: end");
//
//    }


    // Check if the app has location permissions
       /* boolean needLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;

        // Check if the app has background location permissions (Android 10 and later)
        boolean needBackgroundLocationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED;

        if (needLocationPermission || needBackgroundLocationPermission)

    {
        List<String> permissionsToRequest = new ArrayList<>();

        if (needLocationPermission) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (needBackgroundLocationPermission) {
            permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        // Request the combined list of permissions
        ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), MY_PERMISSIONS_REQUEST_COMBINED);
        }

        */



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        switch (requestCode) {
//            case 1:
//                // Check if the permission was granted
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    fine = true;
//                }
//            case 2:
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    background = true;
//                }
////                if (fine && background) {
            if(requestCode == 1) {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_BACKGROUND_LOCATION"}, MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION);
                Log.d("MainActivity", "onRequestPermissionsResult: " + requestCode);
            }
            else if(requestCode == 2) {
                startNavigation(addressesArray.get(0));
            }

            }

//                }
//        }
//    }

//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        boolean shouldShowRationale = false;
//        for (String permission : permissions)
//        {
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission))
//            {
//                shouldShowRationale = true;
//                break;
//            }
//        }
//
//        if (shouldShowRationale) {
//            // Show a rationale to the user
//            showRationaleDialog = false;
//            showRationaleDialog(requestCode,grantResults);
//        }
//        else{
//            allPermissionsGranted = true;
//            for (int result : grantResults) {
//                if (result != PackageManager.PERMISSION_GRANTED) {
//                    allPermissionsGranted = false;
//                    break;
//                }
//            }
//
//            if (allPermissionsGranted) {
//                Log.d("mylog", "onRequestPermissionsResult: ");
//            }
//        }
//
//
//    }
//    private void showRationaleDialog(int requestCode, int[] grantResults )
//    {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Permission Required");
//        builder.setMessage("This app requires certain permissions to function properly.");
//        builder.setNegativeButton("Cancel", null);
//        builder.setNeutralButton("Open Settings", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                openAppSettings(requestCode, grantResults);
//            }
//        });
//        builder.show();
//    }
//    private void openAppSettings(int requestCode, int[]grantResults) {
//        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//        Uri uri = Uri.fromParts("package", getPackageName(), null);
//        intent.setData(uri);
//        startActivity(intent);
//    }


    public void routeCalculateBySimulatedAnealing(){
        progressBar.setVisibility(View.VISIBLE); // Show the spinner

        new Thread(new Runnable() {
            @Override
            public void run() {

                // Execute the code in a new thread
                ArrayList<LatLng> solution = TSPSolver.solveTSP(addressesArray, distanceArray);
                addressesArray.clear();
                addressesArray.addAll(solution);
                Log.d("mylog", "addressesArray: "+ addressesArray.get(0).latitude + " "+ addressesArray.get(0).longitude);
                //    Log.d("MainActivity", "createGeofence");
                  //  createGeofence(addressesArray.get(1));    //address is an object that contains the name of the destination and its coordinates.
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
                //   Log.d("mylog", "run: ");


                // Print the solution
                for (LatLng address : solution) {
                    String stringAddress = convertLatLngToAddress(MainActivity.this, address.latitude, address.longitude);
                    Log.d("mylog", "address: "+ stringAddress);
                }
            }
        }).start();
    };

//    public void navigateToNextDestination(){
//        Log.d("MainActivity", "navigateToNextDestination");
//        routeCalculateBySimulatedAnealing();
//    }

    public void startNavigation(LatLng destination) {
        Log.d("MainActivity", "start navigation");

        double destinationLatitude = destination.latitude;
        double destinationLongitude = destination.longitude;
        String jobId = UUID.randomUUID().toString();


        Intent serviceIntent = new Intent(this, LocationMonitoringForegroundService.class);
        serviceIntent.putExtra("DESTINATION_LATITUDE", destinationLatitude);
        serviceIntent.putExtra("DESTINATION_LONGITUDE", destinationLongitude);
        serviceIntent.putExtra("JOB_ID", jobId);
        Log.d("MainActivity", "DESTINATION_LATITUDE: "+destinationLatitude+" DESTINATION_LONGITUDE: "+destinationLongitude+" JOB_ID: "+jobId);

        startService(serviceIntent);

        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destinationLatitude + "," + destinationLongitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            // Open Google Maps with navigation
            startActivity(mapIntent);
            Log.d("MainACtivity", "navigation started: ");
        }
    }
    private int getJobIdFromDestination(LatLng destination) {
        // Generate a unique job ID based on the destination coordinates
        return (int) ((destination.latitude + destination.longitude) * 1000000);
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

//    private void addAddress(String chosenAddress) {
//        LinearLayout addressLayout = new LinearLayout(MainActivity.this);
//        addressLayout.setOrientation(LinearLayout.HORIZONTAL);
//        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT);
//        addressLayout.setLayoutParams(layoutParams);
//
//        TextView textView = new TextView(MainActivity.this);
//        if (chosenAddress != null && !chosenAddress.isEmpty()) {
//            textView.setText(chosenAddress);
//            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
//            textView.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.black));
//            textView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, android.R.color.white));
//            textView.setPadding(16, 16, 16, 16);
//            textView.setTextDirection(View.TEXT_DIRECTION_RTL);
//        }
//
//        Button deleteButton = new Button(MainActivity.this);
//        LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT);
//        deleteButton.setLayoutParams(buttonLayoutParams);
//        deleteButton.setBackgroundResource(R.drawable.ic_trash_can);
//
//        deleteButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                addressListLayout.removeView(addressLayout);
//                if (addressListLayout.getChildCount() == 0) {
//                    giveRouteButton.setBackgroundColor(0xCCCCCC);
//                    giveRouteButton.setEnabled(false);
//                }
//            }
//        });
//
//        addressLayout.addView(deleteButton);
//        addressLayout.addView(textView);
//        addressesArray.add(chosenAddressCoordinates);
//
//        addressListLayout.addView(addressLayout);
//
//        if (addressListLayout.getChildCount() != 0) {
//            giveRouteButton.setBackgroundColor(0xFFFF0000);
//            giveRouteButton.setEnabled(true);
//        }
//    }

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
