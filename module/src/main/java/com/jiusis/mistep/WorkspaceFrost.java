package com.jiusis.mistep;

import android.graphics.Bitmap;

/** One screen-aligned material shared by the header and every side cell. No colour overlay. */
final class WorkspaceFrost {
    static Bitmap finish(Bitmap source) {
        if (source == null || source.isRecycled()) return source;
        int w = source.getWidth(), h = source.getHeight();
        Bitmap softer = HyperOsStyle.blurredBitmap(source, w, h);
        if (softer != null && softer != source) {
            source.recycle();
            source = softer;
        }
        int[] pixels = new int[w * h];
        source.getPixels(pixels, 0, w, 0, 0, w, h);
        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i], r = (c >>> 16) & 255, g = (c >>> 8) & 255, b = c & 255;
            int grey = (r * 54 + g * 183 + b * 19) >> 8;
            int hash = (i ^ (i >>> 16)) * 0x45d9f3b;
            hash = (hash ^ (hash >>> 16)) * 0x45d9f3b;
            int noise = ((hash ^ (hash >>> 16)) >>> 28) - 8;
            noise = Math.round(noise * .28f);
            r = clamp((r * 93 + grey * 7) / 100 + noise);
            g = clamp((g * 93 + grey * 7) / 100 + noise);
            b = clamp((b * 93 + grey * 7) / 100 + noise);
            pixels[i] = (c & 0xff000000) | (r << 16) | (g << 8) | b;
        }
        Bitmap result = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888);
        source.recycle();
        return result;
    }

    private static int clamp(int n) {
        return Math.max(0, Math.min(255, n));
    }
}
