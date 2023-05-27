package org.ton.wallet.lib.screen.controller

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.bluelinelabs.conductor.Controller

class ControllerLifecycleOwner : Controller.LifecycleListener(), LifecycleOwner {

    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle = registry

    override fun postCreateView(controller: Controller, view: View) {
        super.postCreateView(controller, view)
        registry.currentState = Lifecycle.State.CREATED
    }

    override fun preAttach(controller: Controller, view: View) {
        super.preAttach(controller, view)
        registry.currentState = Lifecycle.State.STARTED
    }

    override fun postAttach(controller: Controller, view: View) {
        super.postAttach(controller, view)
        registry.currentState = Lifecycle.State.RESUMED
    }

    override fun preDetach(controller: Controller, view: View) {
        registry.currentState = Lifecycle.State.STARTED
        super.preDetach(controller, view)
    }

    override fun postDetach(controller: Controller, view: View) {
        registry.currentState = Lifecycle.State.CREATED
        super.postDetach(controller, view)
    }

    override fun preDestroy(controller: Controller) {
        registry.currentState = Lifecycle.State.DESTROYED
        super.preDestroy(controller)
    }
}