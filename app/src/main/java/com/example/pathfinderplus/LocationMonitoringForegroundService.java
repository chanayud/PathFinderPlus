package com.example.pathfinderplus;

import static com.example.pathfinderplus.MainActivity.addressesArray;
import static com.example.pathfinderplus.MainActivity.convertLatLngToAddress;
import static com.example.pathfinderplus.MainActivity.distanceArray;
import static com.example.pathfinderplus.MainActivity.factorial;
//import static com.example.pathfinderplus.MainActivity.navigateToNextDest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class LocationMonitoringForegroundService extends Service implements LocationListener, GetDistanceTask.DistanceCallback {

    private boolean isServiceRunning = false;


    private static final int TRIGGER_DISTANCE_METERS = 100;
    private static final int NOTIFICATION_ID = 123;

    private LocationManager locationManager;
    private double targetLatitude;
    private double targetLongitude;
    private String JOB_ID;
    private static final int MAX_ADDRESS_COUNT = 5;

    private final IBinder binder = new LocalBinder();
    private ArrayList<LatLng> newRoute;
    private Handler handler = new Handler();
    private Runnable routeCalculationRunnable;
    int expectedApiCalls;
    private DistanceCalculator distanceCalculator;
    boolean isTest = true;
    com.google.android.gms.location.LocationCallback locationCallback;
    FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    public Timer timer;
    public boolean timer_running = false;



    private BroadcastReceiver stopTimerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("mainFlow", "start-LocationMonitoringForegroundService-stopTimerReceiver");
            if (intent.getAction() != null && intent.getAction().equals("STOP_TIMER_ACTION")) {
                Log.d("mainFlow", "finish-stopTimerReceiver");
                stopTimer();
            }
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-onCreate");
        newRoute = new ArrayList<>();
        distanceCalculator = new DistanceCalculator(this);
        IntentFilter intentFilter = new IntentFilter("STOP_TIMER_ACTION");
        LocalBroadcastManager.getInstance(this).registerReceiver(stopTimerReceiver, intentFilter);

        // Create the notification channel if running on Android Oreo (API level 26) or higher
        NotificationChannel channel = new NotificationChannel("1234", "service notification", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-onStartCommand");
        Log.d("MainActivity", "onStartCommand: intent: " + intent);
        if (!isServiceRunning) {
            isServiceRunning = true;
            if (!timer_running) {
                timer_running = true;
                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        Log.d("mylog", "timer is here");
                        checkAndCalculateRoute();
                    }
                }, 60000, 60000); // 60000 milliseconds = 1 minute
            }

                try {
//            if (intent != null && intent.getAction() != null && intent.getAction().equals("START_NAVIGATION")) {
                    targetLatitude = intent.getDoubleExtra("DESTINATION_LATITUDE", 0.0);
                    targetLongitude = intent.getDoubleExtra("DESTINATION_LONGITUDE", 0.0);
                    JOB_ID = intent.getStringExtra("JOB_ID");
                    Log.d("MainActivity", "targetLatitude: " + targetLatitude + " targetLongitude: " + targetLongitude + " JOB_ID: " + JOB_ID);
                } catch (Exception e) {
                    Log.d("ex", "exception: " + e.getMessage());
                }
//            handler.post(routeCalculationRunnable);


            startLocationMonitoring();

            // Create a notification to make the service a foreground service
            createNotification();
        }
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-onStartCommand");
        return START_STICKY;
    }


    private void createNotification() {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-createNotification");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, "1234")
                .setContentTitle("Location Monitoring Service")
                .setContentText("Monitoring location in the background")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_ID, notification);
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-createNotification");
    }

    private void startLocationMonitoring() {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-startLocationMonitoring");
        Log.d("MainActivity", "startLocationMonitoring");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "startLocationMonitoring: requestLocationUpdates");

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 100, this);
        } else {
            Log.d("TAG", "startLocationMonitoring: Missing location permissions");
        }
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-startLocationMonitoring");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-onLocationChanged");
        Log.d("MainActivity", "onLocationChanged");

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        Log.d("MainActivity", "currentLatitude: " + currentLatitude + " currentLongitude: " + currentLongitude);
        Log.d("MainActivity", "targetLatitude: " + targetLatitude + " targetLongitude: " + targetLongitude);


        float[] results = new float[1];
        Location.distanceBetween(currentLatitude, currentLongitude, targetLatitude, targetLongitude, results);

        float distanceInMeters = results[0];
        Log.d("MainActivity", "distanceInMeters: " + distanceInMeters);
        if (distanceInMeters < TRIGGER_DISTANCE_METERS) {
            // Trigger action (launch MainActivity)
            Log.d("MainActivity", "distanceInMeters small");
            Notify(JOB_ID);

        }
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-onLocationChanged");
    }

    private void checkAndCalculateRoute() {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-checkAndCalculateRoute");
        calculateDistance();
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-checkAndCalculateRoute");
    }


    private void Notify(String jobId) {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-Notify");
        Log.d("MainActivity", "Navigating to the next destination");

        // Create an intent to open your app when the notification is clicked
        Intent appIntent = new Intent(this, MainActivity.class);
        appIntent.setAction("START_NAVIGATION");
        appIntent.putExtra("DESTINATION_LATITUDE", targetLatitude); // Pass destination latitude
        appIntent.putExtra("DESTINATION_LONGITUDE", targetLongitude); // Pass destination longitude
        appIntent.putExtra("JOB_ID", jobId); // Pass job ID
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent appPendingIntent = PendingIntent.getActivity(
                this, 0, appIntent, PendingIntent.FLAG_IMMUTABLE);

        // Notification channel settings
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create a notification channel with high importance
        NotificationChannel channel = new NotificationChannel("5678", "destination reached notification", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Channel Description");
        channel.setShowBadge(true); // Enable badge icon for this channel
        channel.enableLights(true);
        channel.setLightColor(Color.RED);
        channel.enableVibration(true);

        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }



        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, "5678")
                .setSmallIcon(R.drawable.icons8_notification)
                .setContentTitle("הגעת!!!")
                .setContentText("הגעת ליעד! לחץ כאן כדי לנווט ליעד הבא.")
                .setContentIntent(appPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX) // Set notification priority to high
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true);
//                .setDefaults(NotificationCompat.DEFAULT_ALL); // Set default notification behaviors


        // For heads-up notification (requires API level 21 or above)
        //notificationBuilder.setFullScreenIntent(null, true);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManagerCompat.notify(5678, notification.build());
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-Notify");
    }

    @Override
    public void onDistanceCalculated(Distance distance) {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-onDistanceCalculated");
        if (isTest) {
            Random random = new Random();
            Random random1 = new Random();
            int congestionFactor = random.nextInt(201) - 100; // Adjust the factor as needed
            int congestionFactor1 = random.nextInt(201) - 100;
            distance.setDistance((int) (distance.getDistance() * congestionFactor * congestionFactor1));
        }
        distanceArray.add(distance);
        Log.d("mylog", "expectedApiCalls IN OnDistanceCalculated: " + expectedApiCalls);
        Log.d("mylog", "distanceArray.size: " + distanceArray.size());


        if (distanceArray.size() == expectedApiCalls) {
            // The distanceArray should contain all the responses now
            for (int i = 0; i < distanceArray.size(); i++) {

            }
            // Check the length of "AddressesArray"
            if (addressesArray.size() > 5) {
                // Call the method for route calculation based on conditions
                routeCalculateBySimulatedAnealing();
                // Check if the new route is different from the existing one
            } else {
                // Call the method for route calculation based on conditions
                routeCalculateByNaiveAlgorithm();
            }
            // Check if the new route is different from the existing one
            if (!newRoute.equals(addressesArray)) {
                // Check if the new route is shorter
                if (isRouteShorter(newRoute, addressesArray)) {
                    // Notify the user and update addressesArray if needed
                    notifyUserAndHandleNewRoute(newRoute);
                }
            }
        }
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-onDistanceCalculated");
    }

    @Override
    public void onDistanceCalculationFailed(String errorMessage) {
    }

    public class LocalBinder extends Binder {
        LocationMonitoringForegroundService getService() {
            return LocationMonitoringForegroundService.this;
        }
    }

    public void routeCalculateBySimulatedAnealing() {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-routeCalculateBySimulatedAnealing");
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Execute the code in a new thread
                Log.d("MainActivity", "newRoute.size:" + newRoute.size());

                newRoute = TSPSolver.solveTSP(addressesArray, distanceArray);
                for (LatLng address : newRoute) {
                    String stringAddress = convertLatLngToAddress( address.latitude, address.longitude);
                    Log.d("mylog", "anealing-address: " + stringAddress);
                }

                //   addressesArray.clear();
//                newRoute = solution;
                Log.d("mylog", "addressesArray: " + newRoute.get(0).latitude + " " + newRoute.get(0).longitude);
            }
        }).start();
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-routeCalculateBySimulatedAnealing");
    }

    public void routeCalculateByNaiveAlgorithm() {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-routeCalculateByNaiveAlgorithm");
        Log.d("mylog", "addressesArray: " + addressesArray.get(0).latitude + " " + addressesArray.get(0).longitude);

        newRoute = solveTSPnew();
        for (LatLng address : addressesArray) {
            String stringAddress = convertLatLngToAddress( address.latitude, address.longitude);
            Log.d("mylog", "naive-address: " + stringAddress);
        }
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-routeCalculateByNaiveAlgorithm");
    }

    private double calculateRouteDistance(List<LatLng> route) {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-calculateRouteDistance");
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
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-calculateRouteDistance");
        return totalDistance;
    }

    private int getDistanceBetween(LatLng source, LatLng destination) {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-getDistanceBetween");
        String sourceAddress = convertLatLngToAddress(source.latitude, source.longitude);
        String destinationAddress = convertLatLngToAddress(destination.latitude, destination.longitude);
        Log.d("mylog", "getDisBetween: source: " + sourceAddress + " destination: " + destinationAddress);

        for (Distance d : distanceArray) {
            String tempSource = convertLatLngToAddress( d.getOrigin().latitude, d.getOrigin().longitude);
            String tempDestination = convertLatLngToAddress( d.getDestination().latitude, d.getDestination().longitude);
            Log.d("mylog", "d: " + d.getDistance() + " tempSource: " + tempSource + " d.tempDestination: " + tempDestination);
            //  if (d.getOrigin().latitude == source.latitude && d.getOrigin().longitude == source.longitude  && d.getDestination().latitude == destination.latitude && d.getDestination().longitude == destination.longitude) {
            assert tempSource != null;
            if (tempSource.equals(sourceAddress)) {
                assert tempDestination != null;
                if (tempDestination.equals(destinationAddress)) {
                    Log.d("mylog", "getDistanceBetween: " + d.getDistance());
                    Log.d("mainFlow", "finish-LocationMonitoringForegroundService-getDistanceBetween");
                    return d.getDistance();
                }
            }
        }
        return Integer.MAX_VALUE; // Handle case when distance is not found
    }

    private boolean isRouteShorter(ArrayList<LatLng> newRoute, ArrayList<LatLng> addressesArray) {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-isRouteShorter");
        double newCalculation = calculateRouteDistance(newRoute);
        double existingCalculation = calculateRouteDistance(addressesArray);
        Log.d("ShortestRoute", "newCalculation: "+newCalculation+" existingCalculation: "+existingCalculation);
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-isRouteShorter");
        if (newCalculation < existingCalculation){
            return true;
        }
        return false;
    }

    public ArrayList<LatLng> solveTSPnew() {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-solveTSPnew");
        ArrayList<LatLng> addressesArrayTemp = new ArrayList<>(addressesArray);
        LatLng sourceAddress = addressesArrayTemp.get(0);
//        if (navigateToNextDest)
        addressesArrayTemp.remove(0);
        List<ArrayList<LatLng>> permutations = generatePermutations(addressesArrayTemp, sourceAddress);
        double shortsetDistance = Integer.MAX_VALUE;
        double tempDistance;
        ArrayList<LatLng> resultRoute = new ArrayList<>();
        for (ArrayList<LatLng> permutation : permutations) {
            tempDistance = calculateRouteDistance(permutation);
            if (tempDistance < shortsetDistance) {
                shortsetDistance = tempDistance;
                resultRoute = permutation;
            }
        }
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-solveTSPnew");
        return resultRoute;
    }

    public static List<ArrayList<LatLng>> generatePermutations(ArrayList<LatLng> addressesArray, LatLng sourceAddress) {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-generatePermutations");
        List<ArrayList<LatLng>> result = new ArrayList<>();
        generatePermutationsHelper(addressesArray, 0, result, sourceAddress);
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-generatePermutations");
        return result;
    }

    private static void generatePermutationsHelper(ArrayList<LatLng> array, int index, List<ArrayList<LatLng>> result, LatLng sourceAddress) {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-generatePermutationsHelper");
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
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-generatePermutationsHelper");
    }

    private void notifyUserAndHandleNewRoute(ArrayList<LatLng> newRoute) {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-notifyUserAndHandleNewRoute");
        // Check if the new route is different from the existing addressesArray
//        showNotification("Shorter Route", "A shorter route is available. Tap to update.");

        // TODO: Handle user interaction (e.g., when the user taps the notification)
        // Launch an activity or use other UI elements to handle the user's choice
        launchRouteUpdateActivity(newRoute);
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-notifyUserAndHandleNewRoute");
    }

    private void launchRouteUpdateActivity(ArrayList<LatLng> newRoute) {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-launchRouteUpdateActivity");
        // Launch the RouteUpdateActivity with the new route data
        Intent updateRouteIntent = new Intent(this, MainActivity.class);
        updateRouteIntent.putExtra("newRoute", newRoute);

        // Ensure unique requestCode for each PendingIntent
        int requestCode = (int) System.currentTimeMillis();

        PendingIntent updatePendingIntent = PendingIntent.getActivity(
                this, requestCode, updateRouteIntent, PendingIntent.FLAG_IMMUTABLE);

        // Build the notification action for updating the route
        NotificationCompat.Action updateAction =
                new NotificationCompat.Action.Builder(R.drawable.ic_yes,
                        "Yes", updatePendingIntent)
                        .build();

        // Build the notification
        Notification notification = new NotificationCompat.Builder(this, "5678")
                .setSmallIcon(R.drawable.icons8_notification)
                .setContentTitle("עדכון מסלול")
                .setContentText("מצאנו מסלול טוב יותר בשבילך! לחץ כאן כדי לעדכן מסלול.")
                .addAction(updateAction) // Add the update action
                .setPriority(NotificationCompat.PRIORITY_MAX) // Set notification priority to high
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .build();

        // Display the notification
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManagerCompat.notify(0, notification);
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-launchRouteUpdateActivity");
    }

    // In your RouteUpdateActivity, handle the new route in its onCreate method

    private void showNotification(String title, String content) {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-showNotification");
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "5678")
                .setSmallIcon(R.drawable.icons8_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Set notification priority to high
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManagerCompat.notify(0, notificationBuilder.build());
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-showNotification");
    }

    public void addCurrentLocation() {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-addCurrentLocation");
        Log.d("MainActivity", "in addCurrentLocation");
            // Check for permission and request if needed
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
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
                            if (addressesArray.get(0).latitude != currentLatLng.latitude && addressesArray.get(0).longitude != currentLatLng.longitude)
                                addressesArray.add(0, currentLatLng);
                            Log.d("MainActivity", "in addCurrentLocation - add to addressArray - current location: "+addressesArray.get(0));
                        }

                    }

                };
                startLocationUpdates(locationRequest, locationCallback);

            }
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-addCurrentLocation");
    }


    public void stopLocationUpdates() {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-stopLocationUpdates");
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates((com.google.android.gms.location.LocationCallback) locationCallback);
        }
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-stopLocationUpdates");
    }

    public void calculateDistance(){
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-calculateDistance");
        distanceArray.clear();
        int totalAddresses = addressesArray.size();
        Log.d("mylog", "addressesArray.size: " + addressesArray.size());
        expectedApiCalls = factorial(totalAddresses) / factorial(totalAddresses - 2);
        Log.d("mylog", "expectedApiCalls: " + expectedApiCalls);
        for (int i = 0; i < addressesArray.size(); i++) {
            LatLng address1 = addressesArray.get(i);

            for (int j = 0; j < addressesArray.size(); j++) {
                if (j == i)
                    continue;
                LatLng address2 = addressesArray.get(j);
                try {
                    distanceCalculator.calculateDistance(address1, address2, expectedApiCalls, LocationMonitoringForegroundService.this);
                } catch (IOException e) {
                    Log.d("mylog", "exception: ", e);
                }
            }
        }
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-calculateDistance");
    }

    private void startLocationUpdates(LocationRequest locationRequest, LocationCallback locationCallback) {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-startLocationUpdates");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "missed required permission");
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-calculateDistance");
    }

    public void stopTimer() {
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-stopTimer");
        Log.d("mylog", "stopping Timer");
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer_running = false;
        }
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-stopTimer");
    }
    public void onDestroy() {
        super.onDestroy();
        Log.d("mainFlow", "start-LocationMonitoringForegroundService-onDestroy");
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.cancel(5678);
        notificationManagerCompat.cancel(1234);
        // Unregister the receiver in onDestroy
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stopTimerReceiver);
        Log.d("mainFlow", "finish-LocationMonitoringForegroundService-onDestroy");
    }
}

