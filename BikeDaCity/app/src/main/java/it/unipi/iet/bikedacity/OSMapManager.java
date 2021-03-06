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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OSMapManager{
    public static final String TAG = "OSMapManager";
    private static final String MY_POSITION_TITLE = "I'm here!";
    public static final long DEFAULT_ANIMATION_DURATION = 500L;
    public static double DEFAULT_ZOOM = 18.0;

    private String myPositionTitle;
    private MapView map;
    private Context context;
    private OverlayItem myPosition;

    private Map<String, ItemizedIconOverlay<OverlayItem>> overlayMap;
    private ItemizedIconOverlay.OnItemGestureListener<OverlayItem> defaultGestureListener;

    public OSMapManager (Context ctx, final MapView map){
        this(ctx, map, null);
    }

    public OSMapManager (Context ctx, final MapView map, String myPositionTitle){
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
        this.myPositionTitle = (myPositionTitle == null) ? MY_POSITION_TITLE : myPositionTitle;
    }

    public void setMaxZoom (double maxZoom){
        map.setMaxZoomLevel(maxZoom);
    }

    public void setMinZoom (double minZoom){
        map.setMinZoomLevel(minZoom);
    }

    public void setDefaultZoom (double zoom){
        if (zoom != DEFAULT_ZOOM){
            DEFAULT_ZOOM = zoom;
            map.getController().setZoom(zoom);
        }
    }

    public double getCurrentZoomLevel (){
        return map.getZoomLevelDouble();
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

    public void addMyPositionMarkerOn (String overlayName, Location location, Drawable marker){
        ItemizedIconOverlay<OverlayItem> overlay = overlayMap.get(overlayName);
        if (overlay != null){
            overlay.addItems(buildMyPositionMarker(location, marker));
        }
        else {
            overlay = new ItemizedIconOverlay<>(buildMyPositionMarker(location, marker),
                    marker,
                    defaultGestureListener,
                    context);
            overlayMap.put(overlayName, overlay);
        }
    }

    private List<OverlayItem> buildMyPositionMarker (Location location, Drawable marker){
        String description = String.format(Locale.getDefault(),
                "Lat: %f\nLon: %f",
                location.getLatitude(),
                location.getLongitude());
        myPosition = new OverlayItem(myPositionTitle, description,
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

    public void replaceMyPositionMarkerOn (String overlayName, Location myPosition, Drawable drawable){
        removeMyPositionMarkerOn(overlayName);
        addMyPositionMarkerOn(overlayName, myPosition, drawable);
    }

    public void removeMyPositionMarkerOn (String overlayName){
        ItemizedIconOverlay<OverlayItem> overlay = overlayMap.get(overlayName);
        if (overlay != null && overlay.size() != 0){
            overlay.removeItem(myPosition);
        }
    }

    public void addMarkersTo (String overlayName, Map<CityBikesStation, String> stations, Drawable marker){
        // if there are no stations it isn't needed to add them to the overlay
        if (stations == null) return;
        ItemizedIconOverlay<OverlayItem> overlay = overlayMap.get(overlayName);
        if (overlay == null) {
            addOverlay(overlayName, stations, marker);
        }
        else {
            overlay.addItems(buildOverlayItemList(stations, marker));
        }
    }

    public void replaceAllMarkersOn (String overlayName, Map<CityBikesStation, String> newStations,
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
    
    public void moveCameraTo (Location location){
        IMapController controller = map.getController();
        controller.animateTo(new GeoPoint(location.getLatitude(), location.getLongitude()),
                             DEFAULT_ZOOM,
                             DEFAULT_ANIMATION_DURATION);
    }

    public void setVisibleOverlays (List<String> names){
        // remove all overlay from the map, then re-add those who the user wants them to show
        List<Overlay> overlayList = map.getOverlays();
        for (Overlay overlay : overlayList)
            overlayList.remove(overlay);

        for (String overlayName : names)
            setOverlayVisibility(overlayName, true);
    }
}
