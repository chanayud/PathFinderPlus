package com.example.pathfinderplus;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GetDistanceTask extends AsyncTask<String, Void, String> {
    private DistanceCallback distanceCallback;
    private String address1;
    private String address2;
    private int totalApiCalls;

    // Constructor to accept addresses and their indices
    public GetDistanceTask(String address1, String address2, int totalApiCalls) {
        this.address1 = address1;
        this.address2 = address2;
        this.totalApiCalls = totalApiCalls;

    }

    public interface DistanceCallback {
        void onDistanceCalculated(Distance distance);
        void onDistanceCalculationFailed(String errorMessage);
    }
    public void setDistanceCallback(DistanceCallback callback) {
        distanceCallback = callback;
    }
    private static final String TAG = "GetDistanceTask";
    private static final String API_KEY = "AIzaSyAXFwrx6mtQFOfHyTy6umAPjf5GJrAIY0A";
    long currentTimeMillis = System.currentTimeMillis();

    @Override
    protected String doInBackground(String... params) {
        String origin = params[0];
        String destination = params[1];
        String urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origin +
                "&destination=" + destination +
                "&key=" + API_KEY +
                "&departure_time=" + currentTimeMillis +
                "&traffic_model=best_guess";

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                bufferedReader.close();
                return stringBuilder.toString();
            } else {
                Log.e(TAG, "HTTP error code: " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                String status = jsonObject.getString("status");
                if (status.equals("OK")) {
                    JSONObject route = jsonObject.getJSONArray("routes").getJSONObject(0);
                    JSONObject leg = route.getJSONArray("legs").getJSONObject(0);
                    JSONObject duration = leg.getJSONObject("duration_in_traffic");
                    JSONObject startLocation = leg.getJSONObject("start_location");
                    double originLat = startLocation.getDouble("lat");
                    double originLng = startLocation.getDouble("lng");

                    // Destination coordinates
                    JSONObject endLocation = leg.getJSONObject("end_location");
                    double destLat = endLocation.getDouble("lat");
                    double destLng = endLocation.getDouble("lng");

                    Log.d("MYLOG", "Origin Coordinates: " + originLat + ", " + originLng);
                    Log.d("MYLOG", "Destination Coordinates: " + destLat + ", " + destLng);


                    int durationSeconds = duration.getInt("value");
                    Log.d("MYLOG", "Duration in seconds: " + durationSeconds);
                    Distance distance = new Distance();
                    distance.setOrigin(new LatLng(originLat,originLng));
                    distance.setDestination(new LatLng(destLat,destLng));
                    distance.setDistance(durationSeconds);
                    distance.setOriginAddress(address1);
                    distance.setDestinationAddress(address2);

                    if (distanceCallback != null) {
                        distanceCallback.onDistanceCalculated(distance);
                    }
                }
            }catch (JSONException e) {
                    if (distanceCallback != null) {
                        distanceCallback.onDistanceCalculationFailed("Invalid response format");
                    }
                }
                } else {
                    Log.e(TAG, "Directions API error: ");
                }



    }


}
