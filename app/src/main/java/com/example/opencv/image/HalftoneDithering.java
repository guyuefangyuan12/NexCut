package com.example.opencv.image;
import android.graphics.Bitmap;
import android.graphics.Color;

public class HalftoneDithering {
    public static Bitmap applyJJNDithering(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[][] image = new int[height][width];

        // 提取灰度值
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = bitmap.getPixel(x, y);
                int gray = (Color.red(color) + Color.green(color) + Color.blue(color)) / 3;
                image[y][x] = gray;
            }
        }

        int[][] ditheredImage = new int[height][width];

        // Jarvis-Judice-Ninke 误差扩散矩阵
        int[][] matrix = {
                {0, 0, 0, 7, 5},
                {3, 5, 7, 5, 3},
                {1, 3, 5, 3, 1}
        };
        int matrixSum = 48;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int oldPixel = image[y][x];
                int newPixel = (oldPixel < 128) ? 0 : 255;
                ditheredImage[y][x] = newPixel;
                int quantError = oldPixel - newPixel;

                // 误差扩散
                for (int dy = 0; dy < 3; dy++) {
                    int ny = y + dy;
                    if (ny >= height) continue;
                    for (int dx = 0; dx < 5; dx++) {
                        int nx = x + dx - 2;
                        if (nx < 0 || nx >= width) continue;

                        int weight = matrix[dy][dx];
                        if (weight == 0) continue;

                        image[ny][nx] = clamp(image[ny][nx] + quantError * weight / matrixSum);
                    }
                }
            }
        }

        // 生成新的 Bitmap
        Bitmap resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = ditheredImage[y][x];
                resultBitmap.setPixel(x, y, Color.rgb(color, color, color));
            }
        }

        return resultBitmap;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
