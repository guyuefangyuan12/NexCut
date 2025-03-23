package com.example.opencv.whiteboard;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.example.opencv.MainActivity;
import com.example.opencv.R;
import com.yinghe.whiteboardlib.fragment.WhiteBoardFragment;

import java.io.IOException;
import java.util.ArrayList;


public class WhiteboardActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE = 2;
    private ArrayList<String> mSelectPath;
    private WhiteBoardFragment whiteBoardFragment;

    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_whiteboard);
        whiteBoardFragment = WhiteBoardFragment.newInstance();
        // 隐藏导航栏和状态栏
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        FragmentTransaction ts = getSupportFragmentManager().beginTransaction();
        ts.add(R.id.fl_main, whiteBoardFragment, "wb").commitNow();

        whiteBoardFragment.setOnFragmentReadyListener(() -> {
            InitialWhiteboard();  // 确保 UI 加载完成后再调用
        });

    }



    private void InitialWhiteboard() {
        if (getIntent().getStringExtra("imageUri") == null) ;
        else {
            imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));

            try {
                whiteBoardFragment.setCurBackgroundByPath(imageUri.getPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    @Override
//    public void onBackPressed() {
////        whiteBoardFragment.setCurBackgroundByPath("/storage/emulated/0/YingHe/sketchPhoto/2016-06-21_035725.png");
////        whiteBoardFragment.setNewBackgroundByPath("/storage/emulated/0/YingHe/sketchPhoto/2016-06-21_035725.png");
////        whiteBoardFragment.setNewBackgroundByPath("/storage/emulated/0/YingHe/sketchPhoto/2016-06-21_041513.png");
////         File f= whiteBoardFragment.saveInOI(WhiteBoardFragment.FILE_PATH, "ss");
////
////        whiteBoardFragment.addPhotoByPath(f.toString());
////        whiteBoardFragment.setCurBackgroundByPath("/storage/emulated/0/YingHe/sketchPhoto/2016-06-21_04151  3.png");
//        super.onBackPressed();
//        Intent intent = new Intent(Intent.ACTION_MAIN);
//        intent.addCategory(Intent.CATEGORY_HOME);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
//    }

    public void mainPage(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}

