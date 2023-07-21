package com.example.pathfinderplus;

public class DistanceCalculator {

        public void calculateDistance(String origin, String destination, GetDistanceTask.DistanceCallback callback) {
            GetDistanceTask getDistanceTask = new GetDistanceTask();
            getDistanceTask.setDistanceCallback(callback);
            getDistanceTask.execute(origin, destination);
        }


}
