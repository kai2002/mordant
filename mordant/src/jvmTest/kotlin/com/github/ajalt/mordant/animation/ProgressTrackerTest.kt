package com.github.ajalt.mordant.animation

import com.github.ajalt.mordant.internal.CSI
import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.VirtualTerminalInterface
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.util.concurrent.TimeUnit

class ProgressTrackerTest {
    var now = 0.0

    @Test
    fun throttling() {
        val vt = VirtualTerminalInterface()
        val t = Terminal(terminalInterface = vt)
        val pt = t.progressAnimation {
            timeSource = { (now * TimeUnit.SECONDS.toNanos(1)).toLong() }
            padding = 0
            autoUpdate = false
            speed(frameRate = 1)
            text("|")
            timeRemaining(frameRate = 1)
        }

        pt.update(0, 1000)
        now = 0.5
        vt.clearBuffer()
        pt.update(40)
        vt.normalizedBuffer() shouldBe " --.-it/s|eta -:--:--"

        now = 0.6
        vt.clearBuffer()
        pt.update()
        vt.normalizedBuffer() shouldBe " --.-it/s|eta -:--:--"

        now = 1.0
        vt.clearBuffer()
        pt.update()
        vt.normalizedBuffer() shouldBe " 40.0it/s|eta 0:00:24"

        now = 1.9
        vt.clearBuffer()
        pt.update()
        vt.normalizedBuffer() shouldBe " 40.0it/s|eta 0:00:24"
    }

    @Test
    fun allCells() {
        val vt = VirtualTerminalInterface(width = 56)
        val t = Terminal(
            theme = Theme(Theme.PlainAscii) { strings["progressbar.pending"] = "." },
            terminalInterface = vt
        )
        val pt =t.progressAnimation {
            timeSource = { (now * TimeUnit.SECONDS.toNanos(1)).toLong() }
            padding = 0
            autoUpdate = false
            text("text.txt")
            text("|")
            percentage()
            text("|")
            progressBar()
            text("|")
            completed()
            text("|")
            speed(frameRate = null)
            text("|")
            timeRemaining(frameRate = null)
        }
        pt.update(0, 100)
        now = 10.0
        vt.clearBuffer()
        pt.update(40)
        vt.normalizedBuffer() shouldBe "text.txt| 40%|###>....| 40.0/100.0|  4.0it/s|eta 0:00:15"

        now = 20.0
        vt.clearBuffer()
        pt.update()
        vt.normalizedBuffer() shouldBe "text.txt| 40%|###>....| 40.0/100.0|  2.0it/s|eta 0:00:30"

        vt.clearBuffer()
        pt.updateTotal(200)
        vt.normalizedBuffer() shouldBe "text.txt| 20%|#>......| 40.0/200.0|  2.0it/s|eta 0:01:20"

        vt.clearBuffer()
        pt.restart()
        vt.normalizedBuffer() shouldBe "text.txt|  0%|........|  0.0/200.0| --.-it/s|eta -:--:--"

        vt.clearBuffer()
        pt.clear()
        vt.normalizedBuffer() shouldBe ""
    }

    private fun VirtualTerminalInterface.normalizedBuffer(): String {
        return buffer().substringAfter("${CSI}0J").trimEnd()
    }
}
