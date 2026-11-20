package com.jiusis.mistep;

/** Lifecycle of one verified live swap session. */
enum SwapSessionState { NEW, WAITING, RUNNING, SETTLING, FINISHED, DECLINED, CONSUMED, FAILED }
