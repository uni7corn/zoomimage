package com.github.panpf.zoomimage.subsampling

import okio.Buffer
import okio.Source
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.readResourceBytes

/**
 * Create an image source from a compose resource path.
 *
 * @param resourcePath The path of the file to read in the compose resource's directory. For example:
 * * 'composeResources/com.github.panpf.zoomimage.sample.resources/files/huge_china.jpg'
 * * Res.getUri("files/huge_china.jpg") on android: 'jar:file:/data/app/com.github.panpf.sketch4.sample-1==/base.apk!/composeResources/com.github.panpf.zoomimage.sample.resources/files/huge_china.jpg'
 * * Res.getUri("files/huge_china.jpg") on desktop: 'file:/Users/panpf/Workspace/zoomimage/sample/build/processedResources/desktop/main/composeResources/com.github.panpf.zoomimage.sample.resources/files/huge_china.jpg'
 * * Res.getUri("files/huge_china.jpg") on js: 'http://localhost:8080/./composeResources/com.github.panpf.zoomimage.sample.resources/files/huge_china.jpg'
 * * Res.getUri("files/huge_china.jpg") on ios: 'file:///Users/panpf/Library/Developer/ CoreSimulator/Devices/F828C881-A750-432B-8210-93A84C45E/data/Containers/Bundle/Application/CBD75605-D35E-47A7-B56B-6C5690B062CC/SketchSample.app/compose-resources/composeResources/com.github.panpf.zoomimage.sample.resources/files/huge_china.jpg'
 *
 * @see com.github.panpf.zoomimage.compose.resources.test.ComposeResourceImageSourceTest.testFromComposeResource
 */
fun ImageSource.Companion.fromComposeResource(
    resourcePath: String,
): ComposeResourceImageSource.Factory {
    return ComposeResourceImageSource.Factory(composeResourceUriToResourcePath(resourcePath))
}

/**
 * Build a resource path that loads images from the compose resources folder
 *
 * @param resourcePath The path of the file to read in the compose resource's directory. For example:
 * * 'composeResources/com.github.panpf.zoomimage.sample.resources/files/huge_china.jpg'
 * * Res.getUri("files/huge_china.jpg") on android: 'jar:file:/data/app/com.github.panpf.sketch4.sample-1==/base.apk!/composeResources/com.github.panpf.zoomimage.sample.resources/files/huge_china.jpg'
 * * Res.getUri("files/huge_china.jpg") on desktop: 'file:/Users/panpf/Workspace/zoomimage/sample/build/processedResources/desktop/main/composeResources/com.github.panpf.zoomimage.sample.resources/files/huge_china.jpg'
 * * Res.getUri("files/huge_china.jpg") on js: 'http://localhost:8080/./composeResources/com.github.panpf.zoomimage.sample.resources/files/huge_china.jpg'
 * * Res.getUri("files/huge_china.jpg") on ios: 'file:///Users/panpf/Library/Developer/ CoreSimulator/Devices/F828C881-A750-432B-8210-93A84C45E/data/Containers/Bundle/Application/CBD75605-D35E-47A7-B56B-6C5690B062CC/SketchSample.app/compose-resources/composeResources/com.github.panpf.zoomimage.sample.resources/files/huge_china.jpg'
 * @return 'composeResources/com.github.panpf.zoomimage.sample.resources/files/huge_china.jpg'
 * @see com.github.panpf.zoomimage.compose.resources.test.ComposeResourceImageSourceTest.testComposeResourceUriToResourcePath
 */
fun composeResourceUriToResourcePath(resourcePath: String): String {
    if (resourcePath.startsWith("composeResources/")) {
        return resourcePath
    }

    val index = resourcePath.indexOf("/composeResources/")
    if (index != -1) {
        val realResourcePath = resourcePath.substring(index + 1)
        return realResourcePath
    }

    throw IllegalArgumentException("Unsupported compose resource path: $resourcePath")
}

/**
 * Image source for compose resources.
 * @param resourcePath The path of the file to read in the compose resource's directory. For example:
 * * 'composeResources/com.github.panpf.zoomimage.sample.resources/files/huge_china.jpg'
 *
 * @see com.github.panpf.zoomimage.compose.resources.test.ComposeResourceImageSourceTest
 */
class ComposeResourceImageSource(val resourcePath: String, val bytes: ByteArray) : ImageSource {

    override val key: String = "compose.resource://$resourcePath"

    override fun openSource(): Source {
        return Buffer().write(bytes)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as ComposeResourceImageSource
        return resourcePath == other.resourcePath
    }

    override fun hashCode(): Int {
        return resourcePath.hashCode()
    }

    override fun toString(): String {
        return "ComposeResourceImageSource($resourcePath)"
    }

    class Factory(val resourcePath: String) : ImageSource.Factory {

        override val key: String = "compose.resource://$resourcePath"

        @OptIn(InternalResourceApi::class)
        override suspend fun create(): ComposeResourceImageSource {
            val bytes = readResourceBytes(resourcePath)
            return ComposeResourceImageSource(resourcePath, bytes)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as Factory
            return resourcePath == other.resourcePath
        }

        override fun hashCode(): Int {
            return resourcePath.hashCode()
        }

        override fun toString(): String {
            return "ComposeResourceImageSource.Factory($resourcePath)"
        }
    }
}