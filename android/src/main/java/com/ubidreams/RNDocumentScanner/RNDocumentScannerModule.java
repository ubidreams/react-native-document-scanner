package com.ubidreams.RNDocumentScanner;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

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

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.core.content.FileProvider;

public class RNDocumentScannerModule extends ReactContextBaseJavaModule {

    private final String tag = "RNDocumentScanner";
    private final ReactApplicationContext reactContext;

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
        // get uri from path
        Uri uri = FileProvider.getUriForFile(
            this.reactContext,
            this.reactContext.getApplicationContext().getPackageName() + ".provider",
            new File(imagePath)
        );

        try {
            // get image from uri
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.reactContext.getContentResolver(), uri);

            // get layout
            int width = layout.getInt("width");
            int height = layout.getInt("height");

            // resize bitmap according to layout
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);

            // go opencv !
            List<PointF> pointsFound = findPoints(resizedBitmap);

            // resolve promise
            WritableArray points = new WritableNativeArray();

            if (pointsFound != null) {
                for (int i = 0; i < 4; i++) {
                    WritableMap point = new WritableNativeMap();
                    point.putDouble("x", pointsFound.get(i).x);
                    point.putDouble("y", pointsFound.get(i).y);

                    points.pushMap(point);
                }
            } else {
                double defaultWidth = width * 0.5;
                double defaultHeight = defaultWidth;
                double defaultX = (width - defaultWidth) / 2;
                double defaultY = (height - defaultHeight) / 2;

                for (int i = 0; i < 4; i++) {
                    WritableMap point = new WritableNativeMap();

                    switch (i) {
                        // top left point
                        case 0:
                            point.putDouble("x", defaultX);
                            point.putDouble("y", defaultY);
                            break;

                        // top right point
                        case 1:
                            point.putDouble("x", defaultX + defaultWidth);
                            point.putDouble("y", defaultY);
                            break;

                        // bottom right point
                        case 2:
                            point.putDouble("x", defaultX + defaultWidth);
                            point.putDouble("y", defaultY + defaultHeight);
                            break;

                        // bottom left point
                        case 3:
                            point.putDouble("x", defaultX);
                            point.putDouble("y", defaultY + defaultHeight);
                            break;
                    }

                    points.pushMap(point);
                }
            }

            promise.resolve(points);
        } catch (IOException e) {
            promise.reject("DETECT_EDGES_ERROR", e);
            e.printStackTrace();
            Log.d(tag, "Can't find image from path!");
        }
    }

    @ReactMethod
    public void crop(ReadableArray points, Promise promise) {
    }

    /**
     * Attempt to find the four corner points for the largest contour in the image.
     * @param bitmap
     * @return A list of points, or null if a valid rectangle cannot be found.
     */
    private List<PointF> findPoints(Bitmap bitmap) {
        List<PointF> result = null;

        Mat image = new Mat();
        Mat orig = new Mat();
        org.opencv.android.Utils.bitmapToMat(bitmap, image);
        org.opencv.android.Utils.bitmapToMat(bitmap, orig);

        Mat edges = edgeDetection(image);
        MatOfPoint2f largest = findLargestContour(edges);

        if (largest != null) {
            Point[] points = sortPoints(largest.toArray());
            result = new ArrayList<>();
            result.add(new PointF(Double.valueOf(points[0].x).floatValue(), Double.valueOf(points[0].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[1].x).floatValue(), Double.valueOf(points[1].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[2].x).floatValue(), Double.valueOf(points[2].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[3].x).floatValue(), Double.valueOf(points[3].y).floatValue()));
            largest.release();
        } else {
            Log.d(tag, "Can't find rectangle!");
        }

        edges.release();
        image.release();
        orig.release();

        return result;
    }

    /**
     * Detect the edges in the given Mat
     * @param src A valid Mat object
     * @return A Mat processed to find edges
     */
    private Mat edgeDetection(Mat src) {
        Mat edges = new Mat();
        Imgproc.cvtColor(src, edges, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(edges, edges, new Size(5, 5), 0);
        Imgproc.Canny(edges, edges, 75, 200);
        return edges;
    }

    /**
     * Find the largest 4 point contour in the given Mat
     * @param src A valid Mat
     * @return The largest contour as a Mat
     */
    private MatOfPoint2f findLargestContour(Mat src) {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(src, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // Get the 5 largest contours
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                double area1 = Imgproc.contourArea(o1);
                double area2 = Imgproc.contourArea(o2);
                return (int) (area2 - area1);
            }
        });
        if (contours.size() > 5) contours.subList(4, contours.size() - 1).clear();

        MatOfPoint2f largest = null;
        for (MatOfPoint contour : contours) {
            MatOfPoint2f approx = new MatOfPoint2f();
            MatOfPoint2f c = new MatOfPoint2f();
            contour.convertTo(c, CvType.CV_32FC2);
            Imgproc.approxPolyDP(c, approx, Imgproc.arcLength(c, true) * 0.02, true);

            if (approx.total() == 4 && Imgproc.contourArea(contour) > 150) {
                // the contour has 4 points, it's valid
                largest = approx;
                break;
            }
        }

        return largest;
    }

    /**
     * Sort the points
     *
     * The order of the points after sorting:
     * 0------->1
     * ^        |
     * |        v
     * 3<-------2
     *
     * NOTE:
     * Based off of http://www.pyimagesearch.com/2014/08/25/4-point-opencv-getperspective-transform-example/
     *
     * @param src The points to sort
     * @return An array of sorted points
     */
    private Point[] sortPoints(Point[] src) {
        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));
        Point[] result = {null, null, null, null};

        Comparator<Point> sumComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.compare(lhs.y + lhs.x, rhs.y + rhs.x);
            }
        };
        Comparator<Point> differenceComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.compare(lhs.y - lhs.x, rhs.y - rhs.x);
            }
        };

        result[0] = Collections.min(srcPoints, sumComparator);        // Upper left has the minimal sum
        result[2] = Collections.max(srcPoints, sumComparator);        // Lower right has the maximal sum
        result[1] = Collections.min(srcPoints, differenceComparator); // Upper right has the minimal difference
        result[3] = Collections.max(srcPoints, differenceComparator); // Lower left has the maximal difference

        return result;
    }

}
