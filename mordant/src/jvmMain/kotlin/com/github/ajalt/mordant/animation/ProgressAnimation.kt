@file:Suppress("DEPRECATION")

package com.github.ajalt.mordant.animation

import com.github.ajalt.mordant.animation.progress.*
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.ProgressBuilder
import com.github.ajalt.mordant.widgets.ProgressLayout
import com.github.ajalt.mordant.widgets.progress.ANIMATION_FPS
import com.github.ajalt.mordant.widgets.progress.ProgressBarDefinition
import com.github.ajalt.mordant.widgets.progress.ProgressLayoutBuilder
import com.github.ajalt.mordant.widgets.progress.TEXT_FPS
import java.util.concurrent.Future
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource


@Deprecated("Use progressBarLayout instead")
class ProgressAnimationBuilder internal constructor() : ProgressBuilder(
    ProgressLayoutBuilder()
) {
    /**
     * The maximum number of times per second to update idle animations like the progress bar pulse
     * (default: 30fps)
     */
    var animationFrameRate: Int = 30

    /**
     * The maximum number of times per second to update text like the estimated time remaining.
     * (default: 5fps)
     */
    var textFrameRate: Int = 5

    /**
     * The number of seconds worth of history to keep track of for estimating progress speed and
     * time remaining (default: 30s)
     */
    var historyLength: Float = 30f
}

/**
 * A pretty animated progress bar. Manages a timer thread to update the progress bar, so be sure to [stop] it when you're done.
 */
@Deprecated("Use progressBarLayout instead")
class ProgressAnimation internal constructor(
    private val inner: ThreadProgressTaskAnimator<Unit>,
) {
    private val lock = Any()
    private var future: Future<*>? = null

    /**
     * Set the current progress to the [completed] value.
     */
    fun update(completed: Long) {
        inner.update(completed)
        update()
    }

    /**
     * Set the current progress to the [completed] value.
     */
    fun update(completed: Int) {
        update(completed.toLong())
    }

    /**
     * Update the progress bar without changing the current progress amount.
     *
     * This will redraw the animation and update fields like the estimated time remaining.
     */
    fun update() {
        inner.refresh()
    }

    /**
     * Set the current progress to the [completed] value, and set the total to the [total] value.
     */
    fun update(completed: Long, total: Long?) {
        inner.update {
            this.completed = completed
            this.total = total
        }
        update()
    }

    /**
     * Set the [total] amount of work to be done, or `null` to make the progress bar indeterminate.
     */
    fun updateTotal(total: Long?) {
        inner.update { this.total = total }
        update()
    }

    /**
     * Advance the current completed progress by [amount] without changing the total.
     */
    fun advance(amount: Long = 1) {
        inner.advance(amount)
        if (!inner.finished) {
            update()
        }
    }

    /**
     * Start the progress bar animation.
     */
    fun start() = synchronized(lock) {
        if (future != null) return
        future = inner.execute()
    }

    /**
     * Stop the progress bar animation.
     *
     * The progress bar will remain on the screen until you call [clear].
     * You can call [start] again to resume the animation.
     */
    fun stop() = synchronized(lock) {
        future?.cancel(false)
        future = null
    }

    /**
     * Set the progress to 0 and restart the animation.
     */
    fun restart() = synchronized(lock) {
        inner.reset()
        if (future == null) update()
    }

    /**
     * Stop the animation and remove it from the screen.
     *
     * If you want to leave the animation on the screen, call [stop] instead.
     */
    fun clear() = synchronized(lock) {
        stop()
        inner.clear()
    }
}

/**
 * Create an animated progress bar.
 *
 * See [ProgressLayout] for the types of cells that can be added.
 */
@Deprecated("Use progressBarLayout instead")
fun Terminal.progressAnimation(init: ProgressAnimationBuilder.() -> Unit): ProgressAnimation {
    return progressAnimation(TimeSource.Monotonic, init)
}

// internal for testing
internal fun Terminal.progressAnimation(
    timeSource: TimeSource.WithComparableMarks,
    init: ProgressAnimationBuilder.() -> Unit,
): ProgressAnimation {
    val builder = ProgressAnimationBuilder().apply(init)
    val origDef = builder.builder.build(builder.padding, true)
    // Since the new builder requires the fps upfront, we copy the cells and replace the fps
    val cells = origDef.cells.mapTo(mutableListOf()) {
        it.copy(
            fps = when (it.fps) {
                TEXT_FPS -> builder.textFrameRate
                ANIMATION_FPS -> builder.animationFrameRate
                else -> it.fps
            }
        )
    }
    val definition = ProgressBarDefinition(cells, origDef.spacing, origDef.alignColumns)
    return ProgressAnimation(
        definition.animateOnThread(
            this,
            timeSource = timeSource,
            speedEstimateDuration = builder.historyLength.toDouble().seconds
        )
    )
}
