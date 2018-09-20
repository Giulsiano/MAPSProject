package it.unipi.iet.bikedacity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
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

// TODO make about fragment dialog an activity
// TODO add Location Service to respect requirements for settings activity
// TODO new marker for availability (need Inkscape)
// TODO try to understand if there are needed some fragments (maybe only one)
// TODO use onSaveInstateState to save simple values

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    static final String TAG = "MainActivity";
    private MapView cityMap;
    private CityBikesManager cityBikesManager;
    private LocationManager locationManager;
    private Location currentLocation;
    private RecyclerView stationList;
    private TextView infoBox;

    private boolean permissionOk;
    private List<OverlayItem> mapItems;
    private boolean showAvailablePlaces;

    private static final int REQUEST_PERMISSIONS = 0;
    private static final long OLD_THRESHOLD = 1*60*1000;

    enum ProgressState{
        REQUEST_CITY,
        COMPUTING_DISTANCES
    }

    private class DownloadStationsTask extends AsyncTask<Void, ProgressState, TreeMap<Integer, List<CityBikesStation>>> {

        ProgressBar progressBar;
        Button refreshMap;
        Context mainActivity;

        public DownloadStationsTask(Context mainActivity){
            super();
            refreshMap = findViewById(R.id.refreshMap);
            infoBox = findViewById(R.id.infoBox);
            progressBar = findViewById(R.id.indeterminateBar);
            this.mainActivity = mainActivity;
        }

        @Override
        protected void onPreExecute () {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            refreshMap.setVisibility(View.INVISIBLE);
            infoBox.setText(R.string.infobox_starting_text);
        }

        @Override
        protected void onPostExecute (final TreeMap<Integer, List<CityBikesStation>> stationMap) {
            super.onPostExecute(stationMap);
            Resources res = getResources();
            infoBox.setText(res.getString(R.string.infobox_adding_stations));
            if (stationMap == null){
                // Exit the app if there are no stations
                AlertDialog noCityDialog = createAlertDialogWithPositiveButtonOnly(R.string.err_dialog_no_city_found_title,
                        R.string.err_dialog_no_city_found_message,
                        R.string.err_dialog_no_city_found_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick (DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                noCityDialog.show();
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
                        }, mainActivity);
                cityMap.getOverlays().add(stationOverlay);

                // Recreate the RecyclerView list and center the map to the current location
                stationList.invalidate();
                stationList.setAdapter(new ShowStationAdapter(mainActivity,
                                                              stationMap,
                                                              cityMap,
                                                              showAvailablePlaces));
                infoBox.setText(res.getString(R.string.infobox_current_location,
                                              currentLocation.getLatitude(),
                                              currentLocation.getLongitude()));
                progressBar.setVisibility(View.INVISIBLE);
                refreshMap.setVisibility(View.VISIBLE);
                IMapController controller = cityMap.getController();

                // Need to zoom then center due to a bug in OSMDroid
                controller.zoomTo(14, 1000L);
                controller.setCenter(new GeoPoint(currentLocation.getLatitude(),
                                                  currentLocation.getLongitude()));
            }
        }

        @Override
        protected void onProgressUpdate (ProgressState... progresses) {
            super.onProgressUpdate(progresses);
            ProgressState progressState = progresses[0];
            switch(progressState){
                case REQUEST_CITY:
                    infoBox.setText(getResources().getString(R.string.infobox_request_city));
                    break;

                case COMPUTING_DISTANCES:
                    infoBox.setText(getResources().getString(R.string.infobox_station_ordered));
                    break;
            }
        }

        @Override
        protected TreeMap<Integer, List<CityBikesStation>> doInBackground (Void... voids) {
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
            if (distanceMap != null) {
                mapItems = new ArrayList<>();
                Resources res = mainActivity.getResources();
                for (Integer distance : distanceMap.keySet()){
                    for (CityBikesStation station : distanceMap.get(distance)){
                        String description;
                        Drawable marker;
                        if (showAvailablePlaces){
                            description = res.getString(R.string.marker_description_available_places,
                                                        station.getEmptySlots(),
                                                        distance);
                            marker = res.getDrawable(R.drawable.ic_local_parking_24px);
                        }
                        else {
                            description = res.getString(R.string.marker_description_available_bikes,
                                                        station.getFreeBikes(),
                                                        distance);
                            marker = res.getDrawable(R.drawable.ic_directions_bike_24px);
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
                OverlayItem currentLocationItem = new OverlayItem(res.getString(R.string.marker_my_location_name),
                                                                  res.getString(R.string.marker_my_location_description),
                                                                  new GeoPoint(currentLocation.getLatitude(),
                                                                        currentLocation.getLongitude())
                );
                currentLocationItem.setMarker(res.getDrawable(R.drawable.ic_place_24px));
                mapItems.add(currentLocationItem);
            }
            return distanceMap;
        }
    }

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isExternalStorageAvailable()){
            // Initialize the map
            Context ctx = getApplicationContext();
            Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
            setContentView(R.layout.activity_main);
            stationList = findViewById(R.id.stationList);
            stationList.setHasFixedSize(true);
            stationList.setLayoutManager(new LinearLayoutManager(this));
            cityMap = findViewById(R.id.map);
            cityMap.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
            cityMap.setTilesScaledToDpi(true);
            cityMap.setBuiltInZoomControls(true);
            cityMap.setMultiTouchControls(true);
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            permissionOk = false;
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            showAvailablePlaces = sp.getBoolean(getResources().getString(R.string.map_default_view_list_key),true);
            infoBox = findViewById(R.id.infoBox);
            infoBox.setText(R.string.infobox_init_app);
        }
        else {
            AlertDialog errorAlert = createAlertDialogWithPositiveButtonOnly(
                    R.string.err_dialog_no_ext_storage_title,
                    R.string.err_dialog_no_ext_storage_message,
                    R.string.err_dialog_no_ext_storage_button,
                    new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick (DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            errorAlert.show();
        }
    }

    private AlertDialog createAlertDialogWithPositiveButtonOnly (int title,
                                                                 int message,
                                                                 int buttonText,
                                                                 DialogInterface.OnClickListener listener){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(buttonText, listener);
        return builder.create();
    }

    private AlertDialog createAlertDialogWithTwoButton (int title,
                                                        int message,
                                                        int positiveButtonText,
                                                        int negativeButtonText,
                                                        DialogInterface.OnClickListener positiveListener,
                                                        DialogInterface.OnClickListener negativeListener){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(positiveButtonText, positiveListener);
        builder.setNegativeButton(negativeButtonText, negativeListener);
        return builder.create();
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
                AlertDialog errorDialog = createAlertDialogWithPositiveButtonOnly(
                        R.string.err_dialog_perm_not_granted_title,
                        R.string.err_dialog_perm_not_granted_message,
                        R.string.err_dialog_perm_not_granted_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick (DialogInterface dialog, int which) {
                                finish();
                            }
                        }
                );
                errorDialog.show();
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
            showPermissionsDialog();
        }
        else {
            Log.i(TAG, "App has the right permissions granted");
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                requestEnablingProvider();
            }
            else {
                currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (isCurrentLocationOlderThan(OLD_THRESHOLD)){
                    // TODO delete part of this code since it is useless with locationBroadcastReceiver
                    Log.i(TAG, "Current location is too old. Request a new one");
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER,
                            this,
                            null);
                }
            }
        }
    }

    private boolean isCurrentLocationOlderThan (long time){
        return currentLocation == null || Math.abs(currentLocation.getTime() - System.currentTimeMillis()) > time;
    }

    private void showPermissionsDialog (){
        Log.i(TAG, "Request permissions to the user");
        final AppCompatActivity mainActivity = this;
        AlertDialog permissionDialog = createAlertDialogWithPositiveButtonOnly(
                R.string.perm_dialog_title,
                R.string.perm_dialog_message,
                R.string.perm_dialog_button,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick (DialogInterface dialog, int which) {
                        String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        ActivityCompat.requestPermissions(mainActivity, permissions, REQUEST_PERMISSIONS);
                    }
                });
        permissionDialog.show();
    }

    private void requestEnablingProvider (){
        Log.w(TAG, "Request the user to enable the GPS provider");
        AlertDialog enableGPS = createAlertDialogWithTwoButton(
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
        );
        enableGPS.show();
    }

    @Override
    public void onResume (){
        super.onResume();
        cityMap.onResume();
        if (currentLocation == null){
            Log.i(TAG, "Current location is null. Require new location if known");
            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    Log.i(TAG, LocationManager.GPS_PROVIDER + " is enabled");
                    currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (isCurrentLocationOlderThan(OLD_THRESHOLD)){
                        Log.i(TAG, "Current location is too old. Request a new one");
                        Criteria criteria = new Criteria();
                        criteria.setAccuracy(Criteria.ACCURACY_FINE);
                        List<String> providers = locationManager.getProviders(criteria, true);
                        if (providers.isEmpty() || !providers.contains(LocationManager.GPS_PROVIDER)){
                            Log.w(TAG, "No providers satisfy criteria found");

                        }
                        infoBox.setText(getResources().getString(R.string.infobox_waiting_location));
                        // TODO delete part of this code since it is useless with locationBroadcastReceiver
                        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER,
                                this,
                                null);
                    }
                }
                else {
                    Log.w(TAG, "GPS is not enabled");
                    requestEnablingProvider();
                }
            }
            catch (SecurityException e){
                Log.e(TAG, e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onPause (){
        Log.d(TAG, "onPause() Called");
        super.onPause();
        cityMap.onPause();
        locationManager.removeUpdates(this);
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
            if (context instanceof MainActivity){
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
        }

        public void onLocationChanged (Context context, Location location){
            currentLocation = location;
            new DownloadStationsTask(context).execute();
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
