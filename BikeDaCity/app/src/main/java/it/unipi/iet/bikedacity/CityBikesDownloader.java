package it.unipi.iet.bikedacity;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class CityBikesDownloader {
    //
    //TODO: if there is enough time before delivery try to do a cache manager via external
    //storage
    //
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

    private JSONObject downloadFilteredJSON (String filter) throws JSONException {
        String content = downloadContentFrom(CITYBIKESAPIURL + NETWORKSENDPOINT + filter);
        return new JSONObject(content);
    }

    /**
     * Downloads the citybikes network json
     * @return The JSON from citybik.es or null if an error has occourred.
     */
    private JSONObject downloadNetworksJSON (){
        final String url = CITYBIKESAPIURL + NETWORKSENDPOINT;
        try {
            return new JSONObject(downloadContentFrom(url));
        }
        catch (JSONException e) {
            Log.e(TAG, "Error downloading networks JSON from " + url + ".\n" +
                    e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * THis method downloads the JSON from citybikes and return only the array of the
     * networks known by the site. It filters out useless information.
     * @return  A JSONArray with all the networks of citybikes or null if an error has occourred
     */
    public JSONArray getNetworks (){
        JSONObject cityBikesJSON = downloadNetworksJSON();
        if (cityBikesJSON == null){
            Log.e(TAG, "Error on downloading JSON networks from " + CITYBIKESAPIURL);
            return null;
        }
        else {
            try {
                jsonNetworks = cityBikesJSON.getJSONArray("networks");
            }
            catch (JSONException e) {
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
        if ("".equals(city) || city == null) return null;
        List<CityBikesStation> stations = new LinkedList<>();
        String networkEndpoint = null;
        JSONArray networks = getNetworks();
        if (networks == null){
            Log.w(TAG, "Error retrieving network information for "+ city);
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
                Log.e(TAG, e.getMessage());
            }
        }
        if (networkEndpoint == null) {
            Log.w(TAG, "City isn't in the list of Citybikes");
            return stations;
        }
        try {
            JSONObject cityStations = new JSONObject(downloadContentFrom(CITYBIKESAPIURL + networkEndpoint));
            JSONArray jsonStations = cityStations.getJSONObject("network")
                                                 .getJSONArray("stations");
            for (int i = 0; i < jsonStations.length(); ++i){
                stations.add(new CityBikesStation(jsonStations.getJSONObject(i)));
            }
        }
        catch (JSONException e) {
            Log.e(TAG, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return stations;
    }
}
