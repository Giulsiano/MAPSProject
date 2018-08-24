package it.unipi.iet.bikedacity;

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
    private String city;

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

    private Map<Float, List<CityBikesStation>> getStationsOrderedByDistanceFrom (Location location){
        // Use a TreeMap that implements SortedMap interface to automatically obtain an ordered map
        float distance;
        Map<Float, List<CityBikesStation>> distanceMap = new TreeMap<>();
        List<CityBikesStation> cityStations = downloader.getStationsOf(city);
        for (CityBikesStation station : cityStations){
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

        Map<Float, List<CityBikesStation>> distanceMap = getStationsOrderedByDistanceFrom(location);
        Collection<List<CityBikesStation>> mapValues = distanceMap.values();
        for (List<CityBikesStation> stations : mapValues){
            Collections.sort(stations, CityBikesStation.FreePlaceComparator);
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

        Map<Float, List<CityBikesStation>> distanceMap = getStationsOrderedByDistanceFrom(location);
        Collection<List<CityBikesStation>> mapValues = distanceMap.values();
        for (List<CityBikesStation> stations : mapValues){
            Collections.sort(stations, CityBikesStation.AvailableBikesComparator);
        }
        return distanceMap;
    }
}
