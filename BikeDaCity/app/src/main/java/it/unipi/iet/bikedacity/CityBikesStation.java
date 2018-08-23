package it.unipi.iet.bikedacity;


import android.location.Location;
import android.util.Log;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

public class CityBikesStation {

    private static final String TAG = "CityBikesStation";
    private final int lowThreshold;
    private final int mediumThreshold;
    private Location location;
    private String name;
    private DateTime timestamp;
    private int freeBikes;
    private int emptySlots;
    private String description;
    private Boolean isOnline;

    public enum Status {
        NO_FREE,
        NO_AVAILABLE,
        LOW_FREE,
        LOW_AVAILABLE,
        MEDIUM_FREE,
        MEDIUM_AVAILABLE,
        HIGH_FREE,
        HIGH_AVAILABLE
    }

    private Status freeStatus;
    private Status availableStatus;

    public CityBikesStation(JSONObject jsonObject) throws JSONException {
        name = jsonObject.getString("name");
        double longitude = jsonObject.getDouble("longitude");
        double latitude = jsonObject.getDouble("latitude");
        location = new Location("CityBikes");
        location.setLongitude(longitude);
        location.setLatitude(latitude);
        freeBikes = jsonObject.getInt("free_bikes");
        emptySlots = jsonObject.getInt("empty_slots");
        lowThreshold = Math.min(2, (freeBikes + emptySlots) >> 3);
        mediumThreshold = Math.min(4, (freeBikes + emptySlots) >> 2);
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

    public Status getFreeStatus () {
        if (freeBikes == 0){
            freeStatus = Status.NO_FREE;
        }
        else if (freeBikes <= lowThreshold){
            freeStatus = Status.LOW_FREE;
        }
        else if (freeBikes <= mediumThreshold){
            freeStatus = Status.MEDIUM_FREE;
        }
        else {
            freeStatus = Status.HIGH_FREE;
        }
        return freeStatus;
    }

    public Status getAvailableStatus () {
        if (emptySlots == 0){
            availableStatus = Status.NO_AVAILABLE;
        }
        else if (emptySlots <= lowThreshold){
            availableStatus = Status.LOW_AVAILABLE;
        }
        else if (emptySlots <= mediumThreshold){
            availableStatus = Status.MEDIUM_AVAILABLE;
        }
        else {
            availableStatus = Status.HIGH_AVAILABLE;
        }
        return availableStatus;
    }

    public String getName () {
        return name;
    }

    public DateTime getTimestamp () {
        return timestamp;
    }

    public Location getLocation (){
        return location;
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
