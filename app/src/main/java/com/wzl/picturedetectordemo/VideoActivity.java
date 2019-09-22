package com.wzl.picturedetectordemo;

import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.MediaController;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class VideoActivity extends AppCompatActivity implements
        MediaController.MediaPlayerControl,
        MediaPlayer.OnBufferingUpdateListener,
        SurfaceHolder.Callback {

    private MediaPlayer mediaPlayer;
    private MediaController controller;
    private int bufferPercentage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        mediaPlayer = new MediaPlayer();
        controller = new MediaController(this);
        controller.setAnchorView(findViewById(R.id.root));

        initSurfaceView();
    }

    private void initSurfaceView() {
        SurfaceView videoSuf = findViewById(R.id.video);
        videoSuf.setZOrderOnTop(false);
        videoSuf.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        videoSuf.getHolder().addCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            File file = new File("/storage/emulated/0/输出图片/out.mp4");
            if (!file.exists()) {
                Toast.makeText(this, "视频不存在，请先识别视频！", Toast.LENGTH_LONG).show();
                return;
            }
            Uri videoUrI = Uri.parse("/storage/emulated/0/输出图片/out.mp4");
            mediaPlayer.setDataSource(this, videoUrI);
            mediaPlayer.setOnBufferingUpdateListener(this);
            //mediaPlayer.prepare();

            controller.setMediaPlayer(this);
            controller.setEnabled(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mediaPlayer) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        controller.show();
        return super.onTouchEvent(event);
    }


    //MideaPlayerControl
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

        bufferPercentage = percent;
    }

    //SurfaceHolder.callback
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        System.out.println("回调函数");
        mediaPlayer.setDisplay(holder);
        mediaPlayer.prepareAsync();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void start() {
        if (null != mediaPlayer){
            mediaPlayer.start();
        }
    }

    @Override
    public void pause() {
        if (null != mediaPlayer){
            mediaPlayer.pause();
        }
    }

    @Override
    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        mediaPlayer.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return bufferPercentage;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}
