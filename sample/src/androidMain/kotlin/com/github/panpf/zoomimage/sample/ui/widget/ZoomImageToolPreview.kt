package com.github.panpf.zoomimage.sample.ui.widget

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.github.panpf.zoomimage.compose.rememberZoomState
import com.github.panpf.zoomimage.sample.image.PhotoPalette
import com.github.panpf.zoomimage.sample.ui.gallery.ZoomImageTool

@Preview
@Composable
fun ZoomImageToolPreview() {
    val zoomState = rememberZoomState()
    val colorScheme = MaterialTheme.colorScheme
    ZoomImageTool(
        imageUri = "ic_rotate_right.xml",
        zoomableState = zoomState.zoomable,
        subsamplingState = zoomState.subsampling,
        infoDialogState = rememberMyDialogState(),
        photoPaletteState = remember { mutableStateOf(PhotoPalette(colorScheme)) }
    )
}