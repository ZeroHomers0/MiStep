package com.jiusis.mistep;

/** Every broadcast action this module sends or listens for, in one place. */
final class MiStepActions {
    /** Debug console: toggles MiStep and accepts developer commands. */
    static final String DEBUG = "com.jiusis.mistep.DEBUG";
    static final String TOGGLE = "com.jiusis.mistep.TOGGLE";
    /** Desktop icon launch and the side-task registry it consults. */
    static final String DESKTOP_ICON_LAUNCH = "com.jiusis.mistep.DESKTOP_ICON_LAUNCH";
    static final String DESKTOP_SIDE_TASKS = "com.jiusis.mistep.DESKTOP_SIDE_TASKS";
    /** Shell hands one live layout handoff to the host. */
    static final String ZOOM = "com.jiusis.mistep.ZOOM";
    static final String ZOOM64 = "com.jiusis.mistep.ZOOM64";
    static final String ZOOM64_ACK = "com.jiusis.mistep.ZOOM64_ACK";
    static final String TRANSFER_TOUCH = "com.jiusis.mistep.TRANSFER_TOUCH";
    static final String PREPARE_TOP_VIEWPORT = "com.jiusis.mistep.PREPARE_TOP_VIEWPORT";
    static final String SWAP_OVERLAY_SURFACES = "com.jiusis.mistep.SWAP_OVERLAY_SURFACES";
    /** Workspace chrome and the retained native window it reveals. */
    static final String RETAINED_WINDOW = "com.jiusis.mistep.RETAINED_WINDOW";
    static final String RETAINED_READY = "com.jiusis.mistep.RETAINED_READY";
    static final String UNLOCK_HANDOFF_RELEASE = "com.jiusis.mistep.UNLOCK_HANDOFF_RELEASE";
    static final String WORKSPACE_WALLPAPER_PARENT = "com.jiusis.mistep.WORKSPACE_WALLPAPER_PARENT";
    static final String MAIN_UI_CONFIGURATION = "com.jiusis.mistep.MAIN_UI_CONFIGURATION";
    static final String LANDSCAPE_MODE = "com.jiusis.mistep.LANDSCAPE_MODE";
    /** Top task controls and the edge-back gesture they own. */
    static final String TOP_TASK_STATE = "com.jiusis.mistep.TOP_TASK_STATE";
    static final String TOP_EDGE_BACK = "com.jiusis.mistep.TOP_EDGE_BACK";
    /** Native video overlay docked into the workspace. */
    static final String VIDEO_HOST = "com.jiusis.mistep.VIDEO_HOST";
    static final String VIDEO_STATE = "com.jiusis.mistep.VIDEO_STATE";
    static final String VIDEO_COMMAND = "com.jiusis.mistep.VIDEO_COMMAND";
    /** Bottom input replay and raw input tracing. */
    static final String BOTTOM_INPUT_OPTIONS = "com.jiusis.mistep.BOTTOM_INPUT_OPTIONS";
    static final String TRACE_INPUT = "com.jiusis.mistep.TRACE_INPUT";

    private MiStepActions() {}
}
