package it.unipi.iet.bikedacity;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class OSMNominatimService {

    private static final String TAG = "OSMNominatimService";
    private static final String OSMNominatimURL = "https://nominatim.openstreetmap.org/reverse?format=json";
    private static final String[] fieldNames = {"city", "town", "village"};
    private static final String displayNameField = "display_name";
    private static JSONObject response;
    // latitude then longitude
    private static double[] previousCoordinates = {0.0, 0.0};


    private static JSONObject getResponseFor (double latitude, double longitude){
        JSONObject response = null;
        try {
            URL reverseGeocodingUrl = new URL(OSMNominatimURL + "&lat=" + latitude +
                    "&lon=" + longitude);
            response = new JSONObject(downloadContentFrom(reverseGeocodingUrl));
        }
        catch (MalformedURLException e){
            Log.e("TAG", "Nominatim URL is not valid");
            throw new RuntimeException("OSM nominatim URL is not valid");
        }
        catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON: " + e.getMessage());
        }
        return response;
    }

    private static boolean hasChangedCoordinates(double latitude, double longitude){
        return previousCoordinates[0] != latitude || previousCoordinates[1] != longitude;
    }

    /**
     * This method requests the name of a city based on the latitude and logitude passed by parameter
     * @param latitude  Latitude of the city you want
     * @param longitude Longityde of the city you want
     * @return  The city returned by the Open Street Maps nominatim service, if it not found it returns the
     * displayed_name field of the JSON returned from the service
     */
    public static String getCityFrom (double latitude, double longitude){
        String city = null;
        if (response == null){
            Log.i(TAG, "Reverse geocoding for (" + latitude + ", " + longitude +")");
            response = getResponseFor(latitude, longitude);
        }
        else if (hasChangedCoordinates(latitude, longitude)){
            Log.i(TAG, "getCityFrom: Coordinate has changed. Request new position");
            previousCoordinates[0] = latitude;
            previousCoordinates[1] = longitude;
            Log.i(TAG, "Reverse geocoding for (" + latitude + ", " + longitude +")");
            response = getResponseFor(latitude, longitude);
            if (response == null) return null;
        }

        try {
            JSONObject address = response.getJSONObject("address");
            for (String fieldName : fieldNames){
                if (address.has(fieldName)) {
                    city = address.getString(fieldName);
                    break;
                }
            }
        }
        catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON: " + e.getMessage());
        }
        return city;
    }

    public static String getDisplayNameFrom (double latitude, double longitude){
        if (response == null || response.length() == 0) {
            response = getResponseFor(latitude, longitude);
        }
        try {
            if (response.has(displayNameField)){
                return response.getString(displayNameField);
            }
        }
        catch (JSONException e){
            Log.e(TAG, "Error parsing JSON: " + e.getMessage());
        }
        return null;
    }

    private static String downloadContentFrom (URL url){
        StringBuilder builder = new StringBuilder();
        Log.i(TAG, "Downlading content from " + url.toString());
        try {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-agent", OSMNominatimURL.getClass().getSimpleName() + " " +
                                                        Double.doubleToLongBits(Math.random()));
            connection.connect();
            int status = connection.getResponseCode();
            Log.d(TAG, "status is : " + status);
            if (status >= 200 && status < 300){
                Log.i(TAG, "Received response from the site: OK");
                try (InputStreamReader is = new InputStreamReader(connection.getInputStream())){
                    BufferedReader reader = new BufferedReader(is);
                    String line;
                    while ((line = reader.readLine()) != null){
                        builder.append(line);
                    }
                    reader.close();
                }
                connection.disconnect();
            }
            else {
                Log.e(TAG, "Received response from the site, status code: " + status);
                connection.disconnect();
                throw new RuntimeException("Unexpected response from the site");
            }
        }
        catch (IOException e){
            Log.e(TAG, "Exception " + e.getClass().getSimpleName() + ". Message: " + e.getMessage());

        }
        return builder.toString();
    }
}
