/*
 * Copyright (C) 2023 panpf <panpfpanpf@outlook.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.panpf.zoomimage

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.github.panpf.zoomimage.subsampling.TileAnimationSpec
import com.github.panpf.zoomimage.util.IntSizeCompat
import com.github.panpf.zoomimage.util.Logger
import com.github.panpf.zoomimage.view.R.styleable
import com.github.panpf.zoomimage.view.internal.applyTransform
import com.github.panpf.zoomimage.view.internal.findLifecycle
import com.github.panpf.zoomimage.view.internal.intrinsicSize
import com.github.panpf.zoomimage.view.internal.toAlignment
import com.github.panpf.zoomimage.view.internal.toContentScale
import com.github.panpf.zoomimage.view.subsampling.SubsamplingEngine
import com.github.panpf.zoomimage.view.subsampling.internal.TileDrawHelper
import com.github.panpf.zoomimage.view.zoom.OnViewLongPressListener
import com.github.panpf.zoomimage.view.zoom.OnViewTapListener
import com.github.panpf.zoomimage.view.zoom.ScrollBarSpec
import com.github.panpf.zoomimage.view.zoom.ZoomAnimationSpec
import com.github.panpf.zoomimage.view.zoom.ZoomableEngine
import com.github.panpf.zoomimage.view.zoom.internal.ScrollBarHelper
import com.github.panpf.zoomimage.view.zoom.internal.TouchHelper
import com.github.panpf.zoomimage.zoom.AlignmentCompat
import com.github.panpf.zoomimage.zoom.ContentScaleCompat
import com.github.panpf.zoomimage.zoom.ReadMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 * A native ImageView that zoom and subsampling huge images
 *
 * Example usages:
 *
 * ```kotlin
 * val zoomImageView = ZoomImageView(context)
 * zoomImageView.setImageResource(R.drawable.huge_image_thumbnail)
 * val imageSource = ImageSource.fromResource(context, R.drawable.huge_image)
 * zoomImageView.subsampling.setImageSource(imageSource)
 * ```
 *
 * @see com.github.panpf.zoomimage.view.test.ZoomImageViewTest
 */
open class ZoomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(context, attrs, defStyle) {

    protected val _zoomableEngine: ZoomableEngine?  // Used when the overridden method is called by the parent class constructor
    protected val _subsamplingEngine: SubsamplingEngine?  // Used when the overridden method is called by the parent class constructor

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val touchHelper: TouchHelper
    private val tileDrawHelper: TileDrawHelper
    private val cacheImageMatrix = Matrix()
    private var wrappedScaleType: ScaleType
    private var scrollBarHelper: ScrollBarHelper? = null

    val logger = Logger(tag = getLogTag() ?: "ZoomImageView")

    /**
     * Control the ability to zoom, pan, rotate
     */
    val zoomable: ZoomableEngine
        get() = _zoomableEngine ?: throw IllegalStateException("zoomable not initialized")

    /**
     * Control the ability to subsampling
     */
    val subsampling: SubsamplingEngine
        get() = _subsamplingEngine
            ?: throw IllegalStateException("subsampling not initialized")

    /**
     * Setup whether to enable scroll bar and configure scroll bar style
     */
    var scrollBar: ScrollBarSpec? = ScrollBarSpec.Default
        set(value) {
            if (field != value) {
                field = value
                resetScrollBarHelper()
            }
        }

    /**
     * Click event listener with touch location
     */
    var onViewTapListener: OnViewTapListener?
        get() = touchHelper.onViewTapListener
        set(value) {
            touchHelper.onViewTapListener = value
        }

    /**
     * Long press event listener with touch location
     */
    var onViewLongPressListener: OnViewLongPressListener?
        get() = touchHelper.onViewLongPressListener
        set(value) {
            touchHelper.onViewLongPressListener = value
        }

    init {
        /* ScaleType */
        val initScaleType = super.getScaleType()
        super.setScaleType(ScaleType.MATRIX)
        wrappedScaleType = initScaleType

        /* ZoomableEngine */
        val zoomableEngine = ZoomableEngine(logger, this).apply {
            this@ZoomImageView._zoomableEngine = this
            contentScaleState.value = initScaleType.toContentScale()
            alignmentState.value = initScaleType.toAlignment()
            resetDrawableSize()
        }
        touchHelper = TouchHelper(this, zoomableEngine)

        /* SubsamplingEngine */
        val subsamplingEngine = SubsamplingEngine(logger, zoomableEngine, this).apply {
            this@ZoomImageView._subsamplingEngine = this
            post {
                val view = this@ZoomImageView
                if (view.isAttachedToWindow) {
                    val lifecycle =
                        view.findViewTreeLifecycleOwner()?.lifecycle ?: view.context.findLifecycle()
                    if (lifecycle != null) {
                        this.lifecycle = lifecycle
                    }
                }
            }
        }
        tileDrawHelper =
            TileDrawHelper(logger, this@ZoomImageView, zoomableEngine, subsamplingEngine)

        /* ScrollBar */
        resetScrollBarHelper()

        parseAttrs(attrs)

        // Must be immediate, otherwise the user will see the image move quickly from the top to the center
        coroutineScope.launch(Dispatchers.Main.immediate) {
            zoomable.transformState.collect { transform ->
                val containerSize = zoomable.containerSizeState.value
                val matrix = cacheImageMatrix.applyTransform(transform, containerSize)
                super.setImageMatrix(matrix)

                scrollBarHelper?.onMatrixChanged()
            }
        }
    }


    /**************************************** Internal ********************************************/

    open fun getLogTag(): String? = null

    private fun parseAttrs(attrs: AttributeSet?) {
        val array = context.obtainStyledAttributes(attrs, styleable.ZoomImageView)
        try {
            if (array.hasValue(styleable.ZoomImageView_contentScale)) {
                val contentScaleCode = array.getInt(styleable.ZoomImageView_contentScale, -1)
                zoomable.contentScaleState.value = when (contentScaleCode) {
                    0 -> ContentScaleCompat.Crop
                    1 -> ContentScaleCompat.Fit
                    2 -> ContentScaleCompat.FillHeight
                    3 -> ContentScaleCompat.FillWidth
                    4 -> ContentScaleCompat.Inside
                    5 -> ContentScaleCompat.None
                    6 -> ContentScaleCompat.FillBounds
                    else -> throw IllegalArgumentException("Unknown contentScaleCode: $contentScaleCode")
                }
            }

            if (array.hasValue(styleable.ZoomImageView_alignment)) {
                val alignmentCode = array.getInt(styleable.ZoomImageView_alignment, -1)
                zoomable.alignmentState.value = when (alignmentCode) {
                    0 -> AlignmentCompat.TopStart
                    1 -> AlignmentCompat.TopCenter
                    2 -> AlignmentCompat.TopEnd
                    3 -> AlignmentCompat.CenterStart
                    4 -> AlignmentCompat.Center
                    5 -> AlignmentCompat.CenterEnd
                    6 -> AlignmentCompat.BottomStart
                    7 -> AlignmentCompat.BottomCenter
                    8 -> AlignmentCompat.BottomEnd
                    else -> throw IllegalArgumentException("Unknown alignmentCode: $alignmentCode")
                }
            }

            if (array.hasValue(styleable.ZoomImageView_animateScale)) {
                val animateScale = array.getBoolean(styleable.ZoomImageView_animateScale, false)
                zoomable.animationSpecState.value =
                    if (animateScale) ZoomAnimationSpec.Default else ZoomAnimationSpec.None
            }

            if (array.hasValue(styleable.ZoomImageView_rubberBandScale)) {
                val rubberBandScale =
                    array.getBoolean(styleable.ZoomImageView_rubberBandScale, false)
                zoomable.rubberBandScaleState.value = rubberBandScale
            }

            if (array.hasValue(styleable.ZoomImageView_threeStepScale)) {
                val threeStepScale = array.getBoolean(styleable.ZoomImageView_threeStepScale, false)
                zoomable.threeStepScaleState.value = threeStepScale
            }

            if (array.hasValue(styleable.ZoomImageView_limitOffsetWithinBaseVisibleRect)) {
                val limitOffsetWithinBaseVisibleRect = array.getBoolean(
                    styleable.ZoomImageView_limitOffsetWithinBaseVisibleRect,
                    false
                )
                zoomable.limitOffsetWithinBaseVisibleRectState.value =
                    limitOffsetWithinBaseVisibleRect
            }

            if (array.hasValue(styleable.ZoomImageView_showTileBounds)) {
                val showTileBounds = array.getBoolean(styleable.ZoomImageView_showTileBounds, false)
                subsampling.showTileBoundsState.value = showTileBounds
            }

            if (array.hasValue(styleable.ZoomImageView_pausedContinuousTransformTypes)) {
                val pausedContinuousTransformTypes =
                    array.getInt(styleable.ZoomImageView_pausedContinuousTransformTypes, 0)
                subsampling.pausedContinuousTransformTypesState.value =
                    pausedContinuousTransformTypes
            }

            if (array.hasValue(styleable.ZoomImageView_disabledBackgroundTiles)) {
                val disabledBackgroundTiles =
                    array.getBoolean(styleable.ZoomImageView_disabledBackgroundTiles, false)
                subsampling.disabledBackgroundTilesState.value = disabledBackgroundTiles
            }

            if (array.hasValue(styleable.ZoomImageView_tileAnimation)) {
                val tileAnimation = array.getBoolean(styleable.ZoomImageView_tileAnimation, false)
                subsampling.tileAnimationSpecState.value =
                    if (tileAnimation) TileAnimationSpec.Default else TileAnimationSpec.None
            }

            val disabledScrollBar =
                array.getBoolean(styleable.ZoomImageView_disabledScrollBar, false)
            scrollBar = if (!disabledScrollBar) {
                ScrollBarSpec(
                    color = array.getColor(
                        styleable.ZoomImageView_scrollBarColor,
                        ScrollBarSpec.DEFAULT_COLOR
                    ),
                    size = array.getDimension(
                        styleable.ZoomImageView_scrollBarSize,
                        ScrollBarSpec.DEFAULT_SIZE * resources.displayMetrics.density
                    ),
                    margin = array.getDimension(
                        styleable.ZoomImageView_scrollBarMargin,
                        ScrollBarSpec.DEFAULT_MARGIN * resources.displayMetrics.density
                    ),
                )
            } else {
                null
            }

            if (array.hasValue(styleable.ZoomImageView_readMode)) {
                val readModeCode = array.getInt(styleable.ZoomImageView_readMode, -1)
                zoomable.readModeState.value = when (readModeCode) {
                    0 -> ReadMode.Default
                    1 -> ReadMode.Default.copy(sizeType = ReadMode.SIZE_TYPE_HORIZONTAL)
                    2 -> ReadMode.Default.copy(sizeType = ReadMode.SIZE_TYPE_VERTICAL)
                    3 -> null
                    else -> throw IllegalArgumentException("Unknown readModeCode: $readModeCode")
                }
            }
        } finally {
            array.recycle()
        }
    }

    private fun resetDrawableSize() {
        _zoomableEngine?.contentSizeState?.value = drawable?.intrinsicSize() ?: IntSizeCompat.Zero
    }

    private fun resetScrollBarHelper() {
        scrollBarHelper?.cancel()
        scrollBarHelper = null
        val scrollBarSpec = this.scrollBar
        if (scrollBarSpec != null) {
            scrollBarHelper = ScrollBarHelper(this, scrollBarSpec, zoomable)
        }
    }

    override fun setImageDrawable(drawable: Drawable?) {
        val oldDrawable = this.drawable
        super.setImageDrawable(drawable)
        val newDrawable = this.drawable
        if (oldDrawable !== newDrawable) {
            onDrawableChanged(oldDrawable, newDrawable)
        }
    }

    override fun setImageURI(uri: Uri?) {
        val oldDrawable = this.drawable
        super.setImageURI(uri)
        val newDrawable = this.drawable
        if (oldDrawable !== newDrawable) {
            onDrawableChanged(oldDrawable, newDrawable)
        }
    }

    open fun onDrawableChanged(oldDrawable: Drawable?, newDrawable: Drawable?) {
        resetDrawableSize()
    }

    override fun setScaleType(scaleType: ScaleType) {
        val zoomEngine = _zoomableEngine
        if (zoomEngine != null) {
            this.wrappedScaleType = scaleType
            zoomEngine.contentScaleState.value = scaleType.toContentScale()
            zoomEngine.alignmentState.value = scaleType.toAlignment()
        } else {
            super.setScaleType(scaleType)
        }
    }

    override fun getScaleType(): ScaleType {
        return if (_zoomableEngine != null) wrappedScaleType else super.getScaleType()
    }

    override fun setImageMatrix(matrix: Matrix?) {
        logger.w("ZoomImageView. setImageMatrix() is intercepted")
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val zoomable = _zoomableEngine ?: return

        val oldContainerSize = zoomable.containerSizeState.value
        val newContainerSize = IntSizeCompat(
            width = width - paddingLeft - paddingRight,
            height = height - paddingTop - paddingBottom
        )
        if (newContainerSize != oldContainerSize) {
            zoomable.containerSizeState.value = newContainerSize
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        tileDrawHelper.drawTiles(canvas)
        scrollBarHelper?.onDraw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return touchHelper.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun canScrollHorizontally(direction: Int): Boolean =
        _zoomableEngine?.canScroll(horizontal = true, direction) == true

    override fun canScrollVertically(direction: Int): Boolean =
        _zoomableEngine?.canScroll(horizontal = false, direction) == true
}