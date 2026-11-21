package io.github.zerohomers0.onesteplite;

/** Local rectangular crop for a live cell behind the moving fullscreen main task. */
final class WorkspaceCellReveal {
    static int[] crop(int x, int y, int right, int bottom, int bandTop, int bandBottom,
            boolean aboveMain, boolean mainOnRight, boolean moving,
            float mainLeft, float mainTop, float mainRight, float mainBottom) {
        int left = x, top = Math.max(y, bandTop);
        int r = right, b = Math.min(bottom, bandBottom);
        if (moving && mainLeft < r && mainRight > left && mainTop < b && mainBottom > top) {
            if (aboveMain) b = Math.min(b, Math.max(top, (int) Math.floor(mainTop)));
            else if (mainOnRight) r = Math.min(r, Math.max(left, (int) Math.floor(mainLeft)));
            else left = Math.max(left, Math.min(r, (int) Math.ceil(mainRight)));
        }
        return new int[]{left - x, top - y, Math.max(left, r) - x, Math.max(top, b) - y};
    }
}
