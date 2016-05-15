package com.example.pietrogirardi.piplayerexample;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ListView listView ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        listView = (ListView) findViewById(R.id.list_videos);

        ArrayAdapter<Sample> adapter = new MyCustomAdapter(this, getSamples());
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Sample s = (Sample) listView.getItemAtPosition(position);

                Intent mpdIntent = new Intent(MainActivity.this, PlayerActivity.class)
                        .setData(Uri.parse(s.getUri()))
                        .putExtra(PlayerActivity.CONTENT_TYPE_EXTRA, s.getType())
                        .putExtra(PlayerActivity.CONTENT_ID_EXTRA, s.contentId)
                        .putExtra(PlayerActivity.PROVIDER_EXTRA, s.provider);

                startActivity(mpdIntent);
            }
        });

    }



    private List<Sample> getSamples() {
        List<Sample> list = new ArrayList<Sample>();
        list.add(new Sample("Smurfs", "http://demo.unified-streaming.com/video/smurfs/smurfs.ism/smurfs.mpd"));
        list.add(new Sample("Manifest", "http://dash.edgesuite.net/envivio/dashpr/clear/Manifest.mpd"));

        return list;
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
