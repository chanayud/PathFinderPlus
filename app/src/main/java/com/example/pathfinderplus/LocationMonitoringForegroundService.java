package com.example.pathfinderplus;

import static com.example.pathfinderplus.MainActivity.addressesArray;
import static com.example.pathfinderplus.MainActivity.convertLatLngToAddress;
import static com.example.pathfinderplus.MainActivity.distanceArray;
import static com.example.pathfinderplus.MainActivity.factorial;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LocationMonitoringForegroundService extends Service implements LocationListener, GetDistanceTask.DistanceCallback{

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





    @Override
    public void onCreate() {
        super.onCreate();
        distanceCalculator = new DistanceCalculator(this);

        // Create the notification channel if running on Android Oreo (API level 26) or higher
        NotificationChannel channel = new NotificationChannel("1234", "service notification", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
//        routeCalculationRunnable = new Runnable() {
//            @Override
//        public void run() {
//            // Add your route calculation logic here
//                Log.d("mylog", "run: 3 minutes past");
//            checkAndCalculateRoute();
//
//            // Schedule the next execution after 3 minutes (180,000 milliseconds)
//            handler.postDelayed(this, 180000);
//        }
//    };
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("MainActivity", "onStartCommand: intent: " + intent);
        if (!isServiceRunning) {
            isServiceRunning = true;
            Timer timer = new Timer();

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Log.d("mylog", "timer is here");
                    checkAndCalculateRoute();
                }}, 180000 ,180000); // 60000 milliseconds = 1 minute

            try{
//            if (intent != null && intent.getAction() != null && intent.getAction().equals("START_NAVIGATION")) {
                targetLatitude = intent.getDoubleExtra("DESTINATION_LATITUDE", 0.0);
                targetLongitude = intent.getDoubleExtra("DESTINATION_LONGITUDE", 0.0);
                JOB_ID = intent.getStringExtra("JOB_ID");
                Log.d("MainActivity", "targetLatitude: " + targetLatitude + " targetLongitude: " + targetLongitude + " JOB_ID: " + JOB_ID);
            }
            catch (Exception e){
                Log.d("ex", "exception: " + e.getMessage());
            }
            handler.post(routeCalculationRunnable);


            startLocationMonitoring();

            // Create a notification to make the service a foreground service
            createNotification();
        }
            return START_STICKY;
        }


    private void createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, "1234")
                .setContentTitle("Location Monitoring Service")
                .setContentText("Monitoring location in the background")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void startLocationMonitoring() {
        Log.d("MainActivity", "startLocationMonitoring");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "startLocationMonitoring: requestLocationUpdates");

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 100, this);
        } else {
            Log.d("TAG", "startLocationMonitoring: Missing location permissions");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onLocationChanged(Location location) {
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
//            Intent broadcastIntent = new Intent("com.example.pathfinderplus.ACTION_TRIGGERED");
//            broadcastIntent.putExtra("JOB_ID", JOB_ID);
//            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
//            Log.d("MainActivity", "Broadcast sent");
            notify(JOB_ID);

        }
    }

    private void checkAndCalculateRoute() {
        int totalAddresses = addressesArray.size();
        expectedApiCalls = factorial(totalAddresses) / factorial(totalAddresses - 2);
        for (int i = 0; i < addressesArray.size(); i++) {
            LatLng address1 = addressesArray.get(i);

            for (int j = 0; j < addressesArray.size(); j++) {
                if(j==i)
                    continue;
                LatLng address2 = addressesArray.get(j);

                try {
                    distanceCalculator.calculateDistance(address1, address2, expectedApiCalls, LocationMonitoringForegroundService.this);
                } catch (IOException e) {
                    Log.d("mylog", "exception: ", e);
                }
            }
        }
    }


    private void notify(String jobId) {
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


        // Build the notification with a button to go back to our app
//        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "channel_id")
//                .setContentTitle("Destination Reached")
//                .setContentText("You have reached your destination.")
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setContentIntent(appPendingIntent)
//                .setAutoCancel(true)
//                .setPriority(NotificationCompat.PRIORITY_HIGH) // Set notification priority to high
//                .setDefaults(NotificationCompat.DEFAULT_ALL); // Set default notification behaviors

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, "5678")
                .setSmallIcon(R.drawable.icons8_notification)
                .setContentTitle("Destination Reached")
                .setContentText("You have reached your destination.")
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
        notificationManagerCompat.notify(0, notification.build());


        // Build the notification
        //   Notification notification = notificationBuilder.build();

        // Display the notification
      /*  NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(jobId.hashCode(), notification);
        }*/
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Implement if needed
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Implement if needed
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Implement if needed
    }

    @Override
    public void onDistanceCalculated(Distance distance)

    {
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
            // Check the length of "AddressesArray"
            if (addressesArray.size() > 5) {
                // Call the method for route calculation based on conditions
                routeCalculateBySimulatedAnealing();
                // Check if the new route is different from the existing one
                }
             else {
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Execute the code in a new thread
                Log.d("MainActivity", "newRoute.size:" + newRoute.size());

                ArrayList<LatLng> solution = TSPSolver.solveTSP(addressesArray, distanceArray);
                //   addressesArray.clear();
                newRoute = solution;
                Log.d("mylog", "addressesArray: " + newRoute.get(0).latitude + " " + newRoute.get(0).longitude);
                // Remove the first address since you've already reached it
                // addressesArray.remove(0);
                //  startNavigation(addressesArray.get(0));


//                // Print the solution
//                for (LatLng address : solution) {
//                    String stringAddress = convertLatLngToAddress(, address.latitude, address.longitude);
//                    Log.d("mylog", "anealing-address: "+ stringAddress);
//                }
            }
        }).start();
    }

    ;

    public void routeCalculateByNaiveAlgorithm() {
        Log.d("mylog", "addressesArray: " + addressesArray.get(0).latitude + " " + addressesArray.get(0).longitude);

        newRoute = solveTSPnew();
        // Print the solution
//        for (LatLng address : newRoute) {
//            String stringAddress = convertLatLngToAddress(MainActivity.this, address.latitude, address.longitude);
//            Log.d("mylog", "naive-address: " + stringAddress);
//        }
//        if (addressesArray.size() > 1) {
//            // Remove the first address since you've already reached it
//            addressesArray.remove(0);
//        }
    }

    private double calculateRouteDistance(List<LatLng> route) {
        double totalDistance = 0.0;
        for (int i = 0; i < route.size() - 1; i++) {
            LatLng current = route.get(i);
            Log.d("distance", "current: " + convertLatLngToAddress(this, current.latitude, current.longitude));

            LatLng next = route.get(i + 1);
            Log.d("distance", "next: " + convertLatLngToAddress(this, next.latitude, next.longitude));

            int distance = getDistanceBetween(current, next);
            Log.d("distance", "distance: " + distance);

            totalDistance += distance;
        }
        Log.d("distance", "totalDistance: " + totalDistance);

        return totalDistance;
    }

    private int getDistanceBetween(LatLng source, LatLng destination) {
        String sourceAddress = convertLatLngToAddress(this, source.latitude, source.longitude);
        String destinationAddress = convertLatLngToAddress(this, destination.latitude, destination.longitude);
        Log.d("mylog", "getDisBetween: source: " + sourceAddress + " destination: " + destinationAddress);

        for (Distance d : distanceArray) {
            String tempSource = convertLatLngToAddress(this, d.getOrigin().latitude, d.getOrigin().longitude);
            String tempDestination = convertLatLngToAddress(this, d.getDestination().latitude, d.getDestination().longitude);
            Log.d("mylog", "d: " + d.getDistance() + " tempSource: " + tempSource + " d.tempDestination: " + tempDestination);
            //  if (d.getOrigin().latitude == source.latitude && d.getOrigin().longitude == source.longitude  && d.getDestination().latitude == destination.latitude && d.getDestination().longitude == destination.longitude) {
            assert tempSource != null;
            if (tempSource.equals(sourceAddress)) {
                assert tempDestination != null;
                if (tempDestination.equals(destinationAddress)) {
                    Log.d("mylog", "getDistanceBetween: " + d.getDistance());
                    return d.getDistance();
                }
            }
        }
        return Integer.MAX_VALUE; // Handle case when distance is not found
    }

    private boolean isRouteShorter(ArrayList<LatLng> newRoute, ArrayList<LatLng> addressesArray) {
        double newCalculation = calculateRouteDistance(newRoute);
        double existingCalculation = calculateRouteDistance(addressesArray);
        if (newCalculation < existingCalculation)
            return true;
        return false;
    }

    public ArrayList<LatLng> solveTSPnew() {
        LatLng sourceAddress = addressesArray.get(0);
        addressesArray.remove(0);
        List<ArrayList<LatLng>> permutations = generatePermutations(addressesArray, sourceAddress);
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
        return resultRoute;

    }

    public static List<ArrayList<LatLng>> generatePermutations(ArrayList<LatLng> addressesArray, LatLng sourceAddress) {
        List<ArrayList<LatLng>> result = new ArrayList<>();
        generatePermutationsHelper(addressesArray, 0, result, sourceAddress);
        return result;
    }

    private static void generatePermutationsHelper(ArrayList<LatLng> array, int index, List<ArrayList<LatLng>> result, LatLng sourceAddress) {
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
    }

    private void notifyUserAndHandleNewRoute(ArrayList<LatLng> newRoute) {
        // Check if the new route is different from the existing addressesArray
        showNotification("Shorter Route", "A shorter route is available. Tap to update.");

        // TODO: Handle user interaction (e.g., when the user taps the notification)
        // Launch an activity or use other UI elements to handle the user's choice
        launchRouteUpdateActivity(newRoute);
    }

    private void launchRouteUpdateActivity(ArrayList<LatLng> newRoute) {
        // Launch the RouteUpdateActivity with the new route data
        Intent updateRouteIntent = new Intent(this, MainActivity.class);
        updateRouteIntent.putParcelableArrayListExtra("NEW_ROUTE", newRoute);
        PendingIntent updatePendingIntent = PendingIntent.getActivity(
                this, 0, updateRouteIntent, PendingIntent.FLAG_IMMUTABLE);

        // Build the notification action for updating the route
        NotificationCompat.Action updateAction =
                new NotificationCompat.Action.Builder(R.drawable.ic_yes,
                        "Yes", updatePendingIntent)
                        .build();

        // Build the notification
        Notification notification = new NotificationCompat.Builder(this, "5678")
                .setSmallIcon(R.drawable.icons8_notification)
                .setContentTitle("Destination Reached")
                .setContentText("You have reached your destination.")
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
    }

    // In your RouteUpdateActivity, handle the new route in its onCreate method

    private void showNotification(String title, String content) {
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
        }

    }

