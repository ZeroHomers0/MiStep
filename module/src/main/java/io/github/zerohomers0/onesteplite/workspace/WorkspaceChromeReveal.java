package io.github.zerohomers0.onesteplite;

/** Whole controls fade only after their bounds clear the moving main task. */
final class WorkspaceChromeReveal {
    static float alpha(float left, float top, float right, float bottom,
            float mainLeft, float mainTop, float mainRight, float mainBottom, float feather) {
        float clearance = Math.max(Math.max(mainLeft - right, left - mainRight),
                Math.max(mainTop - bottom, top - mainBottom));
        float t = Math.max(0f, Math.min(1f, clearance / Math.max(1f, feather)));
        return t * t * (3f - 2f * t);
    }
}
