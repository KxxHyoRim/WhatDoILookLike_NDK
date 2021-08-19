package com.sungshin.whatdoilooklike;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

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
