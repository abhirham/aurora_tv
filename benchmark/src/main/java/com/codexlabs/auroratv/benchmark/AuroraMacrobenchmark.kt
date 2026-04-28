package com.codexlabs.auroratv.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TargetPackage = "com.codexlabs.auroratv"
private const val Iterations = 5

@RunWith(AndroidJUnit4::class)
class AuroraMacrobenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStart() = benchmarkRule.measureRepeated(
        packageName = TargetPackage,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.COLD,
        iterations = Iterations,
    ) {
        startActivityAndWait()
    }

    @Test
    fun homeLoadAndRailBrowse() = benchmarkRule.measureRepeated(
        packageName = TargetPackage,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.WARM,
        iterations = Iterations,
        setupBlock = { startActivityAndWait() },
    ) {
        repeat(8) {
            device.pressDPadRight()
            device.waitForIdle()
        }
        repeat(4) {
            device.pressDPadDown()
            device.waitForIdle()
        }
    }

    @Test
    fun liveCategoryNavigationAndChannelZapping() = benchmarkRule.measureRepeated(
        packageName = TargetPackage,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.WARM,
        iterations = Iterations,
        setupBlock = { startActivityAndWait() },
    ) {
        openTopNavOffset(1)
        repeat(6) {
            device.pressDPadDown()
            device.waitForIdle()
        }
        device.pressDPadRight()
        repeat(8) {
            device.pressDPadDown()
            device.waitForIdle()
        }
        device.pressDPadCenter()
        device.waitForIdle()
        repeat(3) {
            device.pressKeyCode(android.view.KeyEvent.KEYCODE_CHANNEL_UP)
            device.waitForIdle()
        }
    }

    @Test
    fun movieAndSeriesGridBrowsing() = benchmarkRule.measureRepeated(
        packageName = TargetPackage,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.WARM,
        iterations = Iterations,
        setupBlock = { startActivityAndWait() },
    ) {
        openTopNavOffset(2)
        browseGrid()
        openTopNavOffset(3)
        browseGrid()
    }

    @Test
    fun searchBrowse() = benchmarkRule.measureRepeated(
        packageName = TargetPackage,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.WARM,
        iterations = Iterations,
        setupBlock = { startActivityAndWait() },
    ) {
        openTopNavOffset(4)
        device.pressDPadCenter()
        device.executeShellCommand("input text news")
        device.waitForIdle()
        repeat(5) {
            device.pressDPadDown()
            device.waitForIdle()
        }
    }

    @Test
    fun playerLaunch() = benchmarkRule.measureRepeated(
        packageName = TargetPackage,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.WARM,
        iterations = Iterations,
        setupBlock = { startActivityAndWait() },
    ) {
        openTopNavOffset(1)
        device.pressDPadRight()
        device.pressDPadCenter()
        device.waitForIdle()
        device.pressBack()
    }

    private fun MacrobenchmarkScope.openTopNavOffset(offset: Int) {
        startActivityAndWait()
        device.waitForIdle()
        repeat(6) { device.pressDPadUp() }
        repeat(offset) {
            device.pressDPadRight()
            device.waitForIdle()
        }
        device.pressDPadCenter()
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.browseGrid() {
        device.pressDPadRight()
        device.waitForIdle()
        repeat(18) {
            device.pressDPadDown()
            device.waitForIdle()
        }
        device.findObject(By.focused(true))?.swipe(Direction.DOWN, 0.7f)
        device.waitForIdle()
    }
}
