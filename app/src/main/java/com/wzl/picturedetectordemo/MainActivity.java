package com.wzl.picturedetectordemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.jcodec.api.android.AndroidSequenceEncoder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;

    private ProgressBar progressBar;
    private TextView percentView;

    String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "输出图片" + File.separator;
    private DetectorUtil detector;


    //可信度最小值
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.show);

        detector = new DetectorUtil(MainActivity.this);

    }

    public void startImage(View view) {

        //方式一：大图片会内存溢出
        //Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.car);

        //方式二：适用于读取大图片
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
        InputStream is = getResources().openRawResource(+R.drawable.detector);
        Bitmap bitmap = BitmapFactory.decodeStream(is, null, opt);

        Bitmap resultBitmap = detector.detectImage(bitmap);
        imageView.setImageBitmap(resultBitmap);
    }

    public void startVideo(View view) throws InterruptedException {

        //  显示进度对话框
        View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_progress, null, false);
        progressBar = dialogView.findViewById(R.id.progressBar);
        percentView = dialogView.findViewById(R.id.percent);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("处理中");
        builder.setView(dialogView);
        AlertDialog dialog = builder.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                File folder = new File(dir);
                if (!folder.exists()) {
                    folder.mkdir();
                }
                AndroidSequenceEncoder encoder = null;
                File videoFile = new File(dir + "out.mp4");
                try {
                    //1秒15张
                    int fps = 15;
                    encoder = AndroidSequenceEncoder.createSequenceEncoder(videoFile, fps);

                    Uri videoUrI = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.test_5s);
                    System.out.println(videoUrI);
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(MainActivity.this, videoUrI);

                    //持续时间（ms）
                    String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    //视频长度（s）
                    int seconds = Integer.parseInt(duration) / 1000 * fps;
                    Log.d("MainActivity", "seconds:" + seconds);

                    for (int i = 0; i < seconds; i++) {

                        int finalI = i;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setProgress(finalI *100/seconds);
                                percentView.setText(finalI *100/seconds + " %");
                            }
                        });
                        //Log.d("MainActivity", "开始解码第" + i + "张");
                        Bitmap bitmap = retriever.getFrameAtTime(i * 1000 * 1000 / fps, MediaMetadataRetriever.OPTION_CLOSEST);
                        if (bitmap == null  ) continue;
                        //Log.d("MainActivity", "第" + i + "张解码结束");
                        // Log.d("MainActivity", "时间：" + i * 1000 * (1000 / fps));
                        // Log.d("MainActivity", "第" + i + "张识别开始");
                        Bitmap tempBitmap = detector.detectImage(bitmap);
                        // Log.d("MainActivity", "第" + i + "张识别结束");
                        encoder.encodeImage(tempBitmap);



                        //保存识别的图片
                        /*FileOutputStream fos;
                        try {
                            String name = null;
                            name = i < 10 ? dir + "0" + i + ".png" : dir + i + ".png";;
                            fos = new FileOutputStream(name, false);
                            tempBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                            fos.flush();
                            fos.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d("MainActivity", "处理结束");
                try {
                    if (encoder != null) {
                        encoder.finish();
                    }
                    dialog.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "处理完成,视频位于[/输出视频]目录下", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
                long endTime = System.currentTimeMillis();
                Log.d("MainActivity", "用时：" + (endTime - startTime) / 1000 + "秒");
            }
        }).start();

    }

    public void openVideo(View view) {
        startActivity(new Intent(this, VideoActivity.class));
    }
}


