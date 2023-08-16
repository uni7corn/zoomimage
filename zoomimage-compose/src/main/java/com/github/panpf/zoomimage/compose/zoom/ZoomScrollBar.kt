package com.github.panpf.zoomimage.compose.zoom

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalDensity
import com.github.panpf.zoomimage.compose.internal.isNotEmpty
import com.github.panpf.zoomimage.compose.internal.toCompat
import com.github.panpf.zoomimage.util.rotate
import com.github.panpf.zoomimage.util.rotateInSpace
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

fun Modifier.zoomScrollBar(
    zoomableState: ZoomableState,
    scrollBarSpec: ScrollBarSpec = ScrollBarSpec.Default
): Modifier = composed {
    val contentSize = zoomableState.contentSize
    val contentVisibleRect = zoomableState.contentVisibleRect
    val rotation = zoomableState.transform.rotation
    val density = LocalDensity.current
    val sizePx = remember(scrollBarSpec.size) { with(density) { scrollBarSpec.size.toPx() } }
    val marginPx = remember(scrollBarSpec.margin) { with(density) { scrollBarSpec.margin.toPx() } }
    val cornerRadius = remember(sizePx) { CornerRadius(sizePx / 2f, sizePx / 2f) }
    val alphaAnimatable = remember { Animatable(1f) }
    LaunchedEffect(contentVisibleRect) {
        alphaAnimatable.snapTo(targetValue = 1f)
        delay(800)
        alphaAnimatable.animateTo(
            targetValue = 0f,
            animationSpec = TweenSpec(300, easing = LinearOutSlowInEasing)
        )
    }
    if (contentSize.isNotEmpty() && !contentVisibleRect.isEmpty) {
        this.drawWithContent {
            drawContent()

            val alpha = alphaAnimatable.value
            val rotatedContentVisibleRect = contentVisibleRect.toCompat().rotateInSpace(contentSize.toCompat(), rotation.roundToInt())
            val rotatedContentSize = contentSize.toCompat().rotate(rotation.roundToInt())

            @Suppress("UnnecessaryVariable")
            val scrollBarSize = sizePx
            val drawSize = this.size
            if (rotatedContentVisibleRect.width < rotatedContentSize.width) {
                val widthScale = (drawSize.width - marginPx * 4) / rotatedContentSize.width
                drawRoundRect(
                    color = scrollBarSpec.color,
                    topLeft = Offset(
                        x = (marginPx * 2) + (rotatedContentVisibleRect.left * widthScale),
                        y = drawSize.height - marginPx - scrollBarSize
                    ),
                    size = Size(
                        width = rotatedContentVisibleRect.width * widthScale,
                        height = scrollBarSize
                    ),
                    cornerRadius = cornerRadius,
                    style = Fill,
                    alpha = alpha
                )
            }
            if (rotatedContentVisibleRect.height < rotatedContentSize.height) {
                val heightScale = (drawSize.height - marginPx * 4) / rotatedContentSize.height
                drawRoundRect(
                    color = scrollBarSpec.color,
                    topLeft = Offset(
                        x = drawSize.width - marginPx - scrollBarSize,
                        y = (marginPx * 2) + (rotatedContentVisibleRect.top * heightScale)
                    ),
                    size = Size(
                        width = scrollBarSize,
                        height = rotatedContentVisibleRect.height * heightScale
                    ),
                    cornerRadius = cornerRadius,
                    style = Fill,
                    alpha = alpha
                )
            }
        }
    } else {
        this
    }
}