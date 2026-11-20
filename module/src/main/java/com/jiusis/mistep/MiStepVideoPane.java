package com.jiusis.mistep;

import android.app.PendingIntent;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import de.robv.android.xposed.XposedHelpers;

/** Transparent when idle; native video replaces the header in place when a source is ready. */
final class MiStepVideoPane extends FrameLayout implements SurfaceHolder.Callback {
    private final SurfaceView video;
    private final LinearLayout controls;
    private final MiStepIconButton toggle;
    private final PopupWindow controlPopup;
    private android.graphics.Bitmap wellWallpaper;
    private int wallpaperWidth=1,wallpaperHeight=1;
    private final Runnable hideControls=this::fadeControls;
    private final Handler handler;
    private final BooleanSupplier allowed;
    private final Consumer<Boolean> changed;
    private final Binder owner=new Binder();
    private final PendingIntent identity;
    private IBinder releaseToken;
    private SurfaceControl carrier;
    private long sequence,source;
    private String lastPublished="";
    private boolean embedded,registered;
    private final BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent intent){
        Bundle binding=intent.getBundleExtra("identity");
        if(binding==null||!owner.equals(binding.getBinder("owner")))return;
        releaseToken=binding.getBinder("release");source=intent.getLongExtra("source",0);
        boolean showing=intent.getBooleanExtra("embedded",false)&&allowed.getAsBoolean()&&MiStepEntryGuard.canOpen(getContext());
        setEmbedded(showing);
        boolean playing=intent.getBooleanExtra("playing",false);
        toggle.setIcon(playing?MiStepIconButton.PAUSE:MiStepIconButton.PLAY);
        toggle.setContentDescription(playing?"暂停视频":"播放视频");
        toggle.setEnabled(intent.getBooleanExtra("controllable",false));toggle.setAlpha(toggle.isEnabled()?1f:.4f);
    }};
    MiStepVideoPane(Context context,Handler handler,BooleanSupplier allowed,Consumer<Boolean> changed){
        super(context);this.handler=handler;this.allowed=allowed;this.changed=changed;
        identity=PendingIntent.getBroadcast(context,9301,new Intent(MiStepVideoDock.HOST).setPackage(context.getPackageName()),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        video=new SurfaceView(context);video.setZOrderOnTop(true);video.getHolder().setFormat(PixelFormat.TRANSLUCENT);video.getHolder().addCallback(this);
        video.setContentDescription("置顶视频，点按显示或隐藏播放控制");
        video.setOnClickListener(view->{if(embedded)toggleControls();});video.setClickable(false);
        addView(video,new FrameLayout.LayoutParams(-1,-1));
        controls=new LinearLayout(context);controls.setGravity(Gravity.CENTER);
        toggle=button(MiStepIconButton.PLAY,"播放视频",()->{command("toggle");scheduleControlsHide();});
        controls.addView(toggle,new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout.LayoutParams returned=new LinearLayout.LayoutParams(dp(44),dp(44));returned.leftMargin=dp(8);
        controls.addView(button(MiStepIconButton.VIDEO_RETURN,"返回悬浮窗",()->{hideControlsNow();command("release");}),returned);
        // A separate native window stays above the on-top video SurfaceView. It
        // covers only these buttons, leaving the full video viewport unchanged.
        controlPopup=new PopupWindow(controls,dp(96),dp(44),false);
        controlPopup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        controlPopup.setOutsideTouchable(false);controlPopup.setTouchModal(false);
        controlPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        setBackgroundColor(Color.TRANSPARENT);
        context.registerReceiver(receiver,new IntentFilter(MiStepVideoDock.STATE),android.Manifest.permission.WRITE_SECURE_SETTINGS,handler,Context.RECEIVER_EXPORTED);registered=true;
    }
    boolean embedded(){return embedded;}
    void setWallpaper(android.graphics.Bitmap bitmap,int width,int height){wellWallpaper=bitmap;wallpaperWidth=Math.max(1,width);wallpaperHeight=Math.max(1,height);controls.invalidate();}
    private MiStepIconButton button(int icon,String text,Runnable action){
        MiStepIconButton view=new MiStepIconButton(getContext(),icon,text);view.setIconColor(WorkspaceCellStyle.WELL_ICON_COLOR);
        view.setBackground(new FrostWell(view));view.setOnClickListener(v->action.run());return view;
    }
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void setEmbedded(boolean value){if(embedded==value)return;embedded=value;hideControlsNow();video.setClickable(value);setBackgroundColor(value?Color.BLACK:Color.TRANSPARENT);changed.accept(value);invalidate();}
    private void toggleControls(){
        if(controlPopup.isShowing()){hideControlsNow();return;}
        if(!embedded||!isAttachedToWindow()||!allowed.getAsBoolean())return;
        int[] origin=new int[2];getLocationOnScreen(origin);controls.animate().cancel();controls.setAlpha(0f);
        try{controlPopup.showAtLocation(this,Gravity.TOP|Gravity.LEFT,origin[0]+Math.max(dp(8),getWidth()-dp(108)),origin[1]+Math.max(dp(8),getHeight()-dp(56)));
            controls.animate().alpha(1f).setDuration(140).start();scheduleControlsHide();}
        catch(RuntimeException unavailable){controlPopup.dismiss();}
    }
    private void scheduleControlsHide(){handler.removeCallbacks(hideControls);handler.postDelayed(hideControls,2600L);}
    private void fadeControls(){if(controlPopup.isShowing())controls.animate().alpha(0f).setDuration(140).withEndAction(()->controlPopup.dismiss()).start();}
    private void hideControlsNow(){handler.removeCallbacks(hideControls);controls.animate().cancel();controlPopup.dismiss();}
    private final class FrostWell extends android.graphics.drawable.Drawable {
        final View owner;final android.graphics.Paint paint=new android.graphics.Paint(3);
        final android.graphics.Path circle=new android.graphics.Path();final int[] origin=new int[2];
        FrostWell(View owner){this.owner=owner;}
        @Override public void draw(android.graphics.Canvas canvas){
            android.graphics.Rect b=getBounds();float radius=Math.min(b.width(),b.height())*.5f-dp(2);
            if(radius<=0)return;int saved=canvas.save();circle.reset();circle.addCircle(b.exactCenterX(),b.exactCenterY(),radius,android.graphics.Path.Direction.CW);canvas.clipPath(circle);
            android.graphics.Bitmap bitmap=wellWallpaper;
            if(bitmap!=null&&!bitmap.isRecycled()){
                owner.getLocationOnScreen(origin);paint.setAlpha(190);
                android.graphics.Rect src=new android.graphics.Rect(Math.round(origin[0]*bitmap.getWidth()/(float)wallpaperWidth),Math.round(origin[1]*bitmap.getHeight()/(float)wallpaperHeight),Math.round((origin[0]+b.width())*bitmap.getWidth()/(float)wallpaperWidth),Math.round((origin[1]+b.height())*bitmap.getHeight()/(float)wallpaperHeight));
                canvas.drawBitmap(bitmap,src,new android.graphics.RectF(b),paint);
            }
            paint.setColor(WorkspaceCellStyle.WELL_COLOR);canvas.drawCircle(b.exactCenterX(),b.exactCenterY(),radius,paint);canvas.restoreToCount(saved);
        }
        @Override public void setAlpha(int alpha){}@Override public void setColorFilter(android.graphics.ColorFilter filter){}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    void refresh(){publish(allowed.getAsBoolean()&&getWindowVisibility()==VISIBLE&&MiStepEntryGuard.canOpen(getContext()));}
    private void publish(boolean active){
        String key=System.identityHashCode(carrier)+":"+video.getWidth()+":"+video.getHeight()+":"+active;
        if(key.equals(lastPublished))return;lastPublished=key;
        Bundle binding=new Bundle();binding.putBinder("owner",owner);
        Intent intent=new Intent(MiStepVideoDock.HOST).setPackage("android").putExtras(binding).putExtra("identity",identity)
                .putExtra("sequence",++sequence).putExtra("surface",carrier).putExtra("width",video.getWidth()).putExtra("height",video.getHeight())
                .putExtra("active",active&&carrier!=null&&carrier.isValid());
        getContext().sendBroadcast(intent,android.Manifest.permission.WRITE_SECURE_SETTINGS);
    }
    private void command(String action){Bundle binding=new Bundle();binding.putBinder("owner",owner);
        getContext().sendBroadcast(new Intent(MiStepVideoDock.COMMAND).setPackage("android").putExtras(binding)
                .putExtra("identity",identity).putExtra("command",action).putExtra("source",source),android.Manifest.permission.WRITE_SECURE_SETTINGS);}
    /** Return the native child before SurfaceView can destroy its temporary parent. */
    void releaseForPrivacy(){
        hideControlsNow();
        if(releaseToken!=null){Parcel data=Parcel.obtain(),reply=Parcel.obtain();
            try{data.writeInterfaceToken("com.jiusis.mistep.VideoRelease");data.writeStrongBinder(owner);
                releaseToken.transact(IBinder.FIRST_CALL_TRANSACTION,data,reply,0);reply.readException();}
            catch(Throwable failure){android.util.Log.w("MiStepProbe","video native release pending",failure);}
            finally{reply.recycle();data.recycle();}}
        publish(false);setEmbedded(false);
    }
    @Override public void surfaceCreated(SurfaceHolder holder){
        try{android.graphics.Canvas canvas=holder.lockCanvas();if(canvas!=null){canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);holder.unlockCanvasAndPost(canvas);}}
        catch(RuntimeException ignored){}
        try{carrier=(SurfaceControl)XposedHelpers.callMethod(video,"getSurfaceControl");}catch(Throwable error){carrier=null;}
        refresh();
    }
    @Override public void surfaceChanged(SurfaceHolder holder,int format,int width,int height){refresh();}
    @Override public void surfaceDestroyed(SurfaceHolder holder){releaseForPrivacy();carrier=null;}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);if(controlPopup!=null)hideControlsNow();post(this::refresh);}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(video==null)return;
        if(visibility!=VISIBLE)releaseForPrivacy();else post(this::refresh);}
    @Override protected void onDetachedFromWindow(){releaseForPrivacy();if(registered){getContext().unregisterReceiver(receiver);registered=false;}super.onDetachedFromWindow();}
}
