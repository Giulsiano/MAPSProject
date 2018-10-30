package it.unipi.iet.bikedacity;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// TODO Add clearing of tiles for osmdroid
public class StationMapManager {
    public static final String TAG = "StationMapManager";
    private static final String MY_CURRENT_LOCATION_TITLE = "I'm here!";
    public static final long DEFAULT_ANIMATION_DURATION = 500L;
    public static final double DEFAULT_ZOOM = 18.0;
    public static final String DEFAULT_OVERLAY = "default";
    private final String CURRENT_LOCATION_OVERLAY = "CurrentLocation";

    private MapView map;
    private Context context;
    private OverlayItem currentMarkerLocation;
    private ItemizedIconOverlay<OverlayItem> lastOverlay;
    private Map<String, ItemizedIconOverlay<OverlayItem>> overlayMap;
    private ItemizedIconOverlay.OnItemGestureListener<OverlayItem> defaultGestureListener;

    public StationMapManager (Context ctx, final MapView map){
        this.context = ctx;
        this.map = map;
        this.map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        this.map.setTilesScaledToDpi(true);
        this.map.setBuiltInZoomControls(true);
        this.map.setMultiTouchControls(true);
        this.overlayMap = new HashMap<>();
        this.lastOverlay = null;
        this.defaultGestureListener = new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>(){
            @Override
            public boolean onItemSingleTapUp (int index, OverlayItem item) {
                IMapController controller = map.getController();
                controller.animateTo(item.getPoint(), DEFAULT_ZOOM, DEFAULT_ANIMATION_DURATION);
                Toast.makeText(context, item.getSnippet(), Toast.LENGTH_LONG).show();
                return true;
            }

            @Override
            public boolean onItemLongPress (int index, OverlayItem item) {
                return true;
            }
        };
    }

    public MapView getMapView () {
        return map;
    }

    public void onPause (){
        map.onPause();
    }

    public void onResume (){
        map.onResume();
    }

    public void removeOverlay (String overlayName){
        ItemizedIconOverlay<OverlayItem> overlay = overlayMap.get(overlayName);
        if (overlay == null) return;
        overlayMap.remove(overlayName);
        map.getOverlays().remove(overlay);
    }

    public void changeOverlay (String overlayName){
        ItemizedIconOverlay<OverlayItem> tempOverlay = overlayMap.get(overlayName);
        if (tempOverlay == null) return;
        else lastOverlay = tempOverlay;
    }

    public void setOverlayVisibility (String overlayName, boolean isVisible){
        ItemizedIconOverlay<OverlayItem> overlay = overlayMap.get(overlayName);
        List<Overlay> overlayList = map.getOverlays();
        if (overlayList.contains(overlay)) {
            if (!isVisible) overlayList.remove(overlay);
        }
        else {
            if (isVisible) overlayList.add(overlay);
        }
    }

    public List<OverlayItem> buildOverlayItemList (CityBikesStation station, String description,
                                                   Drawable marker){
        List<OverlayItem> items = new LinkedList<>();
        OverlayItem item = new OverlayItem(station.getName(), description,
                                           new GeoPoint(station.getLocation().getLatitude(),
                                                        station.getLocation().getLongitude()));
        item.setMarker(marker);
        items.add(item);
        return items;
    }

    private List<OverlayItem> buildOverlayItemList (List<CityBikesStation> stations, Drawable marker){
        List<OverlayItem> items = new LinkedList<>();
        for (CityBikesStation station : stations){
            OverlayItem item = new OverlayItem(station.getName(), station.getDescription(),
                                               new GeoPoint(station.getLocation().getLatitude(),
                                                            station.getLocation().getLongitude()));
            item.setMarker(marker);
            items.add(item);
        }
        return items;
    }

    private List<OverlayItem> buildOverlayItemList (Map<CityBikesStation, String> stations,
                                                    Drawable marker){
        List<OverlayItem> items = new LinkedList<>();
        for (CityBikesStation station : stations.keySet()){
            OverlayItem item = new OverlayItem(station.getName(), stations.get(station),
                                               new GeoPoint(station.getLocation().getLatitude(),
                                                            station.getLocation().getLongitude()));
            item.setMarker(marker);
            items.add(item);
        }
        return items;
    }

    private ItemizedIconOverlay<OverlayItem> createStationOverlay (List<CityBikesStation> stations,
                                                                   Drawable marker){
        // Create the list of items to be further added to the map overlay
        List<OverlayItem> items = buildOverlayItemList(stations, marker);

        // Create the overlay which will show the list of items built above
        return new ItemizedIconOverlay<>(items, defaultGestureListener, context);
    }

    private ItemizedIconOverlay<OverlayItem> createStationOverlay (Map<CityBikesStation, String> stations,
                                                                   Drawable marker){
        List<OverlayItem> items = buildOverlayItemList(stations, marker);

        // Create the overlay which will show the list of items built above
        return new ItemizedIconOverlay<>(items, defaultGestureListener, context);
    }

    public void addNamedStationOverlay (String overlayName, Map<CityBikesStation,
                                        String> stations,
                                        Drawable marker){
        List<OverlayItem> itemList = buildOverlayItemList(stations, marker);
        overlayMap.put(overlayName, new ItemizedIconOverlay<>(itemList, defaultGestureListener, context));
    }

    public void addNamedStationOverlay (String overlayName) {
        if (overlayName == null) throw new NullPointerException("overlayName can't be null");
        overlayMap.put(overlayName, null);
    }

    public void addStationMarkers (List<CityBikesStation> stations, Drawable marker){
        if (lastOverlay == null){
            lastOverlay = createStationOverlay(stations, marker);
        }
        else {
            List<OverlayItem> items = buildOverlayItemList(stations, marker);
            lastOverlay.addItems(items);
        }
        if (!map.getOverlays().contains(lastOverlay)){
            map.getOverlays().add(lastOverlay);
        }
    }

    public void addStationMarkers (Map<CityBikesStation, String> stations, Drawable marker){
        if (lastOverlay == null){
            lastOverlay = createStationOverlay(stations, marker);
            overlayMap.put(DEFAULT_OVERLAY, lastOverlay);
        }
        else {
            List<OverlayItem> items = buildOverlayItemList(stations, marker);
            lastOverlay.addItems(items);
        }
        if (!map.getOverlays().contains(lastOverlay)){
            map.getOverlays().add(lastOverlay);
        }
    }

    public void addStationMarkers (String overlayName, Map<CityBikesStation, String> stations, Drawable marker){
        // if there are no stations it isn't needed to add them to the overlay
        if (stations == null) return;
        lastOverlay = overlayMap.get(overlayName);
        if (lastOverlay == null) {
            // overlay doesn't exist, create new one with the name given
            addNamedStationOverlay(overlayName, stations, marker);
        }
        else addStationMarkers(stations, marker);
    }

    public void removeStationMarkers (Iterable<CityBikesStation> stations){
        if (lastOverlay == null || lastOverlay.size() == 0){
            return;
        }
        Iterator<CityBikesStation> stationIt = stations.iterator();
        List<OverlayItem> displayedItems = lastOverlay.getDisplayedItems();
        if (stationIt.hasNext()){
            for (OverlayItem item : displayedItems){
                if (item.getTitle().equals(stationIt.next().getName())){
                    displayedItems.remove(item);
                }
            }
        }
    }

    public void removeStationMarkers (String overlayName, Iterable<CityBikesStation> stations){
        lastOverlay = overlayMap.get(overlayName);
        if (lastOverlay == null) return;
        removeStationMarkers(stations);
    }

    public void replaceStationMarkers (String overlayName, Map<CityBikesStation, String> stations, Drawable marker){
        lastOverlay = overlayMap.get(overlayName);
        if (lastOverlay == null) {
            addNamedStationOverlay(overlayName, stations, marker);
            setOverlayVisibility(overlayName, true);
        }
        replaceStationMarkers(stations, marker);
    }

    public void replaceStationMarkers (Map<CityBikesStation, String> stations, Drawable marker){
        if (stations == null) return;
        removeStationMarkers(stations.keySet());
        addStationMarkers(stations, marker);
    }

    public void replaceAllStationMarkers (String overlayName, Map<CityBikesStation, String> newStations,
                                          Drawable newMarker){
        lastOverlay = overlayMap.get(overlayName);
        removeAllMarkers();
        addStationMarkers(overlayName, newStations, newMarker);
    }

    public void removeAllMarkers (String overlayName) {
        lastOverlay = overlayMap.get(overlayName);
        removeAllMarkers();
    }

    public void removeAllMarkers (){
        if (lastOverlay == null) return;
        lastOverlay.removeAllItems();
    }

    public void removeStationMarker (CityBikesStation station){
        if (lastOverlay == null || lastOverlay.size() == 0){
            return;
        }
        List<OverlayItem> displayedItems = lastOverlay.getDisplayedItems();
        for (OverlayItem item : displayedItems){
            if (item.getTitle().equals(station.getName())){
                displayedItems.remove(item);
            }
        }
    }

    public void addCurrentLocationMarker (Location currentLocation, Drawable marker){
        if (currentMarkerLocation == null){
            double lat = currentLocation.getLatitude();
            double lon = currentLocation.getLongitude();
            String snippet = "Lat: " + lat + "\nLon: " + lon;
            currentMarkerLocation = new OverlayItem(MY_CURRENT_LOCATION_TITLE, snippet,
                                                    new GeoPoint(currentLocation.getLatitude(),
                                                            currentLocation.getLongitude()));
            currentMarkerLocation.setMarker(marker);
        }
        if (lastOverlay == null){
            List<OverlayItem> items = new LinkedList<>();
            items.add(currentMarkerLocation);
            lastOverlay = new ItemizedIconOverlay<>(items,defaultGestureListener, context);
            map.getOverlays().add(lastOverlay);
        }
        else {
            lastOverlay.addItem(currentMarkerLocation);
        }
        setOverlayVisibility(true);
    }

    public void moveCameraTo (Location location){
        IMapController controller = map.getController();
        controller.animateTo(new GeoPoint(location.getLatitude(), location.getLongitude()),
                             DEFAULT_ZOOM,
                             DEFAULT_ANIMATION_DURATION);
    }

    public void removeCurrentLocationMarker (){
        if (currentMarkerLocation == null) return;
        if (lastOverlay == null || lastOverlay.size() == 0) return;
        lastOverlay.removeItem(currentMarkerLocation);
        currentMarkerLocation = null;
    }

    public void replaceCurrentLocationMarker (Location currentLocation, Drawable marker){
        removeCurrentLocationMarker();
        addCurrentLocationMarker(currentLocation, marker);
    }

    private void setOverlayVisibility (boolean isVisible){
        List<Overlay> overlays = map.getOverlays();
        if (isVisible){
            if (!overlays.contains(lastOverlay)){
                overlays.add(lastOverlay);
                map.invalidate();
            }
        }
        else {
            if (overlays.contains(lastOverlay)){
                overlays.remove(lastOverlay);
                map.invalidate();
            }
        }
    }

    public void setVisibleOverlays (List<String> names){
        // remove all overlay from the map, then re-add those who the user wants them to show
        List<Overlay> overlayList = map.getOverlays();
        for (Overlay overlay : overlayList) overlayList.remove(overlay);
        for (String overlayName : names){
            // Get the overlay, if it exist re-add to the visible overlay
            lastOverlay = overlayMap.get(overlayName);
            if (lastOverlay == null) continue;
            setOverlayVisibility(overlayName, true);
        }
    }
}
