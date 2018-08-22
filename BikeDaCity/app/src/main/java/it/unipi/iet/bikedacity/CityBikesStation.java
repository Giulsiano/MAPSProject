package it.unipi.iet.bikedacity;


import android.util.Log;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

public class CityBikesStation {
    private static final String TAG = "CityBikesStation";
    private String name;
    private DateTime timestamp;
    private double longitude;
    private double latitude;
    private int freeBikes;
    private int emptySlots;
    private String description;
    private Boolean isOnline;

    public CityBikesStation(JSONObject jsonObject) throws JSONException {
        name = jsonObject.getString("name");
        longitude = jsonObject.getDouble("longitude");
        latitude = jsonObject.getDouble("latitude");
        freeBikes = jsonObject.getInt("free_bikes");
        emptySlots = jsonObject.getInt("empty_slots");
        JSONObject extra;
        try {
            extra = (JSONObject) jsonObject.get("extra");
            description = extra.getString("description");
            isOnline = "online".equals(extra.getString("status"));
        }
        catch (JSONException e){
            Log.w(TAG, "Station " + name + " don't have an extra field");
            description = null;
            isOnline = null;
        }
        timestamp = new DateTime(jsonObject.getString("timestamp"));
    }

    public String getName () {
        return name;
    }

    public DateTime getTimestamp () {
        return timestamp;
    }

    public double getLongitude () {
        return longitude;
    }

    public double getLatitude () {
        return latitude;
    }

    public int getFreeBikes () {
        return freeBikes;
    }

    public int getEmptySlots () {
        return emptySlots;
    }

    public String getDescription () {
        return description;
    }

    public boolean isOnline () {
        return isOnline;
    }
}
