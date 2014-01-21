package ru.omniverse.android.stargreeter;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

import static ru.omniverse.android.stargreeter.Utils.TAG;

public class AudioService extends Service implements MediaPlayer.OnErrorListener {
    private MediaPlayer mediaPlayer;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "AudioService onStartCommand");

        String audioName = intent.getStringExtra("name");
        Log.d(TAG, "Loading audio from " + audioName);
        if (audioName != null && audioName.length() > 0) {
            try {
                AssetFileDescriptor afd = getAssets().openFd(audioName);
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnErrorListener(this);
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                mediaPlayer.prepare();
                mediaPlayer.setLooping(true);

                mediaPlayer.start();

            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Cannot load audio", e);
            }
        }


        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onStart(Intent intent, int startid) {
//        Log.d(TAG, "AudioService started");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "AudioService destroyed");
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer error what = " + what + " extra = " + extra);
        return false;
    }
}