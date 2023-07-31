package com.example.pathfinderplus;

public class DistanceCalculator {

        public void calculateDistance(String  origin, String destination,int totalApiCalls, GetDistanceTask.DistanceCallback callback) {
            GetDistanceTask getDistanceTask = new GetDistanceTask(origin, destination, totalApiCalls);
            getDistanceTask.setDistanceCallback(callback);
            getDistanceTask.execute(origin, destination);
        }


}
