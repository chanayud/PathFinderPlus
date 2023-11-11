package com.example.pathfinderplus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class GeofenceForegroundService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
    }

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "PennSkanvTicChannel", NotificationManager.IMPORTANCE_MAX);
//        channel.setDescription("PennSkanvTic channel for foreground service notification");
//
//        notificationManager = getSystemService(NotificationManager.class);
//        notificationManager.createNotificationChannel(channel);
//
//        if ("STOP_SERVICE_ACTION".equals(intent.getAction())) {
//            stopForeground(true);
//            stopSelf();
//        } else {
//            // Place your background processing logic here
//
//            // Display a notification to indicate that the service is running in the foreground
//            Notification notification = createNotification();
//            startForeground(1, notification);
//        }
//
//        return START_STICKY;
//    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification createNotification() {
        // Create a notification for the foreground service
        // You can customize this notification to provide relevant information to the user
        // For example, show the geofence event details
        // Add an action to stop the foreground service if needed

        Intent stopIntent = new Intent(this, GeofenceForegroundService.class);
        stopIntent.setAction("STOP_SERVICE_ACTION");
        PendingIntent pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Geofence Processing")
                .setContentText("Background processing is in progress")
                .setSmallIcon(R.drawable.icons8_notification)
                .setContentIntent(pendingStopIntent)
                .build();

        return notification;
    }
}
