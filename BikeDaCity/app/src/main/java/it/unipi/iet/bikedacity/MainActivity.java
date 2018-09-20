package it.unipi.iet.bikedacity;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

// TODO add setting for location updates
// TODO get settings, reorder of the code
// TODO add Location Service to respect requirements for settings activity
// TODO new marker for availability (need Inkscape)
// TODO use onSaveInstateState to save simple values

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "MainActivity";
    private static final String CHANGE_LOCATION_ACTION = "it.unipi.iet.bikedacity.CHANGE_LOCATION";
    private static final int REQUEST_CODE = 0;

    private SharedPreferences preferences;
    private Resources resources;
    private MapView cityMap;
    private CityBikesManager cityBikesManager;
    private LocationManager locationManager;
    private Location currentLocation;
    private RecyclerView stationList;
    private TextView infoBox;
    private LocationBroadcastReceiver locationReceiver;
    private PendingIntent pendingIntent;

    private boolean permissionOk;
    private List<OverlayItem> mapItems;
    private boolean showAvailablePlaces;

    private static final int REQUEST_PERMISSIONS = 0;
    private static final long OLD_THRESHOLD = 1*60*1000;

    enum ProgressState{
        REQUEST_CITY,
        COMPUTING_DISTANCES,
        MAKE_STATION_LIST
    }

    private class DownloadStationsTask extends AsyncTask<Void, ProgressState, TreeMap<Integer, List<CityBikesStation>>> {

        ProgressBar progressBar;
        Button refreshMap;
        Context context;

        public DownloadStationsTask (Context ctx){
            super();
            refreshMap = findViewById(R.id.refreshMap);
            infoBox = findViewById(R.id.infoBox);
            progressBar = findViewById(R.id.indeterminateBar);
            this.context = ctx;
        }

        @Override
        protected void onPreExecute (){
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            refreshMap.setVisibility(View.INVISIBLE);
            infoBox.setText(R.string.infobox_starting_text);
        }

        @Override
        protected void onPostExecute (final TreeMap<Integer, List<CityBikesStation>> stationMap){
            super.onPostExecute(stationMap);
            infoBox.setText(resources.getString(R.string.infobox_adding_stations));
            if (stationMap == null){
                // Exit the app if there are no stations
                BikeDaCityUtil.createAlertDialogWithPositiveButtonOnly(context,
                        R.string.err_dialog_no_city_found_title,
                        R.string.err_dialog_no_city_found_message,
                        R.string.err_dialog_no_city_found_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick (DialogInterface dialog, int which) {
                                finish();
                            }
                        }).show();
            }
            else {
                if (mapItems == null || mapItems.isEmpty()){
                    Log.e(TAG, "No mapItems found");
                    return;
                }
                // Fill the map with the marker for the stations and add them to the list
                ItemizedIconOverlay<OverlayItem> stationOverlay = new ItemizedIconOverlay<>(mapItems,
                        new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                            @Override
                            public boolean onItemSingleTapUp (int index, OverlayItem item) {
                                IMapController controller = cityMap.getController();
                                controller.setCenter(item.getPoint());
                                return true;
                            }

                            @Override
                            public boolean onItemLongPress (int index, OverlayItem item) {
                                return false;
                            }
                        }, context);
                cityMap.getOverlays().add(stationOverlay);

                // Recreate the RecyclerView list and center the map to the current location
                stationList.invalidate();
                stationList.setAdapter(new ShowStationAdapter(context,
                                                              stationMap,
                                                              cityMap,
                                                              showAvailablePlaces));
                infoBox.setText(resources.getString(R.string.infobox_current_location,
                                              currentLocation.getLatitude(),
                                              currentLocation.getLongitude()));
                progressBar.setVisibility(View.INVISIBLE);
                refreshMap.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onProgressUpdate (ProgressState... progresses){
            super.onProgressUpdate(progresses);
            switch(progresses[0]){
                case REQUEST_CITY:
                    infoBox.setText(resources.getString(R.string.infobox_request_city));
                    break;

                case COMPUTING_DISTANCES:
                    infoBox.setText(resources.getString(R.string.infobox_station_ordered));
                    break;

                case MAKE_STATION_LIST:
                    infoBox.setText(resources.getString(R.string.infobox_make_stations));
                    break;
            }
        }

        @Override
        protected TreeMap<Integer, List<CityBikesStation>> doInBackground (Void... voids){
            Log.d(TAG, "doInBackground()");
            publishProgress(ProgressState.REQUEST_CITY);
            String city = OSMNominatimService.getCityFrom(currentLocation.getLatitude(),
                                                          currentLocation.getLongitude());
            if (city == null) {
                return null;
            }
            if (cityBikesManager == null) {
                cityBikesManager = new CityBikesManager(city);
            }
            publishProgress(ProgressState.COMPUTING_DISTANCES);
            TreeMap<Integer, List<CityBikesStation>> distanceMap = (showAvailablePlaces) ?
                    cityBikesManager.getNearestAvailableBikesFrom(currentLocation) :
                    cityBikesManager.getNearestFreePlacesFrom(currentLocation);

            // Create the list of item that will be added to the city map
            publishProgress(ProgressState.MAKE_STATION_LIST);
            if (distanceMap != null) {
                mapItems = new ArrayList<>();
                for (Integer distance : distanceMap.keySet()){
                    for (CityBikesStation station : distanceMap.get(distance)){
                        String description;
                        Drawable marker;
                        if (showAvailablePlaces){
                            description = resources.getString(R.string.marker_description_available_places,
                                                        station.getEmptySlots(),
                                                        distance);
                            marker = resources.getDrawable(R.drawable.ic_local_parking_24px);
                        }
                        else {
                            description = resources.getString(R.string.marker_description_available_bikes,
                                                        station.getFreeBikes(),
                                                        distance);
                            marker = resources.getDrawable(R.drawable.ic_directions_bike_24px);
                        }
                        OverlayItem stationItem = new OverlayItem(station.getName(),
                                description,
                                new GeoPoint(station.getLocation().getLatitude(),
                                        station.getLocation().getLongitude()));
                        stationItem.setMarker(marker);
                        mapItems.add(stationItem);
                    }
                }
                // Add current location marker also
                OverlayItem currentLocationItem = new OverlayItem(resources.getString(R.string.marker_my_location_name),
                                                                  resources.getString(R.string.marker_my_location_description),
                                                                  new GeoPoint(currentLocation.getLatitude(),
                                                                        currentLocation.getLongitude())
                );
                currentLocationItem.setMarker(resources.getDrawable(R.drawable.ic_place_24px));
                mapItems.add(currentLocationItem);
            }
            return distanceMap;
        }
    }

    @Override
    protected void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        if (isExternalStorageAvailable()){
            // Initialize the map
            Context ctx = getApplicationContext();
            Configuration.getInstance().load(ctx,
                            PreferenceManager.getDefaultSharedPreferences(ctx));
            setContentView(R.layout.activity_main);
            resources = getResources();
            infoBox = findViewById(R.id.infoBox);
            infoBox.setText(R.string.infobox_init_app);
            locationReceiver = new LocationBroadcastReceiver();
            pendingIntent = createPendingIntent();
            stationList = findViewById(R.id.stationList);
            stationList.setHasFixedSize(true);
            stationList.setLayoutManager(new LinearLayoutManager(this));
            cityMap = findViewById(R.id.map);
            cityMap.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
            cityMap.setTilesScaledToDpi(true);
            cityMap.setBuiltInZoomControls(true);
            cityMap.setMultiTouchControls(true);
            pendingIntent = createPendingIntent();
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            permissionOk = false;
            preferences = PreferenceManager.getDefaultSharedPreferences(this);
            showAvailablePlaces = preferences.getBoolean(resources.getString(R.string.map_default_view_list_key),true);
        }
        else {
            BikeDaCityUtil.createAlertDialogWithPositiveButtonOnly(this,
                    R.string.err_dialog_no_ext_storage_title,
                    R.string.err_dialog_no_ext_storage_message,
                    R.string.err_dialog_no_ext_storage_button,
                    new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick (DialogInterface dialog, int which) {
                            finish();
                        }
                    }).show();
        }
    }

    private PendingIntent createPendingIntent () {
        Intent intent = new Intent(CHANGE_LOCATION_ACTION);
        return PendingIntent.getBroadcast(getApplicationContext(), REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission has been granted, preview can be displayed
                this.permissionOk = true;
            }
            else {
                BikeDaCityUtil.createAlertDialogWithPositiveButtonOnly(this,
                        R.string.err_dialog_perm_not_granted_title,
                        R.string.err_dialog_perm_not_granted_message,
                        R.string.err_dialog_perm_not_granted_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick (DialogInterface dialog, int which) {
                                finish();
                            }
                        }
                ).show();
            }
        }
        else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean isExternalStorageAvailable () {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public void onStart (){
        super.onStart();
        // Request permission for this app to work properly
        permissionOk = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        permissionOk &= ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        if (!permissionOk) {
            Log.i(TAG, "Request permissions to the user");
            BikeDaCityUtil.getPermissionsRationaleDialog(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE}).show();
        }
        else {
            Log.i(TAG, "App has the right permissions granted");
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                requestEnablingProvider();
            }
            else {
                if (currentLocation == null){
                    currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (isCurrentLocationTooOld()){
                        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, pendingIntent);
                    }
                }
                int minTime = preferences.getInt(resources.getString(R.string.min_time_key),
                                                 1000);
                int minDistance = preferences.getInt(resources.getString(R.string.min_dist_key),
                                                     100);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                                       minTime,
                                                       minDistance,
                                                       pendingIntent);
            }
        }
    }

    private boolean isCurrentLocationTooOld(){
        return Math.abs(currentLocation.getTime() - System.currentTimeMillis()) > OLD_THRESHOLD;
    }

    private void requestEnablingProvider (){
        Log.w(TAG, "Request the user to enable the GPS provider");
        BikeDaCityUtil.createAlertDialogWithTwoButton(this,
                R.string.enable_provider_title,
                R.string.enable_provider_message,
                R.string.enable_provider_positive_button,
                R.string.enable_provider_negative_button,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick (DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick (DialogInterface dialog, int which) {
                        finish();
                    }
                }
        ).show();
    }

    @Override
    public void onResume (){
        super.onResume();
        cityMap.onResume();
        registerReceiver(locationReceiver, new IntentFilter(CHANGE_LOCATION_ACTION));
    }

    @Override
    public void onPause (){
        Log.d(TAG, "onPause() Called");
        super.onPause();
        cityMap.onPause();
        unregisterReceiver(locationReceiver);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()){
            case R.id.about:
                startActivityForResult(new Intent(MainActivity.this, AboutActivity.class), 1);
                return true;

            case R.id.settings:
                startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), 0);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void refreshMap (View v){
        // TODO implement the refreshing of the map, it could require some changes on design
    }

    public class LocationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive (Context context, Intent intent) {
            if (intent.hasExtra(LocationManager.KEY_LOCATION_CHANGED)) {
                Location location = (Location)
                        intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);
                onLocationChanged(context, location);
            }
            else if (intent.hasExtra(LocationManager.KEY_PROVIDER_ENABLED)) {
                if (intent.getExtras().getBoolean(LocationManager.KEY_PROVIDER_ENABLED)) {
                    onProviderEnabled(null);
                }
                else {
                    onProviderDisabled(null);
                }
            }
            else if (intent.hasExtra(LocationManager.KEY_PROXIMITY_ENTERING)) {
                if (intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)) {
                    onEnteringProximity(context);
                }
                else {
                    onExitingProximity(context);
                }
            }
            else if (intent.hasExtra(LocationManager.KEY_STATUS_CHANGED)) {
                onStatusChanged();
            }
        }

        public void onLocationChanged (Context context, Location location){
            currentLocation = location;
            new DownloadStationsTask(context).execute();
            IMapController controller = cityMap.getController();

            // Need to zoom then center due to a bug in OSMDroid
            controller.animateTo(new GeoPoint(currentLocation.getLatitude(),
                                              currentLocation.getLongitude()),
                                 18.0,
                                 500L);
        }

        public void onProviderEnabled (Context context){

        }

        public void onProviderDisabled (Context context){

        }

        public void onEnteringProximity (Context context){

        }

        public void onExitingProximity (Context context){

        }

        public void onStatusChanged (){

        }

    }
}
