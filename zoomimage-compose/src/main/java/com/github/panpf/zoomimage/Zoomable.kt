package com.github.panpf.zoomimage

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import com.github.panpf.zoomimage.internal.detectCanDragGestures
import com.github.panpf.zoomimage.internal.detectZoomGestures
import kotlinx.coroutines.launch

fun Modifier.zoomable(
    state: ZoomableState,
    animationConfig: AnimationConfig
): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    this.onSizeChanged {
            val newContainerSize = it.toSize()
            val oldContainerSize = state.containerSize
            if (newContainerSize != oldContainerSize) {
                state.containerSize = newContainerSize
            }
        }
        .pointerInput(animationConfig) {
            detectTapGestures(onDoubleTap = { offset ->
                coroutineScope.launch {
                    val nextStepScale = state.getNextStepScale()
                    if (animationConfig.doubleTapScaleEnabled) {
                        state.animateScaleTo(
                            newScale = nextStepScale,
                            touchPosition = offset,
                            animationDurationMillis = animationConfig.durationMillis,
                            animationEasing = animationConfig.easing,
                            initialVelocity = animationConfig.initialVelocity
                        )
                    } else {
                        state.snapScaleTo(newScale = nextStepScale, touchPosition = offset)
                    }
                }
            })
        }
        .pointerInput(Unit) {
            detectCanDragGestures(
                canDrag = { horizontally: Boolean, direction: Int ->
                    val scrollEdge =
                        if (horizontally) state.horizontalScrollEdge else state.verticalScrollEdge
                    val targetEdge = if (direction > 0) Edge.END else Edge.START
                    scrollEdge == Edge.NONE || scrollEdge == targetEdge
                },
                onDragStart = {
                    coroutineScope.launch {
                        state.dragStart()
                    }
                },
                onDrag = { change, dragAmount ->
                    coroutineScope.launch {
                        state.drag(change, dragAmount)
                    }
                },
                onDragEnd = {
                    coroutineScope.launch {
                        state.dragEnd()
                    }
                },
                onDragCancel = {
                    coroutineScope.launch {
                        state.dragCancel()
                    }
                }
            )
        }
        .pointerInput(Unit) {
            detectZoomGestures(panZoomLock = true) { centroid: Offset, zoom: Float, _ ->
                coroutineScope.launch {
                    state.transform(zoomChange = zoom, touchCentroid = centroid)
                }
            }
        }
}