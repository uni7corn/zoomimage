/*
 * Copyright (C) 2024 panpf <panpfpanpf@outlook.com>
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

package com.github.panpf.zoomimage.glide

import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.EngineResourceWrapper
import com.bumptech.glide.load.engine.GlideEngine
import com.bumptech.glide.load.engine.createGlideEngine
import com.bumptech.glide.load.engine.newEngineKey
import com.github.panpf.zoomimage.subsampling.AndroidTileBitmap
import com.github.panpf.zoomimage.subsampling.BitmapFrom
import com.github.panpf.zoomimage.subsampling.ImageInfo
import com.github.panpf.zoomimage.subsampling.TileBitmap
import com.github.panpf.zoomimage.subsampling.TileBitmapCache

/**
 * Implement [TileBitmapCache] based on Glide
 *
 * @see com.github.panpf.zoomimage.core.glide.test.GlideTileBitmapCacheTest
 */
class GlideTileBitmapCache(private val glide: Glide) : TileBitmapCache {

    private val glideEngine: GlideEngine? by lazy {
        createGlideEngine(glide)
    }

    @Suppress("INACCESSIBLE_TYPE")
    override fun get(key: String): TileBitmap? {
        val engineKey = newEngineKey(key)
        val resource = glideEngine?.loadFromMemory(key = engineKey, isMemoryCacheable = true)
        return resource?.let { GlideTileBitmap(it, key, BitmapFrom.MEMORY_CACHE) }
    }

    @Suppress("INACCESSIBLE_TYPE")
    override fun put(
        key: String,
        tileBitmap: TileBitmap,
        imageUrl: String,
        imageInfo: ImageInfo,
    ): TileBitmap? {
        val glideEngine = glideEngine ?: return null
        tileBitmap as AndroidTileBitmap
        val bitmap = tileBitmap.bitmap!!
        val engineKey = newEngineKey(key)
        val resource = glideEngine.put(bitmap, engineKey)
        return GlideTileBitmap(EngineResourceWrapper(resource), key, tileBitmap.bitmapFrom)
    }
}