package com.example.pathfinderplus;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class LatLngAdapter extends RecyclerView.Adapter<LatLngAdapter.ViewHolder> {

    private List<LatLng> latLngList;

    public LatLngAdapter(List<LatLng> latLngList) {
        this.latLngList = latLngList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_latlng, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LatLng latLng = latLngList.get(position);
        holder.bind(latLng);
    }

    @Override
    public int getItemCount() {
        return latLngList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textViewLatLng);
        }

        public void bind(LatLng latLng) {
            // Bind data to views
            textView.setText("LatLng: " + latLng.latitude + ", " + latLng.longitude);
        }
    }
}
