package it.unipi.iet.bikedacity;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

public class ShowStationAdapter extends RecyclerView.Adapter<ShowStationAdapter.StationViewHolder>{

    private TreeMap<Float, List<CityBikesStation>> dataSource;
    private List<Station> stations;

    public ShowStationAdapter (TreeMap<Float, List<CityBikesStation>> distanceMap){
        dataSource = distanceMap;
        stations = null;
    }

    private class Station {

        private CityBikesStation station;
        private float distance;

        public Station (CityBikesStation station, float distance){
            this.station = station;
            this.distance = distance;
        }

        public float getDistance () {
            return distance;
        }

        public CityBikesStation getCityBikeStation () {
            return station;
        }
    }

    private List<Station> createStationList (){
        List<Station> stations = new LinkedList<>();
        // It is a TreeMap and it is guaranteed the iterator over the set returns the
        // keys in ascending order
        for (Float distance : dataSource.keySet()){
            for (CityBikesStation cityBikesStation : dataSource.get(distance)){
                stations.add(new Station(cityBikesStation, distance));
            }
        }
        return stations;
    }

    private List<Station> getStations (){
        if (stations == null){
            stations = createStationList();
        }
        return stations;
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType) {
        Context ctx = parent.getContext();
        View stationView = LayoutInflater.from(ctx).inflate(R.layout.station_layout, parent, false);
        return new StationViewHolder(stationView);
    }

    @Override
    public void onBindViewHolder (@NonNull StationViewHolder holder, int position) {
        if (stations == null){
            stations = createStationList();
        }
        Station station = stations.get(position);
        holder.stationDistance.setText(String.format(Locale.getDefault(), "%f", station.getDistance()));
        holder.stationName.setText(station.getCityBikeStation().getName());
    }

    @Override
    public int getItemCount () {
        if (stations == null) {
            stations = createStationList();
        }
        return stations.size();
    }

    public static class StationViewHolder extends RecyclerView.ViewHolder{
        private TextView stationName;
        private TextView stationDistance;

        public StationViewHolder (View itemView) {
            super(itemView);
            stationName = itemView.findViewById(R.id.station_name);
            stationDistance = itemView.findViewById(R.id.station_distance);
        }
    }
}
