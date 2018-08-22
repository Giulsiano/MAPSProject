package it.unipi.iet.bikedacity;


import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

public class CityBikesStation {
    private String name;
    private DateTime timestamp;
    private double longitude;
    private double latitude;
    private int freeBikes;
    private int emptySlots;
    private String city;
    private String description;
    private boolean isOnline;

    public CityBikesStation(JSONObject jsonObject) throws JSONException {
        name = jsonObject.getString("name");
        longitude = jsonObject.getDouble("longitude");
        latitude = jsonObject.getDouble("latitude");
        freeBikes = jsonObject.getInt("free_bikes");
        emptySlots = jsonObject.getInt("empty_slots");
        city = jsonObject.getString("city");
        JSONObject extra = (JSONObject) jsonObject.get("extra");
        description = extra.getString("description");
        isOnline = "online".equals(extra.getString("status")) ? true : false;
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

    public String getCity () {
        return city;
    }

    public String getDescription () {
        return description;
    }

    public boolean isOnline () {
        return isOnline;
    }
}
