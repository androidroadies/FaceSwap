package com.alex.faceswap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.camera2.params.Face;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import static org.opencv.android.Utils.bitmapToMat;
import static org.opencv.android.Utils.matToBitmap;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Created by alexander on 2016-06-29.
 */
public class FaceSwap {
    private Resources resources;
    private Context context;
    protected ArrayList<ArrayList<PointF>> landmarks1, landmarks2, landmarks;
    private Bitmap bitmap1, bitmap2, bitmap;

    /* Native methods --------------------------------------------------------------------------- */
    public native void portraitSwapNative(   long addrImg1,
                                            long addrImg2,
                                            int[] landmarksX1,
                                            int[] landmarksY1,
                                            int[] landmarksX2,
                                            int[] landmarksY2,
                                            long addrResult);

    /* ------------------------------------------------------------------------------------------ */

    /* Load Native Library */
    static {
        if (!OpenCVLoader.initDebug());
        else System.loadLibrary("nativefaceswap");
    }


    /**
     * Constructor for SelfieFaceSwap
     * @param resources
     * @param bitmap1, photo 1 with person 1 (face)
     * @param bitmap2, photo 2 with person 2 (body)
     */
    public FaceSwap(Resources resources, Bitmap bitmap1, Bitmap bitmap2)  {
        this.resources = resources;
        this.bitmap1 = bitmap1;
        this.bitmap2 = bitmap2;
    }


    /**
     * Constructor for ManyFacedFaceSwap
     * @param resources
     * @param bitmap, photo to perform face swap on
     */
    public FaceSwap(Resources resources, Bitmap bitmap) {
        this.resources = resources;
        this.bitmap = bitmap;
    }


    /**
     * Prepares selfie face swapping (extracts facial landmarks with DLib).
     * @throws FaceSwapException if no faces were found.
     */
    public void prepareSelfieSwapping() throws  FaceSwapException{
        // Get facial landmarks
        FacialLandmarkDetector landmarkDetector = new FacialLandmarkDetector();
        landmarks1 = landmarkDetector.detectPeopleAndLandmarks(bitmap1);
        landmarks2 = landmarkDetector.detectPeopleAndLandmarks(bitmap2);

        // Uer has an image where faces cannot be detected.
        if (landmarks1.size() <= 1) throw new FaceSwapException("Face(s) missing");
        if (landmarks2.size() <= 1) throw new FaceSwapException("Face(s) missing");
    }


    /**
     * Prepares the face swapping by making extraction of facial landmarks.
     * @throws FaceSwapException if no face(s) was (were) found.
     */
    public void prepareManyFacedSwapping() throws FaceSwapException {
        FacialLandmarkDetector landmarkDetector = new FacialLandmarkDetector();
        landmarks = landmarkDetector.detectPeopleAndLandmarks(bitmap);

        // Uer has an image where faces cannot be detected.
        if (landmarks.size() <= 1) throw new FaceSwapException("Face(s) missing");
    }


    /**
     * Performs a selfie face swap of two photos.
     * @return a bitmap containting the face of one person on the second person's body.
     */
    public Bitmap selfieSwap() {
        ArrayList<PointF> pts1 = landmarks1.get(0);
        ArrayList<PointF> pts2 = landmarks2.get(1);
        return swap(bitmap1, bitmap2, pts1, pts2);
    }


    /**
     * Returns a many faced swap of a photo with many (>=2) faces.
     * @return a bitmap with swapped faces.
     */
    public Bitmap manyFacedSwap() {
        Bitmap swp = swap(bitmap, bitmap, landmarks.get(0), landmarks.get(landmarks.size()-1));
        swp = swap(bitmap, swp, landmarks.get(landmarks.size()-1), landmarks.get(0));

        Log.d("SIZE", "Size " + landmarks.size() + " ------------------------------------");

        if (landmarks.size() > 2) {

            for (int i = 1; i < landmarks.size() - 1; i += 2) {
                swp = swap(swp, swp, landmarks.get(i), landmarks.get(i+1));
                swp = swap(bitmap, swp, landmarks.get(i+1), landmarks.get(i));
            }
        }

        return swp;
    }

    /**
     * Swaps the faces of two photos where the faces have landmarks pts1 and pts2.
     * @param bmp1 photo 1.
     * @param bmp2 photo 2.
     * @param pts1 landmarks for a face in bmp1.
     * @param pts2 landmarks for a face in bmp2.
     * @return a bitmap where a face in bmp1 has been transfered onto a face in bmp2.
     */
    private Bitmap swap(Bitmap bmp1, Bitmap bmp2, ArrayList<PointF> pts1, ArrayList<PointF> pts2) {

        int[] X1 = new int[pts1.size()];
        int[] Y1 = new int[pts1.size()];
        int[] X2 = new int[pts2.size()];
        int[] Y2 = new int[pts2.size()];

        for (int i = 0; i < pts1.size(); ++i) {
            int x = pts1.get(i).X();
            int y = pts1.get(i).Y();
            X1[i] = x;
            Y1[i] = y;
        }

        for (int i = 0; i < pts2.size(); ++i) {
            int x = pts2.get(i).X();
            int y = pts2.get(i).Y();
            X2[i] = x;
            Y2[i] = y;
        }

        Mat img1 = new Mat();
        bitmapToMat(bmp1, img1);
        Mat img2 = new Mat();
        bitmapToMat(bmp2, img2);


        Imgproc.cvtColor(img1,img1, Imgproc.COLOR_BGRA2BGR);
        Imgproc.cvtColor(img2,img2, Imgproc.COLOR_BGRA2BGR);

        Mat swapped = new Mat();
        portraitSwapNative(img1.getNativeObjAddr(), img2.getNativeObjAddr(), X1, Y1, X2, Y2, swapped.getNativeObjAddr());

        Bitmap bmp = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), Bitmap.Config.ARGB_8888);
        matToBitmap(swapped, bmp);
        return  bmp;
    }


    public class FaceSwapException extends Exception {
        public FaceSwapException(String message) {
            super(message);
        }
    }


}
