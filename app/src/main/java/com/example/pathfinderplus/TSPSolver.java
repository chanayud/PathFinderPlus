package com.example.pathfinderplus;

import static com.example.pathfinderplus.MainActivity.ConstraintsArray;
import static com.example.pathfinderplus.MainActivity.addressesArray;
import static com.example.pathfinderplus.MainActivity.distanceArray;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import kotlin.jvm.internal.Lambda;

public class TSPSolver {

    private static final double INITIAL_TEMPERATURE = 100;
    private static final double COOLING_RATE = 0.9;
    private static final int ITERATIONS_PER_TEMPERATURE = 10;


    public static ArrayList<LatLng> solveTSP(ArrayList<LatLng> cities, ArrayList<Distance> distances) {
        Log.d("parralele", "routeCalculateBySimulatedAnealing: parralele2");
        Log.d("MainActivity", "cities.size:"+cities.size());
        ArrayList<LatLng> bestSolution = new ArrayList<>();
        ArrayList<LatLng> currentSolution = new ArrayList<>(cities);

        double temperature = INITIAL_TEMPERATURE;

        while (temperature > 1) {
            Log.d("parralele", "routeCalculateBySimulatedAnealing: parralele3");
            for (int i = 0; i < ITERATIONS_PER_TEMPERATURE; i++) {
                ArrayList<LatLng> newSolution = new ArrayList<>(currentSolution);

                // Generate two random indices
                int index1, index2;
                do {
                    index1 = getRandomIndex(newSolution.size());
                } while (index1 == 0);
                do {
                    index2 = getRandomIndex(newSolution.size());
                } while (index2 == 0);

                // Swap two cities in the new solution
                Collections.swap(newSolution, index1, index2);

                // Calculate the energy (total distance) of current and new solution
                double currentEnergy = calculateEnergy(currentSolution, distances);
                double newEnergy = calculateEnergy(newSolution, distances);

                // Determine if we should accept the new solution
                if (acceptSolution(currentEnergy, newEnergy, temperature)) {
//                    int totalDistanceInSeconds;
//                    int currentAddressIndex = 0;
//                    int constraintAddressIndex = 0;
                    boolean constraintFlag = checkConstraints(newSolution, ConstraintsArray);
                    if(constraintFlag) {
                        currentSolution = new ArrayList<>(newSolution);
                        // Update the best solution
                        if (newEnergy < calculateEnergy(bestSolution, distances)) {
                            bestSolution = new ArrayList<>(newSolution);
                        }
                    }
                }
            }

            temperature *= COOLING_RATE;  // Reduce the temperature
        }
        return bestSolution;
    }

    private static int getRandomIndex(int size) {
        Log.d("MainActivity", "size: "+size);
        Random random = new Random();
        return random.nextInt(size);
    }

    private static double calculateEnergy(ArrayList<LatLng> solution, ArrayList<Distance> distances) {
        double energy = 0;
        if (solution.size() == 0)
            return Double.MAX_VALUE;
        for (int i = 0; i < solution.size() - 1; i++) {
            LatLng currentCity = solution.get(i);
            LatLng nextCity = solution.get(i + 1);
            for(int j = 0; j<distances.size(); j++){
                if(distances.get(j).getOrigin().latitude == currentCity.latitude && distances.get(j).getOrigin().longitude == currentCity.longitude && distances.get(j).getDestination().latitude == nextCity.latitude && distances.get(j).getDestination().longitude == nextCity.longitude)
                    energy+=distances.get(j).getDistance();
            }

        }

        return energy;
    }

    private static boolean acceptSolution(double currentEnergy, double newEnergy, double temperature) {
        if (newEnergy < currentEnergy) {
            return true;  // Always accept better solution
        }

        double acceptanceProbability = Math.exp((currentEnergy - newEnergy) / temperature);
        Random random = new Random();
        return random.nextDouble() < acceptanceProbability;
    }
    private static boolean checkConstraints(ArrayList<LatLng> route, List<Constraint> constraintArray) {
        for (Constraint constraint : constraintArray) {
            // Check time constraint
            if (constraint.getTimeConstraint() != 0) {
                int totalDistanceInSeconds = 0;
                for (int j = 0; j < route.size() - 1; j++) {
                    if (route.get(j).equals(constraint.getAddress()))
                        break;

                    for (Distance distance : distanceArray) {
                        if (route.get(j).equals(distance.getOrigin()) && route.get(j + 1).equals(distance.getDestination())) {
                            totalDistanceInSeconds += distance.getDistance();
                            break;
                        }
                    }
                }
                if (totalDistanceInSeconds > constraint.getTimeConstraint()) {
                    return false; // Constraint violation
                }
            }

            // Check place constraint
            if (constraint.getAddressConstraint() != null) {
                int currentAddressIndex = route.indexOf(constraint.getAddress());
                int constraintAddressIndex = route.indexOf(constraint.getAddressConstraint());

                if (constraintAddressIndex < currentAddressIndex) {
                    return false; // Constraint violation
                }
            }
        }
        return true; // All constraints met
    }

}



