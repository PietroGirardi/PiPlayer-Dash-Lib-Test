package com.example.pietrogirardi.piplayerexample;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.Button;

import com.example.pietrogirardi.piplayer.PiPlayer;

public class PlayerActivity extends AppCompatActivity {

    public static final String CONTENT_TYPE_EXTRA = "content_type";
    public static final String CONTENT_ID_EXTRA = "content_id";
    public static final String PROVIDER_EXTRA = "provider";

    private Uri contentUri;
    private SurfaceView surfaceView;
    PiPlayer piPlayer;

    private Button btnVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        surfaceView = (SurfaceView) findViewById(R.id.surface);
        btnVideo = (Button) findViewById(R.id.video_controls);

        Intent intent = getIntent();
        contentUri = intent.getData();
        piPlayer = new PiPlayer(this, contentUri, surfaceView);
        piPlayer.Play();
        piPlayer.setAddMediaController(true);
        piPlayer.addQualityConfiguration(btnVideo);

    }

    @Override
    public void onNewIntent(Intent intent) {
        piPlayer.releasePlayer();
        setIntent(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        piPlayer.releasePlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        piPlayer.releasePlayer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        piPlayer.releasePlayer();
    }

}
