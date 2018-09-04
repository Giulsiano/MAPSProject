package it.unipi.iet.bikedacity;


import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Comparator;

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

    public enum Availability {
        HIGH,
        MEDIUM,
        LOW,
        NO,
    }

    private Availability freePlacesLevel;
    private Availability availableBikesLevel;

    public CityBikesStation(JSONObject jsonObject) throws JSONException {
        name = jsonObject.getString("name");
        double longitude = jsonObject.getDouble("longitude");
        double latitude = jsonObject.getDouble("latitude");
        location = new Location(LocationManager.GPS_PROVIDER);
        location.setLongitude(longitude);
        location.setLatitude(latitude);
        location.setAccuracy(0.0f);
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
        freePlacesLevel = getFreePlacesLevel();
        availableBikesLevel = getAvailableBikesLevel();
    }

    public static Comparator<CityBikesStation> FreePlaceComparator = new
            Comparator<CityBikesStation>(){

        @Override
        public int compare (CityBikesStation o1, CityBikesStation o2) {
            return o1.freePlacesLevel.compareTo(o2.getFreePlacesLevel());
        }
    };

    public static Comparator<CityBikesStation> AvailableBikesComparator = new Comparator<CityBikesStation>() {
        @Override
        public int compare (CityBikesStation o1, CityBikesStation o2) {
            return o1.availableBikesLevel.compareTo(o2.getAvailableBikesLevel());
        }
    };

    private Availability getLevelOf (int it){
        if (it == 0){
            return Availability.NO;
        }
        if (it <= lowThreshold){
            return Availability.LOW;
        }
        if (it <= mediumThreshold){
            return Availability.MEDIUM;
        }
        else return Availability.HIGH;
    }

    public Availability getAvailableBikesLevel () {
        availableBikesLevel = getLevelOf(freeBikes);
        return availableBikesLevel;
    }

    public Availability getFreePlacesLevel () {
        freePlacesLevel = getLevelOf(emptySlots);
        return freePlacesLevel;
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
