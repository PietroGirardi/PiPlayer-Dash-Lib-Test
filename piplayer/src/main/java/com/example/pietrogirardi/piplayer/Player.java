package com.example.pietrogirardi.piplayer;

import android.media.MediaCodec;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import com.google.android.exoplayer.CodecCounters;
import com.google.android.exoplayer.DummyTrackRenderer;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.TimeRange;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.chunk.ChunkSampleSource;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.dash.DashChunkSource;
import com.google.android.exoplayer.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.TextRenderer;
import com.google.android.exoplayer.upstream.BandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.util.PlayerControl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by pietrogirardi on 14/05/16.
 */
public class Player implements ExoPlayer.Listener, DefaultBandwidthMeter.EventListener,
        MediaCodecVideoTrackRenderer.EventListener, MediaCodecAudioTrackRenderer.EventListener,
        StreamingDrmSessionManager.EventListener, DashChunkSource.EventListener, ChunkSampleSource.EventListener,
        TextRenderer {


    private final Handler mainHandler;
    private final ExoPlayer player;

    private InfoListener infoListener;
    private final CopyOnWriteArrayList<Listener> listeners;

    public static final int TRACK_DISABLED = ExoPlayer.TRACK_DISABLED;

    private static final int RENDERER_BUILDING_STATE_IDLE = 1;
    private static final int RENDERER_BUILDING_STATE_BUILDING = 2;
    private static final int RENDERER_BUILDING_STATE_BUILT = 3;

    public static final int RENDERER_COUNT = 4;
    public static final int TYPE_VIDEO = 0;
    public static final int TYPE_AUDIO = 1;
    public static final int TYPE_TEXT = 2;

    private Format videoFormat;
    private TrackRenderer videoRenderer;
    private CodecCounters codecCounters;
    private BandwidthMeter bandwidthMeter;

    private int rendererBuildingState;
    private InternalErrorListener internalErrorListener;

    private Surface surface;
    private final PlayerControl playerControl;

    private final RendererBuilder rendererBuilder;

    private CaptionListener captionListener;


    public Player(RendererBuilder rendererBuilder) {
        this.rendererBuilder = rendererBuilder;
        player = ExoPlayer.Factory.newInstance(RENDERER_COUNT, 1000, 5000);
        player.addListener(this);
        playerControl = new PlayerControl(player);
        mainHandler = new Handler();
        listeners = new CopyOnWriteArrayList<>();
        // lastReportedPlaybackState = STATE_IDLE;
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        // Disable text initially.
        player.setSelectedTrack(TYPE_TEXT, TRACK_DISABLED);
    }


    public void prepare() {
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT) {
            player.stop();
        }
        rendererBuilder.cancel();
        videoFormat = null;
        videoRenderer = null;
        rendererBuildingState = RENDERER_BUILDING_STATE_BUILDING;
        //maybeReportPlayerState();
        rendererBuilder.buildRenderers(this);
    }


    public PlayerControl getPlayerControl() {
        return playerControl;
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        player.setPlayWhenReady(playWhenReady);
    }

    public void setSurface(Surface surface) {
        this.surface = surface;
        pushSurface(false);
    }

    public void blockingClearSurface() {
        surface = null;
        pushSurface(true);
    }

    public void release() {
        rendererBuilder.cancel();
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        surface = null;
        player.release();
    }


    Handler getMainHandler() {
        return mainHandler;
    }


    Looper getPlaybackLooper() {
        return player.getPlaybackLooper();
    }

    public long getDuration() {
        return player.getDuration();
    }

    public int getTrackCount(int type) {
        return player.getTrackCount(type);
    }

    public MediaFormat getTrackFormat(int type, int index) {
        return player.getTrackFormat(type, index);
    }

    public int getSelectedTrack(int type) {
        return player.getSelectedTrack(type);
    }

    public void setSelectedTrack(int type, int index) {
        player.setSelectedTrack(type, index);
        if (type == TYPE_TEXT && index < 0 && captionListener != null) {
            captionListener.onCues(Collections.<Cue>emptyList());
        }
    }


    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void setInternalErrorListener(InternalErrorListener listener) {
        internalErrorListener = listener;
    }


    /**
     * Invoked if a {@link RendererBuilder} encounters an error.
     *
     * @param e Describes the error.
     */
    void onRenderersError(Exception e) {
        if (internalErrorListener != null) {
            internalErrorListener.onRendererInitializationError(e);
        }
        for (Listener listener : listeners) {
            listener.onError(e);
        }
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        //maybeReportPlayerState();
    }


    /**
     * Invoked with the results from a {@link RendererBuilder}.
     *
     * @param renderers      Renderers indexed by {@link Player} TYPE_* constants. An individual
     *                       element may be null if there do not exist tracks of the corresponding type.
     * @param bandwidthMeter Provides an estimate of the currently available bandwidth. May be null.
     */
  /* package */ void onRenderers(TrackRenderer[] renderers, BandwidthMeter bandwidthMeter) {
        for (int i = 0; i < RENDERER_COUNT; i++) {
            if (renderers[i] == null) {
                // Convert a null renderer to a dummy renderer.
                renderers[i] = new DummyTrackRenderer();
            }
        }
        // Complete preparation.
        this.videoRenderer = renderers[TYPE_VIDEO];
        this.codecCounters = videoRenderer instanceof MediaCodecTrackRenderer
                ? ((MediaCodecTrackRenderer) videoRenderer).codecCounters
                : renderers[TYPE_AUDIO] instanceof MediaCodecTrackRenderer
                ? ((MediaCodecTrackRenderer) renderers[TYPE_AUDIO]).codecCounters : null;
        this.bandwidthMeter = bandwidthMeter;
        pushSurface(false);
        player.prepare(renderers);
        rendererBuildingState = RENDERER_BUILDING_STATE_BUILT;
    }


    //ExoPlayer.Listener

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        // maybeReportPlayerState();
    }


    @Override
    public void onPlayWhenReadyCommitted() {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        for (Listener listener : listeners) {
            listener.onError(error);
        }
    }


    //DefaultBandwidthMeter.EventListener
    @Override
    public void onBandwidthSample(int elapsedMs, long bytes, long bitrateEstimate) {
        if (infoListener != null) {
            infoListener.onBandwidthSample(elapsedMs, bytes, bitrateEstimate);
        }
    }


    //StreamingDrmSessionManager.EventListener
    @Override
    public void onDrmKeysLoaded() {
        // Do nothing.
    }

    @Override
    public void onDrmSessionManagerError(Exception e) {
        if (internalErrorListener != null) {
            internalErrorListener.onDrmSessionManagerError(e);
        }
    }


    //MediaCodecVideoTrackRenderer.EventListener
    @Override
    public void onDroppedFrames(int count, long elapsed) {
        if (infoListener != null) {
            infoListener.onDroppedFrames(count, elapsed);
        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                   float pixelWidthHeightRatio) {
        for (Listener listener : listeners) {
            listener.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
        }
    }

    @Override
    public void onDrawnToSurface(Surface surface) {
        // Do nothing.
    }


    @Override
    public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
                                     long initializationDurationMs) {
        if (infoListener != null) {
            infoListener.onDecoderInitialized(decoderName, elapsedRealtimeMs, initializationDurationMs);
        }
    }


    @Override
    public void onCryptoError(MediaCodec.CryptoException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onCryptoError(e);
        }
    }

    @Override
    public void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onDecoderInitializationError(e);
        }
    }


    //MediaCodecAudioTrackRenderer.EventListener
    @Override
    public void onAudioTrackWriteError(AudioTrack.WriteException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onAudioTrackWriteError(e);
        }
    }

    @Override
    public void onAudioTrackInitializationError(AudioTrack.InitializationException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onAudioTrackInitializationError(e);
        }
    }

    @Override
    public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
        if (internalErrorListener != null) {
            internalErrorListener.onAudioTrackUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
        }
    }


    //DashChunkSource.EventListener
    @Override
    public void onAvailableRangeChanged(int sourceId, TimeRange availableRange) {
        if (infoListener != null) {
            infoListener.onAvailableRangeChanged(sourceId, availableRange);
        }
    }


    //ChunkSampleSource.EventListener
    @Override
    public void onLoadStarted(int sourceId, long length, int type, int trigger, Format format,
                              long mediaStartTimeMs, long mediaEndTimeMs) {
        if (infoListener != null) {
            infoListener.onLoadStarted(sourceId, length, type, trigger, format, mediaStartTimeMs,
                    mediaEndTimeMs);
        }
    }

    @Override
    public void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format,
                                long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs) {
        if (infoListener != null) {
            infoListener.onLoadCompleted(sourceId, bytesLoaded, type, trigger, format, mediaStartTimeMs,
                    mediaEndTimeMs, elapsedRealtimeMs, loadDurationMs);
        }
    }


    @Override
    public void onLoadCanceled(int sourceId, long bytesLoaded) {
        // Do nothing.
    }

    @Override
    public void onDownstreamFormatChanged(int sourceId, Format format, int trigger,
                                          long mediaTimeMs) {
        if (infoListener == null) {
            return;
        }
        if (sourceId == TYPE_VIDEO) {
            videoFormat = format;
            infoListener.onVideoFormatEnabled(format, trigger, mediaTimeMs);
        } else if (sourceId == TYPE_AUDIO) {
            infoListener.onAudioFormatEnabled(format, trigger, mediaTimeMs);
        }
    }

    @Override
    public void onLoadError(int sourceId, IOException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onLoadError(sourceId, e);
        }
    }

    @Override
    public void onUpstreamDiscarded(int sourceId, long mediaStartTimeMs, long mediaEndTimeMs) {
        // Do nothing.
    }


    //TextRenderer
    @Override
    public void onCues(List<Cue> cues) {
        if (captionListener != null && getSelectedTrack(TYPE_TEXT) != TRACK_DISABLED) {
            captionListener.onCues(cues);
        }
    }


    /**
     * Builds renderers for the player.
     */
    public interface RendererBuilder {
        /**
         * Builds renderers for playback.
         *
         * @param player The player for which renderers are being built. {@link Player#onRenderers}
         *               should be invoked once the renderers have been built. If building fails,
         *               {@link Player#onRenderersError} should be invoked.
         */
        void buildRenderers(Player player);

        /**
         * Cancels the current build operation, if there is one. Else does nothing.
         * <p/>
         * A canceled build operation must not invoke {@link Player#onRenderers} or
         * {@link Player#onRenderersError} on the player, which may have been released.
         */
        void cancel();
    }


    /**
     * A listener for internal errors.
     * <p/>
     * These errors are not visible to the user, and hence this listener is provided for
     * informational purposes only. Note however that an internal error may cause a fatal
     * error if the player fails to recover. If this happens, {@link Listener#onError(Exception)}
     * will be invoked.
     */
    public interface InternalErrorListener {
        void onRendererInitializationError(Exception e);

        void onAudioTrackInitializationError(AudioTrack.InitializationException e);

        void onAudioTrackWriteError(AudioTrack.WriteException e);

        void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs);

        void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e);

        void onCryptoError(MediaCodec.CryptoException e);

        void onLoadError(int sourceId, IOException e);

        void onDrmSessionManagerError(Exception e);
    }


    /**
     * A listener for core events.
     */
    public interface Listener {
        void onStateChanged(boolean playWhenReady, int playbackState);

        void onError(Exception e);

        void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                float pixelWidthHeightRatio);
    }

    /**
     * A listener for receiving notifications of timed text.
     */
    public interface CaptionListener {
        void onCues(List<Cue> cues);
    }


    /**
     * A listener for debugging information.
     */
    public interface InfoListener {
        void onVideoFormatEnabled(Format format, int trigger, long mediaTimeMs);

        void onAudioFormatEnabled(Format format, int trigger, long mediaTimeMs);

        void onDroppedFrames(int count, long elapsed);

        void onBandwidthSample(int elapsedMs, long bytes, long bitrateEstimate);

        void onLoadStarted(int sourceId, long length, int type, int trigger, Format format,
                           long mediaStartTimeMs, long mediaEndTimeMs);

        void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format,
                             long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs);

        void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
                                  long initializationDurationMs);

        void onAvailableRangeChanged(int sourceId, TimeRange availableRange);
    }


    private void pushSurface(boolean blockForSurfacePush) {
        if (videoRenderer == null) {
            return;
        }

        if (blockForSurfacePush) {
            player.blockingSendMessage(
                    videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface);
        } else {
            player.sendMessage(
                    videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface);
        }
    }
}