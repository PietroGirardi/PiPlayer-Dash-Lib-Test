package com.example.pietrogirardi.piplayer;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.PopupMenu;

import com.example.pietrogirardi.piplayer.Player.RendererBuilder;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.util.Util;

/**
 * Created by pietrogirardi on 14/05/16.
 */
public class PiPlayer implements SurfaceHolder.Callback{

    private static final int MENU_GROUP_TRACKS = 1;
    private static final int ID_OFFSET = 2;

    private Activity activity;
    private Uri uri;
    private SurfaceView surfaceView;
    private boolean addMediaController;
    private Player player;
    private KeyCompatibleMediaController mediaController;
    private View viewQuality;
    private boolean playerNeedsPrepare;

    public PiPlayer(Activity activity, Uri uri, SurfaceView surfaceView) {
        this.activity = activity;
        this.uri = uri;
        this.surfaceView = surfaceView;
    }

    public PiPlayer(Activity activity, Uri uri, SurfaceView surfaceView, boolean addMediaController) {
        this.activity = activity;
        this.uri = uri;
        this.surfaceView = surfaceView;
        this.addMediaController = addMediaController;
    }

    public void Play(){

        if (Util.SDK_INT <= 23 || player == null) {
            onShown();
        }
        else if (Util.SDK_INT > 23) {
            onShown();
        }
    }

    public void setAddMediaController(boolean addMediaController) {
        this.addMediaController = addMediaController;

        if(addMediaController){
            mediaController = new KeyCompatibleMediaController(activity);
            mediaController.setAnchorView(surfaceView);
            mediaController.setMediaPlayer(player.getPlayerControl());
        }
        if(mediaController!=null)
        mediaController.setEnabled(addMediaController);
    }

    public boolean isMediaControllerShowing() {
        if(mediaController !=null){
            return mediaController.isShowing();
        }
        return false;
    }

    public void addQualityConfiguration(View v){
        this.viewQuality = v;

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showVideoPopup(v);
            }
        });
    }

    public void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void onShown() {

        if (!maybeRequestPermission()) {
            setOntouchListner();
            preparePlayer(true);
        }
    }


    private void preparePlayer(boolean playWhenReady) {
        player = new Player(getRendererBuilder());

        //player.addListener(this);
        //player.setCaptionListener(this);
        //player.setMetadataListener(this);
        //player.seekTo(playerPosition);

        playerNeedsPrepare = true;

        setAddMediaController(addMediaController);


        if (playerNeedsPrepare) {
            player.prepare();
            playerNeedsPrepare = false;
        }
        player.setSurface(surfaceView.getHolder().getSurface());
        player.setPlayWhenReady(playWhenReady);
    }

    private void setOntouchListner(){
        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    toggleControlsVisibility();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
                return true;
            }
        });
    }

    private void toggleControlsVisibility()  {
        if(mediaController != null){
            if (mediaController.isShowing()) {
                mediaController.hide();
                viewQuality.setVisibility(View.GONE);
            } else {
                showControls();
            }
        }
    }

    private void showControls() {
        mediaController.show(0);
        viewQuality.setVisibility(View.VISIBLE);
    }


    public void showVideoPopup(View v) {
        PopupMenu popup = new PopupMenu(activity, v);
        configurePopupWithTracks(popup, null, com.example.pietrogirardi.piplayer.Player.TYPE_VIDEO);
        popup.show();
    }



    private void configurePopupWithTracks(PopupMenu popup,
                                          final PopupMenu.OnMenuItemClickListener customActionClickListener,
                                          final int trackType) {
        if (player == null) {
            return;
        }
        int trackCount = player.getTrackCount(trackType);
        if (trackCount == 0) {
            return;
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return (customActionClickListener != null
                        && customActionClickListener.onMenuItemClick(item))
                        || onTrackItemClick(item, trackType);
            }
        });
        Menu menu = popup.getMenu();
        // ID_OFFSET ensures we avoid clashing with Menu.NONE (which equals 0).
        //menu.add(MENU_GROUP_TRACKS, Player.TRACK_DISABLED + ID_OFFSET, Menu.NONE, "OFF");
        for (int i = 0; i < trackCount; i++) {
            menu.add(MENU_GROUP_TRACKS, i + ID_OFFSET, Menu.NONE, buildResolutionString(player.getTrackFormat(trackType, i)));
        }
        menu.setGroupCheckable(MENU_GROUP_TRACKS, true, true);
        menu.findItem(player.getSelectedTrack(trackType) + ID_OFFSET).setChecked(true);
    }


    private boolean onTrackItemClick(MenuItem item, int type) {
        if (player == null || item.getGroupId() != MENU_GROUP_TRACKS) {
            return false;
        }
        player.setSelectedTrack(type, item.getItemId() - ID_OFFSET);
        return true;
    }

    private static String buildResolutionString(MediaFormat format) {
        if (format.adaptive) {
            return "auto";
        }
        return format.width == MediaFormat.NO_VALUE || format.height == MediaFormat.NO_VALUE
                ? "" : format.width + "x" + format.height;
    }


    private RendererBuilder getRendererBuilder() {

        //// TODO: 14/05/16 verify contentid and provider!!!
        String userAgent = Util.getUserAgent(activity, "ExoPlayerDemo");
                return new DashRendererBuilder(activity, userAgent, uri.toString(),
                        new WidevineTestMediaDrmCallback("contentid", "provider"));

    }




    /**
     * Checks whether it is necessary to ask for permission to read storage. If necessary, it also
     * requests permission.
     *
     * @return true if a permission request is made. False if it is not necessary.
     */
    @TargetApi(23)
    private boolean maybeRequestPermission() {
        if (requiresPermission(uri)) {
            activity.requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            return true;
        } else {
            return false;
        }
    }

    @TargetApi(23)
    private boolean requiresPermission(Uri uri) {
        return Util.SDK_INT >= 23
                && Util.isLocalFileUri(uri)
                && activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED;
    }



    private static final class KeyCompatibleMediaController extends android.widget.MediaController {

        private MediaPlayerControl playerControl;

        public KeyCompatibleMediaController(Context context) {
            super(context);
        }

        @Override
        public void setMediaPlayer(MediaPlayerControl playerControl) {
            super.setMediaPlayer(playerControl);
            this.playerControl = playerControl;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            int keyCode = event.getKeyCode();
            if (playerControl.canSeekForward() && (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                    || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    playerControl.seekTo(playerControl.getCurrentPosition() + 15000); // milliseconds
                    show();
                }
                return true;
            } else if (playerControl.canSeekBackward() && (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND
                    || keyCode == KeyEvent.KEYCODE_DPAD_LEFT)) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    playerControl.seekTo(playerControl.getCurrentPosition() - 5000); // milliseconds
                    show();
                }
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (player != null) {
            player.setSurface(holder.getSurface());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (player != null) {
            player.blockingClearSurface();
        }
    }
}
