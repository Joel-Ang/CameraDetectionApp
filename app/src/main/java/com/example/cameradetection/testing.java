package com.example.cameradetection;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
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

public class testing extends CameraActivity {

    CameraBridgeViewBase cameraBridgeViewBase;

    Mat curr_gray, blur_frame, background_gray, rgb, diff;
    List<MatOfPoint> cnts;
    SeekBar sensitivity_bar;
    TextView sensitivity_value;
    BackgroundSubtractor backSub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testing);

        // Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Camera Test");
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        sensitivity_bar = findViewById(R.id.sensitivity_bar);

        sensitivity_value = findViewById(R.id.sensitivity_value);
        sensitivity_value.setTextColor(Color.WHITE);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        int sens = sharedPreferences.getInt("sensitivity_value", 50); // Default value is 50 if the key doesn't exist

        sensitivity_bar.setProgress(sens);
        sensitivity_value.setText(String.valueOf(sensitivity_bar.getProgress()));


        sensitivity_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sensitivity_bar, int progress, boolean fromUser) {
                // Called when the progress level has changed.
                sensitivity_value.setText(String.valueOf(progress));
                // Store in SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("sensitivity_value", progress);
                editor.apply();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Called when the user starts dragging the thumb.
                sensitivity_value.setText(String.valueOf(sensitivity_bar.getProgress()));
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Called when the user stops dragging the thumb.
                // set the text view to the current progress
                sensitivity_value.setText(String.valueOf(seekBar.getProgress()));
            }
        });

        cameraBridgeViewBase = findViewById(R.id.cameraView);
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                // initialize variables
                curr_gray = new Mat();
                background_gray = new Mat();
                blur_frame = new Mat();
                rgb = new Mat();
                diff = new Mat();
                cnts = new ArrayList<MatOfPoint>();

                // create background subtractor
                backSub = Video.createBackgroundSubtractorMOG2();

            }

            @Override
            public void onCameraViewStopped() {

            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                int min_area = sensitivity_bar.getProgress() * 500;
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
                    if (area > min_area && area < max_area){
                        Rect r = Imgproc.boundingRect(cnt);
                        Imgproc.rectangle(rgb, r, new Scalar(0,255,0),2);
                    }
                }
                // clear contours list
                cnts.clear();
                return rgb;
            }
        });


        if(OpenCVLoader.initLocal()){
            Log.d("OpenCV", "OpenCV loaded");
            cameraBridgeViewBase.enableView();
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }

}