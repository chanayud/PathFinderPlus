package com.example.pathfinderplus;

import static com.example.pathfinderplus.MainActivity.ConstraintsArray;
import static com.example.pathfinderplus.MainActivity.addressesArray;
import static com.example.pathfinderplus.MainActivity.distanceArray;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;

public class MinimalSpanningTree {

    // Class to represent an edge
    private static class Edge implements Comparable<Edge> {
        int src, dest, weight;
//        Context context;

        public Edge(int src, int dest, int weight) {
            this.src = src;
            this.dest = dest;
            this.weight = weight;
        }

        @Override
        public int compareTo(Edge edge) {
            return Integer.compare(this.weight, edge.weight);
        }
    }

    // Function to find the minimal spanning tree using Prim's algorithm
    private static ArrayList<Edge> findMST(ArrayList<Edge>[] graph) {
        Log.d("parralele", "routeCalculateByMinimalSpanningTree: parralele3 "+graph[0]);
        PriorityQueue<Edge> minHeap = new PriorityQueue<>();
        ArrayList<Edge> result = new ArrayList<>();
        boolean[] visited = new boolean[graph.length];

        visited[0] = true;

        for (Edge edge : graph[0]) {
            minHeap.add(edge);
        }

        while (!minHeap.isEmpty()) {
            Edge currentEdge = minHeap.poll();

            if (!visited[currentEdge.dest]) {
                visited[currentEdge.dest] = true;
                result.add(currentEdge);

                for (Edge edge : graph[currentEdge.dest]) {
                    if (!visited[edge.dest]) {
                        minHeap.add(edge);
                    }
                }
            }
        }

        return result;
    }

    // Function to find the best route based on MST
// Function to find the best route based on MST
// Function to find the best route based on MST
    public static ArrayList<LatLng> findBestRoute(ArrayList<LatLng> addresses, ArrayList<Distance> distances, Context context) {
        Log.d("parralele", "routeCalculateByMinimalSpanningTree: parralele2");
        int n = addresses.size();
        ArrayList<Edge>[] graph = new ArrayList[n];

        for (int i = 0; i < n; i++) {
            graph[i] = new ArrayList<>();
        }

        for (Distance distance : distances) {
            int src = addresses.indexOf(distance.getOrigin());
            int dest = addresses.indexOf(distance.getDestination());
            int weight = distance.getDistance();

            graph[src].add(new Edge(src, dest, weight));
            graph[dest].add(new Edge(dest, src, weight));  // Ensure to add the reverse edge as well
        }

        ArrayList<Edge> minimalSpanningTree = findMST(graph);

        // Convert edge destinations to LatLng
        ArrayList<LatLng> route = new ArrayList<>();
        for (Edge edge : minimalSpanningTree) {
            route.add(addresses.get(edge.dest));
        }

        // Adding the first LatLng in the cycle
        route.add(0, addresses.get(minimalSpanningTree.get(0).src));
        int maxAttempts = addressesArray.size() * addressesArray.size(); // Set a maximum number of attempts
        int attempt = 1;

        while (attempt <= maxAttempts && !isRouteAcceptable(route, context)) {
            // If the route is not acceptable, find a new one or take appropriate action
            route = findNewRoute(addresses, distances, context, route);
            attempt ++ ;
        }

        return route;
    }
    private static ArrayList<LatLng> findNewRoute(ArrayList<LatLng> addresses, ArrayList<Distance> distances, Context context, ArrayList<LatLng> previousRoute) {
        int maxAttempts = addresses.size() * addresses.size(); // Set a maximum number of attempts
        int attempt = 1;

        while (attempt <= maxAttempts) {
            ArrayList<Edge>[] graph = buildGraph(addresses, distances, previousRoute);
            ArrayList<Edge> minimalSpanningTree = findMST(graph);

            // Convert edge destinations to LatLng
            ArrayList<LatLng> newRoute = new ArrayList<>();
            if (!minimalSpanningTree.isEmpty()) {
                for (Edge edge : minimalSpanningTree) {
                    newRoute.add(addresses.get(edge.dest));
                }

                // Adding the first LatLng in the cycle
                newRoute.add(0, addresses.get(minimalSpanningTree.get(0).src));

                // Check user constraints
                if (isRouteAcceptable(newRoute, context)) {
                    return newRoute;
                }
            }

            attempt++;
        }

        // If no acceptable route is found within the attempts limit, handle it accordingly
        // You can throw an exception, return null, or take other appropriate action
        return new ArrayList<>();
    }

    private static ArrayList<Edge>[] buildGraph(ArrayList<LatLng> addresses, ArrayList<Distance> distances, ArrayList<LatLng> previousRoute) {
        int n = addresses.size();
        ArrayList<Edge>[] graph = new ArrayList[n];

        for (int i = 0; i < n; i++) {
            graph[i] = new ArrayList<>();
        }

        for (Distance distance : distances) {
            int src = addresses.indexOf(distance.getOrigin());
            int dest = addresses.indexOf(distance.getDestination());

            if (!previousRoute.contains(addresses.get(src)) && !previousRoute.contains(addresses.get(dest))) {
                // Edge not part of the previous route, consider it
                // Add edge to the graph
                graph[src].add(new Edge(src, dest, distance.getDistance()));
                graph[dest].add(new Edge(dest, src, distance.getDistance()));
            }
        }

        return graph;
    }

    private static boolean isRouteAcceptable(ArrayList<LatLng> route, Context context) {
        // Return true if the route is acceptable, false otherwise
        return checkConstraints(route);
    }

    private static boolean checkConstraints(ArrayList<LatLng> route) {
        for (Constraint constraint : ConstraintsArray) {
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