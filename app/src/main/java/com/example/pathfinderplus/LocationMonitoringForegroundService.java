package com.example.pathfinderplus;

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
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class LocationMonitoringForegroundService extends Service implements LocationListener {

    private boolean isServiceRunning = false;


    private static final int TRIGGER_DISTANCE_METERS = 100;
    private static final int NOTIFICATION_ID = 123;

    private LocationManager locationManager;
    private double targetLatitude;
    private double targetLongitude;
    private String JOB_ID;

    private final IBinder binder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();

        // Create the notification channel if running on Android Oreo (API level 26) or higher
        NotificationChannel channel = new NotificationChannel("channel_id", "Channel Name", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("MainActivity", "onStartCommand: intent: " + intent);
        if (!isServiceRunning) {
            isServiceRunning = true;

            // if (intent != null && intent.getAction() != null && intent.getAction().equals("START_NAVIGATION")) {
            targetLatitude = intent.getDoubleExtra("DESTINATION_LATITUDE", 0.0);
            targetLongitude = intent.getDoubleExtra("DESTINATION_LONGITUDE", 0.0);
            JOB_ID = intent.getStringExtra("JOB_ID");
            Log.d("MainActivity", "targetLatitude: " + targetLatitude + " targetLongitude: " + targetLongitude + " JOB_ID: " + JOB_ID);
            //  }
            // Clear the intent extras to avoid using the same values if the activity is recreated
            //intent.removeExtra("DESTINATION_LATITUDE");
            //intent.removeExtra("DESTINATION_LONGITUDE");
            //intent.removeExtra("JOB_ID");
        }

        startLocationMonitoring();

        // Create a notification to make the service a foreground service
        createNotification();

        return START_STICKY;
    }

    private void createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, "channel_id")
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

//    private void stopLocationMonitoring() {
//        Log.d("MainActivity", "stopLocationMonitoring: ");
//        if (locationManager != null) {
//            locationManager.removeUpdates(this);
//        }
//    }

    //@Override
//    public void onDestroy() {
//        super.onDestroy();
//        isServiceRunning = false;
//        stopLocationMonitoring();
//    }

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
            NotificationChannel channel = new NotificationChannel("channel_id", "Channel Name", NotificationManager.IMPORTANCE_HIGH);
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

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, "channel_id")
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

    public class LocalBinder extends Binder {
        LocationMonitoringForegroundService getService() {
            return LocationMonitoringForegroundService.this;
        }
    }
}
