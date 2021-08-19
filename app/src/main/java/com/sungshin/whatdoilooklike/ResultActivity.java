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
    private Button galleryBtn;
    private Mat mat;
    private int case_code;
    static final int CASE_FROM_CAMERA = 0;
    static final int CASE_FROM_GALLERY = 1;
    private static final String TAG = "ResultActivity:";
    private float animal_rate[];
    private float celebrity_rate[];
    private String celebrity[];
    private String animal[];

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        imageview = (ImageView)findViewById(R.id.imageView);
        galleryBtn = (Button) findViewById(R.id.button);

        animal_rate = new float[8];
        celebrity_rate = new float[16];
        animal = new String[8];
        celebrity = new String[16];

        Intent intent = getIntent();
        case_code = intent.getIntExtra("case_code", 0);
        animal_rate = intent.getFloatArrayExtra("animal_rate");
        celebrity_rate = intent.getFloatArrayExtra("celebrity_rate");
        animal = intent.getStringArrayExtra("animal");
        celebrity = intent.getStringArrayExtra("celebrity");

        int ani1 = find_max_idx();
        float ani1_rate = animal_rate[ani1];
        Log.e("Animal Rate::", animal[ani1]);
        Log.e("Animal Rate:: ", String.valueOf(ani1_rate * 100));
        animal_rate[ani1] = 0;

        int ani2 = find_max_idx();
        float ani2_rate =  animal_rate[ani2];
        Log.e("Animal Rate::", animal[ani2]);
        Log.e("Animal Rate:: ", String.valueOf(ani2_rate * 100));
        animal_rate[ani2] = 0;


        int ani3 = find_max_idx();
        float ani3_rate = animal_rate[ani3];
        Log.e("Animal Rate::", animal[ani3]);
        Log.e("Animal Rate:: ", String.valueOf(ani3_rate * 100));
        animal_rate[ani3] = 0;


        if (case_code == CASE_FROM_CAMERA){

            Log.e(TAG, "Case_from_Camera");

            // get Data from Intent
            long addr = intent.getLongExtra("Image", 0);
            Mat tempImg = new Mat(addr);
            Mat img = tempImg.clone();

            // Convert Mat to Bitmap to display 'captured Image' on ImageView
            Bitmap bitmap;
            bitmap = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, bitmap);
            imageview.setImageBitmap(bitmap);


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
