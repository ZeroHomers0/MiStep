package io.github.zerohomers0.onesteplite;

/** A native detach can finish before our bounded observation runs. */
final class TaskHandoffPolicy {

    static HandoffOwner owner(Object expectedToken, Object currentToken, Object listener,
            Object controller, Object fullscreenListener) {
        if (expectedToken == null || controller == null || fullscreenListener == null
                || fullscreenListener == controller) return HandoffOwner.UNAVAILABLE;
        if (expectedToken.equals(currentToken) && listener == controller)
            return HandoffOwner.OUR_CONTROLLER;
        if (currentToken == null && listener == fullscreenListener)
            return HandoffOwner.NATIVE_FULLSCREEN;
        return HandoffOwner.UNAVAILABLE;
    }
}
