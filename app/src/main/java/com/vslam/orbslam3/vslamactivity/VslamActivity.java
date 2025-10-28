package com.vslam.orbslam3.vslamactivity;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;

public class VslamActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "VslamActivity";
    private GLSurfaceView glSurfaceView;
    private CameraBridgeViewBase mOpenCvCameraView;
    private SeekBar seek;
    private TextView myTextView;
    public static double SCALE = 1;
    private static long count = 0;

    /**
     * Copy SLAM files from assets to internal storage on first launch
     */
    private void initializeSlamFiles() {
        File slamDir = new File(getFilesDir(), "SLAM");
        File vocabFile = new File(slamDir, "VOC/ORBvoc.bin");
        File configFile = new File(slamDir, "Calibration/PARAconfig.yaml");

        // Check if files already exist
        if (vocabFile.exists() && configFile.exists()) {
            Log.i(TAG, "SLAM files already exist, skipping copy");
            return;
        }

        Log.i(TAG, "Copying SLAM files from assets...");

        try {
            // Create directories
            new File(slamDir, "VOC").mkdirs();
            new File(slamDir, "Calibration").mkdirs();

            // Copy vocabulary file
            if (!vocabFile.exists()) {
                copyAssetFile("SLAM/VOC/ORBvoc.bin", vocabFile);
                Log.i(TAG, "✓ Copied ORBvoc.bin (" + vocabFile.length() + " bytes)");
            }

            // Copy config file
            if (!configFile.exists()) {
                copyAssetFile("SLAM/Calibration/PARAconfig.yaml", configFile);
                Log.i(TAG, "✓ Copied PARAconfig.yaml (" + configFile.length() + " bytes)");
            }

            Log.i(TAG, "SLAM files copied successfully");
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy SLAM files from assets", e);
        }
    }

    /**
     * Copy a single file from assets to destination
     */
    private void copyAssetFile(String assetPath, File destFile) throws IOException {
        InputStream in = getAssets().open(assetPath);
        FileOutputStream out = new FileOutputStream(destFile);

        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }

        in.close();
        out.flush();
        out.close();
    }

    /**
     * Get app-specific storage directory for SLAM files
     */
    private String getSlamDirectory() {
        // Use app-specific external storage (doesn't need special permissions)
        File slamDir = new File(getFilesDir(), "SLAM");
        if (!slamDir.exists()) {
            slamDir.mkdirs();
            Log.i(TAG, "Created SLAM directory: " + slamDir.getAbsolutePath());
        }
        return slamDir.getAbsolutePath();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        //显示窗口初始化设置
        MatrixState.set_projection_matrix(445f, 445f, 319.5f, 239.500000f, 850, 480, 0.01f, 100f);
        // Copy SLAM files from assets FIRST
        initializeSlamFiles();
        super.onCreate(savedInstanceState);
        //hide the status bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //hide the title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //读取布局xml设置
        setContentView(R.layout.activity_vslam_activity);

        //预览SurfaceView初始化
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.mOpenCvCameraView);
        mOpenCvCameraView.setMaxFrameSize(640, 480);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        //文本框初始化
        myTextView = (TextView) findViewById(R.id.myTextView);
        myTextView.setText("当前值Scale为:" + SCALE);

        //SeekBar初始化
        seek = (SeekBar) findViewById(R.id.mySeekBar);
        seek.setProgress(60);
        seek.setOnSeekBarChangeListener(seekListener);

        //OpenGL图层初始化和监听
        //opengl图层
        glSurfaceView = (GLSurfaceView) findViewById(R.id.glSurfaceView);
        //OpenGL ES 2.0
        glSurfaceView.setEGLContextClientVersion(2);
        //设置透明背景
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        final MyRender earthRender = new MyRender(this);
        glSurfaceView.setRenderer(earthRender);
        // 设置渲染模式为主动渲染
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        glSurfaceView.setZOrderOnTop(true);
        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event != null) {
                    // Convert touch coordinates into normalized device
                    // coordinates, keeping in mind that Android's Y
                    // coordinates are inverted.
                    final float normalizedX = ((event.getX() / (float) v.getWidth()) * 2 - 1) * 4f;
                    final float normalizedY = (-((event.getY() / (float) v.getHeight()) * 2 - 1)) * 1.5f;

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        glSurfaceView.queueEvent(new Runnable() {
                            @Override
                            public void run() {
                                earthRender.handleTouchPress(
                                        normalizedX, normalizedY);
                            }
                        });
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        glSurfaceView.queueEvent(new Runnable() {
                            @Override
                            public void run() {
                                earthRender.handleTouchDrag(
                                        normalizedX, normalizedY);
                            }
                        });
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        glSurfaceView.queueEvent(new Runnable() {
                            @Override
                            public void run() {
                                earthRender.handleTouchUp(
                                        normalizedX, normalizedY);
                            }
                        });
                    }

                    return true;
                } else {
                    return false;
                }
            }
        });

        //检查配置文件路径是否有读取权限
        boolean havePermission = getPermissionCamera(this);
        Log.i(TAG, "getPermissionCamera " + havePermission);

        // Test app-specific storage path
        String slamDir = getSlamDirectory();
        Log.i(TAG, "SLAM directory: " + slamDir);
        Log.i(TAG, "Files should be placed at:");
        Log.i(TAG, "  Vocabulary: " + slamDir + "/VOC/ORBvoc.bin");
        Log.i(TAG, "  Config: " + slamDir + "/Calibration/PARAconfig.yaml");
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.i(TAG, "=== onResume called ===");

        // Initialize OpenCV first
        if (!OpenCVLoader.initLocal()) {
            Log.e(TAG, "OpenCV initialization failed!");
            glSurfaceView.onResume();
            return;
        }

        Log.i(TAG, "OpenCV loaded successfully");

        // Check camera permission
        boolean cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;

        Log.i(TAG, "Camera permission: " + (cameraGranted ? "GRANTED" : "DENIED"));

        // Only enable camera if permissions are granted
        if (cameraGranted) {
            Log.i(TAG, "All permissions OK - enabling camera");
            if (mOpenCvCameraView != null) {
                mOpenCvCameraView.setCameraPermissionGranted();
                mOpenCvCameraView.enableView();
                Log.i(TAG, "Camera enableView() called");
            }
        } else {
            Log.e(TAG, "Permissions not granted - camera NOT enabled");
        }

        glSurfaceView.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        glSurfaceView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "=== CAMERA STARTED! Width: " + width + ", Height: " + height + " ===");
    }

    @Override
    public void onCameraViewStopped() {
        //mRgba.release();
    }


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    //private native float[] CVTest(long matAddr, String timeStamp);  //调用 c++代码
    private native float[] CVTest(long matAddr, String vocabPath, String configPath);  //调用 c++代码

    /**
     * 处理图像的函数，这个函数在相机刷新每一帧都会调用一次，而且每次的输入参数就是当前相机视图信息
     * * @param inputFrame
     * @return
     */


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgb = inputFrame.rgba();
// Get app-specific paths for SLAM files
        String slamDir = getSlamDirectory();
        String vocabPath = slamDir + "/VOC/ORBvoc.bin";
        String configPath = slamDir + "/Calibration/PARAconfig.yaml";

        float[] poseMatrix = CVTest(rgb.getNativeObjAddr(), vocabPath, configPath); //从slam系统获得相机位姿矩阵

        if (poseMatrix.length != 0) {
            double[][] pose = new double[4][4];
            for (int i = 0; i < poseMatrix.length / 4; i++) {
                for (int j = 0; j < 4; j++) {

                    if (j == 3 && i != 3) {
                        pose[i][j] = poseMatrix[i * 4 + j] * SCALE;
                    } else {
                        pose[i][j] = poseMatrix[i * 4 + j];
                    }
                    System.out.print(pose[i][j] + "\t ");
                }

                System.out.print("\n");
            }

            System.out.println("Total count =" + count + "frame,SCALE=============" + SCALE);
            double[][] R = new double[3][3];
            double[] T = new double[3];

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    R[i][j] = pose[i][j];
                }
            }
            for (int i = 0; i < 3; i++) {
                T[i] = pose[i][3];
            }
            RealMatrix rotation = new Array2DRowRealMatrix(R);
            RealMatrix translation = new Array2DRowRealMatrix(T);
            MatrixState.set_model_view_matrix(rotation, translation);
            printMatrix(rotation);
            printMatrix(translation);
            MyRender.flag = true;
            count++;

        } else {
            //如果没有得到相机的位姿矩阵，就不画地球/立方体
            MyRender.flag = false;
        }

//      CVTest(rgb.getNativeObjAddr());
        return rgb;
    }

    //@Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            System.out.println("CameraBridgeViewBase> cameraViews == null!!!");
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
            }
        }
    }

    /**
     * 确认camera权限
     * @param activity
     * @return
     */
    public boolean getPermissionCamera(Activity activity) {
        int cameraPermissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
        int readPermissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        int writePermissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (cameraPermissionCheck != PackageManager.PERMISSION_GRANTED
                || readPermissionCheck != PackageManager.PERMISSION_GRANTED
                || writePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(
                    activity,
                    permissions,
                    0);
            return false;
        } else {
            onCameraPermissionGranted();
            return true;
        }
    }

    /**
     * 用于测试java读取文件权限的函数
     **/
    void readFileOnLine(String strFileName) throws Exception {

        int REQUEST_EXTERNAL_STORAGE = 1;
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        int permission = ActivityCompat.checkSelfPermission(VslamActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    VslamActivity.this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
        FileInputStream fis = new FileInputStream(new File(strFileName));
        StringBuffer sBuffer = new StringBuffer();
        DataInputStream dataIO = new DataInputStream(fis);
        String strLine = null;
        while ((strLine = dataIO.readLine()) != null) {
            Log.i(TAG, strLine + "+++++++++++++++++++++++++++++++++++++++");
        }
        dataIO.close();
        fis.close();
    }

    /**
     * SeekBar监听器
     **/
    private SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Log.i(TAG, "onStopTrackingTouch");
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            Log.i(TAG, "onStartTrackingTouch");
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            Log.i(TAG, "onProgressChanged");
            if (progress > 50) {
                SCALE = (progress - 50) * 10;
            } else {
                SCALE = (50 - progress) * 0.5;
            }
            myTextView.setText("当前值 为: " + SCALE);

        }
    };

    /**
     * 打印Mat矩阵函数
     **/
    void printMatrix(RealMatrix input) {
        double matrixtoarray[][] = input.getData();
        for (int i = 0; i < matrixtoarray.length; i++) {
            for (int j = 0; j < matrixtoarray[0].length; j++) {
                System.out.print(matrixtoarray[i][j] + "\t");
            }
            System.out.print("\n");
        }
    }

    /**
     * Handle permission request result
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.i(TAG, "onRequestPermissionsResult called with requestCode: " + requestCode);

        if (requestCode == 0) {
            // Check if all permissions granted
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Log.i(TAG, "All permissions granted - enabling camera");
                onCameraPermissionGranted();

                // Enable camera view if OpenCV is already loaded
                if (mOpenCvCameraView != null) {
                    mOpenCvCameraView.enableView();
                }
            } else {
                Log.e(TAG, "Permissions denied by user");
            }
        }
    }

}
