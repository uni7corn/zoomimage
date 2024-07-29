package com.github.panpf.zoomimage.view.test.internal

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.util.Size
import android.widget.ImageView.ScaleType
import androidx.test.platform.app.InstrumentationRegistry
import com.github.panpf.tools4a.test.ktx.getActivitySync
import com.github.panpf.tools4a.test.ktx.launchActivity
import com.github.panpf.zoomimage.test.SizeDrawableWrapper
import com.github.panpf.zoomimage.test.TestActivity
import com.github.panpf.zoomimage.util.IntSizeCompat
import com.github.panpf.zoomimage.util.OffsetCompat
import com.github.panpf.zoomimage.util.ScaleFactorCompat
import com.github.panpf.zoomimage.util.TransformCompat
import com.github.panpf.zoomimage.util.TransformOriginCompat
import com.github.panpf.zoomimage.view.internal.applyTransform
import com.github.panpf.zoomimage.view.internal.findLifecycle
import com.github.panpf.zoomimage.view.internal.intrinsicSize
import com.github.panpf.zoomimage.view.internal.requiredMainThread
import com.github.panpf.zoomimage.view.internal.requiredWorkThread
import com.github.panpf.zoomimage.view.internal.scale
import com.github.panpf.zoomimage.view.internal.toAlignment
import com.github.panpf.zoomimage.view.internal.toContentScale
import com.github.panpf.zoomimage.zoom.AlignmentCompat
import com.github.panpf.zoomimage.zoom.ContentScaleCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ViewPlatformUtilsTest {

    @Test
    fun testRequiredMainThread() {
        assertFailsWith(IllegalStateException::class) {
            requiredMainThread()
        }
        runBlocking(Dispatchers.Main) {
            requiredMainThread()
        }
    }

    @Test
    fun testRequiredWorkThread() {
        requiredWorkThread()

        assertFailsWith(IllegalStateException::class) {
            runBlocking(Dispatchers.Main) {
                requiredWorkThread()
            }
        }
    }

    @Test
    fun testRectScale() {
        assertEquals(
            expected = Rect(146, 230, 533, 1457),
            actual = Rect(97, 153, 355, 971).scale(1.5f),
        )
        assertEquals(
            expected = Rect(320, 505, 1172, 3204),
            actual = Rect(97, 153, 355, 971).scale(3.3f),
        )
    }

    @Test
    fun testContextFindLifecycle() {
        val context = InstrumentationRegistry.getInstrumentation().context
        Assert.assertNull(context.findLifecycle())

        Assert.assertNull(context.applicationContext.findLifecycle())

        val activity = TestActivity::class.launchActivity().getActivitySync()
        Assert.assertSame(activity.lifecycle, activity.findLifecycle())
    }

    @Test
    fun testDrawableIntrinsicSize() {
        assertEquals(
            expected = IntSizeCompat(100, 200),
            actual = SizeDrawableWrapper(ColorDrawable(Color.RED), Size(100, 200)).intrinsicSize()
        )
        assertEquals(
            expected = IntSizeCompat(101, 203),
            actual = SizeDrawableWrapper(ColorDrawable(Color.RED), Size(101, 203)).intrinsicSize()
        )
        assertEquals(
            expected = IntSizeCompat(0, 100),
            actual = SizeDrawableWrapper(ColorDrawable(Color.RED), Size(0, 100)).intrinsicSize()
        )
        assertEquals(
            expected = IntSizeCompat(100, 0),
            actual = SizeDrawableWrapper(ColorDrawable(Color.RED), Size(100, 0)).intrinsicSize()
        )
        assertEquals(
            expected = IntSizeCompat(-100, -300),
            actual = SizeDrawableWrapper(ColorDrawable(Color.RED), Size(-100, -300)).intrinsicSize()
        )
    }

    @Test
    fun testScaleTypeToContentScale() {
        assertEquals(
            expected = ContentScaleCompat.None,
            actual = ScaleType.MATRIX.toContentScale()
        )
        assertEquals(
            expected = ContentScaleCompat.FillBounds,
            actual = ScaleType.FIT_XY.toContentScale()
        )
        assertEquals(
            expected = ContentScaleCompat.Fit,
            actual = ScaleType.FIT_START.toContentScale()
        )
        assertEquals(
            expected = ContentScaleCompat.Fit,
            actual = ScaleType.FIT_CENTER.toContentScale()
        )
        assertEquals(
            expected = ContentScaleCompat.Fit,
            actual = ScaleType.FIT_END.toContentScale()
        )
        assertEquals(
            expected = ContentScaleCompat.None,
            actual = ScaleType.CENTER.toContentScale()
        )
        assertEquals(
            expected = ContentScaleCompat.Crop,
            actual = ScaleType.CENTER_CROP.toContentScale()
        )
        assertEquals(
            expected = ContentScaleCompat.Inside,
            actual = ScaleType.CENTER_INSIDE.toContentScale()
        )
    }

    @Test
    fun testScaleTypeToAlignment() {
        assertEquals(
            expected = AlignmentCompat.TopStart,
            actual = ScaleType.MATRIX.toAlignment()
        )
        assertEquals(
            expected = AlignmentCompat.TopStart,
            actual = ScaleType.FIT_XY.toAlignment()
        )
        assertEquals(
            expected = AlignmentCompat.TopStart,
            actual = ScaleType.FIT_START.toAlignment()
        )
        assertEquals(
            expected = AlignmentCompat.Center,
            actual = ScaleType.FIT_CENTER.toAlignment()
        )
        assertEquals(
            expected = AlignmentCompat.BottomEnd,
            actual = ScaleType.FIT_END.toAlignment()
        )
        assertEquals(
            expected = AlignmentCompat.Center,
            actual = ScaleType.CENTER.toAlignment()
        )
        assertEquals(
            expected = AlignmentCompat.Center,
            actual = ScaleType.CENTER_CROP.toAlignment()
        )
        assertEquals(
            expected = AlignmentCompat.Center,
            actual = ScaleType.CENTER_INSIDE.toAlignment()
        )
    }

    @Test
    fun testMatrixApplyTransform() {
        val containerSize = IntSizeCompat(1080, 1920)
        assertEquals(
            expected = "[1.0, 0.0, 0.0][0.0, 1.0, 0.0][0.0, 0.0, 1.0]",
            actual = Matrix().toShortString()
        )
        assertEquals(
            expected = "[1.0, 0.0, 0.0][0.0, 1.0, 0.0][0.0, 0.0, 1.0]",
            actual = Matrix()
                .applyTransform(TransformCompat.Origin, containerSize)
                .toShortString()
        )
        assertEquals(
            expected = "[0.0, 1.5, -1058487.0][-0.5, 0.0, 713607.0][0.0, 0.0, 1.0]",
            actual = Matrix()
                .applyTransform(
                    transform = TransformCompat.Origin.copy(
                        scale = ScaleFactorCompat(1.5f, 0.5f),
                        rotation = 270f,
                        rotationOrigin = TransformOriginCompat(333f, 555f),
                        offset = OffsetCompat(453f, 987f)
                    ),
                    containerSize = containerSize
                ).toShortString()
        )
    }
}