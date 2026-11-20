package com.jiusis.mistep;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Native, theme-aware equivalents of Panes' three video placement diagrams. */
final class VideoModeDiagram extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrow=new Path();
    private final int mode,outline,empty,accent;
    VideoModeDiagram(Context context,int mode,int outline,int empty,int accent) {
        super(context);this.mode=mode;this.outline=outline;this.empty=empty;this.accent=accent;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float scale=Math.min(getWidth()/56f,getHeight()/80f);
        int save=canvas.save();canvas.translate((getWidth()-56*scale)/2,(getHeight()-80*scale)/2);
        canvas.scale(scale,scale);
        boolean fullscreen=mode==WorkspaceOptions.VIDEO_FULLSCREEN;
        RectF frame=fullscreen?new RectF(2,28,54,52):new RectF(11,3,45,77);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);paint.setColor(outline);
        canvas.drawRoundRect(frame,4,4,paint);paint.setStyle(Paint.Style.FILL);
        frame.inset(3,3);
        if(fullscreen) {block(canvas,frame,accent);play(canvas,frame,false);}
        else {
            RectF header=new RectF(frame.left,frame.top,frame.right,
                    frame.top+frame.height()*(mode==WorkspaceOptions.VIDEO_TOP?.30f:.16f));
            block(canvas,header,mode==WorkspaceOptions.VIDEO_TOP?accent:empty);
            if(mode==WorkspaceOptions.VIDEO_TOP)play(canvas,header,false);
            float side=frame.right-frame.width()*.24f, gap=1.5f;
            RectF main=new RectF(frame.left,header.bottom+gap,side-gap,frame.bottom);
            block(canvas,main,mode==WorkspaceOptions.VIDEO_TURNED?accent:empty);
            if(mode==WorkspaceOptions.VIDEO_TURNED)play(canvas,main,true);
            float height=(main.height()-2*gap)/3f;
            for(int i=0;i<3;i++) {
                float top=main.top+i*(height+gap);
                block(canvas,new RectF(side,top,frame.right,top+height),empty);
            }
        }
        canvas.restoreToCount(save);
    }
    private void block(Canvas canvas,RectF rect,int color) {
        paint.setColor(color);canvas.drawRoundRect(rect,1.5f,1.5f,paint);
    }
    private void play(Canvas canvas,RectF rect,boolean turned) {
        float size=Math.min(rect.width(),rect.height())*.32f,x=rect.centerX(),y=rect.centerY();
        arrow.reset();
        if(turned) {arrow.moveTo(x-size/2,y-size*.4f);arrow.lineTo(x+size/2,y-size*.4f);arrow.lineTo(x,y+size/2);}
        else {arrow.moveTo(x-size*.4f,y-size/2);arrow.lineTo(x+size/2,y);arrow.lineTo(x-size*.4f,y+size/2);}
        arrow.close();paint.setColor(0xffffffff);canvas.drawPath(arrow,paint);
    }
}
