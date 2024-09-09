package com.example.cameradetection;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.Video;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class camera extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener  {

    private CameraBridgeViewBase cameraBridgeViewBase;
    Mat curr_gray, blur_frame, background_gray, rgb, diff;
    List<MatOfPoint> cnts;
    BackgroundSubtractor backSub;
    int sens;
    private String outputFilePath;
    private boolean isRecording = false;
    private int numRecordings;
    private static final long RECORDING_DURATION_MS = 30000; // 30 seconds
    private Handler handler = new Handler();
    private Runnable stopRecordingRunnable = new Runnable() {
        @Override
        public void run() {
            stopRecording();
        }
    };

    private int fps = 30;
    private VideoWriter videoWriter = null;
    private Mat videoFrame;
    private int mWidth = 0, mHeight = 0;
    private boolean useBuiltInMJPG = false;
    boolean motionDetected = false;
    private ExecutorService executor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Log.d("OpenCV", "onCreate");

        // Initialize OpenCV
        cameraBridgeViewBase = findViewById(R.id.cameraView);
        if(OpenCVLoader.initLocal()){
            Log.d("OpenCV", "OpenCV loaded");
            cameraBridgeViewBase.enableView();
        }
        // Set listener
        cameraBridgeViewBase.setCvCameraViewListener(this);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Initialize the SharedPreferences object
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        // Get the number of recordings from SharedPreferences
        numRecordings = sharedPreferences.getInt("numRecordings", 0);

        executor = Executors.newSingleThreadExecutor();
    }
    @Override
    protected void onDestroy(){
        Log.d("onDestroy", "onDestroy");

        // Clear keep screen on flag
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Stop Recording
        if (isRecording){ stopRecording(); }

        // Initialize the SharedPreferences object
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        // Update numRecordings
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("numRecordings", numRecordings);
        editor.apply();

        // Shutdown executor
        executor.shutdown();

        super.onDestroy();
    }
    public void onCameraViewStarted(int width, int height) {
        // Initialize variables
        curr_gray = new Mat();
        background_gray = new Mat();
        blur_frame = new Mat();
        rgb = new Mat();
        diff = new Mat();
        cnts = new ArrayList<MatOfPoint>();
        mWidth = width;
        mHeight = height;
        // Get sensitivity value from shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        sens = sharedPreferences.getInt("sensitivity_value", 50);

        // Create background subtractor
        backSub = Video.createBackgroundSubtractorMOG2();

    }


    public void onCameraViewStopped() {
        Log.d("onCameraViewStopped" ," onCameraViewStopped");

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.d("onCameraFrame", "onCameraFrame");
        // Get rgb of frame
        rgb = inputFrame.rgba();

        int min_area = sens * 500;
        int max_area = 300000;// - (sensitivity_bar.getProgress()*1000);

        // Get grayscale of frame
        curr_gray = inputFrame.gray();
        // Apply gaussian blur
        Imgproc.GaussianBlur(curr_gray, blur_frame, new Size(25,25),0);
        // Apply background subtraction
        backSub.apply(blur_frame, background_gray);
        // Find contours
        Imgproc.findContours(background_gray, cnts, new Mat(),Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);

        motionDetected = false;
        // Draw contours
        for(MatOfPoint cnt : cnts){
            // Get area of contours
            double area = Imgproc.contourArea(cnt);
            // Reduce noise
            if (area > min_area && area < max_area){
                Log.d("onCameraFrame", "Motion detected");
                motionDetected = true;
                break;
            }
        }
        // Clear contours list
        cnts.clear();
        // Check if motion detected and is not recording
        if (motionDetected && !isRecording) {
            isRecording = true;
            motionDetected = false;
            // Set filepath
            outputFilePath = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "video_" + numRecordings + ".mp4").getAbsolutePath();
            startRecording();
            // Increment recording number
            numRecordings += 1;
        }
        // Write frame to video
        if (isRecording && videoWriter != null && videoWriter.isOpened()) {
            // Recording video
            videoFrame = new Mat();
            // Converting RGB color to BGR to be able to write into file
            Imgproc.cvtColor(rgb, videoFrame, Imgproc.COLOR_RGBA2BGR);
            videoWriter.write(videoFrame);
        }
        return rgb;
    }


    private void startRecording() {
        // Taken from sample application
        File file = new File(outputFilePath);
        file.delete();
        videoWriter = new VideoWriter();
        if (!useBuiltInMJPG) {
            videoWriter.open(outputFilePath, Videoio.CAP_ANDROID, VideoWriter.fourcc('H', '2', '6', '4'), fps, new Size(mWidth, mHeight));
            if (videoWriter != null && !videoWriter.isOpened()) {
                Log.i("TAG","Can't record H264. Switching to MJPG");
                useBuiltInMJPG = true;
                // Added my own file path string
                outputFilePath = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "video_" + numRecordings + ".avi").getAbsolutePath();
            }
        }
        if (useBuiltInMJPG){
            videoWriter.open(outputFilePath, VideoWriter.fourcc('M', 'J', 'P', 'G'), fps, new Size(mWidth, mHeight));
        }
        // Schedule stop recording after 30 seconds
        handler.postDelayed(stopRecordingRunnable, RECORDING_DURATION_MS);

    }

    private void stopRecording() {
        videoWriter.release();
        videoWriter = null;
        isRecording = false;
        sendEmail(outputFilePath);
        Log.d("MediaRecorder", "Recording stopped. File saved at: " + outputFilePath);
    }

    private void sendEmail(String filePath){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // Get the user's email address from shared preferences
                SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                String toEmail = sharedPreferences.getString("toEmail", "");
                if (!toEmail.isEmpty()) {
                    String subject = "Motion Detected";
                    String body = "Motion detected";
                    // Send the email
                    EmailSender.sendEmail(toEmail, subject, body, filePath);
                }
            }
        });
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }

    @Override
    public void onClick(View v) {

    }
}