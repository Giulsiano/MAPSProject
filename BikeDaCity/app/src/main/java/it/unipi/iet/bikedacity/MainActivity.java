package it.unipi.iet.bikedacity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback, LocationListener {

    static final String TAG = "MainActivity";
    private MapView cityMap;
    private CitybikesManager citybikesManager;
    private LocationManager locationManager;
    private Location currentLocation;
    private boolean permissionOk;

    private static final int REQUEST_PERMISSIONS = 0;
    private static final long OLD_THRESHOLD = 1*60*1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.mainmenu, menu);
        return true;
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
