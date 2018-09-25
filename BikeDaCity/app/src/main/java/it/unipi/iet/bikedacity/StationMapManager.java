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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// TODO replace markers
public class StationMapManager {
    public static final String TAG = "StationMapManager";
    private static final String MY_CURRENT_LOCATION_TITLE = "I'm here!";

    private MapView map;
    private Context context;
    private OverlayItem currentMarkerLocation;
    private ItemizedIconOverlay<OverlayItem> stationOverlay;

    public StationMapManager (Context ctx, MapView map){
        this.context = ctx;
        this.map = map;
        this.map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        this.map.setTilesScaledToDpi(true);
        this.map.setBuiltInZoomControls(true);
        this.map.setMultiTouchControls(true);
    }

    public MapView getMap () {
        return map;
    }

    public void onPause (){
        map.onPause();
    }

    public void onResume (){
        map.onResume();
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

    public List<OverlayItem> buildOverlayItemList (List<CityBikesStation> stations, Drawable marker){
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

    public List<OverlayItem> buildOverlayItemList (Map<CityBikesStation, String> stations,
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
        ItemizedIconOverlay<OverlayItem> overlay = new ItemizedIconOverlay<>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp (int index, OverlayItem item) {
                        IMapController controller = map.getController();
                        controller.setCenter(item.getPoint());
                        Toast.makeText(context, item.getSnippet(), Toast.LENGTH_LONG).show();
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress (int index, OverlayItem item) {
                        return false;
                    }
                }, context);
        return overlay;
    }

    private ItemizedIconOverlay<OverlayItem> createStationOverlay (Map<CityBikesStation, String> stations,
                                                                   Drawable marker){
        List<OverlayItem> items = buildOverlayItemList(stations, marker);

        // Create the overlay which will show the list of items built above
        ItemizedIconOverlay<OverlayItem> overlay = new ItemizedIconOverlay<>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp (int index, OverlayItem item) {
                        IMapController controller = map.getController();
                        controller.setCenter(item.getPoint());
                        Toast.makeText(context, item.getSnippet(), Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress (int index, OverlayItem item) {
                        return false;
                    }
                }, context);
        return overlay;
    }

    public void addStationMarkers (List<CityBikesStation> stations, Drawable marker){
        if (stationOverlay == null){
            stationOverlay = createStationOverlay(stations, marker);
        }
        else {
            List<OverlayItem> items = buildOverlayItemList(stations, marker);
            stationOverlay.addItems(items);
        }
        if (!map.getOverlays().contains(stationOverlay)){
            map.getOverlays().add(stationOverlay);
        }
    }

    public void addStationMarkers (Map<CityBikesStation, String> stations, Drawable marker){
        if (stationOverlay == null){
            stationOverlay = createStationOverlay(stations, marker);
        }
        else {
            List<OverlayItem> items = buildOverlayItemList(stations, marker);
            stationOverlay.addItems(items);
        }
        if (!map.getOverlays().contains(stationOverlay)){
            map.getOverlays().add(stationOverlay);
        }
    }

    public void removeStationMarkers (Iterable<CityBikesStation> stations){
        if (stationOverlay == null || stationOverlay.size() == 0){
            return;
        }
        Iterator<CityBikesStation> stationIt = stations.iterator();
        List<OverlayItem> displayedItems = stationOverlay.getDisplayedItems();
        if (stationIt.hasNext()){
            for (OverlayItem item : displayedItems){
                if (item.getTitle().equals(stationIt.next().getName())){
                    displayedItems.remove(item);
                }
            }
        }
    }

    public void replaceStationMarkers (Map<CityBikesStation, String> stations, Drawable marker){
        removeStationMarkers(stations.keySet());
        addStationMarkers(stations, marker);
    }

    public void removeAllMarkers (){
        if (stationOverlay == null) return;
        stationOverlay.removeAllItems();
    }

    public void removeStationMarker (CityBikesStation station){
        if (stationOverlay == null || stationOverlay.size() == 0){
            return;
        }
        List<OverlayItem> displayedItems = stationOverlay.getDisplayedItems();
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
        if (stationOverlay == null){
            List<OverlayItem> items = new LinkedList<>();
            items.add(currentMarkerLocation);
            stationOverlay = new ItemizedIconOverlay<>(items,
                    new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                        @Override
                        public boolean onItemSingleTapUp (int index, OverlayItem item) {
                            IMapController controller = map.getController();
                            controller.setCenter(item.getPoint());
                            Toast.makeText(context, item.getSnippet(), Toast.LENGTH_SHORT).show();
                            return true;
                        }

                        @Override
                        public boolean onItemLongPress (int index, OverlayItem item) {
                            return false;
                        }
                    }, context);
            map.getOverlays().add(stationOverlay);
        }
        else {
            stationOverlay.addItem(currentMarkerLocation);
        }
        setMarkerVisibility(true);
    }

    public void moveTo (Location location){
        IMapController controller = map.getController();

        // Need to zoom then center due to a bug in OSMDroid
        controller.animateTo(new GeoPoint(location.getLatitude(),
                        location.getLongitude()),
                18.0,
                500L);
    }

    public void removeCurrentLocationMarker (){
        if (currentMarkerLocation == null) return;
        if (stationOverlay == null || stationOverlay.size() == 0) return;
        stationOverlay.removeItem(currentMarkerLocation);
        currentMarkerLocation = null;
    }

    public void replaceCurrentLocationMarker (Location currentLocation, Drawable marker){
        removeCurrentLocationMarker();
        addCurrentLocationMarker(currentLocation, marker);
    }

    private void setMarkerVisibility (boolean visibility){
        List<Overlay> overlays = map.getOverlays();
        if (visibility == true){
            if (!overlays.contains(stationOverlay)){
                overlays.add(stationOverlay);
                map.invalidate();
            }
        }
        else {
            if (overlays.contains(stationOverlay)){
                overlays.remove(stationOverlay);
                map.invalidate();
            }
        }
    }
}
