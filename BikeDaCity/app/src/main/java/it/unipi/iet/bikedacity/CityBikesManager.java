package it.unipi.iet.bikedacity;

import android.location.Location;
import android.util.Log;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class CityBikesManager {
    private static String TAG = "CityBikesManager";
    private CityBikesDownloader downloader;
    private String city;
    private List<CityBikesStation> stations;

    // TODO add storing networks' JSON to a file
    public CityBikesManager (String city, CityBikesDownloader downloader){
        this.city = city;
        this.downloader = downloader;
    }

    public CityBikesManager (String city){
        downloader = new CityBikesDownloader();
        this.city = city;
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
        Log.i(TAG, "Order stations by distance");
        if (stations == null){
            Log.d(TAG, "Station object is null. Downloading stations");
            stations = downloader.getStationsOf(city);
        }
        for (CityBikesStation station : stations){
            distance = location.distanceTo(station.getLocation());
            if (distanceMap.containsKey(distance)){
                Log.d("TAG", "Add station to list");
                List<CityBikesStation> stationList = distanceMap.get(distance);
                stationList.add(station);
                distanceMap.put(distance, stationList);
            }
            else {
                Log.d(TAG, "Add new key: " + distance);
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
        Log.i(TAG, "Get nearest station with free places available");
        if (location == null){
            Log.e(TAG, "Location passed is null");
            return null;
        }
        if (city == null){
            Log.e(TAG, "City passed is null");
            return null;
        }
        // Order the station by their free place availability
        TreeMap<Float, List<CityBikesStation>> distanceMap = getStationsOrderedByDistanceFrom(location);
        Log.i(TAG,"Ordering the list of stations by availability");
        for (Iterator<Float> it = distanceMap.keySet().iterator(); it.hasNext();){
            Float distance = it.next();
            List<CityBikesStation> stations = distanceMap.get(distance);
            Collections.sort(stations, CityBikesStation.FreePlaceComparator);

            // TODO try to find a way to not prune unavailable stations
            // Prune the map by removing no free place stations
            if (stations.get(0).getFreePlacesLevel() == CityBikesStation.Availability.NO){
                it.remove();
            }
        }
        return distanceMap;
    }

    public TreeMap<Float, List<CityBikesStation>> getNearestAvailableBikesFrom (Location location){
        Log.i(TAG, "Get nearest station with available bikes");
        if (location == null){
            Log.e(TAG, "Location passed is null");
            return null;
        }
        if (city == null){
            Log.e(TAG, "City passed is null");
            return null;
        }
        // Order the station by their free place availability
        TreeMap<Float, List<CityBikesStation>> distanceMap = getStationsOrderedByDistanceFrom(location);
        Log.i(TAG,"Ordering the list of stations by availability");
        for (Iterator<Float> it = distanceMap.keySet().iterator(); it.hasNext();){
            Float distance = it.next();
            List<CityBikesStation> stations = distanceMap.get(distance);
            Collections.sort(stations, CityBikesStation.AvailableBikesComparator);

            // Prune the map by removing no available bikes stations
            if (stations.get(0).getAvailableBikesLevel() == CityBikesStation.Availability.NO){
                it.remove();
            }

        }
        return distanceMap;
    }
}