package it.unipi.iet.bikedacity;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class CityBikesDownloader {
    //
    //TODO: if there is enough time before delivery try to do a cache manager via external
    //storage
    //
    private static final String TAG = "CityBikeDownloader";
    public static final String CITYBIKESURL = "https://api.citybik.es/v2/networks/";
    private JSONArray jsonNetworks;

    public CityBikesDownloader(){
        jsonNetworks = new JSONArray();
    }

    private String downloadFilteredJSON (String filter){
        StringBuilder stringBuilder = new StringBuilder();
        try {
            URL site = new URL(CITYBIKESURL + filter);
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
                        stringBuilder.append(line);
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
        catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return stringBuilder.toString().replaceAll("\\s+", "");
    }

    public String downloadNetworksJSON (){
        return downloadFilteredJSON("");
    }

    public JSONArray getNetworks (){
        String jsonString = downloadNetworksJSON();

        // in case of errors downloadNetworksJSON will return an empty string
        if (!"".equals(jsonString)){
            try {
                JSONObject wholeJson = new JSONObject(jsonString);
                jsonNetworks = wholeJson.getJSONArray("networks");
            }
            catch (JSONException e) {
                Log.e(TAG, "JSONException " + e.getLocalizedMessage());
                jsonNetworks = new JSONArray();
            }
        }
        return jsonNetworks;
    }

    public
}
