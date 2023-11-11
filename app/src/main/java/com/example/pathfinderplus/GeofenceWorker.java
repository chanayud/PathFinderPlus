package com.example.pathfinderplus;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class GeofenceWorker extends Worker {
    int GEOFENCE_NOTIFICATION_CHANNEL = 1;
    String GEOFENCE_NOTIFICATION_CHANNEL_ID = "geofence_notifications";


    public GeofenceWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);
    }



    @NonNull
    @Override
    public Result doWork() {
        Log.d("geofence", "doWork: ");
        // Handle geofence transition (user entered the geofence)
        Context context = getApplicationContext();
        String geofenceId = getInputData().getString("geofence_id");

        // Create and send a notification to inform the user about the geofence transition
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        Notification notification = new Notification.Builder(context, GEOFENCE_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Geofence Transition")
                .setContentText("You entered a geofence!")
                .setSmallIcon(R.drawable.icons8_notification)
                        .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        // Notify the user
        notificationManager.notify(GEOFENCE_NOTIFICATION_CHANNEL, notification);

        // Log to confirm that the worker is executed
        Log.d("GeofenceWorker", "Geofence transition detected. Notifying the user.");

        return Result.success();
    }
}
