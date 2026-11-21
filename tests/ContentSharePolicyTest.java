package com.jiusis.mistep;

import java.util.List;

public final class ContentSharePolicyTest {
    static int checks;
    static void check(boolean value) { if (!value) throw new AssertionError(); checks++; }
    public static void main(String[] args) {
        for (String mime : List.of("text/plain", "text/html", "image/png", "video/mp4", "audio/mpeg", "application/pdf"))
            check(ContentSharePolicy.accepts(mime));
        for (String mime : List.of("text/vnd.android.intent", "application/vnd.android.activity",
                "application/vnd.android.shortcut", "application/vnd.android.task", "unknown"))
            check(!ContentSharePolicy.accepts(mime));
        check(!ContentSharePolicy.accepts(null));
        check(ContentSharePolicy.commonMime(List.of("image/png", "image/jpeg")).equals("image/*"));
        check(ContentSharePolicy.commonMime(List.of("image/png", "application/pdf")).equals("*/*"));
        check(ContentSharePolicy.commonMime(List.of("text/uri-list", "image/png")).equals("image/png"));
        check(ContentSharePolicy.commonMime(List.of()).equals("application/octet-stream"));
        for (String scheme : List.of("content", "http", "https")) check(ContentSharePolicy.uriScheme(scheme));
        for (String scheme : List.of("file", "intent", "javascript", "data")) check(!ContentSharePolicy.uriScheme(scheme));
        System.out.println("Content share policy: " + checks + " checks passed");
    }
}
