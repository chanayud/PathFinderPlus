package com.example.pathfinderplus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

    public class GeofenceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.pathfinderplus.GEOFENCE_ACTION".equals(intent.getAction())) {
                // Start the foreground service when the geofence event is received
                Intent serviceIntent = new Intent(context, GeofenceForegroundService.class);
                context.startForegroundService(serviceIntent);
            }
        }


}
