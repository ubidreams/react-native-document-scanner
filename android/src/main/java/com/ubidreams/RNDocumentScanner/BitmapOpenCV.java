package com.ubidreams.RNDocumentScanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import androidx.core.content.FileProvider;

import static org.opencv.android.Utils.bitmapToMat;
import static org.opencv.android.Utils.matToBitmap;

public class BitmapOpenCV {

    private boolean debug = false; // if you want to debug don't forget to enable "Storage" in app permissions
    private Bitmap originalBitmap = null;
    private Size frameSize;
    private Size imageSize;
    private double frameScale;

    public static int KSIZE_BLUR = 3;
    public static int KSIZE_CLOSE = 10;
    public static final int CANNY_THRESH_L = 85;
    public static final int CANNY_THRESH_U = 185;
    public static final int TRUNC_THRESH = 150;
    public static final int CUTOFF_THRESH = 155;

    private static Mat morph_kernel = new Mat(new Size(KSIZE_CLOSE, KSIZE_CLOSE), CvType.CV_8UC1, new Scalar(255));

    public BitmapOpenCV(Context context, String imagePath, int width, int height) {
        // convert string path to Uri
        Uri uri = FileProvider.getUriForFile(
            context,
            context.getApplicationContext().getPackageName() + ".provider",
            new File(imagePath)
        );

        // get image from uri
        Bitmap bitmap = null;

        try {
            bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // transform original image with desired frame ratio
        if (bitmap != null) {
            double desiredRatio = (double) width / height;
            int desiredWidth = (int) Math.round(bitmap.getHeight() * desiredRatio);
            int desiredHeight = bitmap.getHeight();

            this.originalBitmap = ThumbnailUtils.extractThumbnail(bitmap, desiredWidth, desiredHeight);
            if (this.debug) this.saveBitmapAsPicture(this.originalBitmap, "image-original.png");

            this.frameSize = new Size(width, height);
            this.imageSize = new Size(this.originalBitmap.getWidth(), this.originalBitmap.getHeight());
            this.frameScale = Math.min(this.frameSize.width / this.imageSize.width, this.frameSize.height / this.imageSize.height);
        }
    }

    public List<PointF> detectEdges() {
        float defaultWidth = (float) (this.frameSize.width * 0.5);
        float defaultHeight = defaultWidth;
        float defaultX = (float) (this.frameSize.width - defaultWidth) / 2;
        float defaultY = (float) (this.frameSize.height - defaultHeight) / 2;

        List<PointF> defaultResult = new ArrayList<>();
        defaultResult.add(new PointF(defaultX, defaultY));
        defaultResult.add(new PointF(defaultX + defaultWidth, defaultY));
        defaultResult.add(new PointF(defaultX + defaultWidth, defaultY + defaultHeight));
        defaultResult.add(new PointF(defaultX, defaultY + defaultHeight));

        if (this.originalBitmap != null) {
            Mat image = new Mat();
            bitmapToMat(this.originalBitmap, image);

            double contentWidth = this.imageSize.width * this.frameScale;
            double contentHeight = this.imageSize.height * this.frameScale;
            Imgproc.resize(image, image, new Size(contentWidth, contentHeight));
            if (this.debug) this.saveMatAsPicture(image, "image-resized.png");

            List<MatOfPoint> squares = this.findSquares(image);
            Point[] points = this.findLargestSquares(squares);

            if (points != null) {
                List<PointF> result = new ArrayList<>();

                result.add(new PointF(Double.valueOf(points[0].x).floatValue(), Double.valueOf(points[0].y).floatValue()));
                result.add(new PointF(Double.valueOf(points[1].x).floatValue(), Double.valueOf(points[1].y).floatValue()));
                result.add(new PointF(Double.valueOf(points[2].x).floatValue(), Double.valueOf(points[2].y).floatValue()));
                result.add(new PointF(Double.valueOf(points[3].x).floatValue(), Double.valueOf(points[3].y).floatValue()));

                image.release();

                return result;
            } else {
                return defaultResult;
            }
        } else {
            return defaultResult;
        }
    }

    private List<MatOfPoint> findSquares(Mat originalMat) {
        Imgproc.cvtColor(originalMat, originalMat, Imgproc.COLOR_BGR2GRAY, 4);

        /*
         *  1. We shall first blur and normalize the image for uniformity,
         *  2. Truncate light-gray to white and normalize,
         *  3. Apply canny edge detection,
         *  4. Cutoff weak edges,
         *  5. Apply closing(morphology), then proceed to finding contours.
         */
        // step 1.
        Imgproc.blur(originalMat, originalMat, new Size(KSIZE_BLUR, KSIZE_BLUR));
        Core.normalize(originalMat, originalMat, 0, 255, Core.NORM_MINMAX);
        if (this.debug) this.saveMatAsPicture(originalMat, "image-step1.png");

        // step 2.
        // As most papers are bright in color, we can use truncation to make it uniformly bright.
        Imgproc.threshold(originalMat,originalMat, TRUNC_THRESH,255,Imgproc.THRESH_TRUNC);
        Core.normalize(originalMat, originalMat, 0, 255, Core.NORM_MINMAX);
        if (this.debug) this.saveMatAsPicture(originalMat, "image-step2.png");

        // step 3.
        // After above preprocessing, canny edge detection can now work much better.
        Imgproc.Canny(originalMat, originalMat, CANNY_THRESH_U, CANNY_THRESH_L);
        if (this.debug) this.saveMatAsPicture(originalMat, "image-step3.png");

        // step 4.
        // Cutoff the remaining weak edges
        Imgproc.threshold(originalMat,originalMat,CUTOFF_THRESH,255,Imgproc.THRESH_TOZERO);
        if (this.debug) this.saveMatAsPicture(originalMat, "image-step4.png");

        // step 5.
        // Closing - closes small gaps. Completes the edges on canny image; AND also reduces stringy lines near edge of paper.
        Imgproc.morphologyEx(originalMat, originalMat, Imgproc.MORPH_CLOSE, morph_kernel, new Point(-1,-1),1);
        if (this.debug) this.saveMatAsPicture(originalMat, "image-step5.png");

        // Get only the 10 largest contours (each approximated to their convex hulls)
        return findLargestContours(originalMat);
    }

    private static MatOfPoint hull2Points(MatOfInt hull, MatOfPoint contour) {
        List<Integer> indexes = hull.toList();
        List<Point> points = new ArrayList<>();
        List<Point> ctrList = contour.toList();
        for(Integer index:indexes) {
          points.add(ctrList.get(index));
        }
        MatOfPoint point= new MatOfPoint();
        point.fromList(points);
        return point;
    }

    private static List<MatOfPoint> findLargestContours(Mat inputMat) {
        Mat mHierarchy = new Mat();
        List<MatOfPoint> mContourList = new ArrayList<>();
        //finding contours - as we are sorting by area anyway, we can use RETR_LIST - faster than RETR_EXTERNAL.
        Imgproc.findContours(inputMat, mContourList, mHierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // Convert the contours to their Convex Hulls i.e. removes minor nuances in the contour
        List<MatOfPoint> mHullList = new ArrayList<>();
        MatOfInt tempHullIndices = new MatOfInt();
        for (int i = 0; i < mContourList.size(); i++) {
          Imgproc.convexHull(mContourList.get(i), tempHullIndices);
          mHullList.add(hull2Points(tempHullIndices, mContourList.get(i)));
        }
        // Release mContourList as its job is done
        for (MatOfPoint c : mContourList)
          c.release();
        tempHullIndices.release();
        mHierarchy.release();

        if (mHullList.size() != 0) {
          Collections.sort(mHullList, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint lhs, MatOfPoint rhs) {
              return Double.compare(Imgproc.contourArea(rhs),Imgproc.contourArea(lhs));
            }
          });
          return mHullList.subList(0, Math.min(mHullList.size(), 10));
        }
        return null;
    }

    private Point[] findLargestSquares(List<MatOfPoint> squares){
        for (MatOfPoint c : squares) {
            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            double peri = Imgproc.arcLength(c2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true);
            Point[] points = approx.toArray();

            // select biggest 4 angles polygon
            if (approx.rows() == 4) {
                Point[] foundPoints = sortPoints(points);

                if (isPossibleRectangle(foundPoints)) {
                    return foundPoints;
                }
            }
        }

        return null;
    }

    private boolean isPossibleRectangle(Point[] approxPoints) {
        // angles must be ~90° (+/-5°)
        double maxcos = getMaxCosine(0, approxPoints);

        if (!(Math.abs(maxcos) <= 0.087)) {
            return false;
        }

        // check unique points
        HashSet<Point> uniquePoints = new HashSet<>();

        for (Point p: approxPoints) {
            if (!uniquePoints.add(p)) {
                return false;
            }
        }

        return true;
    }

    private double getMaxCosine(double maxCosine, Point[] approxPoints) {
        for (int i = 2; i < 5; i++) {
            double cosine = Math.abs(angle(approxPoints[i % 4], approxPoints[i - 2], approxPoints[i - 1]));
            maxCosine = Math.max(cosine, maxCosine);
        }
        return maxCosine;
    }

    private double angle(Point p1, Point p2, Point p0) {
        double dx1 = p1.x - p0.x;
        double dy1 = p1.y - p0.y;
        double dx2 = p2.x - p0.x;
        double dy2 = p2.y - p0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

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

    private void saveMatAsPicture(Mat mat, String fileName) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        matToBitmap(mat, bitmap);

        this.saveBitmapAsPicture(bitmap, fileName);
    }

    private void saveBitmapAsPicture(Bitmap bitmap, String fileName) {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/" + fileName);

            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Bitmap fourPointTransform(Point[] pts) {
        Point tl = new Point(pts[0].x / this.frameScale, pts[0].y / this.frameScale);
        Point tr = new Point(pts[1].x / this.frameScale, pts[1].y / this.frameScale);
        Point br = new Point(pts[2].x / this.frameScale, pts[2].y / this.frameScale);
        Point bl = new Point(pts[3].x / this.frameScale, pts[3].y / this.frameScale);

        double w1 = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.x - bl.x, 2));
        double w2 = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.x - tl.x, 2));

        double h1 = Math.sqrt(Math.pow(tr.y - br.y, 2) + Math.pow(tr.y - br.y, 2));
        double h2 = Math.sqrt(Math.pow(tl.y - bl.y, 2) + Math.pow(tl.y - bl.y, 2));

        double maxWidth = (w1 < w2) ? w1 : w2;
        double maxHeight = (h1 < h2) ? h1 : h2;

        Mat src = new Mat(4, 1, CvType.CV_32FC2);
        Mat dst = new Mat(4, 1, CvType.CV_32FC2);
        src.put(0, 0, tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y);
        dst.put(0, 0, 0, 0, maxWidth - 1, 0, maxWidth - 1, maxHeight - 1, 0, maxHeight - 1);

        Mat undistorted = new Mat(new Size(maxWidth, maxHeight), CvType.CV_8UC4);
        Mat original = new Mat();
        bitmapToMat(this.originalBitmap, original);

        Imgproc.warpPerspective(original, undistorted, Imgproc.getPerspectiveTransform(src, dst), new Size(maxWidth, maxHeight));

        Bitmap bitmap = Bitmap.createBitmap(undistorted.cols(), undistorted.rows(), Bitmap.Config.ARGB_8888);
        matToBitmap(undistorted, bitmap);

        src.release();
        dst.release();
        undistorted.release();
        original.release();

        return bitmap;
    }

}
