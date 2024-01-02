package com.example.pathfinderplus;

import android.content.Context;
import android.location.Geocoder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Route> routeList;


    private ArrayList<String> addresses;

    public CustomAdapter(Context context, ArrayList<Route> routeList) {
        this.context = context;
        this.routeList = routeList;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView addressTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            addressTextView = itemView.findViewById(R.id.addressTextView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onItemClick(position);
                    }
                }
            });

        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Route route = routeList.get(position);

        holder.titleTextView.setText(route.getTitle()); // Set route title

        // Get the addresses for this route
        addresses = new ArrayList<>();
        for (LatLng latLng : route.getAddresses()) {
            String address = convertLatLngToAddress(context, latLng.latitude, latLng.longitude);
            addresses.add(address);
        }

        // Join the address strings for this route and set in addressTextView
        holder.addressTextView.setText(TextUtils.join("\n", addresses));
    }

    @Override
    public int getItemCount() {
        return routeList.size();
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
    public ArrayList<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(ArrayList<String> addresses) {
        this.addresses = addresses;
    }
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

}
