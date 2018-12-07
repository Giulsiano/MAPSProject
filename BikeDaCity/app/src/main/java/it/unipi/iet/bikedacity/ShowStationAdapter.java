package it.unipi.iet.bikedacity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;

public class ShowStationAdapter extends RecyclerView.Adapter<ShowStationAdapter.StationViewHolder>{

    private SortedMap<Integer, List<CityBikesStation>> dataSource;
    private List<Station> stations;
    private Context context;
    private MapView map;
    private boolean isShowingParking;
    private final long animationDuration = 1000L;
    private double zoom;

    public ShowStationAdapter (Context context, SortedMap<Integer, List<CityBikesStation>> distanceMap,
                               MapView map, boolean isShowingParking){
        dataSource = distanceMap;
        stations = null;
        this.context = context;
        this.map = map;
        this.isShowingParking = isShowingParking;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Resources resources = context.getResources();
        zoom = (double) Float.parseFloat(preferences.getString(resources.getString(R.string.zoom_list_key),
                                                           resources.getString(R.string.default_zoom_value)));
    }

    public void setDataSource (SortedMap<Integer, List<CityBikesStation>> distanceMap){
        if (!(distanceMap == null || distanceMap.equals(dataSource))){
            dataSource = distanceMap;
            stations = createStationList();
        }
    }

    public void setShowingParking (boolean isShowingParking){
        this.isShowingParking = isShowingParking;
    }

    private class Station {

        private CityBikesStation station;
        private int distance;

        public Station (CityBikesStation station, int distance){
            this.station = station;
            this.distance = distance;
        }

        public int getDistance () {
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
        for (Integer distance : dataSource.keySet()){
            for (CityBikesStation cityBikesStation : dataSource.get(distance)){
                stations.add(new Station(cityBikesStation, distance));
            }
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
        Resources res = context.getResources();
        String distance = res.getString(R.string.station_list_distance, station.getDistance());
        String name = station.getCityBikeStation().getName();
        String availability;
        int backgroundId;
        if (isShowingParking){
            availability = res.getString(R.string.station_list_availability,
                                         station.getCityBikeStation().getEmptySlots(),
                                         res.getString(R.string.station_list_free_places));
            backgroundId = BikeDaCityUtil.getOverlayDrawableId(station.getCityBikeStation().getFreePlacesLevel(),
                                                               isShowingParking);
        }
        else {
            availability = res.getString(R.string.station_list_availability,
                                         station.getCityBikeStation().getFreeBikes(),
                                         res.getString(R.string.station_list_free_bikes));
            backgroundId = BikeDaCityUtil.getOverlayDrawableId(station.getCityBikeStation().getAvailableBikesLevel(),
                                                               isShowingParking);
        }
        String details = res.getString(R.string.station_list_details, distance, availability);
        holder.stationDetail.setText(details);
        holder.stationName.setText(name);
        holder.viewOnMap.setBackgroundResource(backgroundId);
        holder.viewOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                Location location = station.getCityBikeStation().getLocation();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                IMapController controller = map.getController();
                controller.zoomTo(zoom, animationDuration);
                controller.setCenter(new GeoPoint(latitude, longitude));
            }
        });

        // Set the button icon to something appropriate
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
