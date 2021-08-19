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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import org.opencv.android.Utils;
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
import java.sql.SQLOutput;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class LoadCameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{


    private static final String TAG="LoadCameraActivity:";

    String baseDir;
    String packageName;
    String pathDir;

    private Mat mRgba;
    private Mat mGray;
    private Mat inputMat;
    private facialDetection facialDetection;
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

    float[][][][] input;
    float[][] output;

    static final int CASE_FROM_CAMERA = 0;
    static final int CASE_FROM_GALLERY = 1;
    private final int ANI0 = 0;
    private final int ANI1 = 1;
    private final int ANI2 = 2;
    private final int ANI3 = 3;
    private final int ANI4 = 4;
    private final int ANI5 = 5;
    private final int ANI6 = 6;
    private final int ANI7 = 7;
    private final int ETC =  8;

    float[] animal_rate = new float[8];
    float[] celebrity_rate = new float[16];

    static private final String[] celebrity = {"황민현", "소희", "박보영", "백현","나연","박지훈",
            "주지훈","제니","김우빈","천우희","안재홍","라미란","최시원","하주연","진","이정은","결과없음"};

    static private final String[] animal = {"cat", "dog", "rabbit",  "fox", "dinosaur", "bear", "horse", "quokka","결과없음"};
    private int temp_idx = 8;

    public native long loadCascade(String cascadeFileName);
    public native long loadCascade(String fileName, String cascadeFileName);
    public native void detect(long cascadeClassifier_face, long matAddrInput, long matAddrResult, long nativeObjAddr);
    public long cascadeClassifier_face=0;

    private Mat[] animalImg = new Mat[8];
    private int animalIdx = 0;
    private int[][][] animalROI = {
            {   // cat
                    {114, 188, 54, 19},
                    {83, 113, 35, 35},
                    {157, 109, 35, 35}
            },

            {   // dog
                    {24, 197, 111, 40},
                    {28, 92, 28, 28},
                    {126, 106, 28, 28}
            },

            {   // rabbit
                    {114, 188, 54, 19},
                    {83, 113, 35, 35},
                    {157, 109, 35, 35}
            },

            {   // fox
                    {66, 180, 40, 15},
                    {51, 110, 25, 25},
                    {111, 111, 25, 25}
            },

            {   // dino
                    {27, 131, 128, 84},
                    {26, 52, 28, 28},
                    {125, 48, 28, 28}
            },

            {   // bear
                    {118, 181, 50, 20},
                    {79, 105, 28, 28},
                    {144, 95, 28, 28}
            },

            {   // horse
                    {13, 226, 37, 14},
                    {22, 100, 25, 25},
                    {86, 98, 25, 25}
            },

            {   // quokka
                    {80, 131, 61, 41},
                    {45, 68, 20, 20},
                    {118, 53, 20, 20}
            }
    };

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

        try{
            int inputSize=150;
            facialDetection=new facialDetection(getAssets(),LoadCameraActivity.this,
                    "landMark.tflite",inputSize);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    private static class MsgHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
//
//            int animal_category = msg.what / 2;

            textView.setText("result : " + animal[msg.what]);
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
        Mat out=new Mat();
        // Imgproc.cvtColor(mRgba,mRgba,Imgproc.COLOR_RGBA2RGB);
        out=facialDetection.recognizeImage(mRgba, animalImg[animalIdx], animalROI[animalIdx]);
        // Display out Mat image
        return out;
//        return mRgba;
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

        //외부저장소 경로 있는지 확인, 없으면 생성
        String path = baseDir +"/android/data/"+packageName;
        File file = new File(path);
        if(file.exists() == false){
            file.mkdir();
        }

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


        if(input.empty()){
            Log.e(TAG, "얼굴 검출이 되지 않음!");
//            msg.what = ETC;
        }
        else{
            Message msg = handler.obtainMessage();

            int result = doInference(input);
            Log.e(TAG,"Result After DoInference = " + result );
            Log.e(TAG, "crop 후 input image 사이즈:" + input.width() +" * " +input.height());

            /** Category 추가 방법
             * 1. celebrity 이름의 배열 수정 : 결과 없음은 마지막 인덱스로 지정
             * 2. 바로 아래의 else if 문 추가
             * */
            if(result == 0.0){  msg.what = ANI0 ; }
            else if(result == 1.0){ msg.what = ANI1 ;}
            else if(result == 2.0){ msg.what = ANI2 ;}
            else if(result == 3.0){ msg.what = ANI3 ;}
            else if(result == 4.0){ msg.what = ANI4 ;}
            else if(result == 5.0){ msg.what = ANI5 ;}
            else if(result == 6.0){ msg.what = ANI6 ;}
            else if(result == 7.0){ msg.what = ANI7 ;}

            handler.sendMessage(msg);
        }


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


            Long imgAddress = save_mat.getNativeObjAddr();

            Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
            Log.e(TAG, "Create Intent");
            intent.putExtra("Image", imgAddress);
            intent.putExtra("animal_rate", animal_rate);
            intent.putExtra("celebrity_rate", celebrity_rate);
            intent.putExtra("celebrity", celebrity);
            intent.putExtra("animal", animal);
            intent.putExtra("case_code", CASE_FROM_CAMERA);
            startActivity(intent);
            finish();

        }
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

        input = new float[1][96][96][1];
        output = new float[1][16];

        Imgproc.resize(image, image, new Size(96, 96));

        for(int i=0;i<image.rows();i++)
            for(int j=0;j<image.cols();j++) {
                double pixel = image.get(i,j)[0] * 0.299 + image.get(i,j)[1] * 0.587 + image.get(i,j)[2] * 0.114;
                input[0][i][j][0] = (float) (pixel / 255.0);
            }

        /** DeepLearning Model 실행 */
        interpreter.run(input, output);

        // 닮은꼴 연예인 찾기
//        int out = 0;        // index
//        float out2 = 0;     // ratio
//        for(int i=0;i<16;i++) {
//            if (output[0][i] > out2) {
//                out2 = output[0][i];
//                out = i;
//            }
//        }

        celebrity_rate = new float[16];

        for (int i = 0 ; i < 16; i++){
            celebrity_rate[i] = output[0][i];
        }

        // 닮은꼴 동물 찾기

        // 1) 배열 선언 및 초기화
        for (int i = 0 ; i < 8; i++){ animal_rate[i] = 0.0f; }

        // 2) 동물별 비율 구하기
        for (int i = 0 ; i< 16; i++){
            animal_rate[i/2] += output[0][i];
        }

        DecimalFormat df = new DecimalFormat("#.##");
//        System.out.println("비율확인 HyoRim::========================================================");
//        System.out.print("HyoRim:: ");
//        for (int i = 0 ; i < 8; i++){
//            System.out.print( animal[i] + ": " + df.format(animal_rate[i] )+ " ");
//        }
//        System.out.println();


        // 3) 가장 닮은 동물 찾기
        int maxIdx = 0;
        float maxVal = 0;
        for (int i = 0 ; i < 8; i++){
            if (animal_rate[i] > maxVal){
                maxVal = animal_rate[i];
                maxIdx = i;
            }
        }

        // 닮은꼴 비율이 30프로가 넘을때만 return 할 변수의 값을 갱신
        // 그렇지 않다면 이전에 검출됐던 결과중 마지막으로 30을 넘은 경우를 return
        if(maxVal >= 0.3) {
            temp_idx = maxIdx;
            System.out.println("HyoRim:: "+  animal[temp_idx] + " (" + df.format(maxVal * 100) + "%)");
        }else {
            System.out.println("HyoRim:: " +  animal[temp_idx] + " X 30");
        }

        return temp_idx;
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

