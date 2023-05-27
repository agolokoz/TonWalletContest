package org.ton.wallet.lib.navigator

import android.os.Bundle
import com.bluelinelabs.conductor.ControllerChangeHandler
import org.ton.wallet.lib.screen.controller.ResultListener

class ScreenParams(
    val screen: Screen,
    val arguments: Bundle? = null,
    val pushChangeHandler: ControllerChangeHandler? = null,
    val popChangeHandler: ControllerChangeHandler? = null,
    val resultListener: ResultListener? = null,
    val resultRequestId: Int? = null
) {

    constructor(screen: Screen, pushChangeHandler: ControllerChangeHandler?, popChangeHandler: ControllerChangeHandler? = null) :
            this(screen, null, pushChangeHandler, popChangeHandler)
}