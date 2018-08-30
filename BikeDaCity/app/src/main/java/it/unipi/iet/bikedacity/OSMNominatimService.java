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

    /**
     * This method requests the name of a city based on the latitude and logitude passed by parameter
     * @param latitude  Latitude of the city you want
     * @param longitude Longityde of the city you want
     * @return  The city returned by the Open Street Maps nominatim service, an empty street if the
     * city isn't in the database of the service or null if an error has occourred
     */
    public static String getCityFrom (double latitude, double longitude){
        String city = null;
        try {
            URL reverseGeocodingUrl = new URL(OSMNominatimURL + "&lat=" + latitude +
                    "&lon=" + longitude);
            JSONObject jsonResponse = new JSONObject(downloadContentFrom(reverseGeocodingUrl));
            JSONObject address = jsonResponse.getJSONObject("address");
            city = address.has("city")? address.getString("city") : "";
        }
        catch (MalformedURLException e){
            Log.e("TAG", "Nominatim URL is not valid");
            throw new RuntimeException("OSM nominatim URL is not valid");
        }
        catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON: " + e.getMessage());
        }
        return city;
    }

    private static String downloadContentFrom (URL url){
        StringBuilder builder = new StringBuilder();
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
