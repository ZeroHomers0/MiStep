package io.github.zerohomers0.onesteplite;

import android.graphics.Canvas;
import android.graphics.Paint;

final class WorkspaceDividers {
    static void draw(Canvas canvas, WorkspaceLayout layout, float density, float alpha, Paint paint) {
        paint.setColor(0x40FFFFFF);
        paint.setAlpha(Math.round(64f * alpha));
        paint.setStrokeWidth(Math.max(1f, .75f * density));
        if (layout.tabletLandscape) {
            for (int i = 1; i < layout.cellCount; i++) {
                float x = layout.cellBounds(i)[0] - layout.gap * .5f;
                canvas.drawLine(x, layout.toolHeight, x, layout.topStrip, paint);
            }
        } else {
            float edge = layout.mainOnRight ? layout.sideWidth : layout.mainWidth;
            canvas.drawLine(edge, layout.topStrip, edge, layout.height, paint);
            float left = layout.mainOnRight ? 0 : layout.mainWidth;
            for (int i = 1; i < layout.cellCount; i++) {
                float y = layout.cellBounds(i - 1)[3] + layout.gap * .5f;
                canvas.drawLine(left, y, left + layout.sideWidth, y, paint);
            }
        }
        canvas.drawLine(0, layout.topStrip, layout.width, layout.topStrip, paint);
    }
}
