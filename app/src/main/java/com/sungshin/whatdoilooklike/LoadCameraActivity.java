package com.sungshin.whatdoilooklike;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class LoadCameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{


    private static final String TAG="Camera";

    String baseDir;
    String packageName;
    String pathDir;

    private Mat mRgba;
    private Mat mGray;
    private Mat inputMat;
//    private Mat mRotate;
//    private Mat rotateInputMat;

    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageView flip_camera;     // call for image view of flip button
    private int mCameraId = 1;         // start with front camera // 0 : back, 1 : front
    private ImageView take_picture_button;
    private int take_image = 0;

    Interpreter interpreter, face_detect_interpreter;
    static TextView textView;
    MsgHandler handler;
    private final int CELEB1 = 0;
    private final int CELEB2 = 1;
    private final int CELEB3 = 2;
    private final int CELEB4 = 3;
    private final int CELEB5 = 4;
    private final int CELEB6 = 5;
    private final int CELEB7 = 6;
    private final int CELEB8 = 7;
    private final int CELEB9 = 8;
    private final int CELEB10 = 9;
    private final int CELEB11 = 10;
    private final int CELEB12 = 11;
    private final int CELEB13 = 12;
    private final int CELEB14 = 13;
    private final int CELEB15 = 14;
    private final int CELEB16 = 15;
    private final int ETC = 16;
    static private final String[] celebrity = {"황민현", "소희", "박보영", "백현","나연","박지훈",
            "주지훈","제니","김우빈","천우희","안재홍","라미란","최시원","하주연","진","이정은","결과없음"};

    static private final String[] animal = {"cat", "dog", "rabbit",  "fox", "dinosaur", "bear", "horse", "quokka"};

    public native long loadCascade(String cascadeFileName);
    public native long loadCascade(String fileName, String cascadeFileName);
    public native void detect(long cascadeClassifier_face, long matAddrInput, long matAddrResult, long nativeObjAddr);
    public long cascadeClassifier_face=0;


    /** 전면카메라로 시작
     *  후면카메라로 시작하고 싶을 시 상단의 mCameraId = 0으로 설정하면됨 */


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }


    private BaseLoaderCallback mLoaderCallback =new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:{
                    Log.i(TAG,"OpenCv Is loaded");
                    mOpenCvCameraView.setCameraIndex(mCameraId);
                    mOpenCvCameraView.enableView();
                }
                default: { super.onManagerConnected(status); }
                break;
            }
        }
    };


    public LoadCameraActivity(){
        Log.i(TAG,"Instantiated new "+this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        // 1. if camera permission is not given it will ask for it on device
        int MY_PERMISSIONS_REQUEST_CAMERA=0;

        if (ContextCompat.checkSelfPermission(LoadCameraActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(LoadCameraActivity.this, new String[] {Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }

        if (ContextCompat.checkSelfPermission(LoadCameraActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(LoadCameraActivity.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_CAMERA);
        }

        if (ContextCompat.checkSelfPermission(LoadCameraActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(LoadCameraActivity.this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_CAMERA);
        }

        // 2. SetContentView
        setContentView(R.layout.activity_camera);


        // 3. Allocate Instance
        interpreter = getTfliteInterpreter("converted_model2.tflite");
        textView = (TextView) findViewById(R.id.textView);
        mOpenCvCameraView=(CameraBridgeViewBase)findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
//        Log.i(TAG,"mOpenCvCameraView :: " +  mOpenCvCameraView.getWidth() + ", "+ mOpenCvCameraView.getHeight());


        handler = new MsgHandler();

        flip_camera = findViewById(R.id.flip_camera);
        flip_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                swapCamera();
            }
        });

        take_picture_button = findViewById(R.id.take_picture_button);
        take_picture_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "pressed");

                Toast toast =  Toast.makeText(getApplicationContext(), "Shot", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP, 200, 200);
                toast.show();

                // if take_image == 1 then take a picture
                if (take_image == 0){
                    take_image = 1;
                } else {
                    take_image = 0;
                }
            }
        });
    }

    private static class MsgHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {

            int animal_category = msg.what / 2;


            textView.setText("result : " + celebrity[msg.what]);
//            textView.setText("result : " + animal[animal_category]);

        }
    }


    private void swapCamera() {
        mCameraId = mCameraId^1;            // ^ is not operation
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(mCameraId);
        mOpenCvCameraView.enableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()){
            //if load success
            Log.d(TAG,"Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            //if not loaded
            Log.d(TAG,"Opencv is not loaded. try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this,mLoaderCallback);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }
    }

    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }

    }

    public void onCameraViewStarted(int width ,int height){
        mRgba   = new Mat(height,width, CvType.CV_8UC4);
        mGray   = new Mat(height,width,CvType.CV_8UC1);
//        mRotate = new Mat(height,width, CvType.CV_8UC4);


    }
    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){

        mRgba=inputFrame.rgba();
        mGray=inputFrame.gray();
//        mRotate = inputFrame.rgba();
        inputMat = new Mat();
//        rotateInputMat = mRotate.clone();




        if (mCameraId == 1){    // front camera
            // rotate camera frame with 180 degree
            Core.flip(mRgba, mRgba, -1);
            Core.flip(mGray, mGray, -1);
            // flipCode == 0 좌우반전
            Core.flip(mRgba, mRgba, 0);
            Core.flip(mGray, mGray, 0);
        }


//        if (mCameraId == 1) {    // front camera
//            Core.flip(mRotate, mRotate, 1);
//            Core.flip(mRotate, mRotate, 0);
//            Core.flip(mRotate, mRotate, -1);
//        }



         //예슬 코드 원본
        detect(cascadeClassifier_face, mRgba.getNativeObjAddr(), mRgba.getNativeObjAddr(), inputMat.getNativeObjAddr());

//        // 변형
//        detect(cascadeClassifier_face, mRotate.getNativeObjAddr(), mRotate.getNativeObjAddr(), rotateInputMat.getNativeObjAddr());
        take_image = take_picture_function_rgb(take_image, mRgba, inputMat);

        return mRgba;
    }

    private void copyFile(String filename) {
        //저장할 외부 저장소의 경로 구하기
        baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        //ContextWrapper cw = new ContextWrapper(getApplicationContext());
        //File directory = cw.getExternalFilesDir(Environment.getExternalStorageDirectory().getPath());
        //패키지명을 구한다
        packageName = getPackageName();
        //String pathDir = baseDir + File.separator + filename;
        pathDir = baseDir +"/android/data/"+packageName + File.separator + filename;

        AssetManager assetManager = this.getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            Log.d( TAG, "copyFile :: 다음 경로로 파일복사 "+ pathDir);
            inputStream = assetManager.open(filename);
            outputStream = new FileOutputStream(pathDir);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            Log.d(TAG, "copyFile :: 파일 복사 중 예외 발생 "+e.toString() );
        }
        Log.d(TAG, "xml 파일 다 읽음" );
    }

    private void read_cascade_file(){
        //copyFile메소드는 assets에서 해당 파일을 가져와 외부 저장소 특정 위치에 저장하도록 구현된 메소드
        copyFile("haarcascade_frontalface_alt.xml");
        Log.d(TAG,"Read_cascade_file");

        //loadCascade 메소드는 외부 저장소의 특정 위치에서 해당 파일을 읽어와서 CascadeClassifier 객체로 로드함
        cascadeClassifier_face = loadCascade("haarcascade_frontalface_alt.xml",pathDir);
        Log.d(TAG,"read_cascade_file:");
    }

    private int take_picture_function_rgb(int take_image, Mat mRgba, Mat input) {

        Mat save_mat = new Mat();

        // rotate img 90 degree
        Core.flip(mRgba.t(), save_mat, 1);

        // convert image from RGBA to BGRA
        Imgproc.cvtColor(save_mat, save_mat, Imgproc.COLOR_RGBA2BGRA);

        if (take_image == 1){

            /** (추후 수정) 촬영 버튼 눌렀을 때 이미지 저장*
             *  개선 : 갤럭시의 '내파일'앱에서는 사진을 확인할 수 있으나,
             *  갤러리에서는 확인이 불가능함
             *  경로 수정으로 해결(미디어 파일 형식으로 저장)
             */


            // create new folder
            File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/WhatDoIlookLike" );

            if (!folder.exists()){
                Log.e(TAG, "Create folder");
                folder.mkdirs();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String currentDateAndTime = sdf.format(new Date());
            String fileName = Environment.getExternalStorageDirectory().getPath() + "/WhatDoIlookLike/" + currentDateAndTime + ".jpg";
            Log.e(TAG, "Create folder Save Image");

            Imgcodecs.imwrite(fileName, save_mat);

            take_image = 0;
        }
        Message msg = handler.obtainMessage();

        if(input.empty()){
            Log.e(TAG, "얼굴 검출이 되지 않음!");
            msg.what = ETC;
        }
        else{
            int result = doInference(input);
            Log.e(TAG,"Result After DoInference = " + result );
            Log.e(TAG, "crop 후 input image 사이즈:" + input.width() +" * " +input.height());



            /** Category 추가 방법
             * 1. celebrity 이름의 배열 수정 : 결과 없음은 마지막 인덱스로 지정
             * 2. 바로 아래의 else if 문 추가
             * */
            if(result == 0.0){  msg.what = CELEB1 ; }
            else if(result == 1.0){ msg.what = CELEB2 ;}
            else if(result == 2.0){ msg.what = CELEB3 ;}
            else if(result == 3.0){ msg.what = CELEB4 ;}
            else if(result == 4.0){ msg.what = CELEB5 ;}
            else if(result == 5.0){ msg.what = CELEB6 ;}
            else if(result == 6.0){ msg.what = CELEB7 ;}
            else if(result == 7.0){ msg.what = CELEB8 ;}
            else if(result == 8.0){ msg.what = CELEB9 ;}
            else if(result == 9.0){ msg.what = CELEB10 ;}
            else if(result == 10.0){ msg.what = CELEB11 ;}
            else if(result == 11.0){ msg.what = CELEB12 ;}
            else if(result == 12.0){ msg.what = CELEB13 ;}
            else if(result == 13.0){ msg.what = CELEB14 ;}
            else if(result == 14.0){ msg.what = CELEB15 ;}
            else if(result == 15.0){ msg.what = CELEB16 ;}
            else if (result == -1.0){ msg.what = ETC ;}
        }
        handler.sendMessage(msg);
        return take_image;
    }

    private Interpreter getTfliteInterpreter(String modelPath) {
        try {
            return new Interpreter(loadModelFile(this, modelPath));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private MappedByteBuffer loadModelFile(Activity activity, String modelPath) throws IOException {

        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

    }

    private int doInference(Mat image){

        float[][][][] input = new float[1][96][96][1];
        float[][] output = new float[1][16];

        Imgproc.resize(image, image, new Size(96, 96));

        for(int i=0;i<image.rows();i++)
            for(int j=0;j<image.cols();j++) {
                double pixel = image.get(i,j)[0] * 0.299 + image.get(i,j)[1] * 0.587 + image.get(i,j)[2] * 0.114;
                input[0][i][j][0] = (float) (pixel / 255.0);
            }

        interpreter.run(input, output);

        int out = 0;        // index
        float out2 = 0;     // ratio
        for(int i=0;i<16;i++) {
            if (output[0][i] > out2) {
                out2 = output[0][i];
                out = i;
            }
        }

        float[] animal_rate = new float[8];
        for (int i = 0 ; i < 8; i++){ animal_rate[i] = 0.0f; }

        for (int i = 0 ; i< 16; i++){
            animal_rate[i/2] += output[0][i];
        }

        for (int i = 0 ; i < 8; i++){
            System.out.println("비율확인 :: " + animal[i] + " : " + animal_rate[i] + " ");
        }

        DecimalFormat df = new DecimalFormat("#.##");

        //Log.i(TAG, "output :: " + df.format(output[0][0]) + ", " + df.format(output[0][1]) + ", " +  df.format(output[0][2]) );

        // category 비율이 40이하일 경우 '결과 없음'으로 표시 (필터 변경 최소화)
        //if (out2 < 0.6) { out = -1; }

        return out;
    }

    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }



    //여기서부턴 퍼미션 관련 메소드
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;


    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();

                read_cascade_file();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean havePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
                havePermission = false;
            }
        }
        if (havePermission) {
            onCameraPermissionGranted();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1]==PackageManager.PERMISSION_GRANTED) {
            onCameraPermissionGranted();
        }else{
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder( LoadCameraActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                requestPermissions(new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }

}

