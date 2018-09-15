package it.unipi.iet.bikedacity;

import android.content.Context;
import android.content.res.Resources;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class ShowStationAdapter extends RecyclerView.Adapter<ShowStationAdapter.StationViewHolder>{

    private TreeMap<Float, List<CityBikesStation>> dataSource;
    private List<Station> stations;
    private MainActivity mainActivity;
    private Button viewOnMap;

    public ShowStationAdapter (MainActivity ma, TreeMap<Float, List<CityBikesStation>> distanceMap){
        dataSource = distanceMap;
        stations = null;
        this.mainActivity = ma;
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

        // Fill textviews with informations
        final Station station = stations.get(position);
        Resources res = mainActivity.getResources();
        String distance = res.getString(R.string.station_list_distance, station.getDistance());
        String name = station.getCityBikeStation().getName();
        String availability;
        if (mainActivity.isShowingAvailablePlaces()){
            availability = res.getString(R.string.station_list_availability,
                                         station.getCityBikeStation().getEmptySlots(),
                                         res.getString(R.string.station_list_free_places));
        }
        else {
            availability = res.getString(R.string.station_list_availability,
                                         station.getCityBikeStation().getFreeBikes(),
                                         res.getString(R.string.station_list_free_bikes));
        }
        String details = res.getString(R.string.station_list_details, distance, availability);
        holder.stationDetail.setText(details);
        holder.stationName.setText(name);
        holder.viewOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                Location location = station.getCityBikeStation().getLocation();
                mainActivity.centreMapOn(location.getLatitude(), location.getLongitude());
            }
        });
    }

    @Override
    public int getItemCount () {
        if (stations == null) {
            stations = createStationList();
        }
        return stations.size();
    }

    public static class StationViewHolder extends RecyclerView.ViewHolder {
        private TextView stationName;
        private TextView stationDetail;
        private Button viewOnMap;

        public StationViewHolder (View itemView) {
            super(itemView);
            stationName = itemView.findViewById(R.id.station_name);
            stationDetail = itemView.findViewById(R.id.station_details);
            viewOnMap = itemView.findViewById(R.id.view_on_map);
        }
    }
}
