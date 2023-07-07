package com.github.panpf.zoomimage.compose

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ScaleFactor
import com.github.panpf.zoomimage.compose.internal.times
import com.github.panpf.zoomimage.compose.internal.toShortString

data class Transform(
    val scale: ScaleFactor,
    val offset: Offset,
    val rotation: Float = 0f,
) {

    constructor(
        scaleX: Float,
        scaleY: Float,
        offsetX: Float,
        offsetY: Float,
        rotation: Float = 0f,
    ) : this(
        scale = ScaleFactor(scaleX = scaleX, scaleY = scaleY),
        offset = Offset(x = offsetX, y = offsetY),
        rotation = rotation,
    )

    val scaleX: Float
        get() = scale.scaleX
    val scaleY: Float
        get() = scale.scaleY
    val offsetX: Float
        get() = offset.x
    val offsetY: Float
        get() = offset.y

    companion object {
        val Origin = Transform(scale = ScaleFactor(1f, 1f), offset = Offset.Zero, rotation = 0f)
    }

    override fun toString(): String {
        return "Transform(scale=${scale.toShortString()}, offset=${offset.toShortString()}, rotation=$rotation)"
    }
}

/**
 * Linearly interpolate between two Transform.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid (and can
 * easily be generated by curves).
 *
 * Values for [fraction] are usually obtained from an [Animation<Float>], such as
 * an `AnimationController`.
 */
@Stable
fun lerp(start: Transform, stop: Transform, fraction: Float): Transform {
    return Transform(
        scale = androidx.compose.ui.layout.lerp(start.scale, stop.scale, fraction),
        offset = androidx.compose.ui.geometry.lerp(start.offset, stop.offset, fraction),
        rotation = androidx.compose.ui.util.lerp(start.rotation, stop.rotation, fraction),
    )
}

fun Transform.toShortString(): String =
    "(${scale.toShortString()},${offset.toShortString()},$rotation)"

fun Transform.times(scaleFactor: ScaleFactor): Transform {
    return this.copy(
        scale = ScaleFactor(
            scaleX = scale.scaleX * scaleFactor.scaleX,
            scaleY = scale.scaleY * scaleFactor.scaleY,
        ),
        offset = Offset(
            x = offset.x * scaleFactor.scaleX,
            y = offset.y * scaleFactor.scaleY,
        ),
    )
}

fun Transform.div(scaleFactor: ScaleFactor): Transform {
    return this.copy(
        scale = ScaleFactor(
            scaleX = scale.scaleX / scaleFactor.scaleX,
            scaleY = scale.scaleY / scaleFactor.scaleY,
        ),
        offset = Offset(
            x = offset.x / scaleFactor.scaleX,
            y = offset.y / scaleFactor.scaleY,
        ),
    )
}

fun Transform.concat(other: Transform): Transform {
    return Transform(
        scale = scale.times(other.scale),
        offset = offset + other.offset,
        rotation = rotation + other.rotation,
    )
}