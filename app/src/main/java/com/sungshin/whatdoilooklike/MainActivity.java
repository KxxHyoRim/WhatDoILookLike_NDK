package com.sungshin.whatdoilooklike;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ImageButton imgBtn_camera;
    private ImageButton imgBtn_gallery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgBtn_camera = findViewById(R.id.imgBtn_camera);       // 지금 사진찍기
        imgBtn_gallery = findViewById(R.id.imgBtn_gallery);     // 갤러리로 이동

        imgBtn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LoadCameraActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                Log.e("MainActivity","Pressed");
            }
        });

        imgBtn_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 갤러리로 이동
            }
        });




    }

}
