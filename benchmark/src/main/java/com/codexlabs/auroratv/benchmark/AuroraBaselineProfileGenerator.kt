package com.codexlabs.auroratv.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val BaselineTargetPackage = "com.codexlabs.auroratv"

@RunWith(AndroidJUnit4::class)
class AuroraBaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = BaselineTargetPackage,
    ) {
        startActivityAndWait()
        device.waitForIdle()

        repeat(5) {
            device.pressDPadRight()
            device.waitForIdle()
        }

        repeat(2) {
            device.pressDPadLeft()
            device.waitForIdle()
        }

        device.pressDPadCenter()
        device.waitForIdle()

        repeat(4) {
            device.pressDPadDown()
            device.waitForIdle()
        }

        repeat(3) {
            device.pressDPadUp()
            device.waitForIdle()
        }
    }
}
