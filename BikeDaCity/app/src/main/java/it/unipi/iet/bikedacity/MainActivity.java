package it.unipi.iet.bikedacity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
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
import java.util.Locale;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback, LocationListener {

    static final String TAG = "MainActivity";
    private MapView cityMap;
    private CitybikesManager citybikesManager;
    private LocationManager locationManager;
    private Location currentLocation;
    private RecyclerView stationList;
    private boolean permissionOk;
    private List<OverlayItem> mapItems;
    private boolean showAvailablePlaces;

    private static final int REQUEST_PERMISSIONS = 0;
    private static final long OLD_THRESHOLD = 1*60*1000;

    enum ProgressState{
        REQUEST_CITY,
        COMPUTING_DISTANCES
    }

    private class DownloadStationsTask extends AsyncTask<Void, ProgressState, TreeMap<Float, List<CityBikesStation>>> {

        TextView infoBox;
        ProgressBar progressBar;

        public DownloadStationsTask(){
            super();
            infoBox = findViewById(R.id.infoBox);
            progressBar = findViewById(R.id.indeterminateBar);
        }

        @Override
        protected void onPreExecute () {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            infoBox.setText(R.string.infobox_starting_text);
        }

        @Override
        protected void onPostExecute (final TreeMap<Float, List<CityBikesStation>> stationMap) {
            super.onPostExecute(stationMap);
            Resources res = getResources();
            infoBox.setText(res.getString(R.string.infobox_adding_stations));
            // Fill the map with the marker for the stations
            new Thread(){
                @Override
                public void run(){
                    IMapController controller = cityMap.getController();
                    controller.setCenter(new GeoPoint(currentLocation.getLatitude(),
                                                      currentLocation.getLongitude()));
                    mapItems = new ArrayList<>();
                    for (Float distance : stationMap.keySet()){
                        for (CityBikesStation station : stationMap.get(distance)){
                            String description;
                            Drawable marker;
                            if (showAvailablePlaces){
                                description = String.format(Locale.getDefault(),
                                        "Available places: %d\nDistance: %f", station.getEmptySlots(), distance);
                                marker = getResources().getDrawable(R.drawable.ic_local_parking_24px);
                            }
                            else {
                                description = String.format(Locale.getDefault(),
                                        "Available bikes: %d\nDistance: %f", station.getFreeBikes(), distance);
                                marker = getResources().getDrawable(R.drawable.ic_directions_bike_24px);
                            }
                            switch ((showAvailablePlaces) ? station.getFreePlacesLevel() :
                                                            station.getAvailableBikesLevel()){
                                case NO:
                                    marker.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                                    break;

                                case LOW:
                                    marker.setColorFilter(Color.YELLOW, PorterDuff.Mode.MULTIPLY);
                                    break;

                                case MEDIUM:
                                case HIGH:
                                    marker.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                                    break;
                            }
                            OverlayItem stationItem = new OverlayItem(station.getName(),
                                                                     description,
                                                                     new GeoPoint(station.getLocation().getLatitude(),
                                                                                  station.getLocation().getLongitude()));
                            stationItem.setMarker(marker);
                            mapItems.add(stationItem);
                        }
                    }
                    fillCityMap();
                }
            }.start();
            stationList.invalidate();
            stationList.setAdapter(new ShowStationAdapter(stationMap));
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
        protected TreeMap<Float, List<CityBikesStation>> doInBackground (Void... voids) {
            publishProgress(ProgressState.REQUEST_CITY);
            String city = OSMNominatimService.getCityFrom(currentLocation.getLatitude(),
                                                          currentLocation.getLongitude());
            citybikesManager = new CitybikesManager(city);
            publishProgress(ProgressState.COMPUTING_DISTANCES);
            return (showAvailablePlaces) ?
                    citybikesManager.getNearestAvailableBikesFrom(currentLocation) :
                    citybikesManager.getNearestFreePlacesFrom(currentLocation);
        }
    }

    private void fillCityMap (){
        if (mapItems == null || mapItems.isEmpty()){
            Log.e(TAG, "No mapItems found");
            return;
        }

        // TODO put map items to the city map
        ItemizedIconOverlay stationOverlay = new ItemizedIconOverlay(mapItems,
                new ItemizedIconOverlay.OnItemGestureListener() {
                    @Override
                    public boolean onItemSingleTapUp (int index, Object item) {
                        return false;
                    }

                    @Override
                    public boolean onItemLongPress (int index, Object item) {
                        return false;
                    }
                }, this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If permissions have not been granted ask the user to do that
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
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

    private boolean isExternalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public void onStart(){
        super.onStart();

        // Request permission for this app to work properly
        permissionOk = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        permissionOk &= ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        if (!permissionOk) {
            dealWithPermissions();
        }
        else {
            Log.i(TAG, "App has the right permissions granted");
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                requestEnablingProvider();
            }
            else {
                currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (isCurrentLocationOlderThan(OLD_THRESHOLD)){
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

    private void dealWithPermissions(){
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.about:
                DialogFragment df = new AboutFragmentDialog();
                df.show(getFragmentManager(), "AboutDialog");
                return true;

            case R.id.settings:
                startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), 0);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void sendEmail (View v){
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto",
                getResources().getString(R.string.devel_email),
                null));
        startActivity(Intent.createChooser(emailIntent, "Send email to developer"));
    }

    public void openSite (View v){
        String url = "https://";
        switch (v.getId()){
            case R.id.source_name_1:
            case R.id.source_site_1:
                url += getResources().getString(R.string.source_site_1);
                break;

            case R.id.source_name_2:
            case R.id.source_site_2:
                url += getResources().getString(R.string.source_site_2);
                break;
        }
        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        browserIntent.setData(Uri.parse(url));
        startActivity(Intent.createChooser(browserIntent, "Open in browser..."));
    }

    @Override
    public void onLocationChanged (Location location) {
        currentLocation = location;

        // Centre the map to the new location
        GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        IMapController mapController = cityMap.getController();
        mapController.setCenter(currentPoint);
        mapController.zoomTo(16, 1000L);
        new DownloadStationsTask().execute();
        // TODO check internet connection
        // TODO Map item to be added
    }

    @Override
    public void onStatusChanged (String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled (String provider) {

    }

    @Override
    public void onProviderDisabled (String provider) {

    }
}
