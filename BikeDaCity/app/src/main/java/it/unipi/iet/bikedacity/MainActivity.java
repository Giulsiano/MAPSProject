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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// TODO Menu key for cleaning osmdroid tiles (?)
// TODO get settings
// TODO Save showAvailablePlaces on calling onSaveInstaces
// TODO

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "MainActivity";
    private static final String CHANGE_LOCATION_ACTION = "it.unipi.iet.bikedacity.CHANGE_LOCATION";
    private static final int REQUEST_CODE = 0;

    private SharedPreferences preferences;
    private Resources resources;
    private StationMapManager mapManager;
    private CityBikesManager cityBikesManager;
    private Location currentLocation;
    private RecyclerView stationList;
    private TextView infoBox;
    private LocationBroadcastReceiver locationReceiver;
    private PendingIntent pendingIntent;
    private BuildStationMapTask task;

    private boolean permissionOk;
    private boolean showAvailablePlaces;

    private static final int REQUEST_PERMISSIONS = 0;
    private static final long OLD_THRESHOLD = 1*60*1000;

    enum ProgressState{
        REQUEST_CITY,
        COMPUTING_DISTANCES,
        MAKE_STATION_LIST
    }

    private class BuildStationMapTask extends AsyncTask<Void, ProgressState,
                                                        TreeMap<Integer, List<CityBikesStation>>> {

        ProgressBar progressBar;
        Button refreshMap;
        Context context;

        public BuildStationMapTask (Context ctx){
            super();
            refreshMap = findViewById(R.id.refreshMapButton);
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
            }
            else {
                // Create maps of (station, description) based on the availability
                Map<CityBikesStation, String> noAvailabilityMap = null;
                Map<CityBikesStation, String> lowAvailabilityMap = null;
                Map<CityBikesStation, String> mediumAvailabilityMap = null;
                Map<CityBikesStation, String> highAvailabilityMap = null;
                switch (preferences.getInt(resources.getString(R.string.default_view_station_key), 2)){
                    case 3:
                        noAvailabilityMap = new HashMap<>();
                    case 2:
                        lowAvailabilityMap = new HashMap<>();
                    case 1:
                        mediumAvailabilityMap = new HashMap<>();
                    case 0:
                        highAvailabilityMap = new HashMap<>();
                        break;
                }
                String description;
                for (Integer distance : stationMap.keySet()){
                    for (CityBikesStation station : stationMap.get(distance)){
                        Map<CityBikesStation, String> map = null;
                        String availabilityString = null;
                        switch ((showAvailablePlaces) ? station.getFreePlacesLevel() :
                                                        station.getAvailableBikesLevel()){
                            case NO:
                                map = noAvailabilityMap;
                                availabilityString = "NO";
                                break;

                            case LOW:
                                map = lowAvailabilityMap;
                                availabilityString = "Low";
                                break;

                            case MEDIUM:
                                map = mediumAvailabilityMap;
                                availabilityString = "Medium";
                                break;

                            case HIGH:
                                map = highAvailabilityMap;
                                availabilityString = "High";
                                break;
                        }
                        description = resources.getString(R.string.marker_description_available_places,
                                                          station.getEmptySlots(),
                                                          distance,
                                                          availabilityString
                                );
                        map.put(station, description);
                    }
                }
                // Add markers to the map view choosing the right marker which depends on the availability
                if (showAvailablePlaces){
                    mapManager.replaceStationMarkers(noAvailabilityMap,
                                            resources.getDrawable(R.drawable.place_no_availability_24dp));
                    mapManager.replaceStationMarkers(lowAvailabilityMap,
                                            resources.getDrawable(R.drawable.place_low_availability_24dp));
                    mapManager.replaceStationMarkers(mediumAvailabilityMap,
                                            resources.getDrawable(R.drawable.place_medium_availability_24dp));
                    mapManager.replaceStationMarkers(highAvailabilityMap,
                                            resources.getDrawable(R.drawable.place_high_availability_24dp));
                }
                else {
                    mapManager.replaceStationMarkers(noAvailabilityMap,
                                        resources.getDrawable(R.drawable.free_bike_no_availability_24dp));
                    mapManager.replaceStationMarkers(lowAvailabilityMap,
                                        resources.getDrawable(R.drawable.free_bike_low_availability_24dp));
                    mapManager.replaceStationMarkers(mediumAvailabilityMap,
                                        resources.getDrawable(R.drawable.free_bike_medium_availability_24dp));
                    mapManager.replaceStationMarkers(highAvailabilityMap,
                                        resources.getDrawable(R.drawable.free_bike_high_availability_24dp));
                }
                // Recreate the RecyclerView list and center the map to the current location
                mapManager.replaceCurrentLocationMarker(currentLocation,
                                                        resources.getDrawable(R.drawable.current_location));
                mapManager.moveTo(currentLocation);
                stationList.invalidate();
                stationList.setAdapter(new ShowStationAdapter(context,
                                                              stationMap,
                                                              mapManager.getMap(),
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
            progressBar.setVisibility(View.VISIBLE);
            refreshMap.setVisibility(View.INVISIBLE);
            if (cityBikesManager == null){
                cityBikesManager = new CityBikesManager();
            }
            String city = cityBikesManager.getCity();
            if (city == null){
                publishProgress(ProgressState.REQUEST_CITY);
                city = OSMNominatimService.getCityFrom(currentLocation.getLatitude(),
                        currentLocation.getLongitude());
                if (city == null) return null;
                else cityBikesManager.setCity(city);
            }
            publishProgress(ProgressState.COMPUTING_DISTANCES);
            return (showAvailablePlaces) ?
                    cityBikesManager.getNearestFreePlacesFrom(currentLocation) :
                    cityBikesManager.getNearestAvailableBikesFrom(currentLocation);
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
            mapManager = new StationMapManager(this, (MapView) findViewById(R.id.map));
            pendingIntent = createPendingIntent();
            permissionOk = false;
            preferences = PreferenceManager.getDefaultSharedPreferences(this);
            showAvailablePlaces = preferences.getBoolean(resources.getString(R.string.map_default_view_list_key), true);
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
        return PendingIntent.getBroadcast(getApplicationContext(), REQUEST_CODE, intent,
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
                int minTime = preferences.getInt(resources.getString(R.string.location_interval_list_key),
                        10000);
                if (minTime == 0){
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, pendingIntent);
                }
                else {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            minTime,
                            0,
                            pendingIntent);
                }
            }
        }
    }

    private boolean isCurrentLocationTooOld(){
        return currentLocation == null ||
                Math.abs(currentLocation.getTime() - System.currentTimeMillis()) > OLD_THRESHOLD;
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
    }

    @Override
    public void onPause (){
        Log.d(TAG, "onPause() Called");
        super.onPause();
        mapManager.onPause();
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

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void refreshMap (View v){
        // TODO implement the refreshing of the map, it could require some changes on design
        locationReceiver.onLocationChanged(this, currentLocation);
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
            currentLocation = location;

            // It could happen a location update before the task has finished its job. This could potentially
            // make a new task running and concurrently changing the data structures of the map.
            if (task == null || task.getStatus() != AsyncTask.Status.RUNNING){
                task = new BuildStationMapTask(context);
                task.execute();
            }
            if (task.getStatus() == AsyncTask.Status.RUNNING){
                Toast.makeText(context, resources.getString(R.string.toast_running_task),
                               Toast.LENGTH_SHORT).show();
            }
            if (task.getStatus() == AsyncTask.Status.PENDING){
                Toast.makeText(context, resources.getString(R.string.toast_pending_task),
                               Toast.LENGTH_SHORT).show();
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
