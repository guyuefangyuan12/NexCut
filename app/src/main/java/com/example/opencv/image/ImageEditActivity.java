package com.example.opencv.image;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;

import org.opencv.android.OpenCVLoader;

import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.opencv.MainActivity;
import com.example.opencv.R;
import com.example.opencv.modbus.ModbusTCPClient;
import com.example.opencv.whiteboard.WhiteboardActivity;

import org.opencv.core.Rect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ImageEditActivity extends AppCompatActivity {
    private static final int REQUEST_GALLERY = 1;
    private static final int REQUEST_CAMERA = 2;
    private ImageView imageView;
    private Bitmap selectedBitmap;
    private PhotoSelector photoSelector;

    private Uri imageUri;

    public static Toolbar toolbar;


    ModbusTCPClient mtcp = ModbusTCPClient.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_imageedit);

//        // **应用启动时复制 .nc 预制文件到可访问目录**
//        GCodeRead.copyNcFilesToStorage(this);

        // 隐藏导航栏和状态栏
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        // 初始化 UI 组件
        imageView = findViewById(R.id.imageView);
//        Button btnSelect = findViewById(R.id.btnSelect);
//        Button btnCapture = findViewById(R.id.btnCapture);
        Button btnGrayscale = findViewById(R.id.btnGrayscale);
        Button btnBlur = findViewById(R.id.btnBlur);
//        Button btnEdge = findViewById(R.id.btnEdge);
        Button btnRotate = findViewById(R.id.btnRotate);
        Button btnHalftone = findViewById(R.id.btnHalftone);
        Button btnCrop = findViewById(R.id.btnCrop);
        Button GCodeGen = findViewById(R.id.GCodeGen);
        Button GCodeRead = findViewById(R.id.readGCode);
        Button Graffiti = findViewById(R.id.graffiti);
        ;
        InitialImage();

        //OpenCV初始化
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        photoSelector = new PhotoSelector();

        // 设置按钮点击事件
//        btnSelect.setOnClickListener(v -> photoSelector.selectFromGallery(ImageEditActivity.this));
//        btnCapture.setOnClickListener(v -> photoSelector.capturePhoto(ImageEditActivity.this, getApplicationContext()));
        btnGrayscale.setOnClickListener(v -> applyGrayscale());
        btnBlur.setOnClickListener(v -> applyBlur());
//        btnEdge.setOnClickListener(v -> applyEdgeDetection());
        btnRotate.setOnClickListener(v -> applyRotation());
        btnHalftone.setOnClickListener(v -> applyHalftone());
        btnCrop.setOnClickListener(v -> applyCrop());
        GCodeGen.setOnClickListener(v -> generateCode());
        GCodeRead.setOnClickListener(v -> readGCode());
        Graffiti.setOnClickListener(v -> graffiti());
        requestAppPermissions();

        //toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
    }


    private void InitialImage() {
        if (getIntent().getStringExtra("imageUri") == null) return;
        else {
            imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));

            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                imageView.setImageBitmap(selectedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 应用灰度处理
     */
    private void applyGrayscale() {
        if (selectedBitmap != null) {
            selectedBitmap = ImageProcessor.toGrayscale(selectedBitmap);
            imageView.setImageBitmap(selectedBitmap);
        }
    }

    /**
     * 应用高斯模糊
     */
    private void applyBlur() {
        if (selectedBitmap != null) {
            selectedBitmap = ImageProcessor.applyGaussianBlur(selectedBitmap, 15);
            imageView.setImageBitmap(selectedBitmap);
        }
    }

    /**
     * 应用 Canny 边缘检测
     */
    private void applyEdgeDetection() {
        if (selectedBitmap != null) {
            selectedBitmap = ImageProcessor.applyCannyEdgeDetection(selectedBitmap, 107, 250);
            imageView.setImageBitmap(selectedBitmap);
        }
    }

    /**
     * 旋转图像 90 度
     */
    private void applyRotation() {
        if (selectedBitmap != null) {
            selectedBitmap = ImageProcessor.rotateImage(selectedBitmap, 90);
            imageView.setImageBitmap(selectedBitmap);
        }
    }

    /**
     * 裁剪图像（默认裁剪中间部分）等待修改为客户可选
     */
    private void applyCrop() {
        if (selectedBitmap != null) {
            Rect cropRect = new Rect(50, 50, selectedBitmap.getWidth() - 100, selectedBitmap.getHeight() - 100);
            selectedBitmap = ImageProcessor.cropImage(selectedBitmap, cropRect);
            imageView.setImageBitmap(selectedBitmap);
        }
    }

    private void generateCode() {
        if (selectedBitmap != null) {
            String gcode = GCode.generateGCode(ImageProcessor.bitmapToMat(selectedBitmap), 96);
            showSaveDialog(this, gcode); // 弹出文件名输入框
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_GALLERY && data != null) {
                // 从相册获取图片
                imageUri = data.getData();
                try {
                    selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    imageView.setImageBitmap(selectedBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == REQUEST_CAMERA) {
                // 从相机拍摄获取图片
                selectedBitmap = BitmapFactory.decodeFile(photoSelector.getCurrentPhotoPath());
                imageView.setImageBitmap(selectedBitmap);
            }
        }
    }

    private void requestAppPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
            }, 101);
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 101);
        }

    }

    public static void showSaveDialog(Context context, String gcode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("输入文件名");

        // 创建输入框
        final EditText input = new EditText(context);
        input.setHint("请输入文件名");
        builder.setView(input);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String fileName = input.getText().toString().trim();
            GCode.saveGCodeToFile(gcode, context, fileName);
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void readGCode() {
        List<File> ncFiles = GCodeRead.getCopiedNcFiles(this);
        if (ncFiles.isEmpty()) {
            Toast.makeText(this, "没有找到已复制的 GCode 文件", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择一个 GCode 文件");

        String[] fileArray = new String[ncFiles.size()];
        for (int i = 0; i < ncFiles.size(); i++) {
            fileArray[i] = ncFiles.get(i).getName();
        }

        builder.setItems(fileArray, (dialog, which) -> {
            File selectedFile = ncFiles.get(which);
            // 创建第二个AlertDialog
            AlertDialog.Builder secondDialogBuilder = new AlertDialog.Builder(ImageEditActivity.this);
            secondDialogBuilder.setMessage("是否选择传输: " + selectedFile.getName());
            secondDialogBuilder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // Looper.prepare();
                                mtcp.FileTransport(1000, selectedFile, ImageEditActivity.this);
                            } catch (ModbusTCPClient.ModbusException e) {
                                //mtcp.onFileFailed(ImageEditActivity.this);
                                //Looper.loop();
                                Log.d("TCPTest", e.getMessage());
                            }
                        }
                    }).start();
                }
            });
            secondDialogBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            // 显示第二个对话框
            secondDialogBuilder.show();
            //Toast.makeText(this, "已选择: " + selectedFile.getName(), Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }

    private void applyHalftone() {
        if (selectedBitmap != null) {
            selectedBitmap = HalftoneDithering.applyJJNDithering(selectedBitmap);
            imageView.setImageBitmap(selectedBitmap);
        }
    }

    // 创建临时图片文件
    private File createImageFile() throws IOException {
        String imageFileName = "PNG_" + System.currentTimeMillis() + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        /* 前缀 */
        /* 后缀 */
        /* 目录 */
        return File.createTempFile(
                imageFileName,  /* 前缀 */
                ".png",         /* 后缀 */
                storageDir      /* 目录 */
        );
    }

    public void graffiti() {

        try {
            File tempFile = createImageFile(); // 创建临时文件
            FileOutputStream out = new FileOutputStream(tempFile);
            selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            imageUri = Uri.fromFile(tempFile);

            // 传递 URI 给下一个 Activity
            Intent intent = new Intent(ImageEditActivity.this, WhiteboardActivity.class);
            intent.putExtra("imageUri", imageUri.toString());
            startActivity(intent);

            //imageView.setImageBitmap(bitmap);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

