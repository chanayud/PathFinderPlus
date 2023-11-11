package com.example.pathfinderplus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;




public class Rout {

    public void getDistance(Address source, Address destination) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://waze.p.rapidapi.com/driving-directions?source_coordinates=" + source.getAddressCoordinates().latitude + "%2C" + source.getAddressCoordinates().longitude + "&destination_coordinates=" + destination.getAddressCoordinates().latitude + "%2C" + destination.getAddressCoordinates().longitude;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("X-RapidAPI-Key", "da76633f1cmshf087b4870fb71efp1ed373jsn2d5697eae636")
                .addHeader("X-RapidAPI-Host", "waze.p.rapidapi.com")
                .build();
        try {

            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String responseData = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    JSONArray routesArray = jsonObject.getJSONArray("data");
                    int fastestDuration = Integer.MAX_VALUE;
                    JSONObject fastestRoute = null;

                    // Iterate over each route object
                    for (int i = 0; i < routesArray.length(); i++) {
                        JSONObject routeObject = routesArray.getJSONObject(i);

                        // Get the "duration_seconds" field from the route object
                        int durationSeconds = routeObject.getInt("duration_seconds");

                        // Check if the current route is faster than the previous fastest route
                        if (durationSeconds < fastestDuration) {
                            fastestDuration = durationSeconds;
                            fastestRoute = routeObject;
                        }
                    }

                    // At this point, fastestRoute will contain the JSONObject of the fastest route
                    // You can access its properties as needed
                    if (fastestRoute != null) {
                        // Example: Get the duration of the fastest route
                        int durationSeconds = fastestRoute.getInt("duration_seconds");
                        // ... process the fastest route data
                    }



                    // Access JSON properties

                    String propertyValue = jsonObject.getString("duration_seconds");
                    // ... process the JSON data
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // Process the response data as needed
    } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
