package top.ltfan.m3x.backdrop.internal

/**
 * Continues cleanup after individual failures and rethrows the first with later ones suppressed.
 */
internal class FailureCollector {
    private var first: Throwable? = null

    fun run(block: () -> Unit) {
        try {
            block()
        } catch (problem: Throwable) {
            val failure = first
            if (failure == null) first = problem else failure.addSuppressed(problem)
        }
    }

    fun throwIfFailed() {
        first?.let { throw it }
    }
}
