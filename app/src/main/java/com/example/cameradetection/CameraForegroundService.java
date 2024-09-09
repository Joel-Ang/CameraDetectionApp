package com.example.cameradetection;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.Manifest;

public class CameraForegroundService extends Service implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String CHANNEL_ID = "CameraForegroundServiceChannel";
    private static final String TAG = "CameraForegroundService";
    CameraBridgeViewBase cameraView;
    Mat curr_gray, blur_frame, background_gray, rgb, diff;
    int sens;
    List<MatOfPoint> cnts;
    BackgroundSubtractor backSub;
    @Override
    public void onCreate() {
        super.onCreate();

        if (!OpenCVLoader.initLocal()) {
            Log.e(TAG, "OpenCV initialization failed");
        } else {
            Log.d(TAG, "OpenCV initialization succeeded");
        }
        cameraView = new JavaCameraView(this, -1);
        cameraView.setCvCameraViewListener(this);
        cameraView.enableView();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Camera Service")
                .setContentText("Running camera in the background")
                .setSmallIcon(R.drawable.ic_camera)
                .build();

        startForeground(1, notification);


        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Camera Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        // initialize variables
        curr_gray = new Mat();
        background_gray = new Mat();
        blur_frame = new Mat();
        rgb = new Mat();
        diff = new Mat();
        cnts = new ArrayList<MatOfPoint>();
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        sens = sharedPreferences.getInt("sensitivity_value", 50); // Default value is 50 if the key doesn't exist
        Log.d("sensitivity", "sensitivity: " + sens);
        // create background subtractor
        backSub = Video.createBackgroundSubtractorMOG2();
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        int min_area = sens * 100;
        int max_area = 300000;
        Log.d("minmax", "min: " + min_area + "max: " + max_area);
        //get rgb of frame
        rgb = inputFrame.rgba();
        //get grayscale of frame
        curr_gray = inputFrame.gray();
        // apply gaussian blur
        Imgproc.GaussianBlur(curr_gray, blur_frame, new Size(25,25),0);
        // apply background subtraction
        backSub.apply(blur_frame, background_gray);
        // find contours
        Imgproc.findContours(background_gray, cnts, new Mat(),Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
        // draw contours
        for(MatOfPoint cnt : cnts){
            // get area of contours
            double area = Imgproc.contourArea(cnt);
            // reduce noise
            //if (area > 5500 && area < 15000){
            if (area > min_area && area < max_area){
                Log.d("motion", "motion detected");
            }
        }
        // clear contours list
        cnts.clear();
        return rgb;
    }
}
