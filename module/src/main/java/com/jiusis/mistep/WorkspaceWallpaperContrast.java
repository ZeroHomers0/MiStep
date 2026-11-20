package com.jiusis.mistep;

import android.graphics.Bitmap;

/** Read only the cached wallpaper; application content is never sampled. */
final class WorkspaceWallpaperContrast {
    static int foreground(Bitmap bitmap, int width, int height,
            float left, float top, float right, float bottom) {
        if (bitmap == null || bitmap.isRecycled()) return 0xff17191e;
        float brightness = 0f;
        for (int row = 0; row < 3; row++) for (int column = 0; column < 3; column++) {
            float x = left + (right - left) * (column + .5f) / 3f;
            float y = top + (bottom - top) * (row + .5f) / 3f;
            int pixel = bitmap.getPixel(Math.max(0, Math.min(bitmap.getWidth() - 1,
                    Math.round(x * bitmap.getWidth() / Math.max(1, width)))),
                    Math.max(0, Math.min(bitmap.getHeight() - 1,
                    Math.round(y * bitmap.getHeight() / Math.max(1, height)))));
            brightness += .2126f * ((pixel >> 16) & 255)
                    + .7152f * ((pixel >> 8) & 255) + .0722f * (pixel & 255);
        }
        return brightness / 9f < 145f ? 0xfff5f7fa : 0xff17191e;
    }
}
