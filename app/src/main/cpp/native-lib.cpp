#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
#ifdef __cplusplus
extern "C" {
#endif

#include <android/log.h>

#ifdef __cplusplus
}
#endif

#define LOG_TAG

using namespace std;
using namespace cv;

float resize(Mat img_src, Mat &img_resize, int resize_width){

    float scale = resize_width / (float)img_src.cols ;
    if (img_src.cols > resize_width) {
        int new_height = cvRound(img_src.rows * scale);
        resize(img_src, img_resize, Size(resize_width, new_height));
    }
    else {
        img_resize = img_src;
    }
    return scale;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_sungshin_whatdoilooklike_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_sungshin_whatdoilooklike_LoadCameraActivity_loadCascade(JNIEnv *env, jobject thiz, jstring cascade_file_name) {
    // TODO: implement loadCascade()
    const char *nativeFileNameString = env->GetStringUTFChars(cascade_file_name, 0);

    string baseDir("/storage/emulated/0/");
    baseDir.append(nativeFileNameString);
    const char *pathDir = baseDir.c_str();

    jlong ret = 0;
    ret = (jlong) new CascadeClassifier(pathDir);
    if (((CascadeClassifier *) ret)->empty()) {
        //__android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ","CascadeClassifier로 로딩 실패  %s", nativeFileNameString);
    }
    else
       //__android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ","CascadeClassifier로 로딩 성공 %s", nativeFileNameString);


    env->ReleaseStringUTFChars(cascade_file_name, nativeFileNameString);

    return ret;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_sungshin_whatdoilooklike_LoadCameraActivity_detect(JNIEnv *env, jobject thiz,
                                                            jlong cascade_classifier_face,
                                                            jlong mat_addr_input, jlong mat_addr_result,
                                                            jlong mat_addr_crop) {
    // TODO: implement detect()
     Mat &img_input = *(Mat *) mat_addr_input;
    Mat &img_result = *(Mat *) mat_addr_result;
    Mat &img_crop = *(Mat *) mat_addr_crop;

    img_result = img_input.clone();
    //img_crop = img_input.clone();

    std::vector<Rect> faces;
    Mat img_gray;

    cvtColor(img_input, img_gray, COLOR_BGR2GRAY);
    equalizeHist(img_gray, img_gray);

    Mat img_resize;
    float resizeRatio = resize(img_gray, img_resize, 640);

    //-- Detect faces
    ((CascadeClassifier *) cascade_classifier_face)->detectMultiScale( img_resize, faces, 1.1, 3, 0|CASCADE_SCALE_IMAGE, Size(30, 30) );


    //__android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ",(char *) "face %d found ", faces.size());

    for (int i = 0; i < faces.size(); i++) {
        double real_facesize_x = faces[i].x / resizeRatio;
        double real_facesize_y = faces[i].y / resizeRatio;
        double real_facesize_width = faces[i].width / resizeRatio;
        double real_facesize_height = faces[i].height / resizeRatio *1.2;
        Rect rect(real_facesize_x, real_facesize_y, real_facesize_width, real_facesize_height);
        Rect bounds(0,0,img_input.cols,img_input.rows);

        //Point center( real_face
        // size_x + real_facesize_width / 2, real_facesize_y + real_facesize_height/2);
        //ellipse(img_result, center, Size( real_facesize_width / 2, real_facesize_height / 2), 0, 0, 360, Scalar(255, 0, 255), 30, 8, 0);
        //얼굴 사각형 처리
        rectangle(img_result,Point(real_facesize_x,real_facesize_y),Point(real_facesize_x+real_facesize_width,real_facesize_y+real_facesize_height),Scalar(255, 0, 255),2,8,0);
        img_crop = img_input(rect & bounds);
    }

}