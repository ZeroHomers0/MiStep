package io.github.zerohomers0.onesteplite;

/** Focus a handed-off task only after the old main task has finished backfilling. */
final class PromotionFocusTransfer {

    interface Tasks {
        State read(int taskId) throws Exception;
        void focus(int taskId) throws Exception;
    }

    static final class State {
        final Object token;
        final int displayId, userId, windowingMode;
        final boolean visible, focused;

        State(Object token, int displayId, int userId, int windowingMode,
                boolean visible, boolean focused) {
            this.token = token;
            this.displayId = displayId;
            this.userId = userId;
            this.windowingMode = windowingMode;
            this.visible = visible;
            this.focused = focused;
        }

        boolean matches(Object expectedToken, int expectedUser) {
            return expectedToken != null && expectedToken.equals(token)
                    && displayId == 0 && userId == expectedUser
                    && windowingMode == 1 && visible;
        }
    }

    static FocusResult transfer(Tasks tasks, int id, Object token, int user) throws Exception {
        FocusResult before = observe(tasks, id, token, user);
        if (before != FocusResult.PENDING) return before;
        tasks.focus(id);
        return observe(tasks, id, token, user);
    }

    /** Request the confirmed live main once, in the existing Shell finish turn. */
    static FocusResult request(Tasks tasks, int id, Object token, int user) throws Exception {
        State state = tasks.read(id);
        if (state == null || !state.matches(token, user)) return FocusResult.UNAVAILABLE;
        if (state.focused) return FocusResult.FOCUSED;
        tasks.focus(id);
        // Acceptance is distinct from observed focus. Do not keep input blocked
        // for a polling read or timer after the real task finish has committed.
        return FocusResult.REQUESTED;
    }

    static FocusResult observe(Tasks tasks, int id, Object token, int user) throws Exception {
        State state = tasks.read(id);
        if (state == null || !state.matches(token, user)) return FocusResult.UNAVAILABLE;
        return state.focused ? FocusResult.FOCUSED : FocusResult.PENDING;
    }

    private PromotionFocusTransfer() {}
}
