package com.example.pathfinderplus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class HistoryList extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView noDataTextView;
    private ArrayList<Route> routeList; // ArrayList to hold routes
    List<String> expandableTitleList;
    ExpandableListView expandableListView;
    ExpandableListAdapter expandableListAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_list);

        expandableListView = (ExpandableListView) findViewById(R.id.expandableListViewSample);


        String email = getIntent().getStringExtra("EMAIL_ADDRESS");
        routeList = new ArrayList<>();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("users").document(email);

        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    Object data = documentSnapshot.get("listsOfLatLng");
                    if (data instanceof ArrayList) {
                        ArrayList<HashMap<String, Object>> dataList = (ArrayList<HashMap<String, Object>>) data;

                        for (HashMap<String, Object> item : dataList) {
                            if (item.containsKey("addresses")) {
                                ArrayList<HashMap<String, Double>> latLngList = (ArrayList<HashMap<String, Double>>) item.get("addresses");

                                Route route = new Route();
                                if (!latLngList.isEmpty()) {
                                    HashMap<String, Double> firstAddress = latLngList.get(0); // Get the first address
                                    double latitude = firstAddress.get("latitude");
                                    double longitude = firstAddress.get("longitude");

                                    Object title =  item.get("title");
                                    route.setTitle(title.toString()); // Set the first address as the title for the route

                                    ArrayList<LatLng> addresses = new ArrayList<>();
                                    for (HashMap<String, Double> latLngMap : latLngList) {
                                        latitude = latLngMap.get("latitude");
                                        longitude = latLngMap.get("longitude");
                                        addresses.add(new LatLng(latitude, longitude));
                                    }
                                    route.setAddresses(addresses); // Set addresses for the route
                                }
                                routeList.add(route);
                            }
                        }
                    }

                    if (!routeList.isEmpty()) {
                        HashMap<String, List<String>> expandableDetailList = ExpandableListDataItems.getData(routeList, HistoryList.this);
                        expandableTitleList = new ArrayList<String>(expandableDetailList.keySet());
                        expandableListAdapter = new CustomizedExpandableListAdapter(HistoryList.this, expandableTitleList, expandableDetailList);
                        expandableListView.setAdapter(expandableListAdapter);

                        // This method is called when the group is expanded
                        expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
                            @Override
                            public void onGroupExpand(int groupPosition) {
                                Toast.makeText(getApplicationContext(), expandableTitleList.get(groupPosition) + " List Expanded.", Toast.LENGTH_SHORT).show();
                            }
                        });

                        // This method is called when the group is collapsed
                        expandableListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
                            @Override
                            public void onGroupCollapse(int groupPosition) {
                                Toast.makeText(getApplicationContext(), expandableTitleList.get(groupPosition) + " List Collapsed.", Toast.LENGTH_SHORT).show();
                            }
                        });

                        // This method is called when the child in any group is clicked
                        // via a toast method, it is shown to display the selected child item as a sample
                        // we may need to add further steps according to the requirements
                        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                            @Override
                            public boolean onChildClick(ExpandableListView parent, View v,
                                                        int groupPosition, int childPosition, long id) {
                                List<String> addresses = expandableDetailList.get(expandableTitleList.get(groupPosition));

                                Intent intent = new Intent(HistoryList.this, MainActivity.class);
                                intent.putExtra("addresses", new ArrayList<>(addresses)); // Pass addresses
                                intent.putExtra("EMAIL_ADDRESS", email); // Pass email
                                startActivity(intent);


                                return false;
                            }
                        });
                    }

//                        CustomAdapter adapter = new CustomAdapter(HistoryList.this, routeList);
//                        recyclerView.setLayoutManager(new LinearLayoutManager(HistoryList.this));
//                        recyclerView.setAdapter(adapter);
//                        recyclerView.setVisibility(RecyclerView.VISIBLE);
//                        noDataTextView.setVisibility(TextView.GONE);
//                        adapter.setOnItemClickListener(new CustomAdapter.OnItemClickListener() {
//                            @Override
//                            public void onItemClick(int position) {
//                                // Handle item click here
//                                Route clickedRoute = routeList.get(position);
//
//                                // Create an Intent to pass the list of addresses back to MainActivity
//                                Intent intent = new Intent(HistoryList.this, MainActivity.class);
//                                intent.putExtra("addresses", clickedRoute.getAddresses()); // Pass addresses
//                                intent.putExtra("PASSWORD_EXTRA", password); // Pass password
//                                startActivity(intent);
//                            }
//                        });
//                    } else {
//                        recyclerView.setVisibility(RecyclerView.GONE);
//                        noDataTextView.setVisibility(TextView.VISIBLE);

                } else {
                    Toast.makeText(HistoryList.this, "Document does not exist", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(HistoryList.this, "Failed to retrieve data", Toast.LENGTH_SHORT).show();
            }
        });
    }


    public static String convertLatLngToAddress(Context context, double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        try {
            List<android.location.Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && addresses.size() > 0) {
                String address = addresses.get(0).getAddressLine(0);
                return address;
            }

        } catch (IOException e) {
            Log.d("mylog", "convertLatLngToAddress exception: ", e);
        }
        return null;
    }

}