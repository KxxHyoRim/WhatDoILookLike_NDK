package com.sungshin.whatdoilooklike;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class facialDetection {
    // define Interpreter
    private Interpreter interpreter;
    // now define input size and pixel size
    private int INPUT_SIZE = 150;
    private int PIXEL_SIZE=1;
    // it is use to divide image by 255 to scale it from 0-1
    private float IMAGE_STD=255.0f;
    private float IMAGE_MEAN=0;
    // it is used to initial GPU on your app
    private GpuDelegate gpuDelegate=null;

    // define height and weight
    private  int height=0;
    private int width=0;
    private CascadeClassifier cascadeClassifier;

    public int animalIdx = 0;
    private Mat animalImg = new Mat();

    public native void crop(Rect rect, long matAddrInput, long nativeObjAddr);

//    public void bmp2Mat() {
//        Drawable vectorDrawable = VectorDrawableCompat.create(facialDetection.this.get(), R.drawable.logo,  getContext().getTheme());
//        Bitmap myLogo = ((BitmapDrawable) vectorDrawable).getBitmap();
//
//        Bitmap tmpBmp = BitmapFactory.decodeResource(Resources.getSystem(), R.drawable.cat);
//        if (tmpBmp != null) {
//            animalImg[0] = new Mat();
//            Utils.bitmapToMat(tmpBmp, animalImg[0]);
//        }
//
//        else {
//            Log.e("FacialDetector","bmp2Mat Failed");
//        }
//
//        tmpBmp = BitmapFactory.decodeResource(Resources.getSystem(), R.drawable.dog);
//        animalImg[1] = new Mat();
//        Utils.bitmapToMat(tmpBmp, animalImg[1]);
//
//        tmpBmp = BitmapFactory.decodeResource(Resources.getSystem(), R.drawable.rabbit);
//        animalImg[2] = new Mat();
//        Utils.bitmapToMat(tmpBmp, animalImg[2]);
//
//        tmpBmp = BitmapFactory.decodeResource(Resources.getSystem(), R.drawable.fox);
//        animalImg[3] = new Mat();
//        Utils.bitmapToMat(tmpBmp, animalImg[3]);
//
//        tmpBmp = BitmapFactory.decodeResource(Resources.getSystem(), R.drawable.dino);
//        animalImg[4] = new Mat();
//        Utils.bitmapToMat(tmpBmp, animalImg[4]);
//
//        tmpBmp = BitmapFactory.decodeResource(Resources.getSystem(), R.drawable.bear);
//        animalImg[5] = new Mat();
//        Utils.bitmapToMat(tmpBmp, animalImg[5]);
//
//        tmpBmp = BitmapFactory.decodeResource(Resources.getSystem(), R.drawable.horse);
//        animalImg[6] = new Mat();
//        Utils.bitmapToMat(tmpBmp, animalImg[6]);
//
//        tmpBmp = BitmapFactory.decodeResource(Resources.getSystem(), R.drawable.quokka);
//        animalImg[7] = new Mat();
//        Utils.bitmapToMat(tmpBmp, animalImg[7]);
//    }

    // on start
    facialDetection(AssetManager assetManager, Context context, String modelPath, int inputSize) throws IOException{
        Log.e("FacialDetector","facialDetection");
        //INPUT_SIZE=inputSize;

        // define GPU and number of thread to Interpreter
        Interpreter.Options options=new Interpreter.Options();
        gpuDelegate=new GpuDelegate();
        options.addDelegate(gpuDelegate);
        options.setNumThreads(4); // change number of thread according to your phone

        // load CNN model
        interpreter=new Interpreter(loadModelFile(assetManager,modelPath),options);
        Log.e("FacialDetector","CNN model is loaded");
        // Now load haar cascade classifier

        try{
            Log.e("FacialDetector","Start");
            // define input stream
            InputStream is=context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            // define folder path
            File cascadeDir=context.getDir("cascade",Context.MODE_PRIVATE);
            File mCascadeFile=new File(cascadeDir,"haarcascade_frontalface_alt.xml");
            // define output stream
            FileOutputStream os=new FileOutputStream(mCascadeFile);

            // copy classifier to that folder
            byte[] buffer =new byte[4096];
            int byteRead;
            while ((byteRead=is.read(buffer)) !=-1){
                os.write(buffer,0,byteRead);
            }
            // close input and output stream
            is.close();
            os.close();
            // define cascade classifier
            cascadeClassifier=new CascadeClassifier(mCascadeFile.getAbsolutePath());

            Log.e("FacialDetector","Classifier is loaded");

        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    // Creata a new function input as Mat and output is also Mat format
    public Mat recognizeImage(Mat mat_image, Mat animalImg, int[][] animalROI) {
        Log.e("FacialDetector","recognizeImage");
        // mat_image is not properly align it is 90 degree off
        // rotate mat_image by 90 degree

        //Log.e("FacialDetector","188");
        // loop through each face in faceArray

//        for(int i=0;i<faceArray.length;i++){
//            // if you want to draw face on frame
//            //                image      // starting point  ending point        green Color         thickness
//            Imgproc.rectangle(mat_image,faceArray[i].tl(),faceArray[i].br(),new Scalar(0,255,0,255),2);
//            // Crop face from mat_imag
//            //                  starting x coordinate    y-coordinate            width of face
//            Rect roi = new Rect((int)faceArray[i].tl().x, (int)faceArray[i].tl().y, (int)(faceArray[i].br().x-(int)faceArray[i].tl().x), (int)(faceArray[i].br().y-(int)faceArray[i].tl().y));
//            // this was important for face cropping so check
//            // cropped grayscale image
//            Mat cropped=new Mat(grayscaleImage,roi);
//            // cropped rgba image
//            Mat cropped_rgba=new Mat(mat_image,roi);
//
//            // now convert cropped gray scale face image to bitmap
//            Bitmap bitmap=null;
//            bitmap=Bitmap.createBitmap(cropped.cols(),cropped.rows(),Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(cropped,bitmap);
//            // define height and width of cropped bitmap
//            int c_height=bitmap.getHeight();
//            int c_width=bitmap.getWidth();
//            // now convert cropped grayscale bitmap to buffer byte
//            // before that scale it to (96,96)
//            // input size of interpreter is 96
//            Bitmap scaledBitmap=Bitmap.createScaledBitmap(bitmap,INPUT_SIZE,INPUT_SIZE,true);
//            ByteBuffer byteBuffer=convertBitmapToByteBuffer(scaledBitmap);
//            // now define output
//            float[][] result=new float[1][136];// total 30 coordinate
//            // predict
//            interpreter.run(byteBuffer,result);
//
//            //Before watching this video please watch Facial Landmark Detection Android App Using TFLite(GPU) and OpenCV: Predict On Frame Part 3
//            // You will end up with this code
//            // In this video, we will draw circle around key point
//
//            // height,width of cropped face is different from input size of Interpreter
//            // we have to scale each key point co-ordinate for cropped face
//            float x_scale=((float)c_width)/((float)INPUT_SIZE);
//            float y_scale=((float)c_height)/((float)INPUT_SIZE); // or you can divide it with INPUT_SIZE
//
//            List<Point> left_eye_points = new ArrayList<>();
//            List<Point> right_eye_points = new ArrayList<>();
//            List<Point> mouth_points = new ArrayList<>();
//
//            for (int j = 72; j <= 84; j+=2) {
//                float x_val=(float)Array.get(Array.get(result,0),j);
//                float y_val=(float)Array.get(Array.get(result,0),j+1);
//                left_eye_points.add(new Point(x_val, y_val));
//            }
//
//            List<MatOfPoint> left_eye_list = new ArrayList<>();
//            left_eye_list.add(new MatOfPoint(
//                    left_eye_points.get(0), left_eye_points.get(1),
//                    left_eye_points.get(2), left_eye_points.get(3),
//                    left_eye_points.get(4), left_eye_points.get(5),
//                    left_eye_points.get(6)
//            ));
//
//            for (int j = 84; j <= 96; j+=2) {
//                float x_val=(float)Array.get(Array.get(result,0),j);
//                float y_val=(float)Array.get(Array.get(result,0),j+1);
//                right_eye_points.add(new Point(x_val, y_val));
//            }
//
//            List<MatOfPoint> right_eye_list = new ArrayList<>();
//            right_eye_list.add(new MatOfPoint(
//                    right_eye_points.get(0), right_eye_points.get(1),
//                    right_eye_points.get(2), right_eye_points.get(3),
//                    right_eye_points.get(4), right_eye_points.get(5),
//                    right_eye_points.get(6)
//            ));
//
//            for (int j = 48; j <= 60; j+=2) {
//                float x_val=(float)Array.get(Array.get(result,0),j);
//                float y_val=(float)Array.get(Array.get(result,0),j+1);
//                mouth_points.add(new Point(x_val, y_val));
//            }
//
//            List<MatOfPoint> mouth_list = new ArrayList<>();
//            mouth_list.add(new MatOfPoint(
//                    mouth_points.get(0), mouth_points.get(1),
//                    mouth_points.get(2), mouth_points.get(3),
//                    mouth_points.get(4), mouth_points.get(5),
//                    mouth_points.get(6), mouth_points.get(7),
//                    mouth_points.get(8), mouth_points.get(9),
//                    mouth_points.get(10), mouth_points.get(11),
//                    mouth_points.get(12)
//            ));
//
//            Scalar WHITE = new Scalar(255, 255, 255);
//            int lineType = Imgproc.LINE_8;
//            Mat mat_composed_image = mat_image.clone();
//
//            Imgproc.fillPoly(mat_composed_image, left_eye_list, WHITE, lineType);
//            Imgproc.fillPoly(mat_composed_image, right_eye_list, WHITE, lineType);
//            Imgproc.fillPoly(mat_composed_image, mouth_list, WHITE, lineType);
//            Core.bitwise_and(mat_image, mat_composed_image, mat_composed_image);
//
//            Mat mat_gray_composed_image = mat_composed_image.clone();
//            Imgproc.cvtColor(mat_gray_composed_image, mat_gray_composed_image,
//                    Imgproc.COLOR_BGR2GRAY);
//            List<MatOfPoint> contours = new ArrayList<>();
//            Mat hierarchy = new Mat();
//            Imgproc.findContours(mat_gray_composed_image, contours, hierarchy,
//                    Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//
//            List<Mat> roiList = new ArrayList<>();
//            for (int j = 0; j >= 0; j = (int) hierarchy.get(0, j)[0]) {
//                MatOfPoint matOfPoint = contours.get(j);
//                Rect rect = Imgproc.boundingRect(matOfPoint);
//                Mat partROI = new Mat(mat_image, rect);
//                Imgproc.resize(partROI, partROI, new Size(animalROI[j][2],
//                        animalROI[j][3]));
//                roiList.add(partROI);
//            }
//
//            if (roiList.size() == 3) {
//                Mat roi_left_eye = roiList.get(1);
//                Mat roi_right_eye = roiList.get(2);
//
//                if (Imgproc.boundingRect(contours.get(1)).tl().x >
//                        Imgproc.boundingRect(contours.get(2)).tl().x) {
//                    roi_left_eye = roiList.get(2);
//                    roi_right_eye = roiList.get(1);
//                }
//
//                roiList.set(1, roi_left_eye);
//                roiList.set(2, roi_right_eye);
//            }
//
//            Mat blank_image = new Mat();
//            Imgproc.resize(blank_image, blank_image, new Size(INPUT_SIZE, INPUT_SIZE));
//
//            for (int j = 0; j < 3; j++) {
//                int x = animalROI[j][0];
//                int y = animalROI[j][1];
//                int w = animalROI[j][2];
//                int h = animalROI[j][3];
//                Rect destRect = new Rect(new Point(x, y), new Size(w, h));
////                Mat destROI = blank_image(destRect);
////                roiList.get(j).copyTo(blank_image(destRect));
//            }
//
//            // loop through each key point
//            for (int j=0;j<136;j=j+2){
//                // now define x,y co-ordinate
//                // every even value is x co-ordinate
//                // every odd value is y co-ordinate
//                float x_val=(float)Array.get(Array.get(result,0),j);
//                float y_val=(float)Array.get(Array.get(result,0),j+1);
//
//                // draw circle around x,y
//                // draw on cropped_rgb not on cropped
//                //              input/output     center                                  radius        color                fill circle
//                Imgproc.circle(cropped_rgba,new Point(x_val*x_scale,y_val*y_scale),3,new Scalar(0,255,0,255),-1);
//
//            }
//            // replace cropped_rgba with original face on mat_image
//            cropped_rgba.copyTo(new Mat(mat_image,roi));
//            // select device and run
//            // If you want me to train on more key point or increase accuracy of model please comment below
//            // Thank you for watching this series of tutorial
//            //If you gets any error comment below
//            // whole code  github link will be in the description
//
//        }
//
//
//        // but returned mat_image should be same as passing mat
//        // rotate back it -90 degree
//        Core.flip(mat_image.t(),mat_image,0);
//        return mat_image;
        Mat a=mat_image.t();
        Core.flip(a,mat_image,1);
        a.release();

        // do all process here
        // face detection
        // Convert mat_image to grayscale image
        Mat grayscaleImage=new Mat();
        Imgproc.cvtColor(mat_image,grayscaleImage, Imgproc.COLOR_RGBA2GRAY);
        // define height, width of grayscaleImage
        int height =grayscaleImage.height();
        int width=grayscaleImage.width();

        // define minimum height of face in original frame below this height no face will detected
        int absoluteFaceSize=(int) (height*0.1); // you can change this number to get better result

        // check if cascadeClassifier is loaded or not
        // define MatOfRect of faces
        MatOfRect faces=new MatOfRect();

        if(cascadeClassifier !=null){
            // detect face                        input       output
            cascadeClassifier.detectMultiScale(grayscaleImage,faces,1.1,2,2,
                    new Size(absoluteFaceSize,absoluteFaceSize),new Size());
            //      minimum size
        }

        // create faceArray
        Rect[] faceArray=faces.toArray();
        // loop through each face in faceArray

        for(int i=0;i<faceArray.length;i++){
            // if you want to draw face on frame
            //                image      // starting point  ending point        green Color         thickness
            Imgproc.rectangle(mat_image,faceArray[i].tl(),faceArray[i].br(),new Scalar(0,255,0,255),2);
            int x1 = (int) faceArray[i].tl().x;
            int y1 = (int) faceArray[i].tl().y;

            int x2 = (int) faceArray[i].br().x;
            int y2 = (int) faceArray[i].br().y;

            if(x1-20>=0)
                x1 = x1-20;
            if(y1-20>=0)
                y1 = y1-20;
            if(y2+20<=height)
                y2=y2+20;
            if(x2+20<=width)
                x2=x2+20;
            int w1 = x2-x1;
            int h1 = y2-y1;
            Imgproc.rectangle(mat_image,faceArray[i].tl(),faceArray[i].br(),new Scalar(0,255,0,255),2);

            Rect face_roi = new Rect(x1, y1, w1, h1);
            //crop(face_roi,mat_image.getNativeObjAddr(),LoadCameraActivity.inputMat.getNativeObjAddr());
            // this was important for face cropping so check
            // cropped grayscale image
            // cropped rgba image
            Mat cropped_rgba=new Mat(mat_image,face_roi);
            LoadCameraActivity.inputMat = cropped_rgba.clone();
            //Log.e("FacialDetector","370");

            Mat resizeImage = new Mat();
            Imgproc.resize(cropped_rgba, resizeImage, new Size(150,150),0,0, Imgproc.INTER_CUBIC);

            Bitmap bitmap=null;
            bitmap=Bitmap.createBitmap(cropped_rgba.cols(),cropped_rgba.rows(),Bitmap.Config.ARGB_8888);
            Log.e("FacialDetector2",cropped_rgba.cols() +"  " +cropped_rgba.rows());
            Utils.matToBitmap(cropped_rgba,bitmap);

            int c_height=bitmap.getHeight();
            int c_width=bitmap.getWidth();
            //Log.e("FacialDetector","379");
            // now convert cropped grayscale bitmap to buffer byte
            // before that scale it to (96,96)
            // input size of interpreter is 96
            // 크롭 -> 확대 -> 모델 넣기
            Bitmap scaledBitmap=Bitmap.createScaledBitmap(bitmap,INPUT_SIZE,INPUT_SIZE,false);
            Log.e("FacialDetector","h" + scaledBitmap.getHeight() + ", w" + scaledBitmap.getWidth());
            ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);
            // now define output
            float[][] result=new float[1][136];// total 30 coordinate
            // predict

            interpreter.run(byteBuffer,result);
            float x_scale=((float)c_width)/((float)INPUT_SIZE);
            float y_scale=((float)c_height)/((float)INPUT_SIZE); // or you can divide it with INPUT_SIZE
            for (int j=72;j<84;j=j+2){
                // now define x,y co-ordinate
                // every even value is x co-ordinate
                // every odd value is y co-ordinate
                float x_val=(float)Array.get(Array.get(result,0),j);
                float y_val=(float)Array.get(Array.get(result,0),j+1);
                //Log.e("FacialDetector", "x" + String.valueOf(x_val));
                //Log.e("FacialDetector", "y" + String.valueOf(y_val));

                // draw circle around x,y
                // draw on cropped_rgb not on cropped
                //              input/output     center                                  radius        color                fill circle

                Imgproc.circle(cropped_rgba,new Point((x_val*1.07-4)*x_scale,(y_val-6)*y_scale),3,new Scalar(0,255,0,255),-1);
                //Imgproc.circle(cropped_rgba,new Point(x_val*x_scale,y_val*y_scale),3,new Scalar(0,255,0,255),-1);
                //Imgproc.circle(cropped_rgba,new Point((x_val)*x_scale,(y_val)*y_scale),3,new Scalar(0,255,0,255),-1);

            }
            for (int j=84;j<96;j=j+2){
                // now define x,y co-ordinate
                // every even value is x co-ordinate
                // every odd value is y co-ordinate
                float x_val=(float)Array.get(Array.get(result,0),j);
                float y_val=(float)Array.get(Array.get(result,0),j+1);
                //Log.e("FacialDetector", "x" + String.valueOf(x_val));
                //Log.e("FacialDetector", "y" + String.valueOf(y_val));

                // draw circle around x,y
                // draw on cropped_rgb not on cropped
                //              input/output     center                                  radius        color                fill circle

                Imgproc.circle(cropped_rgba,new Point((x_val*1.07+1.5)*x_scale,(y_val-6)*y_scale),3,new Scalar(0,255,0,255),-1);
                //Imgproc.circle(cropped_rgba,new Point(x_val*x_scale,y_val*y_scale),3,new Scalar(0,255,0,255),-1);
                //Imgproc.circle(cropped_rgba,new Point((x_val)*x_scale,(y_val)*y_scale),3,new Scalar(0,255,0,255),-1);

            }
            for (int j=96;j<136;j=j+2){
                // now define x,y co-ordinate
                // every even value is x co-ordinate
                // every odd value is y co-ordinate
                float x_val=(float)Array.get(Array.get(result,0),j);
                float y_val=(float)Array.get(Array.get(result,0),j+1);
                //Log.e("FacialDetector", "x" + String.valueOf(x_val));
                //Log.e("FacialDetector", "y" + String.valueOf(y_val));

                // draw circle around x,y
                // draw on cropped_rgb not on cropped
                //              input/output     center                                  radius        color                fill circle

                Imgproc.circle(cropped_rgba,new Point((x_val)*x_scale,(y_val+2.5)*y_scale),3,new Scalar(0,255,0,255),-1);
                //Imgproc.circle(cropped_rgba,new Point(x_val*x_scale,y_val*y_scale),3,new Scalar(0,255,0,255),-1);
                //Imgproc.circle(cropped_rgba,new Point((x_val)*x_scale,(y_val)*y_scale),3,new Scalar(0,255,0,255),-1);

            }

            //Imgproc.resize(resizeImage, cropped_rgba, new Size(c_width, c_height), 0, 0, Imgproc.INTER_CUBIC);
            cropped_rgba.copyTo(new Mat(mat_image,face_roi));

        }


        // but returned mat_image should be same as passing mat
        // rotate back it -90 degree
        Mat b = mat_image.t();
        Core.flip(b,mat_image,0);
        b.release();
        return mat_image;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap scaledBitmap) {
        ByteBuffer byteBuffer;
        int inputSize=INPUT_SIZE;// 96
        int quant=1;
        if(quant==0){
            byteBuffer=ByteBuffer.allocateDirect(4*1*inputSize*inputSize);
        }
        else{
            byteBuffer=ByteBuffer.allocateDirect(4*1*inputSize*inputSize*3);
        }
        byteBuffer.order(ByteOrder.nativeOrder());
        int pixel=0;
        int [] intValues=new int [inputSize*inputSize];
        scaledBitmap.getPixels(intValues,0,scaledBitmap.getWidth(),0,0,scaledBitmap.getWidth(),scaledBitmap.getHeight());

        for (int i=0;i<inputSize;++i){
            for(int j=0;j<inputSize;++j){
                final int val= intValues[pixel++];
                byteBuffer.putFloat((((val>>16) & 0xFF))/255.0f);
                byteBuffer.putFloat((((val>>8) & 0xFF))/255.0f);
                byteBuffer.putFloat(((val & 0xFF))/255.0f);
            }
        }
        return  byteBuffer;

    }

    //Log.e("FacialDetector","474 - face");


    // scaledBitmap.getPixels(intValues,0,scaledBitmap.getWidth(),0,0,scaledBitmap.getWidth(),scaledBitmap.getHeight());

    //Log.e("FacialDetector","448");
/*
        for (int i=0;i<inputSize;++i){
            for(int j=0;j<inputSize;++j){
                Log.e("FacialDetector","452");
                final int val= intValues[pixel++];
                Log.e("FacialDetector","454");
                Log.e("FacialDetector",pixel + " 455");
                byteBuffer.putFloat((float) val/255.0f);// scaling it from 0-255 to 0-1
                Log.e("FacialDetector","460 " + (val/255.0f));
                Log.e("FacialDetector","461 " + val);
            }
        }*/

    // now call this function in CameraActivity

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException{
        // description of file
        AssetFileDescriptor assetFileDescriptor=assetManager.openFd(modelPath);
        FileInputStream inputStream=new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startOffset=assetFileDescriptor.getStartOffset();
        long declaredLength=assetFileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength);
    }

}