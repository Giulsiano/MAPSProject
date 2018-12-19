package it.unipi.iet.bikedacity;

import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;

public class CityBikesManager {
    private static String TAG = "CityBikesManager";
    private CityBikesDownloader downloader;
    private String city;
    private List<CityBikesStation> stations;

    public CityBikesManager (String city){
        downloader = new CityBikesDownloader();
        this.city = city;
    }

    public CityBikesManager(){
        this(null);
    }

    public boolean cityHasBikeService() {
        if (city == null) return false;
        return stations != null && stations.size() != 0;
    }

    public List<CityBikesStation> getStations () {
        return stations;
    }

    public String getCity () {
        return city;
    }

    public void setCity (String city) {
        this.city = city;
    }

    private SortedMap<Integer, List<CityBikesStation>> getStationsOrderedByDistanceFrom (Location location){
        // Use a TreeMap that implements SortedMap interface to automatically obtain an ordered map
        TreeMap<Integer, List<CityBikesStation>> distanceMap = new TreeMap<>();
        Log.i(TAG, "Order stations by distance");
        if (city == null){
            Log.w(TAG, "getStationsOrderedByDistanceFrom: City has not been set.");
            return distanceMap;
        }
        stations = downloader.getStationsOf(city);
        int distance;
        for (CityBikesStation station : stations){
            distance = Math.round(location.distanceTo(station.getLocation()));
            if (distanceMap.containsKey(distance)){
                List<CityBikesStation> stationList = distanceMap.get(distance);
                stationList.add(station);
                distanceMap.put(distance, stationList);
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
    public SortedMap<Integer, List<CityBikesStation>> getNearestFreePlacesFrom (Location location){
        Log.i(TAG, "Get nearest station with free places available");
        if (location == null){
            Log.e(TAG, "Location passed is null");
            return null;
        }
        // Order the station by their free place availability
        SortedMap<Integer, List<CityBikesStation>> distanceMap = getStationsOrderedByDistanceFrom(location);
        Log.i(TAG,"Ordering the list of stations by availability");
        for (Integer distance : distanceMap.keySet()){
            List<CityBikesStation> stations = distanceMap.get(distance);
            Collections.sort(stations, CityBikesStation.FreePlaceComparator);
        }
        return distanceMap;
    }

    public SortedMap<Integer, List<CityBikesStation>> getNearestAvailableBikesFrom (Location location){
        Log.i(TAG, "Get nearest station with available bikes");
        if (location == null){
            Log.e(TAG, "Location passed is null");
            return null;
        }
        // Order the station by their free place availability
        SortedMap<Integer, List<CityBikesStation>> distanceMap = getStationsOrderedByDistanceFrom(location);
        Log.i(TAG,"Ordering the list of stations by availability");
        for (Integer distance : distanceMap.keySet()){
            List<CityBikesStation> stations = distanceMap.get(distance);
            Collections.sort(stations, CityBikesStation.AvailableBikesComparator);
        }
        return distanceMap;
    }

    public class CityBikesDownloader {
        private static final String TAG = "CityBikeDownloader";
        public static final String CITYBIKESAPIURL = "https://api.citybik.es";
        public static final String NETWORKSENDPOINT = "/v2/networks/";
        private JSONArray jsonNetworks;

        public CityBikesDownloader(){
        }

        /**
         * Download the content from the URL passed by parameter. It manages HTTP 200 response
         * code cases only, if the HTTP service returns something different this method returns an
         * empty string.
         * The method behaves like an HTTP client so will return the raw content the HTTP service
         * returns, in particular case it has been designed to return a String that contains raw
         * JSON.
         * @param url   Which is the resource to download from
         * @return  A String representing the content or an empty string in case of errors. The format
         * expected is JSON.
         */
        private String downloadContentFrom (String url){
            StringBuilder content = new StringBuilder();
            try {
                URL site = new URL(url);
                HttpsURLConnection connection = (HttpsURLConnection) site.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                int status = connection.getResponseCode();
                Log.d(TAG, "status is : " + status);
                if (status >= 200 && status < 300){
                    Log.i(TAG, "Received response from the site: OK");
                    try (InputStreamReader is = new InputStreamReader(connection.getInputStream())){
                        BufferedReader reader = new BufferedReader(is);
                        String line;
                        while ((line = reader.readLine()) != null){
                            content.append(line);
                        }
                        reader.close();
                    }
                }
                else if (status >= 300 && status < 400){
                    Log.i(TAG, "Received response from the site, status code: " + status);
                }
                else if (status >= 400 && status < 500){
                    Log.e(TAG, "Error in request, status code: " + status);
                }
                else if (status >= 500){
                    Log.e(TAG, "Server error, status code: " + status);
                }
                connection.disconnect();
            }
            catch (MalformedURLException e){
                Log.e(TAG, e.getClass().getSimpleName() + ": " + e.getMessage());
                Log.e(TAG, "URL used: " + url);
            }
            catch (IOException e) {
                Log.e(TAG, "Exception " + e.getClass().getSimpleName() + ". Message: " + e
                        .getMessage());
            }
            return content.toString();
        }

        /**
         * THis method downloads the JSON from citybikes and return only the array of the
         * networks known by the site. It filters out useless information.
         * @return  A JSONArray with all the networks of citybikes or null if an error has occourred
         */
        public JSONArray getNetworks (){
            if (jsonNetworks == null || jsonNetworks.length() == 0){
                final String url = CITYBIKESAPIURL + NETWORKSENDPOINT;
                try {
                    JSONObject cityBikesJSON = new JSONObject(downloadContentFrom(url));
                    jsonNetworks = cityBikesJSON.getJSONArray("networks");
                }
                catch (JSONException e){
                    Log.e(TAG, e.getClass().getSimpleName() + ": " + e.getMessage());
                    jsonNetworks = null;
                }
            }
            return jsonNetworks;
        }

        /**
         * Returns the list of all the stations belonging to the city. If no stations are present into
         * the city then the list is empty.
         * @param city The city to query the stations from
         * @return A list of stations which is empty if the city doesn't have any station.
         */
        public List<CityBikesStation> getStationsOf (String city) {
            List<CityBikesStation> stations = new LinkedList<>();
            if ("".equals(city) || city == null) {
                Log.w(TAG, "getStationsOf: City is null or empty");
                return stations;
            }
            String networkEndpoint = null;
            JSONArray networks = getNetworks();
            if (networks == null){
                Log.w(TAG, "No networks found for "+ city);
                return stations;
            }
            JSONObject location, network;
            for (int i = 0; i < networks.length(); ++i){
                try {
                    network = networks.getJSONObject(i);
                    location = network.getJSONObject("location");
                    String networkCity = location.getString("city");
                    if (city.equals(networkCity)) {
                        networkEndpoint = network.getString("href");
                        break;
                    }
                }
                catch (JSONException e) {
                    Log.e(TAG, e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
            if (networkEndpoint == null) {
                Log.w(TAG, "City isn't in the list of Citybikes");
                return stations;
            }
            else {
                try {
                    JSONObject cityStations = new JSONObject(downloadContentFrom(CITYBIKESAPIURL + networkEndpoint));
                    JSONArray jsonStations = cityStations.getJSONObject("network").getJSONArray("stations");
                    for (int i = 0; i < jsonStations.length(); ++i){
                        stations.add(new CityBikesStation(jsonStations.getJSONObject(i)));
                    }
                }
                catch (JSONException e) {
                    Log.e(TAG, e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
            return stations;
        }
    }

}
