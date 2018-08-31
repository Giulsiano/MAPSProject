package it.unipi.iet.bikedacity;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class CitybikesManager {
    private static String TAG = "CitybikesManager";
    private CityBikesDownloader downloader;
    private Context context;
    private String city;

    private List<CityBikesStation> stations;

    // TODO add storing networks' JSON to a file

    public CitybikesManager (){
        downloader = new CityBikesDownloader();
        city = null;
    }
    public CitybikesManager (String city){
        downloader = new CityBikesDownloader();
        city = new String(city);
    }

    public List<CityBikesStation> getStations () {
        return stations;
    }

    public String getCity () {
        return city;
    }

    public void setCity (String c) {
        city = c;
    }

    private TreeMap<Float, List<CityBikesStation>> getStationsOrderedByDistanceFrom (Location location){
        // Use a TreeMap that implements SortedMap interface to automatically obtain an ordered map
        float distance;
        TreeMap<Float, List<CityBikesStation>> distanceMap = new TreeMap<>();
        if (stations == null){
            stations = downloader.getStationsOf(city);
        }
        for (CityBikesStation station : stations){
            distance = location.distanceTo(station.getLocation());
            if (distanceMap.containsKey(distance)){
                List<CityBikesStation> stationList = distanceMap.get(distance);
                stationList.add(station);
                distanceMap.put(location.distanceTo(station.getLocation()), stationList);
            }
            else {
                LinkedList<CityBikesStation> stationList = new LinkedList<>();
                stationList.add(station);
                distanceMap.put(distance, stationList);
            }
        }
        return distanceMap;
    }

    /**
     * Returns an ordered by distance Map containing the nearest stations with at least one free place
     * nearest to the location passed by parameter.
     * @param location  Where compute the minimum distance from
     * @return  An ordered by distance Map (TreeMap in this case) which contains the list of stations ordered by
     * availability. This method will return null if the location is null itself.
     */
    public TreeMap<Float, List<CityBikesStation>> getNearestFreePlacesFrom (Location location){
        if (location == null){
            Log.e(TAG, "Location passed is null");
            return null;
        }
        if (city == null){
            throw new NullPointerException("City is null");
        }
        // Order the station by their free place availability
        TreeMap<Float, List<CityBikesStation>> distanceMap = getStationsOrderedByDistanceFrom(location);
        Collection<Float> mapDistances = distanceMap.keySet();
        for (Float distance : mapDistances){
            List<CityBikesStation> stations = distanceMap.get(distance);
            Collections.sort(stations, CityBikesStation.FreePlaceComparator);

            // TODO try to find a way to not prune unavailable stations
            // Prune the map by removing no free place stations
            if (stations.get(0).getFreePlacesLevel() == CityBikesStation.Availability.NO){
                mapDistances.remove(distance);
            }
        }
        return distanceMap;
    }

    public TreeMap<Float, List<CityBikesStation>> getNearestAvailableBikesFrom (Location location){
        if (location == null){
            Log.e(TAG, "Location passed is null");
            return null;
        }
        if (city == null){
            throw new NullPointerException("City is null");
        }
        // Order the station by their free place availability
        TreeMap<Float, List<CityBikesStation>> distanceMap = getStationsOrderedByDistanceFrom(location);
        Collection<Float> mapDistances = distanceMap.keySet();
        for (Float distance : mapDistances){
            List<CityBikesStation> stations = distanceMap.get(distance);
            Collections.sort(stations, CityBikesStation.AvailableBikesComparator);

            // Prune the map by removing no available bikes stations
            if (stations.get(0).getAvailableBikesLevel() == CityBikesStation.Availability.NO){
                mapDistances.remove(distance);
            }
        }
        return distanceMap;
    }
}
