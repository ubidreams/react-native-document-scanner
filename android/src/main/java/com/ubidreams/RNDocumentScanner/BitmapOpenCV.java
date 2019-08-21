package com.ubidreams.RNDocumentScanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
            MatOfPoint largestSquare = this.findLargestSquares(squares);

            if (largestSquare.total() == 4) {
                Point[] points = sortPoints(largestSquare.toArray());
                List<PointF> result = new ArrayList<>();

                result.add(new PointF(Double.valueOf(points[0].x).floatValue(), Double.valueOf(points[0].y).floatValue()));
                result.add(new PointF(Double.valueOf(points[1].x).floatValue(), Double.valueOf(points[1].y).floatValue()));
                result.add(new PointF(Double.valueOf(points[2].x).floatValue(), Double.valueOf(points[2].y).floatValue()));
                result.add(new PointF(Double.valueOf(points[3].x).floatValue(), Double.valueOf(points[3].y).floatValue()));

                image.release();
                largestSquare.release();

                return result;
            } else {
                return defaultResult;
            }
        } else {
            return defaultResult;
        }
    }

    private List<MatOfPoint> findSquares(Mat image) {
        List<MatOfPoint> squares = new ArrayList<>();

        // Convert image to grayscale
        Mat src_gray = new Mat();
        Imgproc.cvtColor(image, src_gray, Imgproc.COLOR_BGR2GRAY);

        // Blur helps to decrease the amount of detected edges
        Mat filtered = new Mat();
        Imgproc.GaussianBlur(src_gray, filtered, new Size(11, 11), 0);

        // Detect edges
        Mat edges = new Mat();
        Imgproc.Canny(filtered, edges, 10, 20, 3, false);

        // Dilate helps to connect nearby line segments
        Mat dilated_edges = new Mat();
        Imgproc.dilate(edges, dilated_edges, new Mat(), new Point(-1, -1), 2);
        if (this.debug) this.saveMatAsPicture(dilated_edges, "image-edges.png");

        // Find contours and store them in a list
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(dilated_edges, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point());

        // Test contours and assemble squares out of them
        MatOfPoint2f approxcurve = new MatOfPoint2f();

        for (int i = 0; i < contours.size(); i++){
            double epsilon = Imgproc.arcLength(new MatOfPoint2f(contours.get(i).toArray()), true) * 0.02;

            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), approxcurve, epsilon, true);

            double contourArea = Math.abs(Imgproc.contourArea(approxcurve.clone()));
            boolean isContourConvex = Imgproc.isContourConvex(new MatOfPoint(approxcurve.toArray()));

            // Note: absolute value of an area is used because area may be positive or negative - in accordance with the contour orientation
            if (approxcurve.toArray().length == 4 && contourArea > 1000 && isContourConvex) {
                double maxCosine = 0;

                for (int j = 2; j < 5; j++){
                    double cosine = Math.abs(this.angle(approxcurve.toArray()[j%4], approxcurve.toArray()[j-2], approxcurve.toArray()[j-1]));
                    maxCosine = Math.max(maxCosine, cosine);
                }

                if (maxCosine < 0.3) {
                    squares.add(new MatOfPoint(approxcurve.toArray()));
                }
            }
        }

        return squares;
    }

    private MatOfPoint findLargestSquares(List<MatOfPoint> squares){
        // no squares detected
        if (squares.size() == 0) {
            return new MatOfPoint();
        }

        // Find largest square
        int max_width = 0;
        int max_height = 0;
        int max_square_idx = 0;

        for (int i = 0; i < squares.size(); i++) {
            // Convert a set of 4 unordered Points into a meaningful cv::Rect structure.
            Rect rectangle = Imgproc.boundingRect(squares.get(i));

            // Store the index position of the biggest square found
            if ((rectangle.width >= max_width) && (rectangle.height >= max_height)) {
                max_width = rectangle.width;
                max_height = rectangle.height;
                max_square_idx = i;
            }
        }

        return squares.get(max_square_idx);
    }

    private double angle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;

        return (dx1*dx2 + dy1*dy2)/Math.sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
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
