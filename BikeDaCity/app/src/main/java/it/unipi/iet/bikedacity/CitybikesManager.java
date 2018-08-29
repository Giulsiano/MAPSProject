package it.unipi.iet.bikedacity;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    public String getCity () {
        return city;
    }

    public void setCity (String c) {
        city = c;
    }

    private Map<Float, List<CityBikesStation>> getStationsOrderedByDistanceFrom (Location location){
        // Use a TreeMap that implements SortedMap interface to automatically obtain an ordered map
        float distance;
        Map<Float, List<CityBikesStation>> distanceMap = new TreeMap<>();
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

    public Map<Float, List<CityBikesStation>> getNearestFreePlacesFrom (Location location){
        if (location == null){
            Log.e(TAG, "Location passed is null");
            return null;
        }
        if (city == null){
            throw new NullPointerException("City is null");
        }
        // Order the station by their free place availability
        TreeMap<Float, List<CityBikesStation>> distanceMap = (TreeMap)
                getStationsOrderedByDistanceFrom(location);
        Collection<Float> mapDistances = distanceMap.keySet();
        for (Float distance : mapDistances){
            List<CityBikesStation> stations = distanceMap.get(distance);
            Collections.sort(stations, CityBikesStation.FreePlaceComparator);

            // Prune the map by removing no free place stations
            if (stations.get(0).getFreePlacesLevel() == CityBikesStation.Availability.NO){
                mapDistances.remove(distance);
            }
        }

        return distanceMap;
    }

    public Map<Float, List<CityBikesStation>> getNearestAvailableBikesFrom (Location location){
        if (location == null){
            Log.e(TAG, "Location passed is null");
            return null;
        }
        if (city == null){
            throw new NullPointerException("City is null");
        }
        // Order the station by their free place availability
        TreeMap<Float, List<CityBikesStation>> distanceMap = (TreeMap)
                getStationsOrderedByDistanceFrom(location);
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
