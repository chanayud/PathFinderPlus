package com.example.pathfinderplus;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;

public class SplashActivity extends AppCompatActivity {
    Handler handler;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);



        handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent intent=new Intent(SplashActivity.this,LoginActivity.class);
                startActivity(intent);
                finish();
            }
        },3000);

    }

}