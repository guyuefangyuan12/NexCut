package com.example.opencv.image;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import org.opencv.core.Mat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.widget.Toast;

public class GCode {
    private static final int MAX_POWER = 255;

    // 生成G代码（通过线密度自动计算物理尺寸）
    // 生成G代码（通过线密度自动计算物理尺寸）
    public static String generateGCode(Mat image, double rho) {
        int laserPower = 20;
        StringBuilder gcode = new StringBuilder();
        gcode.append("G0 X0 Y0 F1000\n");
        gcode.append("M4 S0\n");

        Bitmap bitmap = ImageProcessor.matToBitmap(image);
        Bitmap grayImageBit = ImageProcessor.toGrayscale(bitmap);
        Mat grayImage = ImageProcessor.bitmapToMat(grayImageBit);
        int rows = grayImage.rows();
        int cols = grayImage.cols();

        // 计算物理尺寸
        double height_mm = rows / rho;         // 物理高度
        double width_mm = (cols * height_mm) / rows; // 保持宽高比
        double pixelWidth = width_mm / cols;   // X轴每像素对应的毫米数
        double yStep = 1.0 / rho;             // Y轴步进量（毫米）

        // **调整 Y 轴方向，翻转 Y 轴**
        for (int yPixel = 0; yPixel < rows; yPixel++) {
            double currentY = height_mm - (yPixel / rho); // 反转 Y 轴方向

            boolean isEngraving = false;
            double xStart = -1;

            for (int x = 0; x < cols; x++) {
                double grayValue = grayImage.get(yPixel, x)[0];
                boolean shouldEngrave = grayValue < 128;

                if (shouldEngrave) {
                    if (!isEngraving) {
                        xStart = x * pixelWidth;
                        gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart, currentY));
                        isEngraving = true;
                    }
                } else {
                    if (isEngraving) {
                        double xEnd = (x - 1) * pixelWidth;
                        gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd, currentY, laserPower));
                        isEngraving = false;
                    }
                }
            }

            if (isEngraving) {
                double xEnd = (cols - 1) * pixelWidth;
                gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd, currentY, laserPower));
            }
        }

        gcode.append("M5\n");
        grayImage.release();
        return gcode.toString();
    }

    // 保存方法保持不变
    public static void saveGCodeToFile(String gcode, Context context, String fileName) {
        try {
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = "默认";
            }
            fileName = fileName.replaceAll("[/\\\\:*?\"<>|]", "");
            if (!fileName.endsWith(".nc")) {
                fileName += ".nc";
            }

            File file = new File(context.getExternalFilesDir(null), fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(gcode.getBytes());
            fos.close();

            showSuccessDialog(context, file.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "文件保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    private static void showSuccessDialog(Context context, String filePath) {
        new AlertDialog.Builder(context)
                .setTitle("保存成功")
                .setMessage("GCode 文件已生成：\n" + filePath)
                .setPositiveButton("确定", (dialog, which) -> dialog.dismiss())
                .show();
    }
}