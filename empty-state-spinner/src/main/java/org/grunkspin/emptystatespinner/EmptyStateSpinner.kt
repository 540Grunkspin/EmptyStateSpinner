/**
 * Copyright 2017 Anton Holmberg.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grunkspin.emptystatespinner

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.*

class EmptyStateSpinner @JvmOverloads constructor(
        context: Context,
        attributeSet: AttributeSet? = null,
        defaultStyleAttr: Int = android.R.attr.spinnerStyle
) : Spinner(context, attributeSet, defaultStyleAttr) {

    private var itemHasBeenClicked = false

    /**
     * Set the [adapter] for the spinner and also include an [emptyItem].
     *
     * The [emptyItem] needs to be handled by the adapters [SpinnerAdapter.getView] method but will
     * never be called by the [SpinnerAdapter.getDropDownView] method.
     *
     * @param adapter The adapter (needs to be using a list that can handle insert and remove).
     * @param emptyItem An empty item. This will automatically be added as the first item of the adapter.
     */
    fun <T> setAdapter(adapter: ArrayAdapter<T>, emptyItem: T) {
        this.adapter = EmptyItemAdapter(adapter, emptyItem)
    }

    override fun setSelection(position: Int) {
        getAdapterAsEmptyItemAdapter()?.notifyItemHasBeenClicked()
        super.setSelection(position)
    }


    override fun setSelection(position: Int, animate: Boolean) {
        getAdapterAsEmptyItemAdapter()?.notifyItemHasBeenClicked()
        super.setSelection(position, animate)
    }

    private fun getAdapterAsEmptyItemAdapter() = (adapter as? EmptyItemAdapter<*>)

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(super.onSaveInstanceState()).apply {
            this.itemHasBeenClicked = this@EmptyStateSpinner.itemHasBeenClicked
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            itemHasBeenClicked = state.itemHasBeenClicked
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private inner class EmptyItemAdapter<out T>(
            private val adapter: ArrayAdapter<T>,
            emptyItem: T
    ) : SpinnerAdapter by adapter {

        init {
            @Suppress("LeakingThis")
            adapter.insert(emptyItem, 0)
            adapter.notifyDataSetChanged()
        }

        fun notifyItemHasBeenClicked() {
            itemHasBeenClicked = true
            adapter.notifyDataSetChanged()
        }

        override fun getCount(): Int = adapter.count - 1

        override fun getItem(position: Int): Any = adapter.getItem(getAdjustedPosition(position)) as Any

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View =
                adapter.getView(getAdjustedPosition(position), convertView, parent)

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View =
                adapter.getDropDownView(position + 1, convertView, parent)

        private fun getAdjustedPosition(position: Int) =
                if (itemHasBeenClicked) {
                    position + 1
                } else {
                    position
                }
    }

    class SavedState : BaseSavedState {

        var itemHasBeenClicked: Boolean = false

        constructor(superState: Parcelable) : super(superState)

        private constructor(input: Parcel) : super(input) {
            itemHasBeenClicked = input.readByte() != 0.toByte()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeByte(if (itemHasBeenClicked) 1 else 0)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState = SavedState(parcel)

            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
}

