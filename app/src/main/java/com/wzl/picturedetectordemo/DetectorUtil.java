package com.wzl.picturedetectordemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;

import com.wzl.picturedetectordemo.env.BorderedText;
import com.wzl.picturedetectordemo.env.ImageUtils;
import com.wzl.picturedetectordemo.tflite.Classifier;
import com.wzl.picturedetectordemo.tflite.TFLiteObjectDetectionAPIModel;
import com.wzl.picturedetectordemo.tracking.MultiBoxTracker;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class DetectorUtil {

    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";

    //可信度最小值
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;

    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private Bitmap resultBitmap = null;

    private Classifier detector;
    private MultiBoxTracker tracker;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private Context context;
    private Bitmap bitmap = null;

    private int srcWidth = 0;
    private int srcHeight = 0;

    private static final float TEXT_SIZE_DIP = 18;

    public DetectorUtil(Context context) {
        this.context = context;
    }

    public Bitmap detectImage(Bitmap bitmap) {

        this.bitmap = bitmap;
        tracker = new MultiBoxTracker(context);
        int cropSize = TF_OD_API_INPUT_SIZE;
        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            context.getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
        }

        //获取图片的宽高
        srcWidth = bitmap.getWidth();
        srcHeight = bitmap.getHeight();

        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        srcWidth, srcHeight,
                        cropSize, cropSize,
                        0, false);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        tracker.setFrameConfiguration(srcWidth, srcHeight, 0);

        processImage();

        return resultBitmap;
    }

    public void processImage() {

        //绘制图片
        final Canvas croppedCanvas = new Canvas(croppedBitmap);
        croppedCanvas.drawBitmap(bitmap , frameToCropTransform, null);

        //获得预测结果
        List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);

        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);

        //最低可信度
        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;



        final List<Classifier.Recognition> mappedRecognitions = new LinkedList<Classifier.Recognition>();

        //预设匹配数据
        String[] labels = {"人", "自行车", "汽车", "摩托车"};
        System.out.println("开始预测");
        for (final Classifier.Recognition result : results) {

            //排除指定识别目标之外的对象
            if (!Arrays.asList(labels).contains(result.getTitle())) {
                continue;
            }

            //System.out.println("识别结果：" + result);
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= minimumConfidence) {
                // System.out.println("最终结果：" + result);
                result.setLocation(location);
                //  自带绘图部分（有bug）
                cropToFrameTransform.mapRect(location);
                mappedRecognitions.add(result);

                //  使用自定义绘图
                //draw(cropCopyBitmap, result);
                //newDraw(cropCopyBitmap, result);
            }
        }

        //  自带绘图部分
        tracker.trackResults(mappedRecognitions, 0);
        resultBitmap = Bitmap.createScaledBitmap(cropCopyBitmap, srcWidth, srcHeight, true);
        Canvas canvas = new Canvas(resultBitmap);
        tracker.draw(canvas);
    }

    // 自定义绘图
    public void newDraw(Bitmap bitmap, Classifier.Recognition result) {
        //int cornerSize = 10;
        //绘制矩形框
        Canvas canvas = new Canvas(bitmap);
        final Paint boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Style.STROKE);
        boxPaint.setStrokeWidth(10.0f);
        boxPaint.setStrokeCap(Cap.ROUND);
        boxPaint.setStrokeJoin(Join.ROUND);
        boxPaint.setStrokeMiter(100);
        //canvas.drawRect(result.getLocation(), boxPaint);

        final RectF trackedPos = new RectF(result.getLocation());
        float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 8.0f;
        System.out.println(cornerSize);
        canvas.drawRoundRect(result.getLocation(), cornerSize, cornerSize, boxPaint);

        //  绘制文字
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setStrokeWidth(1.0f);

        //canvas.drawText(result.getTitle(), result.getLocation().left, result.getLocation().top, textPaint);

        float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());

        final String labelString =
                !TextUtils.isEmpty(result.getTitle())
                        ? String.format("%s %.2f", result.getTitle(), (100 * result.getConfidence()))
                        : String.format("%.2f", (100 * result.getConfidence()));
        //            borderedText.drawText(canvas, trackedPos.left + cornerSize, trackedPos.top,
        // labelString);
        System.out.println("textSizePx:" + textSizePx);
        BorderedText borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);
        borderedText.drawText(
                canvas, result.getLocation().left + cornerSize, result.getLocation().top, labelString + "%", boxPaint);
    }

    public void draw(Bitmap bitmap, Classifier.Recognition result) {

        //绘制矩形框
        Canvas canvas = new Canvas(bitmap);
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Cap.ROUND);
        paint.setStrokeJoin(Join.ROUND);
        paint.setStrokeWidth(2.0f);
        canvas.drawRoundRect(result.getLocation(), 15, 15, paint);

        //  绘制文字
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setStrokeWidth(1.0f);

        canvas.drawText(result.getTitle(), result.getLocation().left, result.getLocation().top, textPaint);
    }
}
