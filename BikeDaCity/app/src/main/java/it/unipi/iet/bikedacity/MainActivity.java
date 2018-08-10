package it.unipi.iet.bikedacity;

import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "MainActivity";

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
                DialogFragment df = new MyFragmentDialog();
                df.show(getFragmentManager(), "AboutDialog");
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
}
