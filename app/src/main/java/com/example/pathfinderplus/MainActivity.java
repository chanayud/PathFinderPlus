package com.example.pathfinderplus;

import static com.example.pathfinderplus.Constraint.getAddressConstraintByAddress;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.widget.TooltipCompat;


import android.Manifest;
import android.app.ActivityManager;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.checkerframework.checker.units.qual.C;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity implements GetDistanceTask.DistanceCallback {

    public static LinearLayout addressListLayout;
    public static ArrayList<LatLng> addressesArray = new ArrayList<>();
    public static ArrayList<Distance> distanceArray = new ArrayList<>();
    private DistanceCalculator distanceCalculator;
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
    private String email;
    private ImageButton existingList;

    // Declare this variable at the class level
    private List<String> chosenAddressesList = new ArrayList<>();
    private int selectedAddressIndex = -1;
    private boolean[] selectedStates;
    public static ArrayList<Constraint> ConstraintsArray;
    boolean validRoute = true;
    ArrayList<LatLng> SASolution;

    int chosenAddressPosition = -1; // Initialize with an invalid position
    public LatLng currentLatLng;
    public static Geocoder geocoder;




    com.google.android.gms.location.LocationCallback locationCallback;



    private BroadcastReceiver myReceiver;
    private static final String ACTION_TRIGGERED = "com.example.pathfinderplus.ACTION_TRIGGERED";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("mainFlow", "start-onCreate");
        email = getIntent().getStringExtra("EMAIL_ADDRESS");
        boolean isNewUser = getIntent().getBooleanExtra("IS_NEW_USER", false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setContentView(R.layout.activity_main);
        geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        progressBar = findViewById(R.id.progressBar);
        addressListLayout = findViewById(R.id.addressListLayoutID);
        addAddressButton = findViewById(R.id.saveAddressButtonID);
        giveRouteButton = findViewById(R.id.giveMeRouteButtonID);
        existingList = findViewById(R.id.existingList);
        ConstraintsArray = new ArrayList<>();
        TooltipCompat.setTooltipText(existingList, "בחירה מההיסטוריה");
        if(isNewUser)
            existingList.setEnabled(false);
        Intent intent = getIntent();
        distanceCalculator = new DistanceCalculator(this);
        if (intent != null && "START_NAVIGATION".equals(intent.getAction())) {
            Log.d("MainActivity", "stop service here");
            stopService(serviceIntent);
            if(addressesArray.size()>1){
                addressesArray.remove(0);
                if(addressesArray.size() == 1){
                    startNavigation(addressesArray.get(1));
                }
                calculateDistance();
            }
            else
                finishRoute();
        } else {
            if (intent != null) {
                ArrayList<String> addresses = intent.getStringArrayListExtra("addresses");
                if (addresses != null && !addresses.isEmpty()) {
                    addressListLayout.removeAllViews();
                    if(ConstraintsArray!=null) {
                        ConstraintsArray.clear();
                    }
                    addressesArray.clear();
                    for (String address : addresses) {
                        chosenAddressCoordinates = getLatLngFromAddress(this, address);
                        addView(address);
                    }
                }
                ArrayList<LatLng> newRoute = intent.getParcelableArrayListExtra("newRoute");
                if(newRoute!=null){
                    Log.d("mylog", "newRoute is not null");
                    addressesArray = new ArrayList<>(newRoute);
//                    Log.d("mylog", "before startNavigation1: "+convertLatLngToAddress(this,addressesArray.get(0).latitude, addressesArray.get(0).longitude)+" currentLatLng: "+convertLatLngToAddress(this, currentLatLng.latitude, currentLatLng.longitude));
//                    if (currentLatLng!= null && convertLatLngToAddress(this,addressesArray.get(0).latitude, addressesArray.get(0).longitude).equals(convertLatLngToAddress(this, currentLatLng.latitude, currentLatLng.longitude))){
//                        addressesArray.remove(0);
//                        Log.d("mylog", "removing 1st item: ");
//                    }
//                        Log.d("mylog", "before startNavigation1: "+convertLatLngToAddress(this,addressesArray.get(0).latitude, addressesArray.get(0).longitude)+" currentLatLng: "+convertLatLngToAddress(this, currentLatLng.latitude, currentLatLng.longitude));
                    Log.d("mylog", "addressesArray.get(0): "+addressesArray.get(0));
                    stopService(serviceIntent);
                    startNavigation(addressesArray.get(1));
                }
            }
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

            ImageButton helpButton = findViewById(R.id.helpButton);
            helpButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Navigate to the help page
                    Intent intent = new Intent(MainActivity.this, HelpActivity.class);
                    startActivity(intent);
                }
            });

            addAddressButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addView(chosenAddress);
                    autocompleteFragment.setText("");
                    //chosenAddress = null;
                }
            });
            existingList.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, HistoryList.class);
                    intent.putExtra("EMAIL_ADDRESS", email);
                    startActivity(intent);

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
                            AlertDialog.Builder titleBuilder = new AlertDialog.Builder(MainActivity.this);
                            titleBuilder.setTitle("נא תן כותרת למסלול שלך");

                            // Set up the input
                            final EditText input = new EditText(MainActivity.this);
                            input.setInputType(InputType.TYPE_CLASS_TEXT);
                            titleBuilder.setView(input);

                            // Set up the buttons for the title dialog
                            titleBuilder.setPositiveButton("אישור", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String title = input.getText().toString().trim();
                                    // Check if title is not empty before saving
                                    if (!title.isEmpty()) {
                                        // Save the address list to Firebase history with the obtained title
                                        saveToFirebaseHistory(addressesArray, title);

                                        // Request permissions and start navigation
                                        requestPermissions();
                                    } else {
                                        Toast.makeText(MainActivity.this, "נא להזין כותרת", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            titleBuilder.setNegativeButton("ביטול", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Start navigation without saving the route to Firestore
                                    requestPermissions();
                                }
                            });

                            // Show the title dialog
                            AlertDialog titleDialog = titleBuilder.create();
                            titleDialog.show();

                            // You can set a listener to the dialog's dismissal to manage navigation accordingly
                            titleDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    // This code will execute when the dialog is dismissed
                                    // For example, you can handle navigation cancellation here
                                }
                            });
                        }
                    });

                    builder.setNegativeButton("לא", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            requestPermissions();

                        }
                    });

                    // Create and show the dialog
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
        }
        Log.d("mainFlow", "finish-onCreate");
    }

    @Override

    protected void onDestroy() {
        super.onDestroy();
        Log.d("mainFlow", "start-onDestroy");
        stopService(serviceIntent);
        // Unregister the receiver when the activity is destroyed
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);


        // Stop the location update service
        stopService(new Intent(this, GeofenceForegroundService.class));
        Log.d("mainFlow", "finish-onDestroy");

    }

    @Override
    public void onDistanceCalculated(Distance distance) {
        Log.d("mainFlow", "start-onDistanceCalculated");

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
                calculateRoutesAndStartNavigation();
        }
        Log.d("mainFlow", "finish-onDistanceCalculated");
    }

    public void requestPermissions() {
        Log.d("mainFlow", "start-requestPermissions");
        Log.d("MainActivity", "requestPermissions: ");
        int locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        } else {
            Log.d("MainActivity", "before addCurrentLocation");
            addCurrentLocation();
        }
        Log.d("mainFlow", "finish-requestPermissions");
    }
    public void addCurrentLocation() {
        Log.d("mainFlow", "start-addCurrentLocation");
        progressBar.setVisibility(View.VISIBLE); // Show the ProgressBar
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
                        Log.d("mainFlow", "start-onLocationResult");
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
                            currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            // Add the current location to the addressesArray at the first position
                            if (!convertLatLngToAddress(addressesArray.get(0).latitude, addressesArray.get(0).longitude).equals(convertLatLngToAddress(currentLatLng.latitude, currentLatLng.longitude)))
                                if (!convertLatLngToAddress(addressesArray.get(0).latitude, addressesArray.get(0).longitude).equals(convertLatLngToAddress(currentLatLng.latitude, currentLatLng.longitude)))
                                    addressesArray.add(0, currentLatLng);
                            Log.d("MainActivity", "in addCurrentLocation - add to addressArray - current location: "+addressesArray.get(0));
                            calculateDistance();
                        }
                        Log.d("mainFlow", "finish-onLocationResult");
                    }
                };
                startLocationUpdates(locationRequest, locationCallback);
            }
        }
        Log.d("mainFlow", "finish-addCurrentLocation");
    }

    private void startLocationUpdates(LocationRequest locationRequest, LocationCallback locationCallback) {
        Log.d("mainFlow", "start-startLocationUpdates");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "missed required permission");
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        Log.d("mainFlow", "finish-startLocationUpdates");

    }

    public void stopLocationUpdates() {
        Log.d("mainFlow", "start-stopLocationUpdates");
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates((com.google.android.gms.location.LocationCallback) locationCallback);
        }
        Log.d("mainFlow", "finish-stopLocationUpdates");
    }

    // Method to check if location services are enabled
    private boolean isLocationEnabled() {
        Log.d("mainFlow", "start-isLocationEnabled");
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.d("mainFlow", "finish-isLocationEnabled");
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // Method to prompt user to enable location services
    private void promptEnableLocation() {
        Log.d("mainFlow", "start-promptEnableLocation");
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
        Log.d("mainFlow", "finish-promptEnableLocation");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("mainFlow", "start-onRequestPermissionsResult");
        if(requestCode == 1) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_BACKGROUND_LOCATION"}, MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION);
            Log.d("MainActivity", "onRequestPermissionsResult: " + requestCode);
        }
        else if(requestCode == 2) {
            addCurrentLocation();
        }
        Log.d("mainFlow", "finish-onRequestPermissionsResult");
    }

    public ArrayList<LatLng> routeCalculateBySimulatedAnealing(){
        Log.d("mainFlow", "start-routeCalculateBySimulatedAnealing");
        Log.d("mylog", "addressesArray.size:" + addressesArray.size());

        SASolution = TSPSolver.solveTSP(addressesArray, distanceArray);
        Log.d("mylog", "SASolution.size:" + SASolution.size());
        if (SASolution.size()>0) {
            // Print the solution
            for (LatLng address : SASolution) {
                String stringAddress = convertLatLngToAddress(address.latitude, address.longitude);
                Log.d("mylog", "anealing-address: " + stringAddress);
            }
        }
//                }
        Log.d("mainFlow", "finish-routeCalculateBySimulatedAnealing");
        return SASolution;
//        }
    }



    public void ShowNoRouteErrorMassage(){
        Log.d("mainFlow", "start-ShowNoRouteErrorMassage");
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("לא נמצא מסלול העומד באילוצים שהזנת")
                .setCancelable(false)
                .setPositiveButton("אישור", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button
                        // You can add any additional actions you want here
                        dialog.dismiss(); // Dismiss the dialog
                    }
                });

// Create the AlertDialog
        AlertDialog alert = builder.create();

// Show the AlertDialog
        alert.show();
        ConstraintsArray.clear();
        Log.d("mainFlow", "finish-ShowNoRouteErrorMassage");
    }
    public boolean checkTimeConstrains() {
        Log.d("mainFlow", "start-checkTimeConstrains");
        boolean flag = true;
        StringBuilder invalidDestinations = new StringBuilder();
        for (Constraint constraint : ConstraintsArray) {
            if (constraint.timeConstraint > 0) {
                for (Distance distance : distanceArray) {
                    if (distance.getOrigin().latitude == addressesArray.get(0).latitude
                            && distance.getOrigin().longitude == addressesArray.get(0).longitude
                            && distance.getDestination().latitude == constraint.address.latitude
                            && distance.getDestination().longitude == constraint.address.longitude) {

                        if (distance.getDistance() > constraint.timeConstraint) {
                            flag = false;
                            invalidDestinations.append(convertLatLngToAddress( constraint.address.latitude, constraint.address.longitude))
                                    .append(", "); // Add invalid destination to the string
                        }
                        break;
                    }
                }
            }
        }
        if (!flag) {
            String alertMessage = "האילוצים שהזנת בתחנות: " + invalidDestinations.toString() + "לא קבילים";
            // Remove the trailing comma and space from the string
//            alertMessage = alertMessage.substring(0, alertMessage.length() - 2);

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("שגיאת אילוץ")
                    .setMessage(alertMessage)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            progressBar.setVisibility(View.GONE); // Show the spinner
                        }
                    });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
        Log.d("mainFlow", "finish-checkTimeConstrains");
        return flag;
    }
    public void routeCalculateByNaiveAlgorithm() {
        Log.d("mainFlow", "start-routeCalculateByNaiveAlgorithm");
        progressBar.setVisibility(View.VISIBLE); // Show the spinner
        Log.d("mylog", "addressesArray: " + addressesArray.get(0).latitude + " " + addressesArray.get(0).longitude);
        boolean tempFlag = checkTimeConstrains();
        if (tempFlag) {
            ArrayList <LatLng> temp = new ArrayList<>(solveTSPnew());
            if (temp.size() == 0) {
                ShowNoRouteErrorMassage();
                progressBar.setVisibility(View.GONE); // Show the spinner
            } else {
                // Print the solution
                for (LatLng address : temp) {
                    String stringAddress = convertLatLngToAddress( address.latitude, address.longitude);
                    Log.d("mylog", "naive-address: " + stringAddress);
                }
//                if (temp.size() > 1) {
//                    // Remove the first address since you've already reached it
//                    temp.remove(0);
//                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE); // Dismiss the spinner
                    }
                });
                addressesArray = new ArrayList<>(temp);
                if (currentLatLng!=null && convertLatLngToAddress(addressesArray.get(0).latitude, addressesArray.get(0).longitude).equals(convertLatLngToAddress(currentLatLng.latitude, currentLatLng.longitude))){
                    Log.d("mylog", "removing 1st item: ");
                    addressesArray.remove(0);
                }
                for (LatLng address : addressesArray) {
                    String stringAddress = convertLatLngToAddress(address.latitude, address.longitude);
                    Log.d("printAddressesArray", "naive-address-print: " + stringAddress);
                }
//                Log.d("mylog", "before startNavigation2: "+convertLatLngToAddress(this,addressesArray.get(0).latitude, addressesArray.get(0).longitude));
                startNavigation(addressesArray.get(1));
            }
        }
        Log.d("mainFlow", "finish-routeCalculateByNaiveAlgorithm");
    }

    public ArrayList<LatLng> solveTSPnew() {
        Log.d("mainFlow", "start-solveTSPnew");
        ArrayList<LatLng> addressesArrayTemp = new ArrayList<>(MainActivity.addressesArray);
        LatLng sourceAddress = addressesArrayTemp.get(0);
        addressesArrayTemp.remove(0);
        List<ArrayList<LatLng>> permutations = generatePermutations(addressesArrayTemp, sourceAddress);
        double shortsetDistance = Double.MAX_VALUE;
        ArrayList<LatLng> resultRoute = new ArrayList<>();
        for(ArrayList<LatLng> permutation : permutations){
            double tempDistance = calculateRouteDistance(permutation);
            boolean meetsConstraints = checkConstraints(permutation, ConstraintsArray);
            if(meetsConstraints && tempDistance<shortsetDistance){
                shortsetDistance = tempDistance;
                resultRoute = permutation;
            }
        }
        Log.d("mainFlow", "finish-solveTSPnew");
        return resultRoute;
    }

    private boolean checkConstraints(ArrayList<LatLng> route, List<Constraint> constraintArray) {
        Log.d("mainFlow", "start-checkConstraints");
        for (Constraint constraint : constraintArray) {
            // Check time constraint
            if (constraint.getTimeConstraint() != 0) {
                int totalDistanceInSeconds = 0;
                for (int j = 0; j < route.size() - 1; j++) {
                    if (route.get(j).equals(constraint.getAddress()))
                        break;

                    for (Distance distance : distanceArray) {
                        if (route.get(j).equals(distance.getOrigin()) && route.get(j + 1).equals(distance.getDestination())) {
                            totalDistanceInSeconds += distance.getDistance();
                            break;
                        }
                    }
                }
                if (totalDistanceInSeconds > constraint.getTimeConstraint()) {
                    return false; // Constraint violation
                }
            }

            // Check place constraint
            if (constraint.getAddressConstraint() != null) {
                int currentAddressIndex = route.indexOf(constraint.getAddress());
                int constraintAddressIndex = route.indexOf(constraint.getAddressConstraint());

                if (constraintAddressIndex < currentAddressIndex) {
                    return false; // Constraint violation
                }
            }
        }
        Log.d("mainFlow", "finish-checkConstraints");
        return true; // All constraints met
    }


    public static List<ArrayList<LatLng>> generatePermutations(ArrayList<LatLng> addressesArray, LatLng sourceAddress) {
        Log.d("mainFlow", "start-generatePermutations");
        List<ArrayList<LatLng>> result = new ArrayList<>();
        generatePermutationsHelper(addressesArray, 0, result, sourceAddress);
        Log.d("mainFlow", "finish-generatePermutations");
        return result;
    }

    private static void generatePermutationsHelper(ArrayList<LatLng> array, int index, List<ArrayList<LatLng>> result, LatLng sourceAddress) {
        Log.d("mainFlow", "start-generatePermutationsHelper");
        if (index == array.size() - 1) {
            // We reached the end of the array, add a copy to the result
            ArrayList<LatLng> resultArray = new ArrayList<>(array);
            resultArray.add(0, sourceAddress);
            result.add(resultArray);
            return;
        }

        for (int i = index; i < array.size(); i++) {
            // Swap elements at index and i
            LatLng temp = array.get(index);
            array.set(index, array.get(i));
            array.set(i, temp);

            // Recursively generate permutations for the remaining elements
            generatePermutationsHelper(array, index + 1, result, sourceAddress);

            // Undo the swap to backtrack
            temp = array.get(index);
            array.set(index, array.get(i));
            array.set(i, temp);
        }
        Log.d("mainFlow", "start-generatePermutationsHelper");
    }


    public void solveTSP() {
        Log.d("mainFlow", "start-solveTSP");
        int n = addressesArray.size();
        LatLng firstAddress = addressesArray.get(0);
        Log.d("distance", "firstAddress: "+convertLatLngToAddress(firstAddress.latitude, firstAddress.longitude));

        // Find the index of the first address in the original addressesArray
        int indexOfFirstAddress = addressesArray.indexOf(firstAddress);

        // Create a list to store all possible routes
        List<List<LatLng>> allRoutes = new ArrayList<>();

        // Populate the list of lists with all possible routes starting from the first address
        for (int i = 0; i < n; i++) {
            List<LatLng> currentRoute = new ArrayList<>(addressesArray.subList(i, n));
            currentRoute.addAll(addressesArray.subList(0, i));

            // Ensure the first address remains at the start of the route
            Collections.rotate(currentRoute, -i);

            // Add the current route to the list of routes
            allRoutes.add(new ArrayList<>(currentRoute));
        }

        // Initialize variables to store the shortest route and distance
        List<LatLng> shortestRoute = new ArrayList<>();
        double shortestDistance = Double.MAX_VALUE;

        // Iterate through all possible routes and calculate the length of each route
        for (List<LatLng> route : allRoutes) {
            double currentDistance = calculateRouteDistance(route);
            for(LatLng address : route){
                Log.d("currentDistance", "address: "+convertLatLngToAddress(address.latitude, address.longitude));
            }
            Log.d("distance", "currentDistance: "+currentDistance);

            // Update the shortest route and distance if needed
            if (currentDistance < shortestDistance) {
                shortestDistance = currentDistance;
                shortestRoute = new ArrayList<>(route);
            }
        }
        Log.d("distance", "shortestDistance: "+shortestDistance);
        addressesArray.clear();
        addressesArray = (ArrayList<LatLng>) shortestRoute;

        // Print the solution
        for (LatLng address : shortestRoute) {
            String stringAddress = convertLatLngToAddress( address.latitude, address.longitude);
            Log.d("mylog", "address: " + stringAddress);
        }

        // Remove the first address since you've already reached it
        shortestRoute.remove(0);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE); // Dismiss the spinner
            }
        });
//        Log.d("mylog", "before startNavigation1: "+convertLatLngToAddress(this,shortestRoute.get(0).latitude, shortestRoute.get(0).longitude)+" currentLatLng: "+convertLatLngToAddress(this, currentLatLng.latitude, currentLatLng.longitude));
//        if (currentLatLng!=null && convertLatLngToAddress(shortestRoute.get(0).latitude, shortestRoute.get(0).longitude).equals(convertLatLngToAddress( currentLatLng.latitude, currentLatLng.longitude))){
//            Log.d("mylog", "removing 1st item: ");
//            shortestRoute.remove(0);
//        }

//        Log.d("mylog", "before startNavigation2: "+convertLatLngToAddress(this,shortestRoute.get(0).latitude, shortestRoute.get(0).longitude));
        startNavigation(shortestRoute.get(1));
        Log.d("mainFlow", "finish-solveTSP");
    }

    private void swap(int[] arr, int i, int j) {
        Log.d("mainFlow", "start-swap");
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
        Log.d("mainFlow", "finish-swap");
    }

    private double calculateRouteDistance(List<LatLng> route) {
        Log.d("mainFlow", "start-calculateRouteDistance");
        double totalDistance = 0.0;
        for (int i = 0; i < route.size() - 1; i++) {
            LatLng current = route.get(i);
            Log.d("distance", "current: " + convertLatLngToAddress(current.latitude, current.longitude));

            LatLng next = route.get(i + 1);
            Log.d("distance", "next: " + convertLatLngToAddress(next.latitude, next.longitude));

            int distance = getDistanceBetween(current, next);
            Log.d("distance", "distance: " + distance);

            totalDistance += distance;
        }
        Log.d("distance", "totalDistance: " + totalDistance);
        Log.d("mainFlow", "finish-calculateRouteDistance");
        return totalDistance;
    }

    private int getDistanceBetween(LatLng source, LatLng destination) {
        Log.d("mainFlow", "start-getDistanceBetween");
        String sourceAddress = convertLatLngToAddress(source.latitude, source.longitude);
        String destinationAddress = convertLatLngToAddress( destination.latitude, destination.longitude);
        Log.d("mylog", "getDisBetween: source: "+sourceAddress+" destination: "+destinationAddress);

        for (Distance d : distanceArray) {
            String tempSource = convertLatLngToAddress( d.getOrigin().latitude, d.getOrigin().longitude);
            String tempDestination = convertLatLngToAddress( d.getDestination().latitude, d.getDestination().longitude);
            Log.d("mylog", "d: "+d.getDistance() + " tempSource: "+tempSource+" d.tempDestination: "+tempDestination);
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
        Log.d("mainFlow", "finish-getDistanceBetween");
        return Integer.MAX_VALUE; // Handle case when distance is not found
    }



    public void startNavigation(LatLng destination) {
        Log.d("mainFlow", "start-startNavigation");
        showRouteDetailsDialogWithDelay(destination, 3000);
        Log.d("mainFlow", "finish-startNavigation");
    }

    private void showRouteDetailsDialogWithDelay(final LatLng destination, long delayMillis) {
        Log.d("mainFlow", "start-showRouteDetailsDialogWithDelay");
        showRouteDetailsDialog(destination); // Show the dialog immediately
        new CountDownTimer(delayMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Not needed for this example, but you can perform actions on each tick
            }

            @Override
            public void onFinish() {
                // Your navigation logic here, e.g., startNavigation(destination);

                // For demonstration purposes, let's log a message
                Log.d("MainActivity", "Delayed navigation code executed after 3 seconds");

                // Proceed with the navigation logic
                proceedWithNavigation(destination);
            }
        }.start();
        Log.d("mainFlow", "finish-showRouteDetailsDialogWithDelay");
    }
    private void proceedWithNavigation(LatLng destination) {
        Log.d("mainFlow", "start-proceedWithNavigation");
        if (!isMyServiceRunning(LocationMonitoringForegroundService.class)) {

            double destinationLatitude = destination.latitude;
            double destinationLongitude = destination.longitude;
            String jobId = UUID.randomUUID().toString();


            serviceIntent = new Intent(this, LocationMonitoringForegroundService.class);
            serviceIntent.putExtra("DESTINATION_LATITUDE", destinationLatitude);
            serviceIntent.putExtra("DESTINATION_LONGITUDE", destinationLongitude);
            serviceIntent.putExtra("JOB_ID", jobId);
            Log.d("MainActivity", "DESTINATION_LATITUDE: " + destinationLatitude + " DESTINATION_LONGITUDE: " + destinationLongitude + " JOB_ID: " + jobId);

            startService(serviceIntent);

            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destinationLatitude + "," + destinationLongitude);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                Log.d("MainACtivity", "navigation started: ");
                // Open Google Maps with navigation
                startActivity(mapIntent);
            }
        }else {
            Log.d("MainActivity", "Service is already running");
        }
        Log.d("mainFlow", "finish-proceedWithNavigation");
    }
    public ArrayList<LatLng> routeCalculateByMinimalSpanningTree() {
        Log.d("mainFlow", "start-routeCalculateByMinimalSpanningTree");
        progressBar.setVisibility(View.VISIBLE); // Show the spinner
        ArrayList<LatLng> solution = MinimalSpanningTree.findBestRoute(addressesArray, distanceArray, MainActivity.this);
        if (solution.size() > 0) {
            for (LatLng address : solution) {
                String stringAddress = convertLatLngToAddress(address.latitude, address.longitude);
                Log.d("mylog", "MinimalSpanningTree-address: " + stringAddress);
            }
        }
        Log.d("mainFlow", "finish-routeCalculateByMinimalSpanningTree");
        return solution;
    }
    public void calculateRoutesAndStartNavigation() {
        Log.d("mainFlow", "start-calculateRoutesAndStartNavigation");
        progressBar.setVisibility(View.VISIBLE); // Show the spinner
        boolean flag = checkTimeConstrains();

        if (flag) {
            ExecutorService executorService = Executors.newFixedThreadPool(2);

            Callable<ArrayList<LatLng>> simulatedAnealingTask = new Callable<ArrayList<LatLng>>() {
                @Override
                public ArrayList<LatLng> call() throws Exception {
                    return routeCalculateBySimulatedAnealing();
                }
            };

            Callable<ArrayList<LatLng>> minimalSpanningTreeTask = new Callable<ArrayList<LatLng>>() {
                @Override
                public ArrayList<LatLng> call() throws Exception {
                    return routeCalculateByMinimalSpanningTree();
                }
            };

            Future<ArrayList<LatLng>> simulatedAnealingFuture = executorService.submit(simulatedAnealingTask);
            Future<ArrayList<LatLng>> minimalSpanningTreeFuture = executorService.submit(minimalSpanningTreeTask);

            try {
                ArrayList<LatLng> simulatedAnealingResult = simulatedAnealingFuture.get();
                ArrayList<LatLng> minimalSpanningTreeResult = minimalSpanningTreeFuture.get();

                // Choose the route with the minimum length
//                ArrayList<LatLng> selectedRoute;
                if (calculateTotalDistance(simulatedAnealingResult) <= calculateTotalDistance(minimalSpanningTreeResult)) {
                    addressesArray = new ArrayList<>(simulatedAnealingResult);
                    Log.d("mylog", "simulatedAnealingResult selected");
                } else {
                    addressesArray = new ArrayList<>(minimalSpanningTreeResult);
                    Log.d("mylog", "minimalSpanningTreeResult selected");

                }
                if (currentLatLng!=null && convertLatLngToAddress(addressesArray.get(0).latitude, addressesArray.get(0).longitude).equals(convertLatLngToAddress( currentLatLng.latitude, currentLatLng.longitude))){
                    Log.d("mylog", "removing 1st item: ");
                    addressesArray.remove(0);
                }
                Log.d("mylog", "selectedRoute.get(0) "+convertLatLngToAddress(addressesArray.get(0).latitude, addressesArray.get(0).longitude));


                progressBar.setVisibility(View.GONE); // Show the spinner
                if (addressesArray.size() > 0) {
                    startNavigation(addressesArray.get(1));
                } else {
                    ShowNoRouteErrorMassage();
                    validRoute = false;
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            } finally {
                executorService.shutdown();
            }
        }
        Log.d("mainFlow", "finish-calculateRoutesAndStartNavigation");
    }

    private int calculateTotalDistance(ArrayList<LatLng> route) {
        Log.d("mainFlow", "start-calculateTotalDistance");
        int totalDistance = 0;

        for (int i = 0; i < route.size() - 1; i++) {
            LatLng origin = route.get(i);
            LatLng destination = route.get(i + 1);

            for (Distance distance : distanceArray) {
                if (origin.equals(distance.getOrigin()) && destination.equals(distance.getDestination())) {
                    totalDistance += distance.getDistance();
                    break;
                }
            }
        }
        Log.d("mainFlow", "finish-calculateTotalDistance");
        return totalDistance;
    }

    private void showRouteDetailsDialog(LatLng destination) {
        Log.d("mainFlow", "start-showRouteDetailsDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);


        // Inflate the custom layout
        View customLayout = getLayoutInflater().inflate(R.layout.custom_dialog_layout, null);
        builder.setView(customLayout);

        // Find the TextViews in the custom layout
        TextView titleTextView = customLayout.findViewById(R.id.titleTextView);
        TextView firstItemTextView = customLayout.findViewById(R.id.firstItemTextView);
        TextView messageTextView = customLayout.findViewById(R.id.messageTextView);
        LinearLayout firstItemLayout = customLayout.findViewById(R.id.firstItemLayout);

        // Set title properties
        titleTextView.setText("יתרת המסלול שלך היא");

        // Set the first item properties
        firstItemTextView.setText(convertLatLngToAddress(addressesArray.get(0).latitude, addressesArray.get(0).longitude));
        firstItemLayout.setBackgroundColor(Color.parseColor("#80cc0000")); // Set background color for the first item

        // Create a SpannableStringBuilder to apply styling to the route details
        SpannableStringBuilder routeDetails = new SpannableStringBuilder();

        for (int i = 1; i < addressesArray.size(); i++) {
            String address = convertLatLngToAddress(addressesArray.get(i).latitude, addressesArray.get(i).longitude);
            routeDetails.append(address);

            if (i < addressesArray.size() - 1) {
                routeDetails.append("\n"); // Add a newline between addresses
            }
        }

        // Set the styled text to the TextView for the message
        messageTextView.setText(routeDetails);

        // Show the dialog
        builder.show();
        Log.d("mainFlow", "finish-showRouteDetailsDialog");
    }

    public static int factorial(int n) {
        Log.d("mainFlow", "start-factorial");
        if (n <= 1) {
            Log.d("mainFlow", "finish-factorial");
            return 1;
        }
        return n * factorial(n - 1);
    }


    @Override
    public void onDistanceCalculationFailed(String errorMessage) {
        Log.d("mylog", "onDistanceCalculationFailed: " + errorMessage);
    }

    public static String convertLatLngToAddress( double latitude, double longitude) {
        Log.d("mainFlow", "start-convertLatLngToAddress");
        try {
            List<android.location.Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && addresses.size() > 0) {
                String address = addresses.get(0).getAddressLine(0);
                return address;
            }

        } catch (IOException e) {
            Log.d("mylog", "convertLatLngToAddress exception: ", e);
        }
        Log.d("mainFlow", "finish-convertLatLngToAddress");
        return null;
    }


    public void saveToFirebaseHistory(ArrayList<LatLng> addressesArray, String title) {
        Log.d("mainFlow", "start-saveToFirebaseHistory");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usersCollection = db.collection("users"); // Reference the "users" collection
        if(usersCollection == null){
            Log.d("mylog", "usersCollection is null");
        }
        else{
            Log.d("mylog", "usersCollection is not null");
        }

        // Create a map with the addresses array and title
        Map<String, Object> addressData = new HashMap<>();
        addressData.put("title", title);
        addressData.put("addresses", addressesArray);

        // Check if the user's password already exists in the "users" collection
        usersCollection.document(email).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // Update the existing list of LatLngs
                        List<Map<String, Object>> existingLists = (List<Map<String, Object>>) document.get("listsOfLatLng");
                        existingLists.add(addressData); // Add the new list of LatLngs

                        // Update the 'listsOfLatLng' field in Firestore
                        usersCollection.document(email).update("listsOfLatLng", existingLists)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Update successful
                                        Log.d("Firestore", "Document updated with new list of LatLngs");
                                        // You can perform any further actions here if needed
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Handle errors
                                        Log.e("Firestore", "Error updating document", e);
                                    }
                                });
                    } else {
                        // Create a new document for the user
                        List<Map<String, Object>> newLists = new ArrayList<>();
                        newLists.add(addressData); // Add the new list of LatLngs

                        // Add the new document to Firestore
                        Map<String, Object> newData = new HashMap<>();
                        newData.put("listsOfLatLng", newLists);

                        usersCollection.document(email).set(newData)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Document created successfully
                                        Log.d("Firestore", "New document created with list of LatLngs");
                                        // You can perform any further actions here if needed
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Handle errors
                                        Log.e("Firestore", "Error adding document", e);
                                    }
                                });
                    }
                } else {
                    // Handle failures
                    Log.e("Firestore", "get failed with ", task.getException());
                }
            }
        });
        Log.d("mainFlow", "finish-saveToFirebaseHistory");
    }
    // Function to convert an address string to LatLng coordinates
    public LatLng getLatLngFromAddress(Context context, String address) {
        Log.d("mainFlow", "start-getLatLngFromAddress");
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                double latitude = addresses.get(0).getLatitude();
                double longitude = addresses.get(0).getLongitude();
                Log.d("mainFlow", "finish-getLatLngFromAddress");
                return new LatLng(latitude, longitude);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public void addView(String chosenAddress) {
        Log.d("mainFlow", "start-addView");
        final EditText timeEditText = new EditText(MainActivity.this);
        final EditText positionEditText  = new EditText(MainActivity.this);
        timeEditText.setTag(chosenAddress);
        chosenAddressesList.add(chosenAddress);
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
        LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(
                0, // Width
                LinearLayout.LayoutParams.WRAP_CONTENT, // Height
                45f // Weight
        );
        textViewParams.gravity = Gravity.CENTER_VERTICAL; // Center vertically
        textView.setLayoutParams(textViewParams);

        Button deleteButton = new Button(MainActivity.this);
        LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        deleteButton.setLayoutParams(buttonLayoutParams);
        deleteButton.setBackgroundResource(R.drawable.ic_trash_can);
        LinearLayout.LayoutParams deleteButtonParams = new LinearLayout.LayoutParams(
                0, // Width
                LinearLayout.LayoutParams.WRAP_CONTENT, // Height
                10f // Weight
        );
        deleteButton.setLayoutParams(deleteButtonParams);

        ImageButton clockButton = new ImageButton(MainActivity.this);
        clockButton.setImageResource(R.drawable.clock);
        clockButton.setBackgroundColor(0xCCCCCC);
        LinearLayout.LayoutParams clockButtonParams = new LinearLayout.LayoutParams(
                0, // Width
                LinearLayout.LayoutParams.WRAP_CONTENT, // Height
                10f // Weight
        );
        clockButton.setLayoutParams(clockButtonParams);

        // EditText for manual time input
        timeEditText.setHint("Enter time"); // Set a hint for the user
        timeEditText.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        LinearLayout.LayoutParams timeEditTextParams = new LinearLayout.LayoutParams(
                0, // Width
                LinearLayout.LayoutParams.WRAP_CONTENT, // Height
                10f // Weight (Initially 0 weight to hide the view)
        );
        timeEditText.setLayoutParams(timeEditTextParams);
        timeEditText.setVisibility(View.GONE); // Initially hide the view


        // Add OnClickListener to clockButton
        clockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog(timeEditText);
            }
        });
        addressLayout.setTag(timeEditText);

        ImageButton pathButton = new ImageButton(MainActivity.this);
        pathButton.setImageResource(R.drawable.path);
        pathButton.setBackgroundColor(0xCCCCCC);
       /* LinearLayout.LayoutParams pathLayoutParams = new LinearLayout.LayoutParams( 48, 48);
        pathButton.setLayoutParams(pathLayoutParams);*/
        LinearLayout.LayoutParams pathButtonParams = new LinearLayout.LayoutParams(
                0, // Width
                ViewGroup.LayoutParams.WRAP_CONTENT, // Height (Specified height for pathButton)
                10f // Weight
        );
        pathButton.setLayoutParams(pathButtonParams);
        pathButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChosenAddressesDialog(timeEditText, chosenAddress, positionEditText);
            }
        });

        // EditText for manual time input
        positionEditText.setHint("Enter time"); // Set a hint for the user
        positionEditText.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        LinearLayout.LayoutParams positionEditTextParams = new LinearLayout.LayoutParams(
                0, // Width
                LinearLayout.LayoutParams.WRAP_CONTENT, // Height
                5f // Weight (Initially 0 weight to hide the view)
        );
        positionEditText.setLayoutParams(positionEditTextParams);
        positionEditText.setVisibility(View.GONE); // Initially hide the view

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addressListLayout.removeView(addressLayout);
                // Find the corresponding LatLng from the removed addressLayout
                EditText removedAddress = (EditText) addressLayout.getTag();
                String removedAddressString = (String) removedAddress.getTag();
                Log.d("mylog", "removedAddress: "+removedAddressString);

                // Remove the chosen address from addressesArray
                Iterator<LatLng> iterator = addressesArray.iterator();
                while (iterator.hasNext()) {
                    LatLng latLng = iterator.next();
                    // Replace LatLng with the actual data type of your coordinates
                    // Adjust the condition based on your data structure
                    if (Objects.equals(convertLatLngToAddress( latLng.latitude, latLng.longitude), removedAddress)) {
                        Log.d("mylog", "removedAddress: "+convertLatLngToAddress( latLng.latitude, latLng.longitude));
                        iterator.remove();
                        break;
                    }
                }

                if (addressListLayout.getChildCount() == 0) {
                    giveRouteButton.setBackgroundColor(0xCCCCCC);
                    giveRouteButton.setEnabled(false);
                }
            }
        });

        addressLayout.addView(deleteButton);
        addressLayout.addView(textView);
        addressLayout.addView(pathButton);
        addressLayout.addView(positionEditText);
        addressLayout.addView(clockButton);
        addressLayout.addView(timeEditText);
        addressesArray.add(chosenAddressCoordinates);

        addressListLayout.addView(addressLayout);

        if (addressListLayout.getChildCount() != 0) {
            giveRouteButton.setBackgroundColor(0xffcc0000);
            giveRouteButton.setEnabled(true);
        }
        Log.d("mainFlow", "finish-addView");
    }
    // Declare AlertDialog as a member variable of your class
    private AlertDialog dialog;
    private void showChosenAddressesDialog(EditText timeEditText, String chosenAddress, EditText positionEditText) {
        Log.d("mainFlow", "start-showChosenAddressesDialog");
        String address = (String) timeEditText.getTag();
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("בחר כתובת");

        // Inflate custom layout for the dialog content
        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog_layout, null);
        builder.setView(dialogView);

        // Get the ListView from the custom layout
        ListView listView = dialogView.findViewById(R.id.listView);

        // Convert the list of chosen addresses to an array
        final List<String> addressesList = new ArrayList<>(chosenAddressesList);
        addressesList.remove(chosenAddress);
        final String[] addressesArray = addressesList.toArray(new String[0]);
        // Iterate over the array and prepend each member with a number
        for (int i = 0; i < addressesArray.length; i++) {
            addressesArray[i] = (i + 1) + ". " + addressesArray[i];
        }


        // Find the position of the chosen address in the addressesArray
        for (int i = 0; i < addressesArray.length; i++) {
            if (chosenAddress.equals(addressesArray[i])) {
                chosenAddressPosition = i;
                break;
            }
        }
        // Set up custom adapter

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, addressesArray) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                Log.d("mylog", "position: " + position);

                // Get the current address
                String currentAddress = addressesArray[position];
                Log.d("mylog", "currentAddress: " + currentAddress);
                Log.d("mylog", "chosenAddress: " + chosenAddress);

                // Check if the current position matches the position of the chosen address
                if (position == chosenAddressPosition) {
                    // Apply bold style to the text of the current view
                    TextView textView = (TextView) view.findViewById(android.R.id.text1);
                    textView.setTypeface(null, Typeface.BOLD);
                } else {
                    // Ensure other items are not bolded
                    TextView textView = (TextView) view.findViewById(android.R.id.text1);
                    textView.setTypeface(null, Typeface.NORMAL);
                }

                // Return the modified view
                return view;
            }
        };



        // Set the adapter to the ListView
        listView.setAdapter(adapter);

        // Create and show the AlertDialog
        dialog = builder.create();
        dialog.show();

        // Handle item click listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedAddress = addressesArray[position];
                // Set the selected time to the EditText
                Log.d("mylog", "position: "+position);
                positionEditText.setText(String.valueOf(position+1));
                positionEditText.setFocusable(false); // Make timeEditText non-editable
                positionEditText.setClickable(false); // Make timeEditText non-clickable
                positionEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); // Adjust text size (you can set your desired size)
                positionEditText.setVisibility(View.VISIBLE); // Initially hide the view

                // Handle the selected address from the dialog
                // Perform any necessary actions with the selected address
                boolean exists = false;
                LatLng selectedAddressInLatLng = getLatLngFromAddress(MainActivity.this, selectedAddress);
                LatLng addressInLatLng = getLatLngFromAddress(MainActivity.this, address);

                for (Constraint constraint : ConstraintsArray) {
                    if (constraint.address == addressInLatLng) {
                        constraint.addressConstraint = getLatLngFromAddress(MainActivity.this, selectedAddress);
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    Constraint constraint = new Constraint();
                    constraint.setAddress(addressInLatLng);
                    constraint.setAddressConstraint(selectedAddressInLatLng);
                    constraint.setAddressString(selectedAddress);
                    ConstraintsArray.add(constraint);
                }
                Toast.makeText(MainActivity.this, "Selected Address: " + selectedAddress, Toast.LENGTH_SHORT).show();

                // Close the dialog after all actions are performed
                dialog.dismiss();
            }
        });
        Log.d("mainFlow", "finish-showChosenAddressesDialog");
    }

    // Method to show TimePickerDialog
    private void showTimePickerDialog(final EditText timeEditText) {
        Log.d("mainFlow", "start-showTimePickerDialog");
        Calendar currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                MainActivity.this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minuteInHour) {
                        String address = (String) timeEditText.getTag();
                        // Handle the selected time (hourOfDay and minute)
                        String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteInHour);
                        // Set the selected time to the EditText
                        timeEditText.setText(selectedTime);
                        timeEditText.setFocusable(false); // Make timeEditText non-editable
                        timeEditText.setClickable(false); // Make timeEditText non-clickable
                        timeEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); // Adjust text size (you can set your desired size)
                        timeEditText.setVisibility(View.VISIBLE); // Initially hide the view
                        int curentTimeInSeconds = hour * 3600 + minute * 60;
                        int constraintTimeInSeconds = hourOfDay * 3600 + minuteInHour * 60;
                        if(constraintTimeInSeconds < curentTimeInSeconds){
                            constraintTimeInSeconds = constraintTimeInSeconds + (24 * 3600);
                        }
                        constraintTimeInSeconds -= curentTimeInSeconds;
                        boolean exists = false;
                        for (Constraint constraint : ConstraintsArray) {
                            if (convertLatLngToAddress(constraint.address.latitude, constraint.address.longitude).equals(address)){
                                exists = true;
                                constraint.timeConstraint = constraintTimeInSeconds;
                                break;
                            }
                        }
                        if (!exists){
                            Constraint tempConstraint = new Constraint();
                            tempConstraint.setAddress(getLatLngFromAddress(MainActivity.this,address));
                            tempConstraint.setTimeConstraint(constraintTimeInSeconds);
                            tempConstraint.setAddressString(address);
                            ConstraintsArray.add(tempConstraint);
                        }
                    }
                },
                hour,
                minute,
                true // Set to true if you want the 24-hour format
        );
        timePickerDialog.show();
        Log.d("mainFlow", "finish-showTimePickerDialog");
    }
    private void finishRoute() {
        Log.d("mainFlow", "start-finishRoute");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("סיימת את המסלול!");
        builder.setMessage("האפליקציה תיסגר בעוד מספר שניות.");
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();

        HandlerThread handlerThread = new HandlerThread("CloseAppThread");
        handlerThread.start();

        Handler handler = new Handler(handlerThread.getLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
                finishAndRemoveTask(); // Close all activities in the task and remove the task
                handlerThread.quitSafely(); // Quit the handler thread
            }
        }, 5000); // 5000 milliseconds = 5 seconds
        Log.d("mainFlow", "finish-finishRoute");
    }

    public void calculateDistance(){
        Log.d("mainFlow", "start-calculateDistance");
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
        Log.d("mainFlow", "finish-calculateDistance");
    }
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        Log.d("mainFlow", "start-isMyServiceRunning");
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        // Iterate through the list of running services
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.d("mainFlow", "finish-isMyServiceRunning");
                // The service is running
                return true;
            }
        }

        // The service is not running
        return false;
    }
}
