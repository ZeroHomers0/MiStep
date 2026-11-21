package io.github.zerohomers0.onesteplite;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/** Content drags never interpret launcher tasks, shortcuts or nested intents as shares. */
final class ContentSharePolicy {
    static boolean accepts(String mime) {
        if (mime == null || mime.equals("text/vnd.android.intent")
                || mime.equals("application/vnd.android.activity") || mime.equals("application/vnd.android.task")
                || mime.equals("application/vnd.android.shortcut")) return false;
        return mime.startsWith("text/") || mime.startsWith("image/") || mime.startsWith("video/")
                || mime.startsWith("audio/") || mime.startsWith("application/");
    }
    static boolean uriScheme(String scheme) {
        return "content".equals(scheme) || "http".equals(scheme) || "https".equals(scheme);
    }
    static String commonMime(Collection<String> values) {
        Set<String> types = new LinkedHashSet<>();
        for (String type : values) if (type != null && type.contains("/")
                && !type.equals("text/uri-list") && !type.equals("text/plain")) types.add(type);
        if (types.isEmpty()) return "application/octet-stream";
        if (types.size() == 1) return types.iterator().next();
        String family = null;
        for (String type : types) {
            String next = type.substring(0, type.indexOf('/'));
            if (family != null && !family.equals(next)) return "*/*";
            family = next;
        }
        return family + "/*";
    }
    private ContentSharePolicy() {}
}
