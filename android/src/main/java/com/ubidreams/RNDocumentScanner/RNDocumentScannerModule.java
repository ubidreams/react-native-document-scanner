package com.ubidreams.RNDocumentScanner;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import org.opencv.core.Point;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class RNDocumentScannerModule extends ReactContextBaseJavaModule {

    private final String tag = "RNDocumentScanner";
    private final ReactApplicationContext reactContext;
    private BitmapOpenCV bitmap;

    static {
        System.loadLibrary("opencv_java3");
    }

    public RNDocumentScannerModule(final ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNDocumentScanner";
    }

    @ReactMethod
    public void detectEdges(String imagePath, ReadableMap layout, Promise promise) {
        // get layout
        int width = layout.getInt("width");
        int height = layout.getInt("height");

        // get bitmap from path
        this.bitmap = new BitmapOpenCV(this.reactContext, imagePath, width, height);

        // go opencv !
        List<PointF> pointsFound = this.bitmap.detectEdges();

        // build points array
        WritableArray points = new WritableNativeArray();

        for (int i = 0; i < 4; i++) {
            WritableMap point = new WritableNativeMap();
            point.putDouble("x", pointsFound.get(i).x);
            point.putDouble("y", pointsFound.get(i).y);

            points.pushMap(point);
        }

        // resolve promise
        promise.resolve(points);
    }

    @ReactMethod
    public void crop(ReadableArray points, ReadableMap options, Promise promise) {
        WritableMap result = Arguments.createMap();

        // get points
        ReadableMap topLeftPoint = points.getMap(0);
        ReadableMap topRightPoint = points.getMap(1);
        ReadableMap bottomRightPoint = points.getMap(2);
        ReadableMap bottomLeftPoint = points.getMap(3);

        // get options
        int width = options.getInt("width");
        int height = options.getInt("height");
        boolean thumbnail = options.getBoolean("thumbnail");

        // go opencv !
        Point point1 = new Point(topLeftPoint.getDouble("x"), topLeftPoint.getDouble("y"));
        Point point2 = new Point(topRightPoint.getDouble("x"), topRightPoint.getDouble("y"));
        Point point3 = new Point(bottomRightPoint.getDouble("x"), bottomRightPoint.getDouble("y"));
        Point point4 = new Point(bottomLeftPoint.getDouble("x"), bottomLeftPoint.getDouble("y"));
        Point[] pts = {point1, point2, point3, point4};

        Bitmap croppedBitmap = this.bitmap.fourPointTransform(pts);

        // resize cropped image ?
        if (width > 0 && height > 0) {
            croppedBitmap = this.resizeBitmap(croppedBitmap, width, height);
        }

        // save image to cache directory
        String croppedImageFilePath = this.saveBitmapToCacheDirectory(croppedBitmap);

        // add image file path to result
        result.putString("image", "file://" + croppedImageFilePath);

        // create thumbnail ?
        if (thumbnail) {
            // resize original image to create a thumbnail
            Bitmap thumbnailBitmap = this.resizeBitmap(croppedBitmap, 250, 250);

            // save thumbnail to cache directory
            String thumbnailFilePath = this.saveBitmapToCacheDirectory(thumbnailBitmap);

            // add thumbnail file path to result
            result.putString("thumbnail", "file://" + thumbnailFilePath);
        }

        // resolve promise with result
        promise.resolve(result);
    }

    private Bitmap resizeBitmap (Bitmap bitmap, int width, int height) {
        Bitmap resizedBitmap = null;

        float ratioX = (float) width / bitmap.getWidth();
        float ratioY = (float) height / bitmap.getHeight();
        float ratio = Math.min(ratioX, ratioY);

        int finalWidth = (int) (bitmap.getWidth() * ratio);
        int finalHeight = (int) (bitmap.getHeight() * ratio);

        try {
            resizedBitmap = Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
        } catch (OutOfMemoryError e) {
            Log.d(tag, "Error resizing bitmap");
            e.printStackTrace();
        }

        return resizedBitmap;
    }

    private String saveBitmapToCacheDirectory (Bitmap bitmap) {
        FileOutputStream fos = null;
        String fileName = UUID.randomUUID().toString() + ".png";
        String imageFilePath = this.reactContext.getCacheDir().getAbsolutePath() + "/" + fileName;

        try {
            fos = new FileOutputStream(imageFilePath);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            Log.d(tag, "Error writing file in cache");
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                Log.d(tag, "Error closing file output stream");
                e.printStackTrace();
            }
        }

        return imageFilePath;
    }

}
