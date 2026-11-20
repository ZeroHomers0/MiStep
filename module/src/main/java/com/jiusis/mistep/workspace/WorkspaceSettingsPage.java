package com.jiusis.mistep;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Settings use the installed system Settings resources and layout proportions. */
final class WorkspaceSettingsPage extends FrameLayout {
    private static final int HOME = 0, SWAP = 3, LOGS = 4, GESTURE = 5, VIDEO = 6;
    private static final int BLUE = HyperOsStyle.CONTROL_ACCENT;
    private final SettingsActivity activity;
    private final boolean dark;
    private final int primary, secondary, surface;
    private final int[] scrollPositions = new int[VIDEO + 1];
    private final RuntimeLogController runtimeLogs;
    private TextView logStatus, logPreview;
    private View refreshLogs, exportLogs;
    private TextView recordLogs;
    private final android.database.ContentObserver gestureObserver =
            new android.database.ContentObserver(new android.os.Handler(android.os.Looper.getMainLooper())) {
        @Override public void onChange(boolean selfChange) {
            if (screen == HOME || screen == GESTURE) show(screen, false);
        }
    };
    private int screen;
    private ScrollView scroll;
    private LinearLayout content;

    WorkspaceSettingsPage(SettingsActivity activity, Bundle state, RuntimeLogController runtimeLogs) {
        super(activity);
        this.activity = activity;
        this.runtimeLogs = runtimeLogs;
        dark = (getResources().getConfiguration().uiMode & 48) == 32;
        primary = activity.settingsColor("miuix_preference_primary_text_color_" + (dark ? "dark" : "light"),
                dark ? 0xfff4f5f8 : 0xff000000);
        secondary = activity.settingsColor("miuix_preference_secondary_text_color_" + (dark ? "dark" : "light"),
                dark ? 0xff999ca7 : 0xff999999);
        surface = dark ? 0xff1b1d23 : 0xffffffff;
        if (state != null) {
            int[] saved = state.getIntArray("settings_scroll");
            if (saved != null)
                System.arraycopy(saved, 0, scrollPositions, 0, Math.min(saved.length, scrollPositions.length));
        }
        int restored = state == null ? HOME : state.getInt("settings_page", HOME);
        show(restored == SWAP || restored == LOGS || restored == GESTURE || restored == VIDEO ? restored : HOME, false);
    }

    void save(Bundle state) {
        scrollPositions[screen] = scroll.getScrollY();
        state.putInt("settings_page", screen);
        state.putIntArray("settings_scroll", scrollPositions);
    }

    void refreshLayout() {
        show(screen, false);
    }

    boolean goBack() {
        if (screen == HOME) return false;
        show(HOME, true);
        return true;
    }

    private void show(int next, boolean animate) {
        if (scroll != null) scrollPositions[screen] = scroll.getScrollY();
        screen = next;
        removeAllViews();
        scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        FrameLayout centered = new FrameLayout(activity);
        content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(4), dp(20), dp(32));
        int width = getResources().getDisplayMetrics().widthPixels;
        centered.addView(content, new FrameLayout.LayoutParams(width > dp(680) ? dp(680) : -1,
                -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        scroll.addView(centered, new ScrollView.LayoutParams(-1, -1));
        addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        activity.setPageTitle(next == SWAP ? "主副交换逻辑" : next == LOGS ? "运行日志"
                : next == GESTURE ? "唤出手势" : next == VIDEO ? "视频" : "Mi Step");
        if (next == LOGS) logs();
        else if (next == SWAP) swap();
        else if (next == GESTURE) gesture();
        else if (next == VIDEO) video();
        else home();
        int restoreY = scrollPositions[next];
        scroll.post(() -> scroll.scrollTo(0, restoreY));
        if (animate && android.animation.ValueAnimator.areAnimatorsEnabled()) {
            content.setAlpha(0f);
            content.animate().alpha(1f).setDuration(160).start();
        }
    }

    private void home() {
        WorkspaceOptions options = WorkspaceSettingsStore.read(activity);
        section("外观");
        LinearLayout appearance = card();
        switchOption(appearance, "显示农历", null, options.showLunar,
                checked -> persist(WorkspaceSettingsStore.read(activity).withShowLunar(checked)));
        add(appearance, 0, 12);
        section("交互");
        LinearLayout interaction = card();
        menu(interaction, "视频", WorkspaceOptions.videoLabel(options.landscapeVideo), () -> show(VIDEO, true));
        divider(interaction);
        menu(interaction, "主副交换逻辑", WorkspaceOptions.modeLabel(options.swapMode), () -> show(SWAP, true));
        divider(interaction);
        menu(interaction, "唤出手势", gestureLabel(EntryGesturePolicy.effectiveMode(
                        options.entryGesture, EntryGestureSettings.lineHidden(activity))), () -> show(GESTURE, true));
        divider(interaction);
        switchOption(interaction, "锁屏时保留 MiStep", "开启后解锁恢复工作台；关闭后解锁回到正常界面", options.keepOnLock,
                checked -> persist(WorkspaceSettingsStore.read(activity).withKeepOnLock(checked)));
        divider(interaction);
        switchOption(interaction, "无限小窗", null, options.scrollSideTasks,
                checked -> persist(WorkspaceSettingsStore.read(activity).withScrollSideTasks(checked)));
        divider(interaction);
        switchOption(interaction, "最近任务里不显示小窗里的应用", "只在 One Step 打开时生效，不会关闭应用", options.hideSideRecents,
                checked -> persist(WorkspaceSettingsStore.read(activity).withHideSideRecents(checked)));
        add(interaction, 0, 12);
        section("诊断");
        LinearLayout diagnostics = card();
        menu(diagnostics, "运行日志", "诊断信息与问题反馈", () -> show(LOGS, true));
        add(diagnostics, 0, 0);
    }

    private void switchOption(LinearLayout group, String label, String detail, boolean checked,
            java.util.function.Predicate<Boolean> save) {
        LinearLayout item=row(), labels=optionLabels(label,detail);
        item.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        Switch toggle=settingsSwitch(label,checked);
        item.addView(toggle,new LinearLayout.LayoutParams(-2,-2));
        final boolean[] reverting={false};
        toggle.setOnCheckedChangeListener((button,value)->{
            if(reverting[0])return;
            if(!save.test(value)) {reverting[0]=true;button.setChecked(!value);reverting[0]=false;}
        });
        item.setOnClickListener(v->toggle.toggle());group.addView(item);
    }

    private LinearLayout optionLabels(String label,String detail) {
        LinearLayout labels=new LinearLayout(activity);labels.setOrientation(LinearLayout.VERTICAL);
        TextView title=text(label,17,primary);title.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        labels.addView(title);
        if(detail!=null) {
            TextView description=text(detail,13,secondary);
            LinearLayout.LayoutParams bounds=new LinearLayout.LayoutParams(-1,-2);bounds.topMargin=dp(5);
            labels.addView(description,bounds);
        }
        return labels;
    }

    /** All selection pages share the system indicator, spacing, labels and click target. */
    private void choiceOption(LinearLayout group,String label,String detail,boolean checked,
            boolean enabled,View diagram,Runnable select) {
        LinearLayout item=row();
        if(diagram!=null) {
            LinearLayout.LayoutParams preview=new LinearLayout.LayoutParams(dp(56),dp(80));
            preview.rightMargin=dp(12);item.addView(diagram,preview);
        }
        LinearLayout labels=optionLabels(label,detail);
        LinearLayout.LayoutParams labelBounds=new LinearLayout.LayoutParams(0,-2,1);
        labelBounds.rightMargin=dp(8);item.addView(labels,labelBounds);
        RadioButton radio=new RadioButton(activity);
        Drawable indicator=activity.settingsDrawable("miuix_appcompat_btn_radio_"+(dark?"dark":"light"));
        if(indicator!=null) {radio.setButtonDrawable(indicator);radio.setButtonTintList(null);}
        else radio.setButtonTintList(ColorStateList.valueOf(checked?BLUE:secondary));
        radio.setChecked(checked);radio.setEnabled(enabled);
        radio.setContentDescription(detail==null?label:label+"，"+detail);
        item.addView(radio,new LinearLayout.LayoutParams(-2,-2));
        item.setEnabled(enabled);item.setAlpha(enabled?1f:.45f);
        if(enabled) {item.setOnClickListener(v->select.run());radio.setOnClickListener(v->select.run());}
        group.addView(item);
    }

    private void video() {
        section("视频进入横屏全屏时");
        LinearLayout group=card();
        String[] descriptions={"仅接入播放器的画中画或悬浮视频窗口，下面的主窗和小窗照常使用。不支持的播放器留在主窗。",
                "视频在主窗里横屏显示，小窗照常使用。",
                "关闭工作台，恢复正常的全屏视频界面。"};
        int selected=WorkspaceSettingsStore.read(activity).landscapeVideo;
        for(int mode=0;mode<descriptions.length;mode++) {
            final int choice=mode;if(mode>0)divider(group);
            choiceOption(group,WorkspaceOptions.videoLabel(mode),descriptions[mode],mode==selected,true,
                    new VideoModeDiagram(activity,mode,secondary,dark?0xff353840:0xffe9e9e9,BLUE),()->{
                        if(persist(WorkspaceSettingsStore.read(activity).withLandscapeVideo(choice)))show(VIDEO,false);
                    });
        }
        add(group,0,14);
        TextView note=text("从下一次进入横屏全屏开始生效",13,secondary);
        note.setPadding(dp(16),dp(2),dp(16),0);add(note,0,0);
    }

    private void logs() {
        TextView note = text("先开始录制，再复现问题，返回后点结束并导出；最长60秒，支持锁屏复现。需要 Root 权限。诊断包含系统日志及窗口信息，分享前请检查。", 13, secondary);
        note.setPadding(dp(16), 0, dp(16), 0);
        add(note, 0, 18);
        LinearLayout description = card();
        android.widget.EditText input = new android.widget.EditText(activity);
        input.setTextSize(14); input.setTextColor(primary); input.setHintTextColor(secondary);
        input.setHint("问题现象、发生时间与复现步骤（选填）");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(2); input.setMaxLines(5);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4000)});
        input.setBackgroundColor(0x00000000); input.setPadding(dp(16), dp(12), dp(16), dp(12));
        input.setText(runtimeLogs.description());
        input.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { runtimeLogs.description(s.toString()); }
            public void afterTextChanged(android.text.Editable text) {}
        });
        description.addView(input, new LinearLayout.LayoutParams(-1, -2)); add(description, 0, 18);
        LinearLayout actions = new LinearLayout(activity);
        refreshLogs = action("刷新诊断", false, runtimeLogs::refresh);
        exportLogs = action("导出诊断包", true, runtimeLogs::export);
        LinearLayout.LayoutParams refresh = new LinearLayout.LayoutParams(0, dp(50), 1);
        refresh.rightMargin = dp(12);
        actions.addView(refreshLogs, refresh);
        actions.addView(exportLogs, new LinearLayout.LayoutParams(0, dp(50), 1));
        add(actions, 0, 18);
        recordLogs = action("开始录制问题（最长60秒）", false, runtimeLogs::record);
        recordLogs.setMinimumHeight(dp(50));
        add(recordLogs, 0, 14);
        logStatus = text("", 13, secondary);
        logStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        logStatus.setPadding(dp(16), 0, dp(16), 0);
        add(logStatus, 0, 12);
        LinearLayout preview = card();
        logPreview = text("", 11, primary);
        logPreview.setTypeface(Typeface.MONOSPACE);
        logPreview.setTextIsSelectable(true);
        logPreview.setPadding(dp(16), dp(16), dp(16), dp(16));
        preview.addView(logPreview, new LinearLayout.LayoutParams(-1, -2));
        add(preview, 0, 0);
        runtimeLogs.open(); updateLogs();
    }

    private TextView action(String label, boolean selected, Runnable run) {
        TextView button = text(label, 16, selected ? 0xffffffff : primary);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(Typeface.create("sans-serif-medium", 0));
        button.setBackground(HyperOsStyle.controlButton(activity, selected, 18));
        button.setFocusable(true);
        button.setOnClickListener(v -> run.run());
        return button;
    }

    void updateLogs() {
        if (screen != LOGS || logStatus == null) return;
        logStatus.setText(runtimeLogs.status());
        logPreview.setText(runtimeLogs.preview());
        refreshLogs.setEnabled(!runtimeLogs.busy());
        refreshLogs.setAlpha(runtimeLogs.busy() ? .45f : 1f);
        exportLogs.setEnabled(runtimeLogs.canExport());
        exportLogs.setAlpha(runtimeLogs.canExport() ? 1f : .45f);
        recordLogs.setText(runtimeLogs.recording()?"结束录制并采集诊断":"开始录制问题（最长60秒）");
        recordLogs.setEnabled(runtimeLogs.recording()||!runtimeLogs.busy()); recordLogs.setAlpha(runtimeLogs.recording()||!runtimeLogs.busy()?1f:.45f);
    }

    private Switch settingsSwitch(String label, boolean checked) {
        Switch toggle = new Switch(activity);
        toggle.setShowText(false);
        toggle.setText("");
        toggle.setContentDescription(label);
        toggle.setSwitchMinWidth(activity.settingsDimension("miuix_appcompat_sliding_button_width", 49));
        toggle.setSplitTrack(false);
        int thumbSize = activity.settingsDimension("miuix_appcompat_sliding_button_slider_size", 20);
        int frameHeight = activity.settingsDimension("miuix_appcompat_sliding_button_height", 28);
        toggle.setThumbDrawable(new Drawable() {
            private final Paint knob = new Paint(Paint.ANTI_ALIAS_FLAG);
            { knob.setColor(0xffffffff); }
            @Override public void draw(Canvas canvas) {
                Rect bounds = getBounds();
                canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(),
                        Math.min(bounds.width(), bounds.height()) / 2f, knob);
            }
            @Override public int getIntrinsicWidth() { return thumbSize; }
            @Override public int getIntrinsicHeight() { return frameHeight; }
            @Override public void setAlpha(int alpha) { knob.setAlpha(alpha); invalidateSelf(); }
            @Override public void setColorFilter(ColorFilter filter) { knob.setColorFilter(filter); invalidateSelf(); }
            @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        });
        StateListDrawable track = new StateListDrawable();
        Drawable on = switchTrack(activity.settingsColor("miuix_appcompat_sliding_button_bar_on_"
                + (dark ? "dark" : "light"), BLUE));
        Drawable off = switchTrack(activity.settingsColor(dark
                ? "miuix_appcompat_sliding_button_bar_off_dark"
                : "miuix_appcompat_sliding_button_bar_off_normal_color_light",
                dark ? 0xff42454e : 0xffdedfe5));
        track.addState(new int[]{android.R.attr.state_checked}, on);
        track.addState(new int[]{}, off);
        toggle.setTrackDrawable(track);
        toggle.setThumbTintList(null); toggle.setTrackTintList(null);
        toggle.setChecked(checked);
        return toggle;
    }

    private Drawable switchTrack(int color) {
        int inset = activity.settingsDimension("miuix_appcompat_sliding_button_slider_horizontal_padding", 4);
        float radius = dp(14);
        ShapeDrawable track = new ShapeDrawable(new RoundRectShape(new float[] {
                radius, radius, radius, radius, radius, radius, radius, radius}, null, null));
        track.getPaint().setColor(color);
        track.setPadding(inset, 0, inset, 0);
        track.setIntrinsicWidth(activity.settingsDimension("miuix_appcompat_sliding_button_width", 49));
        track.setIntrinsicHeight(activity.settingsDimension("miuix_appcompat_sliding_button_height", 28));
        return track;
    }

    private void swap() {
        int selected=WorkspaceSettingsStore.read(activity).swapMode;
        for(int mode=0;mode<2;mode++) {
            final int choice=mode;LinearLayout group=card();
            Runnable select=()->{
                if(persist(WorkspaceSettingsStore.read(activity).withSwapMode(choice)))show(SWAP,false);
            };
            choiceOption(group,WorkspaceOptions.modeLabel(mode),mode==WorkspaceOptions.FIXED_MAIN
                    ?"主区域保持在当前一侧，只交换应用":"副任务在原侧放大，主区域随之换边",mode==selected,true,null,select);
            group.setOnClickListener(v->select.run());
            ModeDiagram diagram=new ModeDiagram(mode);
            LinearLayout.LayoutParams preview=new LinearLayout.LayoutParams(-1,dp(104));
            preview.setMargins(dp(18),0,dp(18),dp(16));group.addView(diagram,preview);add(group,0,14);
        }
        TextView note=text("从下一次主副交换开始生效",13,secondary);
        note.setPadding(dp(16),dp(2),dp(16),0);add(note,0,0);
    }

    private String gestureLabel(int mode) {
        return mode == WorkspaceOptions.BOTTOM_CENTER_OUTWARD ? "底部向外滑" : "状态栏向内滑";
    }

    private void gesture() {
        WorkspaceOptions options = WorkspaceSettingsStore.read(activity);
        boolean lineHidden = EntryGestureSettings.lineHidden(activity);
        int selected = EntryGesturePolicy.effectiveMode(options.entryGesture, lineHidden);
        section("唤出手势");
        LinearLayout group = card();
        entryGestureOption(group, WorkspaceOptions.STATUS_BAR_INWARD,
                "状态栏向内滑", "从状态栏左侧或右侧向屏幕中间滑动", selected, true);
        divider(group);
        String detail = lineHidden ? "从屏幕底部任意位置向左或向右滑动"
                : "需先关闭澎湃小白条（手势提示线）";
        entryGestureOption(group, WorkspaceOptions.BOTTOM_CENTER_OUTWARD,
                "底部向外滑", detail, selected, lineHidden);
        add(group, 0, 0);
    }

    private void entryGestureOption(LinearLayout group, int mode, String label, String detail,
            int selectedMode, boolean enabled) {
        choiceOption(group,label,detail,selectedMode==mode,enabled,null,()->{
            if(!EntryGesturePolicy.canSelect(mode,EntryGestureSettings.lineHidden(activity)))
                Toast.makeText(activity,"请先关闭澎湃的手势提示线",Toast.LENGTH_SHORT).show();
            else persist(WorkspaceSettingsStore.read(activity).withEntryGesture(mode));
            show(GESTURE,false);
        });
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        try {
            activity.getContentResolver().registerContentObserver(
                    android.provider.Settings.Global.getUriFor(EntryGestureSettings.HIDE_GESTURE_LINE),
                    false, gestureObserver);
        } catch (RuntimeException unavailable) { /* Selection and runtime still check live state. */ }
    }

    @Override protected void onDetachedFromWindow() {
        activity.getContentResolver().unregisterContentObserver(gestureObserver);
        super.onDetachedFromWindow();
    }

    private boolean persist(WorkspaceOptions options) {
        if (WorkspaceSettingsStore.write(activity, options)) return true;
        Toast.makeText(activity, "未能保存，请重试", Toast.LENGTH_SHORT).show();
        return false;
    }

    private LinearLayout card() {
        LinearLayout group = new LinearLayout(activity);
        group.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable material = new GradientDrawable();
        material.setColor(surface);
        material.setCornerRadius(activity.settingsDimension("miuix_theme_radius_common", 16));
        group.setBackground(material);
        group.setClipToOutline(true);
        return group;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(activity.settingsDimension("miuix_preference_item_min_height", 56));
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(dark ? 0x18ffffff : 0x0b000000),
                null, shape(0xffffffff, 0)));
        return row;
    }

    private LinearLayout menu(LinearLayout group, String title, String summary, Runnable action) {
        LinearLayout row = row();
        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 17, primary));
        TextView detail = text(summary, 14, secondary);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(3); labels.addView(detail, lp);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        View arrow = new View(activity);
        arrow.setBackground(new Chevron());
        arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(22), dp(22)));
        row.setOnClickListener(v -> action.run());
        group.addView(row);
        return row;
    }

    private void divider(LinearLayout group) {
        View divider = new View(activity);
        divider.setBackgroundColor(dark ? 0x08ffffff : 0x04000000);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(.5f));
        lp.leftMargin = lp.rightMargin = dp(20);
        group.addView(divider, lp);
    }

    private void section(String title) {
        TextView section = text(title, 13, secondary);
        section.setPadding(dp(16), dp(8), dp(16), 0);
        section.setAccessibilityHeading(true);
        add(section, 0, 8);
    }

    private TextView text(String value, int size, int color) {
        TextView text = new TextView(activity);
        text.setText(value); text.setTextSize(size); text.setTextColor(color);
        text.setIncludeFontPadding(false);
        text.setLineSpacing(dp(2), 1f);
        return text;
    }

    private void add(View view, int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(top); lp.bottomMargin = dp(bottom);
        content.addView(view, lp);
    }

    private GradientDrawable shape(int color, float radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color); shape.setCornerRadius(dp(radius));
        return shape;
    }

    private int dp(float value) { return Math.max(value > 0 ? 1 : 0, Math.round(value * getResources().getDisplayMetrics().density)); }

    private final class Chevron extends Drawable {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        @Override public void draw(Canvas canvas) {
            paint.setColor(secondary); paint.setStrokeWidth(dp(1.6f)); paint.setStrokeCap(Paint.Cap.ROUND);
            float x = getBounds().exactCenterX(), y = getBounds().exactCenterY();
            canvas.drawLine(x - dp(2), y - dp(4), x + dp(2), y, paint);
            canvas.drawLine(x + dp(2), y, x - dp(2), y + dp(4), paint);
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    private final class ModeDiagram extends View {
        final int mode;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ModeDiagram(int mode) { super(activity); this.mode = mode; setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float scale = Math.min(getWidth() / 280f, getHeight() / 104f);
            canvas.save(); canvas.translate((getWidth() - 280 * scale) / 2, (getHeight() - 104 * scale) / 2);
            canvas.scale(scale, scale);
            scene(canvas, 0, false, false);
            paint.setColor(secondary); paint.setStrokeWidth(1.7f); paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(128, 52, 150, 52, paint);
            canvas.drawLine(145, 47, 150, 52, paint); canvas.drawLine(145, 57, 150, 52, paint);
            scene(canvas, 170, mode == WorkspaceOptions.GROW_IN_PLACE, true);
            canvas.restore();
        }
        void scene(Canvas canvas, float x, boolean right, boolean after) {
            fill(canvas, x, 5, 110, 94, dark ? 0xff272a33 : 0xffeef1f8, 12);
            fill(canvas, x + 8, 13, 94, 10, dark ? 0xff333742 : 0xffe0e5ef, 4);
            float mainX = x + (right ? 38 : 8), sideX = x + (right ? 8 : 80);
            fill(canvas, mainX, 30, 64, 61, dark ? 0xff244b80 : 0xffdceaff, 7);
            label(canvas, after ? "B" : "A", mainX + 32, 64, BLUE);
            for (int i = 0; i < 3; i++) {
                fill(canvas, sideX, 30 + i * 22, 22, 17, i == 0
                        ? dark ? 0xff49405e : 0xffe8e0f6 : dark ? 0xff373b46 : 0xffdfe3ec, 4);
                if (i == 0) label(canvas, after ? "A" : "B", sideX + 11, 43, dark ? 0xffb9a5df : 0xff8b71bb);
            }
        }
        void fill(Canvas canvas, float x, float y, float w, float h, int color, float radius) {
            paint.setColor(color); canvas.drawRoundRect(new RectF(x, y, x + w, y + h), radius, radius, paint);
        }
        void label(Canvas canvas, String text, float x, float y, int color) {
            paint.setColor(color); paint.setTypeface(Typeface.create("sans-serif-medium", 0));
            paint.setTextSize(12); paint.setTextAlign(Paint.Align.CENTER); canvas.drawText(text, x, y, paint);
        }
    }
}
