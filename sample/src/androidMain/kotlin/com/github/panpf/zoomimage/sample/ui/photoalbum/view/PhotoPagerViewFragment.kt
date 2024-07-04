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

package com.github.panpf.zoomimage.sample.ui.photoalbum.view

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle.State
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.github.panpf.assemblyadapter.pager2.AssemblyFragmentStateAdapter
import com.github.panpf.sketch.loadImage
import com.github.panpf.sketch.request.LoadState
import com.github.panpf.sketch.resize.Precision.LESS_PIXELS
import com.github.panpf.sketch.transform.BlurTransformation
import com.github.panpf.tools4a.display.ktx.getScreenSize
import com.github.panpf.tools4k.lang.asOrThrow
import com.github.panpf.zoomimage.sample.R
import com.github.panpf.zoomimage.sample.appSettings
import com.github.panpf.zoomimage.sample.databinding.FragmentPhotoPagerBinding
import com.github.panpf.zoomimage.sample.image.PaletteDecodeInterceptor
import com.github.panpf.zoomimage.sample.image.PhotoPalette
import com.github.panpf.zoomimage.sample.image.simplePalette
import com.github.panpf.zoomimage.sample.ui.base.view.BaseBindingFragment
import com.github.panpf.zoomimage.sample.ui.examples.view.ZoomImageViewOptionsDialogFragment
import com.github.panpf.zoomimage.sample.ui.examples.view.ZoomImageViewOptionsDialogFragmentArgs
import com.github.panpf.zoomimage.sample.ui.examples.view.ZoomViewType
import com.github.panpf.zoomimage.sample.ui.gallery.PhotoPaletteViewModel
import com.github.panpf.zoomimage.sample.ui.model.Photo
import com.github.panpf.zoomimage.sample.util.collectWithLifecycle
import com.github.panpf.zoomimage.sample.util.repeatCollectWithLifecycle
import kotlinx.serialization.json.Json

class PhotoPagerViewFragment : BaseBindingFragment<FragmentPhotoPagerBinding>() {

    private val args by navArgs<PhotoPagerViewFragmentArgs>()
    private val photoList by lazy {
        Json.decodeFromString<List<Photo>>(args.photos)
    }
    private val zoomViewType by lazy { ZoomViewType.valueOf(args.zoomViewType) }
    private val photoPaletteViewModel by viewModels<PhotoPaletteViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lightStatusAndNavigationBar = false
    }

    override fun getStatusBarInsetsView(binding: FragmentPhotoPagerBinding): View {
        return binding.statusBarInsetsLayout
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        binding: FragmentPhotoPagerBinding,
        savedInstanceState: Bundle?
    ) {
        binding.backImage.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.bgImage.requestState.loadState.repeatCollectWithLifecycle(
            viewLifecycleOwner,
            State.STARTED
        ) {
            if (it is LoadState.Success) {
                photoPaletteViewModel.setPhotoPalette(
                    PhotoPalette(
                        palette = it.result.simplePalette,
                        primaryColor = resources.getColor(R.color.md_theme_primary),
                        primaryContainerColor = resources.getColor(R.color.md_theme_primaryContainer)
                    )
                )
            }
        }

        binding.orientationImage.apply {
            appSettings.horizontalPagerLayout.collectWithLifecycle(viewLifecycleOwner) {
                val meuIcon = if (it) R.drawable.ic_swap_ver else R.drawable.ic_swap_hor
                setImageResource(meuIcon)
            }
            setOnClickListener {
                appSettings.horizontalPagerLayout.value =
                    !appSettings.horizontalPagerLayout.value
            }
        }

        if (zoomViewType != ZoomViewType.SubsamplingScaleImageView) {
            binding.settingsImage.setOnClickListener {
                ZoomImageViewOptionsDialogFragment().apply {
                    arguments = ZoomImageViewOptionsDialogFragmentArgs(
                        zoomViewType = args.zoomViewType,
                    ).toBundle()
                }.show(childFragmentManager, null)
            }
        }

        binding.pager.apply {
            offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
            appSettings.horizontalPagerLayout.collectWithLifecycle(viewLifecycleOwner) {
                orientation =
                    if (it) ViewPager2.ORIENTATION_HORIZONTAL else ViewPager2.ORIENTATION_VERTICAL
            }
            adapter = AssemblyFragmentStateAdapter(
                fragment = this@PhotoPagerViewFragment,
                itemFactoryList = listOf(zoomViewType.createPageItemFactory()),
                initDataList = photoList.map { it.originalUrl }
            )

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    val imageUrl = photoList[position].listThumbnailUrl
                    loadBgImage(binding, imageUrl)
                }
            })

            post {
                setCurrentItem(args.position - args.startPosition, false)
            }
        }

        binding.pageNumberText.apply {
            val updateCurrentPageNumber: () -> Unit = {
                val pageNumber = args.startPosition + binding.pager.currentItem + 1
                text = "$pageNumber\n·\n${args.totalCount}"
            }
            binding.pager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateCurrentPageNumber()
                }
            })
            updateCurrentPageNumber()
        }

        photoPaletteViewModel.photoPaletteState.repeatCollectWithLifecycle(
            owner = viewLifecycleOwner,
            state = State.STARTED
        ) { photoPalette ->
            listOf(
                binding.backImage,
                binding.orientationImage,
                binding.settingsImage,
                binding.pageNumberText
            ).forEach {
                it.background.asOrThrow<GradientDrawable>().setColor(photoPalette.containerColorInt)
            }
        }
    }

    private fun loadBgImage(binding: FragmentPhotoPagerBinding, imageUrl: String) {
        binding.bgImage.loadImage(imageUrl) {
            val screenSize = requireContext().getScreenSize()
            resize(
                width = screenSize.x / 4,
                height = screenSize.y / 4,
                precision = LESS_PIXELS
            )
            addTransformations(
                BlurTransformation(
                    radius = 20,
                    maskColor = ColorUtils.setAlphaComponent(Color.BLACK, 100)
                )
            )
            disallowAnimatedImage()
            crossfade(alwaysUse = true, durationMillis = 400)
            resizeOnDraw()
            components {
                addDecodeInterceptor(PaletteDecodeInterceptor())
            }
        }
    }
}