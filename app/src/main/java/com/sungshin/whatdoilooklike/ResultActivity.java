package com.sungshin.whatdoilooklike;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;



import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;

public class ResultActivity extends AppCompatActivity {

    private File tempFile;
    private ImageView imageview;
    private Button galleryBtn, cameraBtn;
    private Mat mat;
    private int case_code;
    static final int CASE_FROM_CAMERA = 0;
    static final int CASE_FROM_GALLERY = 1;
    private static final String TAG = "ResultActivity:";
    private float animal_rate[];
    private float celebrity_rate[];
    private String celebrity[];
    private String animal[];
    protected TextView animal_result_TV;
    protected TextView aniTV1;
    protected TextView aniTV2;
    protected TextView aniTV3;
    protected ProgressBar aniPB1;
    protected ProgressBar aniPB2;
    protected ProgressBar aniPB3;
    protected TextView aniPERC1;
    protected TextView aniPERC2;
    protected TextView aniPERC3;
    private int isFaceRecognized;
    int ani1, ani2, ani3;
    int ani1_rate, ani2_rate, ani3_rate;


    private String animal_result[] = {"시크도도 고양이상", "귀염뽀짝 강아지상",
            "말괄량이 토끼상", "매혹덩어리 여우상", "크와아앙 공룡상", "포근하고 듬직한 곰상",
    "이국적인 매력의 말상", "세상 행복한 쿼카상"};

    float[][][][] input;
    float[][] output;
    Interpreter interpreter;

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

    public native long loadCascade(String fileName, String cascadeFileName);
    public long cascadeClassifier_face=0;
    private facialDetection facialDetection;
    public static Mat inputMat;

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        interpreter = getTfliteInterpreter("converted_model2.tflite");

        animal_result_TV = (TextView)findViewById(R.id.animal_result_TV);
        imageview = (ImageView)findViewById(R.id.imageView);
        galleryBtn = (Button) findViewById(R.id.button);
        cameraBtn = (Button) findViewById(R.id.buttonCamera);
        aniTV1 = (TextView) findViewById(R.id.aniTV1);
        aniTV2 = (TextView) findViewById(R.id.aniTV2);
        aniTV3 = (TextView) findViewById(R.id.aniTV3);
        aniPB1 = (ProgressBar) findViewById(R.id.aniPB1);
        aniPB2 = (ProgressBar) findViewById(R.id.aniPB2);
        aniPB3 = (ProgressBar) findViewById(R.id.aniPB3);
        aniPERC1 = (TextView) findViewById(R.id.aniPERC1);
        aniPERC2 = (TextView) findViewById(R.id.aniPERC2);
        aniPERC3 = (TextView) findViewById(R.id.aniPERC3);


        animal_rate = new float[8];
        celebrity_rate = new float[16];
        animal = new String[8];
        celebrity = new String[16];

        Intent intent = getIntent();
        case_code = intent.getIntExtra("case_code", 0);

        try{
            int inputSize=150;
            facialDetection=new facialDetection(getAssets(),ResultActivity.this,
                    "landMark.tflite",inputSize);
        }
        catch (IOException e){
            e.printStackTrace();
        }



        if (case_code == CASE_FROM_CAMERA){

            Log.e(TAG, "Case_from_Camera");

            // get Data from Intent
            animal_rate = intent.getFloatArrayExtra("animal_rate");
            celebrity_rate = intent.getFloatArrayExtra("celebrity_rate");
            animal = intent.getStringArrayExtra("animal");
            celebrity = intent.getStringArrayExtra("celebrity");
            isFaceRecognized = intent.getIntExtra("isFaceRecognized", 0);

            // get Image from Intent
            long addr = intent.getLongExtra("Image", 0);
            Mat tempImg = new Mat(addr);
            Mat img = tempImg.clone();

            // Convert Mat to Bitmap to display 'captured Image' on ImageView
            Bitmap bitmap;
            bitmap = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, bitmap);
            imageview.setImageBitmap(bitmap);



            if (isFaceRecognized == 0){  markAsNoFace(); }
            if (isFaceRecognized == 1){

                /** ani1 이 나의 동물상 index임 */
                ani1 = find_max_idx();
                ani1_rate = (int) (animal_rate[ani1] * 100);
                Log.e("Animal Rate::", animal[ani1]);
                Log.e("Animal Rate:: ", String.valueOf(ani1_rate));
                animal_rate[ani1] = 0;

                ani2 = find_max_idx();
                ani2_rate = (int)   (animal_rate[ani2] * 100);
                Log.e("Animal Rate::", animal[ani2]);
                Log.e("Animal Rate:: ", String.valueOf(ani2_rate ));
                animal_rate[ani2] = 0;


                ani3 = find_max_idx();
                ani3_rate = (int) (animal_rate[ani3] * 100);
                Log.e("Animal Rate::", animal[ani3]);
                Log.e("Animal Rate:: ", String.valueOf(ani3_rate));
                animal_rate[ani3] = 0;

                markFaceExist(); }


        }

        if (case_code == CASE_FROM_GALLERY){

            Uri uri = Uri.parse(getIntent().getStringExtra("bitmap"));
            Uri mPhotoUri = Uri.parse(getRealPathFromURI(uri));

            ExifInterface exif = null;
            try {
                exif = new ExifInterface(mPhotoUri.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            InputStream in = null;
            try {
                in = getContentResolver().openInputStream(uri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            bitmap = rotateBitmap(bitmap, orientation);

            //갤러리 이미지 Mat
            mat = new Mat();
            Utils.bitmapToMat(bitmap, mat);

            //갤러리 이미지 Mat 모델 돌리기
            inputMat = new Mat();
            Mat out=new Mat();
            // Imgproc.cvtColor(mRgba,mRgba,Imgproc.COLOR_RGBA2RGB);
            out = facialDetection.recognizeImage(mat, animalImg[animalIdx], animalROI[animalIdx],1);

            if(inputMat.empty()){
                Log.e(TAG, "얼굴 검출이 되지 않음!");
//            isFaceRecognized = 0;
//            msg.what = ETC;
            }
            else{
                isFaceRecognized = 1;
                int result = doInference(inputMat);
                Log.e(TAG,"Result After DoInference = " + result );
                Log.e(TAG, "crop 후 input image 사이즈:" + inputMat.width() +" * " +inputMat.height());

                /** Category 추가 방법
                 * 1. celebrity 이름의 배열 수정 : 결과 없음은 마지막 인덱스로 지정
                 * 2. 바로 아래의 else if 문 추가
                 * */

                //효림은 보아라 !!!!! 이 밑에 result 값이 그 결과 output 카테고리 !
//                if(result == 0.0){  msg.what = ANI0 ; }
//                else if(result == 1.0){ msg.what = ANI1 ;}
//                else if(result == 2.0){ msg.what = ANI2 ;}
//                else if(result == 3.0){ msg.what = ANI3 ;}
//                else if(result == 4.0){ msg.what = ANI4 ;}
//                else if(result == 5.0){ msg.what = ANI5 ;}
//                else if(result == 6.0){ msg.what = ANI6 ;}
//                else if(result == 7.0){ msg.what = ANI7 ;}

            }

            // Display out Mat image
            imageview.setImageBitmap(bitmap);

        }

        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 갤러리로 이동
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                case_code = CASE_FROM_GALLERY;
                startActivityForResult(intent, 1);
            }
        });


        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultActivity.this, LoadCameraActivity.class);
                startActivity(intent);
                finish();
            }
        });
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

//        celebrity_rate = new float[16];
//
//        for (int i = 0 ; i < 16; i++){
//            celebrity_rate[i] = output[0][i];
//        }
//
//        // 닮은꼴 동물 찾기
//
//        // 1) 배열 선언 및 초기화
//        for (int i = 0 ; i < 8; i++){ animal_rate[i] = 0.0f; }
//
//        // 2) 동물별 비율 구하기
//        for (int i = 0 ; i< 16; i++){
//            animal_rate[i/2] += output[0][i];
//        }
//
//        DecimalFormat df = new DecimalFormat("#.##");
////        System.out.println("비율확인 HyoRim::========================================================");
////        System.out.print("HyoRim:: ");
////        for (int i = 0 ; i < 8; i++){
////            System.out.print( animal[i] + ": " + df.format(animal_rate[i] )+ " ");
////        }
////        System.out.println();
//
//
//        // 3) 가장 닮은 동물 찾기
//        int maxIdx = 0;
//        float maxVal = 0;
//        for (int i = 0 ; i < 8; i++){
//            if (animal_rate[i] > maxVal){
//                maxVal = animal_rate[i];
//                maxIdx = i;
//            }
//        }
//
//        // 닮은꼴 비율이 30프로가 넘을때만 return 할 변수의 값을 갱신
//        // 그렇지 않다면 이전에 검출됐던 결과중 마지막으로 30을 넘은 경우를 return
//        if(maxVal >= 0.3) {
//            temp_idx = maxIdx;
//            System.out.println("HyoRim:: "+  animal[temp_idx] + " (" + df.format(maxVal * 100) + "%)");
//        }else {
//            System.out.println("HyoRim:: " +  animal[temp_idx] + " X 30");
//        }
//
//        return temp_idx;
          return 1;
    }

    private int find_max_idx(){

        float max = 0;
        int maxIdx = 0;

        for (int i = 0 ; i < animal_rate.length; i++){
            if (animal_rate[i] > max) {
                max = animal_rate[i];
                maxIdx = i;
            }
        }

        return maxIdx;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                Uri mPhotoUri = Uri.parse(getRealPathFromURI(uri));

                ExifInterface exif = null;
                try {
                    exif = new ExifInterface(mPhotoUri.getPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED);

                InputStream in = null;
                try {
                    in = getContentResolver().openInputStream(uri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                bitmap = rotateBitmap(bitmap, orientation);

                //갤러리 이미지 Mat
                Utils.bitmapToMat(bitmap, mat);
                //갤러리 이미지 Mat 모델 돌리기
                inputMat = new Mat();
                Mat out=new Mat();
                // Imgproc.cvtColor(mRgba,mRgba,Imgproc.COLOR_RGBA2RGB);
                out = facialDetection.recognizeImage(mat, animalImg[animalIdx], animalROI[animalIdx],1);

                if(inputMat.empty()){
                    Log.e(TAG, "얼굴 검출이 되지 않음!");
//            isFaceRecognized = 0;
//            msg.what = ETC;
                }
                else{
                    isFaceRecognized = 1;
                    int result = doInference(inputMat);
                    Log.e(TAG,"Result After DoInference = " + result );
                    Log.e(TAG, "crop 후 input image 사이즈:" + inputMat.width() +" * " +inputMat.height());

                    /** Category 추가 방법
                     * 1. celebrity 이름의 배열 수정 : 결과 없음은 마지막 인덱스로 지정
                     * 2. 바로 아래의 else if 문 추가
                     * */

                    //효림은 보아라 !!!!! 이 밑에 result 값이 그 결과 output 카테고리 !
//                if(result == 0.0){  msg.what = ANI0 ; }
//                else if(result == 1.0){ msg.what = ANI1 ;}
//                else if(result == 2.0){ msg.what = ANI2 ;}
//                else if(result == 3.0){ msg.what = ANI3 ;}
//                else if(result == 4.0){ msg.what = ANI4 ;}
//                else if(result == 5.0){ msg.what = ANI5 ;}
//                else if(result == 6.0){ msg.what = ANI6 ;}
//                else if(result == 7.0){ msg.what = ANI7 ;}

                }
                imageview.setImageBitmap(bitmap);
            }
        }
    }

    private void markAsNoFace(){

        Log.e("isFaceRecognized", "얼굴 없음");
        animal_result_TV.setText("얼굴을 찾지 못했어요:(");
        aniTV1.setVisibility(View.INVISIBLE);
        aniTV2.setVisibility(View.INVISIBLE);
        aniTV3.setVisibility(View.INVISIBLE);
        aniPB1.setVisibility(View.INVISIBLE);
        aniPB2.setVisibility(View.INVISIBLE);
        aniPB3.setVisibility(View.INVISIBLE);
        aniPERC1.setVisibility(View.INVISIBLE);
        aniPERC2.setVisibility(View.INVISIBLE);
        aniPERC3.setVisibility(View.INVISIBLE);

    }

    private void markFaceExist(){

        Log.e("isFaceRecognized", "얼굴 있음");

        animal_result_TV.setText(animal_result[ani1]);
        aniTV1.setText(animal[ani1]);
        aniTV2.setText(animal[ani2]);
        aniTV3.setText(animal[ani3]);
        aniPB1.setProgress(ani1_rate);
        aniPB2.setProgress(ani2_rate);
        aniPB3.setProgress(ani3_rate);
        aniPERC1.setText(ani1_rate + "%");
        aniPERC2.setText(ani2_rate + "%");
        aniPERC3.setText(ani3_rate + "%");
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx); cursor.close();
        }
        return result;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }
}
