package org.ton.wallet.lib.screen.controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

abstract class BaseBinding private constructor(val root: View) {

    protected constructor(@LayoutRes layoutRes: Int, inflater: LayoutInflater, container: ViewGroup?) : this(inflater.inflate(layoutRes, container, false))

    protected constructor(@LayoutRes layoutRes: Int, viewGroup: ViewGroup): this(layoutRes, LayoutInflater.from(viewGroup.context), viewGroup)
}