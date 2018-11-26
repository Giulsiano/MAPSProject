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
import android.widget.Toast;

import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

// TODO get settings
// TODO move OverlayAvailability to BikeDaCityUtils
// TODO first fix logic for moving the camera only the first time the location is updated
// TODO Save showAvailablePlaces on calling onSaveInstaces

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "MainActivity";
    private static final String CHANGE_LOCATION_ACTION = "it.unipi.iet.bikedacity.CHANGE_LOCATION";
    private static final int REQUEST_CODE = 0;
    private SharedPreferences preferences;
    private Resources resources;
    private OSMapManager mapManager;
    private CityBikesManager cityBikesManager;
    private Location currentLocation;
    private RecyclerView stationListView;
    private TextView infoBox;
    private MenuItem showOptionItem;
    private LocationBroadcastReceiver locationReceiver;
    private PendingIntent pendingIntent;
    private BuildStationMapTask task;
    private Map<OverlayAvailability, String> overlayNames;
    private Map<OverlayAvailability, Drawable> overlayDrawables;
    private int[] showVisibleOverlaysButtonBackgroundIds;

    private boolean permissionOk;
    private boolean isFirstFix;
    private boolean showAvailablePlaces;
    private int visibleOverlayCounter;

    private static final int REQUEST_PERMISSIONS = 0;

    private enum ProgressState {
        REQUEST_CITY,
        COMPUTING_DISTANCES,
        MAKE_STATION_LIST
    }

    // Ordered by crescent priority
    private enum OverlayAvailability{
        NO_AVAILABILITY,
        LOW_AVAILABILITY,
        MEDIUM_AVAILABILITY,
        HIGH_AVAILABILITY
    }

    private class BuildStationMapTask extends AsyncTask<Void, ProgressState, Map<OverlayAvailability, Map<CityBikesStation, String>>> {
        ProgressBar progressBar;
        TextView infoBox;
        Button refreshMap;
        Button visibleOverlayButton;
        Context context;
        SortedMap<Integer, List<CityBikesStation>> stationMap;
        String address;
        Resources resources;

        public BuildStationMapTask (Context ctx){
            super();
            refreshMap = findViewById(R.id.refresh_map_button);
            visibleOverlayButton = findViewById(R.id.view_overlay_button);
            infoBox = findViewById(R.id.infoBox);
            progressBar = findViewById(R.id.indeterminate_bar);
            this.context = ctx;
            this.resources = ctx.getResources();
            address = this.resources.getString(R.string.address_text,
                                               this.resources.getString(R.string.default_address_text));
        }

        @Override
        protected void onPreExecute (){
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);

            // Disable everything can make the app explodes if another instance of this task is running
            refreshMap.setEnabled(false);
            visibleOverlayButton.setEnabled(false);
            infoBox.setText(R.string.infobox_starting_text);
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
        protected Map<OverlayAvailability, Map<CityBikesStation, String>> doInBackground (Void... voids){
            if (cityBikesManager == null){
                cityBikesManager = new CityBikesManager();
            }
            String city = cityBikesManager.getCity();
            if (city == null){
                publishProgress(ProgressState.REQUEST_CITY);
                city = OSMNominatimService.getCityFrom(currentLocation.getLatitude(),
                        currentLocation.getLongitude());

                // There is no city found at the current location coordinates, try to get the name displayed
                // to OSM Nominatim service
                if (city == null) {
                    String displayName = OSMNominatimService.getDisplayNameFrom(currentLocation.getLatitude(),
                                                                                currentLocation.getLongitude());
                    if (displayName != null) address = displayName;
                    return null;
                }
                else cityBikesManager.setCity(city);
            }
            // Get ordered stations
            publishProgress(ProgressState.COMPUTING_DISTANCES);
            stationMap = (showAvailablePlaces) ? cityBikesManager.getNearestFreePlacesFrom(currentLocation) :
                                                 cityBikesManager.getNearestAvailableBikesFrom(currentLocation);

            // Create the list for the overlay to be added to the map
            // TODO Verify a better method without recurring to this creation every time the doInBackground is called
            publishProgress(ProgressState.MAKE_STATION_LIST);
            Map<OverlayAvailability, Map<CityBikesStation, String>> availabilityMap = new EnumMap<>(OverlayAvailability.class);
            Map<CityBikesStation, String> noAvailabilityMap = new HashMap<>();
            Map<CityBikesStation, String> lowAvailabilityMap = new HashMap<>();
            Map<CityBikesStation, String> mediumAvailabilityMap = new HashMap<>();
            Map<CityBikesStation, String> highAvailabilityMap = new HashMap<>();

            String description;
            for (Integer distance : stationMap.keySet()){
                for (CityBikesStation station : stationMap.get(distance)){
                    Map<CityBikesStation, String> map = null;
                    String availability = null;
                    switch ((showAvailablePlaces) ? station.getFreePlacesLevel() :
                                                    station.getAvailableBikesLevel()){
                        case NO:
                            map = noAvailabilityMap;
                            availability = "NO";
                            break;

                        case LOW:
                            map = lowAvailabilityMap;
                            availability = "Low";
                            break;

                        case MEDIUM:
                            map = mediumAvailabilityMap;
                            availability = "Medium";
                            break;

                        case HIGH:
                            map = highAvailabilityMap;
                            availability = "High";
                            break;
                    }
                    description = resources.getString(R.string.marker_description_available_places,
                                                      station.getEmptySlots(),
                                                      distance,
                                                      availability
                                                      );
                    map.put(station, description);
                }
            }
            // Add all map to the list to be returned
            availabilityMap.put(OverlayAvailability.NO_AVAILABILITY, noAvailabilityMap);
            availabilityMap.put(OverlayAvailability.LOW_AVAILABILITY, lowAvailabilityMap);
            availabilityMap.put(OverlayAvailability.MEDIUM_AVAILABILITY, mediumAvailabilityMap);
            availabilityMap.put(OverlayAvailability.HIGH_AVAILABILITY, highAvailabilityMap);
            return availabilityMap;
        }

        @Override
        protected void onPostExecute (Map<OverlayAvailability, Map<CityBikesStation, String>> availabilityMap){
            // stationList is ordered by availability, from no to high
            super.onPostExecute(availabilityMap);
            if (availabilityMap != null || availabilityMap.size() != 0){
                infoBox.setText(R.string.infobox_adding_stations);

                // Add markers to the map view choosing the right marker which depends on the availability
                // The String element of the stationMap entry is the description of the station the user can
                // read when he or she tap to a statation on the map
                for (OverlayAvailability availability : OverlayAvailability.values()){
                    mapManager.replaceAllMarkersOn(overlayNames.get(availability),
                            availabilityMap.get(availability),
                            overlayDrawables.get(availability));
                    mapManager.setOverlayVisibility(overlayNames.get(availability), true);
                }
                mapManager.replaceMyPositionMarker(currentLocation,
                        resources.getDrawable(R.drawable.ic_my_location_24px));
                mapManager.setMyPositionOverlayVisibility(true);
                stationListView.setAdapter(new ShowStationAdapter(context,
                        stationMap,
                        mapManager.getMapView(),
                        showAvailablePlaces));
                stationListView.invalidate();
                visibleOverlayButton.setEnabled(true);
            }
            else {
                // Show the problem to the user but still maitain the app active
                BikeDaCityUtil.createAlertDialogWithPositiveButtonOnly(context,
                        R.string.err_dialog_no_city_found_title,
                        R.string.err_dialog_no_city_found_message,
                        R.string.err_dialog_no_city_found_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick (DialogInterface dialog, int which) {
                            }
                        }).show();
                stationListView.setAdapter(new NoStationAdapter(context, address));
            }
            infoBox.setText(resources.getString(R.string.infobox_current_location,
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude()));
            refreshMap.setEnabled(true);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        if (isExternalStorageAvailable()){
            preferences = PreferenceManager.getDefaultSharedPreferences(this);
            Configuration.getInstance().load(getApplicationContext(), preferences);
            setContentView(R.layout.activity_main);
            resources = getResources();
            infoBox = findViewById(R.id.infoBox);
            infoBox.setText(R.string.infobox_init_app);
            locationReceiver = new LocationBroadcastReceiver();
            pendingIntent = createPendingIntent();
            stationListView = findViewById(R.id.station_list);
            stationListView.setHasFixedSize(true);
            stationListView.setLayoutManager(new LinearLayoutManager(this));
            mapManager = new OSMapManager(this, (MapView) findViewById(R.id.map));
            permissionOk = false;

            if (savedInstanceState == null){
                Log.d(TAG, "Getting showAvailablePlaces from preferences");
                String defaultView = preferences.getString(resources.getString(R.string.default_view_list_key),
                                                           resources.getString(R.string.default_pref_view_value));
                showAvailablePlaces = defaultView.equals(resources.getString(R.string.default_pref_view_value));

            }
            else {
                Log.d(TAG, "Getting showAvailablePlaces from savedInstanceState");
                showAvailablePlaces = savedInstanceState.getBoolean(resources.getString(R.string.pref_show_available_places));
            }
            overlayNames = getOverlayNames();
            showVisibleOverlaysButtonBackgroundIds = buildShowOptionButtonBackgrounds();
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

    private int[] buildShowOptionButtonBackgrounds (){

        // This part is application specific, pay attention to the priority order into OverlayAvailability enum
        int[] backgrounds = new int[OverlayAvailability.values().length << 1];
        backgrounds[0] = R.drawable.ic_place_view_all_h24;
        backgrounds[1] = R.drawable.ic_place_view_up_to_low_h24;
        backgrounds[2] = R.drawable.ic_place_view_up_to_medium_h24;
        backgrounds[3] = R.drawable.ic_place_view_high_h24;
        backgrounds[4] = R.drawable.ic_free_bike_view_all_h24;
        backgrounds[5] = R.drawable.ic_free_bike_view_up_to_low_h24;
        backgrounds[6] = R.drawable.ic_free_bike_view_up_to_medium_h24;
        backgrounds[7] = R.drawable.ic_free_bike_view_high_h24;
        return backgrounds;
    }

    private int getShowOptionButtonBackground (int idx){
        return showVisibleOverlaysButtonBackgroundIds[(showAvailablePlaces) ? idx :
                                                                   idx + OverlayAvailability.values().length];
    }

    private Map<OverlayAvailability, String> getOverlayNames (){
        Map<OverlayAvailability, String> overlayNames = new EnumMap<>(OverlayAvailability.class);
        overlayNames.put(OverlayAvailability.NO_AVAILABILITY, resources.getString(R.string.no_availability_overlay_name));
        overlayNames.put(OverlayAvailability.LOW_AVAILABILITY, resources.getString(R.string.low_availability_overlay_name));
        overlayNames.put(OverlayAvailability.MEDIUM_AVAILABILITY, resources.getString(R.string.medium_availability_overlay_name));
        overlayNames.put(OverlayAvailability.HIGH_AVAILABILITY, resources.getString(R.string.high_availability_overlay_name));
        return overlayNames;
    }

    private Map<OverlayAvailability, Drawable> getOverlayDrawables (){
        Map<OverlayAvailability, Drawable> drawables = new EnumMap<>(OverlayAvailability.class);
        if (showAvailablePlaces){
            drawables.put(OverlayAvailability.NO_AVAILABILITY,
                          resources.getDrawable(R.drawable.ic_place_no_availability));
            drawables.put(OverlayAvailability.LOW_AVAILABILITY,
                          resources.getDrawable(R.drawable.ic_place_low_availability));
            drawables.put(OverlayAvailability.MEDIUM_AVAILABILITY,
                          resources.getDrawable(R.drawable.ic_place_medium_availability));
            drawables.put(OverlayAvailability.HIGH_AVAILABILITY,
                          resources.getDrawable(R.drawable.ic_place_high_availability));
        }
        else {
            drawables.put(OverlayAvailability.NO_AVAILABILITY,
                          resources.getDrawable(R.drawable.ic_free_bike_no_availability));
            drawables.put(OverlayAvailability.LOW_AVAILABILITY,
                          resources.getDrawable(R.drawable.ic_free_bike_low_availability));
            drawables.put(OverlayAvailability.MEDIUM_AVAILABILITY,
                          resources.getDrawable(R.drawable.ic_free_bike_medium_availability));
            drawables.put(OverlayAvailability.HIGH_AVAILABILITY,
                          resources.getDrawable(R.drawable.ic_free_bike_high_availability));
        }
        return drawables;
    }

    private PendingIntent createPendingIntent () {
        return PendingIntent.getBroadcast(getApplicationContext(),
                                          REQUEST_CODE,
                                          new Intent(CHANGE_LOCATION_ACTION),
                                          PendingIntent.FLAG_UPDATE_CURRENT);
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
        mi.inflate(R.menu.main_menu, menu);
        showOptionItem = menu.findItem(R.id.show_option);
        showOptionItem.setTitle(showAvailablePlaces ? resources.getString(R.string.show_available_places_entry) :
                                                      resources.getString(R.string.show_free_bikes_entry));
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
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Log.i(TAG, "App has the right permissions granted");
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                requestEnablingProvider();
            }
            else {
                registerReceiver(locationReceiver, new IntentFilter(CHANGE_LOCATION_ACTION));
                String minTimePreference =
                        preferences.getString(resources.getString(R.string.location_interval_list_key),
                                              resources.getString(R.string.default_pref_location_value));
                int minTime = Integer.parseInt(minTimePreference);
                if (minTime == 0){
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, pendingIntent);
                }
                else {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 0,
                                                           pendingIntent);
                }
                String counter = preferences.getString(resources.getString(R.string.default_view_station_key),
                                                       resources.getString(R.string.default_view_station_value));
                visibleOverlayCounter = Integer.parseInt(counter);
                isFirstFix = true;
            }
        }
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
        mapManager.onResume();
        overlayDrawables = getOverlayDrawables();

        // Get values saved into onPause(), if they don't exist take values from preferences
        visibleOverlayCounter = preferences.getInt(
                                resources.getString(R.string.pref_visible_overlays),
                                Integer.parseInt(preferences.getString(resources.getString(R.string.default_view_station_key),
                                                      resources.getString(R.string.default_view_station_value))));
        Button showOptionButton = findViewById(R.id.view_overlay_button);
        showOptionButton.setBackgroundResource(getShowOptionButtonBackground(visibleOverlayCounter));
    }

    @Override
    public void onPause (){
        Log.d(TAG, "onPause() Called");
        super.onPause();
        mapManager.onPause();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(resources.getString(R.string.pref_show_available_places), showAvailablePlaces);
        editor.putInt(resources.getString(R.string.pref_visible_overlays), visibleOverlayCounter);
        editor.apply();
    }

    @Override
    public void onStop(){
        super.onStop();
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

            case R.id.show_option:
                if (task == null || task.getStatus() != AsyncTask.Status.RUNNING){
                    // If user taps the menuItem then they want the app to show the other one mode, where
                    // mode is one of available places or free bikes
                    showAvailablePlaces = !showAvailablePlaces;
                }
                else {
                    Toast.makeText(this, resources.getString(R.string.toast_running_task),
                            Toast.LENGTH_SHORT).show();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void refreshMap (View v){
        locationReceiver.onLocationChanged(this, currentLocation);
    }

    public void centreMapOnCurrentLocation (View v){
        mapManager.moveCameraTo(currentLocation);
    }

    public void addVisibleOverlay (View v){
        List<String> nameList = new LinkedList<>();
        visibleOverlayCounter %= overlayNames.size();
        OverlayAvailability[] knownOverlay = OverlayAvailability.values();

        // Set from higher to lower priority depending on the number of tap the user does
        for (int i = knownOverlay.length - 1; i >= visibleOverlayCounter; --i){
            nameList.add(overlayNames.get(knownOverlay[i]));
        }
        nameList.add(resources.getString(R.string.current_location_overlay_name));
        mapManager.setVisibleOverlays(nameList);
        v.setBackgroundResource(getShowOptionButtonBackground(visibleOverlayCounter));
    }


    public class LocationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive (Context context, Intent intent) {
            if (intent.hasExtra(LocationManager.KEY_LOCATION_CHANGED)) {
                Location location = (Location) intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);
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

            if (currentLocation == null || !currentLocation.equals(location)){
                currentLocation = location;

                if (isFirstFix){
                    mapManager.moveCameraTo(currentLocation);
                    isFirstFix = false;
                }

                task = new BuildStationMapTask(context);
                task.execute();

// This have to rethinked a bit, maybe it is useful to show the toast when the refresh map has tapped
//                // It could happen a location update before the task has finished its job. This could potentially
//                // make a new task running and concurrently changing the data structures of the map.
//                if (task == null || task.getStatus() != AsyncTask.Status.RUNNING){
//                    task = new BuildStationMapTask(context);
//                    task.execute();
//                }
//                else if (task.getStatus() == AsyncTask.Status.RUNNING){
//                    Toast.makeText(context, resources.getString(R.string.toast_running_task),
//                            Toast.LENGTH_SHORT).show();
//                }
//                else if (task.getStatus() == AsyncTask.Status.PENDING){
//                    Toast.makeText(context, resources.getString(R.string.toast_pending_task),
//                            Toast.LENGTH_SHORT).show();
//                }
            }
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
