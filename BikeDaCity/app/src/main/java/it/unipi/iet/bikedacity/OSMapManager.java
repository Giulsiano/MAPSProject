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
import java.util.Locale;
import java.util.Map;

// TODO Add clearing of tiles for osmdroid
public class OSMapManager{
    public static final String TAG = "OSMapManager";
    private static final String MY_POSITION_TITLE = "I'm here!";
    public static final long DEFAULT_ANIMATION_DURATION = 500L;
    public static double DEFAULT_ZOOM = 18.0;
    private static String MY_POSITION_OVERLAY_NAME = "myPosition";

    private MapView map;
    private Context context;
    private OverlayItem myPosition;
    private Drawable myPositionMarker;

    private Map<String, ItemizedIconOverlay<OverlayItem>> overlayMap;
    private ItemizedIconOverlay.OnItemGestureListener<OverlayItem> defaultGestureListener;

    public OSMapManager (Context ctx, final MapView map){
        this.context = ctx;
        this.map = map;
        this.map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        this.map.setTilesScaledToDpi(true);
        this.map.setBuiltInZoomControls(true);
        this.map.setMultiTouchControls(true);
        this.overlayMap = new HashMap<>();
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

    public void setDefaultZoom (double zoom){
        DEFAULT_ZOOM = zoom;
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
        if (overlay != null) {
            overlayMap.remove(overlayName);
            map.getOverlays().remove(overlay);
        }
    }

    public void setOverlayVisibility (String overlayName, boolean isVisible){
        ItemizedIconOverlay<OverlayItem> overlay = overlayMap.get(overlayName);
        if (overlay == null) return;
        List<Overlay> overlayList = map.getOverlays();
        if (overlayList.contains(overlay)) {
            if (!isVisible) overlayList.remove(overlay);
        }
        else {
            if (isVisible) overlayList.add(overlay);
        }
        map.invalidate();
    }


    /**
     * Add a my position marker to the default "myPosition" overlay. The title of the item created is
     * MY_POSITION_TITLE while the description is a String that shows current latitude and longitude.
     * @param location where put the marker on the map
     * @param marker the icon which will be shown on the map
     */
    public void addMyPositionMarker (Location location, Drawable marker){
        ItemizedIconOverlay<OverlayItem> defaultOverlay = overlayMap.get(MY_POSITION_OVERLAY_NAME);

        if (defaultOverlay == null){
            // There is no position overlay, so create the item list and the default overlay
            defaultOverlay = new ItemizedIconOverlay<>(buildMyPositionMarker(location, marker),
                                                       marker,
                                                       defaultGestureListener,
                                                       context);
            overlayMap.put(MY_POSITION_OVERLAY_NAME, defaultOverlay);
        }
        else {
            if (defaultOverlay.size() == 0){
                defaultOverlay.addItems(buildMyPositionMarker(location, marker));
            }
        }
    }

    public void addMyPositionMarkerOn (String overlayName, Location location, Drawable marker){
        ItemizedIconOverlay<OverlayItem> overlay = overlayMap.get(overlayName);
        if (overlay != null){
            overlay.addItems(buildMyPositionMarker(location, marker));
        }
    }

    private List<OverlayItem> buildMyPositionMarker (Location location, Drawable marker){
        String description = String.format(Locale.getDefault(),
                "Lat: %f\nLon: %f",
                location.getLatitude(),
                location.getLongitude());
        OverlayItem myPosition = new OverlayItem(MY_POSITION_TITLE, description,
                new GeoPoint(location.getLatitude(),
                        location.getLongitude()));
        myPosition.setMarker(marker);
        List<OverlayItem> items = new LinkedList<>();
        items.add(myPosition);
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

    public void addOverlay (String overlayName, Map<CityBikesStation,
                                        String> stations,
                            Drawable marker){
        List<OverlayItem> itemList = buildOverlayItemList(stations, marker);
        overlayMap.put(overlayName, new ItemizedIconOverlay<>(itemList, defaultGestureListener, context));
    }


    public void addMarkersTo (String overlayName, Map<CityBikesStation, String> stations, Drawable marker){
        // if there are no stations it isn't needed to add them to the overlay
        if (stations == null) return;
        ItemizedIconOverlay overlay = overlayMap.get(overlayName);
        if (overlay == null) {
            addOverlay(overlayName, stations, marker);
        }
        else {
            overlay.addItems(buildOverlayItemList(stations, marker));
        }
    }

    public void removeMarkersOn (String overlayName, Iterable<CityBikesStation> stations){
        ItemizedIconOverlay overlay = overlayMap.get(overlayName);
        if (overlay == null || overlay.size() == 0)
            return;

        Iterator<CityBikesStation> stationIt = stations.iterator();
        List<OverlayItem> displayedItems = overlay.getDisplayedItems();
        if (stationIt.hasNext()){
            for (OverlayItem item : displayedItems){
                if (item.getTitle().equals(stationIt.next().getName())){
                    displayedItems.remove(item);
                }
            }
        }
    }

    public void replaceAllMarkers (String overlayName, Map<CityBikesStation, String> newStations,
                                   Drawable newMarker){
        removeAllMarkersOn(overlayName);
        addMarkersTo(overlayName, newStations, newMarker);
    }

    public void removeAllMarkersOn (String overlayName){
        ItemizedIconOverlay overlay = overlayMap.get(overlayName);
        if (overlay != null) {
            overlay.removeAllItems();
        }
    }

    public void removeMarkerOn (String overlayName, CityBikesStation station){
        ItemizedIconOverlay overlay = overlayMap.get(overlayName);
        if (overlay == null || overlay.size() == 0){
            return;
        }
        List<OverlayItem> displayedItems = overlay.getDisplayedItems();
        for (OverlayItem item : displayedItems){
            if (item.getTitle().equals(station.getName())){
                displayedItems.remove(item);
            }
        }
    }

    public void replaceMyPositionMarker (Drawable marker){
        myPosition.setMarker(marker);
    }
    
    public void moveCameraTo (Location location){
        IMapController controller = map.getController();
        controller.animateTo(new GeoPoint(location.getLatitude(), location.getLongitude()),
                             DEFAULT_ZOOM,
                             DEFAULT_ANIMATION_DURATION);
    }

    public void setVisibleOverlays (List<String> names){
        // remove all overlay from the map, then re-add those who the user wants them to show
        List<Overlay> overlayList = map.getOverlays();
        for (Overlay overlay : overlayList) overlayList.remove(overlay);
        for (String overlayName : names){
            // Get the overlay, if it exist re-add to the visible overlay
            ItemizedIconOverlay overlay = overlayMap.get(overlayName);
            if (overlay != null){
                setOverlayVisibility(overlayName, true);
            }
        }
    }
}
