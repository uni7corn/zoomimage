/*
 * Copyright (C) 2022 panpf <panpfpanpf@outlook.com>
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
package com.github.panpf.zoomimage.sample.ui.zoomimage

import android.os.Bundle
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.panpf.assemblyadapter.recycler.AssemblyRecyclerAdapter
import com.github.panpf.zoomimage.sample.databinding.RecyclerFragmentBinding
import com.github.panpf.zoomimage.sample.prefsService
import com.github.panpf.zoomimage.sample.ui.base.BindingDialogFragment
import com.github.panpf.zoomimage.sample.ui.common.list.ListSeparatorItemFactory
import com.github.panpf.zoomimage.sample.ui.common.menu.MultiSelectMenu
import com.github.panpf.zoomimage.sample.ui.common.menu.MultiSelectMenuItemFactory
import com.github.panpf.zoomimage.sample.ui.common.menu.SwitchMenuFlow
import com.github.panpf.zoomimage.sample.ui.common.menu.SwitchMenuItemFactory

class SettingsDialogFragment : BindingDialogFragment<RecyclerFragmentBinding>() {

    override fun onViewCreated(binding: RecyclerFragmentBinding, savedInstanceState: Bundle?) {
        binding.recyclerRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = AssemblyRecyclerAdapter<Any>(
                listOf(
                    SwitchMenuItemFactory(compactModel = true),
                    MultiSelectMenuItemFactory(requireActivity(), compactModel = true),
                    ListSeparatorItemFactory(),
                ),
                buildList {
                    add(
                        MultiSelectMenu(
                            title = "Scale Type",
                            desc = null,
                            values = ImageView.ScaleType.values()
                                .filter { it != ImageView.ScaleType.MATRIX }.map { it.name },
                            getValue = { prefsService.scaleType.value },
                            onSelect = { _, value ->
                                prefsService.scaleType.value = value
                            }
                        )
                    )
                    add(
                        SwitchMenuFlow(
                            title = "Scroll Bar",
                            desc = null,
                            data = prefsService.scrollBarEnabled,
                        )
                    )
                    add(
                        SwitchMenuFlow(
                            title = "Read Mode",
                            data = prefsService.readModeEnabled,
                            desc = "Long images are displayed in full screen by default"
                        )
                    )
                    add(
                        SwitchMenuFlow(
                            title = "Show Tile Bounds",
                            desc = "Overlay the state and area of the tile on the View",
                            data = prefsService.showTileBounds,
                        )
                    )
                }
            )
        }
    }
}