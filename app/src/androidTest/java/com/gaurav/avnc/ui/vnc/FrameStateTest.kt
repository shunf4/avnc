/*
 * Copyright (c) 2023  Gaurav Ujjwal.
 *
 * SPDX-License-Identifier:  GPL-3.0-or-later
 *
 * See COPYING.txt for more details.
 */

package com.gaurav.avnc.ui.vnc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameStateTest {

    @Test
    fun scalingCoerceTest() {
        val minMaxScale = arrayOf(Pair(1F, 1F), Pair(0F, 1F), Pair(1F, 2F), Pair(0F, 5F))

        for (limit in minMaxScale) {
            for (scaleFactor in arrayOf(0F, 0.5F, 1F, 1.5F, 2F, 2.5F, 5F, 10F)) {

                val state = FrameState(limit.first, limit.second)

                state.updateZoom(scaleFactor)

                assertTrue(state.zoomScale >= limit.first)
                assertTrue(state.zoomScale <= limit.second)
            }
        }
    }

    @Test
    fun baseScaleTest1() {
        val state = FrameState()
        state.setWindowSize(100f, 100f)
        state.setFramebufferSize(100f, 100f) //Same as window
        assertEquals(1F, state.baseScale)
    }


    @Test
    fun baseScaleTest2() {
        val state = FrameState()
        state.setWindowSize(100f, 100f)
        state.setFramebufferSize(50f, 100f) //Width is half, But height is same
        assertEquals(1F, state.baseScale)
    }

    @Test
    fun baseScaleTest3() {
        val state = FrameState()
        state.setWindowSize(100f, 100f)
        state.setFramebufferSize(50f, 50f) //Half size
        assertEquals(2F, state.baseScale)
    }

    @Test
    fun baseScaleTest4() {
        val state = FrameState()
        state.setWindowSize(100f, 100f)
        state.setFramebufferSize(200f, 200f) //Double size
        assertEquals(.5F, state.baseScale)
    }

    @Test
    fun baseScaleTest5() {
        val state = FrameState()
        state.setWindowSize(100f, 100f)
        state.setFramebufferSize(100f, 100f)

        state.setWindowSize(50f, 50f)         //If window size is reduced
        assertEquals(.5F, state.baseScale) //base scale should also reduce
    }


    @Test
    fun baseScaleTest6() {
        val state = FrameState()
        state.setWindowSize(100f, 100f)
        state.setFramebufferSize(100f, 100f)

        state.setWindowSize(200f, 200f)      //If window size is increased
        assertEquals(2F, state.baseScale) //base scale should also increase
    }

    @Test
    fun baseScaleTest7() {
        val state = FrameState()
        state.setWindowSize(50f, 100f)
        state.setFramebufferSize(50f, 100f)  //Remote screen in portrait mode
        assertEquals(1F, state.baseScale)
    }

    @Test
    fun baseScaleTest8() {
        val state = FrameState()
        state.setWindowSize(50f, 100f)
        state.setFramebufferSize(25f, 50f)  //Remote screen in portrait mode, but half size
        assertEquals(2F, state.baseScale)
    }

    @Test
    fun baseScaleTest9() {
        val state = FrameState()
        state.setWindowSize(50f, 100f)
        state.setFramebufferSize(100f, 200f)  //Remote screen in portrait mode, but double size
        assertEquals(.5F, state.baseScale)
    }

    @Test
    fun perOrientationZoomScaleEnabledTest() {
        val state = FrameState(usePerOrientationZoom = true)

        state.setWindowSize(100f, 50f)
        state.updateZoom(2f)
        assertEquals(2F, state.zoomScale)

        //Zoom scale should be separate for each orientation
        state.setWindowSize(50f, 100f)
        assertEquals(1f, state.zoomScale)

        state.updateZoom(5f)
        assertEquals(5f, state.zoomScale)

        //Restore orientation
        state.setWindowSize(100f, 50f)
        assertEquals(2f, state.zoomScale)
    }

    @Test
    fun perOrientationZoomScaleDisabledTest() {
        val state = FrameState(usePerOrientationZoom = false)

        state.setWindowSize(100f, 50f)
        state.updateZoom(2f)
        assertEquals(2F, state.zoomScale)

        //Zoom scale should be same in all orientations
        state.setWindowSize(50f, 100f)
        assertEquals(2f, state.zoomScale)
    }

    @Test
    fun positionCoerceTest1() {
        val state = FrameState()
        state.setViewportSize(100f, 100f)
        state.setFramebufferSize(100f, 100f)

        //Because frame and viewport are of same size, we should not be able to pan
        state.pan(10f, 10f)
        assertEquals(0f, state.frameX)
        assertEquals(0f, state.frameY)
    }


    @Test
    fun positionCoerceTest2() {
        val state = FrameState()
        state.setViewportSize(100f, 100f)
        state.setFramebufferSize(100f, 100f)
        state.updateZoom(.5F)

        //Because frame size is less than viewport size, we should be centered
        assertEquals(25f, state.frameX)
        assertEquals(25f, state.frameY)
    }


    @Test
    fun positionCoerceTest3() {
        val state = FrameState()
        state.setViewportSize(100f, 100f)
        state.setFramebufferSize(100f, 100f)

        //Double size
        state.updateZoom(2F)

        //Should not be able to move too much right/down
        state.pan(150f, 150f)
        assertEquals(0f, state.frameX) //Left side of frame and Viewport are aligned
        assertEquals(0f, state.frameY) //Top side of frame and Viewport are aligned


        //Should not be able to move too much left/up
        state.pan(-150f, -150f)
        assertEquals(-100f, state.frameX) //Right side of frame and Viewport are aligned
        assertEquals(-100f, state.frameY) //Bottom side of frame and Viewport are aligned
    }

    //Sanity check for snapshot
    @Test
    fun snapshotTest() {
        val state = FrameState()
        state.setWindowSize(10f, 20f)
        state.setViewportSize(10f, 20f)
        state.setFramebufferSize(40f, 80f)
        state.updateZoom(2f)
        state.pan(-2f, -3f)

        val snapshot = state.getSnapshot()
        assertEquals(10f, snapshot.vpWidth)
        assertEquals(20f, snapshot.vpHeight)
        assertEquals(40f, snapshot.fbWidth)
        assertEquals(80f, snapshot.fbHeight)
        assertEquals(.5f, snapshot.scale)    // (10/40) * 2
        assertEquals(-2f, snapshot.frameX)
        assertEquals(-3f, snapshot.frameY)
    }


    /*********************** Snapping ****************************************/
    @Test
    fun noSnappingInInitialState() {
        val state = FrameState()

        state.updateZoom(1.02f)
        assertEquals(1.02f, state.zoomScale)

        state.setZoom(1f, 1f)
        state.updateZoom(.98f)
        assertEquals(.98f, state.zoomScale)
    }

    @Test
    fun snapUpWhenGoingBelow100Percent() {
        val state = FrameState()

        state.setZoom(2f, 2f)
        assertEquals(2f, state.zoomScale)

        state.updateZoom(1.5f / 2f)
        assertEquals(1.5f, state.zoomScale)

        state.updateZoom(1f / 1.5f)
        assertEquals(1f, state.zoomScale)

        state.updateZoom(.9f / 1f)
        assertEquals(1f, state.zoomScale) // 90% should be snapped-up to 100%

        state.updateZoom(.5f)  // Decreased too much,
        assertTrue(state.zoomScale < 1)  // should no longer be snapped
    }

    @Test
    fun snapDownWhenGoingAbove100Percent() {
        val state = FrameState()

        state.setZoom(.5f, .5f)
        assertEquals(.5f, state.zoomScale)

        state.updateZoom(.8f / .5f)
        assertEquals(.8f, state.zoomScale)

        state.updateZoom(1f / .8f)
        assertEquals(1f, state.zoomScale)

        state.updateZoom(1.1f / 1f)
        assertEquals(1f, state.zoomScale) // 110% should be snapped-down to 100%

        state.updateZoom(2f)   // Increased too much,
        assertTrue(state.zoomScale > 1)  // should no longer be snapped
    }
}