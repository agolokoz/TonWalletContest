package org.ton.wallet.lib.screen.viewmodel

import com.bluelinelabs.conductor.Controller

class ViewModelStore(
    appViewModelStoreHolder: ViewModelStoreHolder
) {

    private val map = HashMap<String, BaseViewModel>()

    init {
        viewModelStoreHolder = appViewModelStoreHolder
    }

    @Suppress("UNCHECKED_CAST")
    fun <VM : BaseViewModel> get(key: String, factory: () -> VM): VM {
        var viewModel = map[key]
        if (viewModel == null) {
            viewModel = factory.invoke()
            map[key] = viewModel
        }
        return viewModel as VM
    }

    fun <VM : BaseViewModel> remove(viewModel: VM) {
        map.values.remove(viewModel)
    }

    companion object {
        lateinit var viewModelStoreHolder: ViewModelStoreHolder
    }
}

inline fun <reified VM : BaseViewModel> Controller.appViewModels(
    noinline factory: () -> VM
): Lazy<VM> {
    return viewModels(ViewModelRetainType.Application, factory)
}

inline fun <reified VM : BaseViewModel> Controller.viewModels(
    noinline factory: () -> VM
): Lazy<VM> {
    return viewModels(ViewModelRetainType.Controller, factory)
}

inline fun <reified VM : BaseViewModel> Controller.viewModels(
    retainType: ViewModelRetainType,
    noinline factory: () -> VM
): Lazy<VM> {
    val viewModelStore = when (retainType) {
        ViewModelRetainType.Application -> ViewModelStore.viewModelStoreHolder.viewModelStore
        ViewModelRetainType.Controller -> null
    }
    return ViewModelLazy(
        key = VM::class.java.canonicalName!!,
        factory = factory,
        retainType = retainType,
        store = viewModelStore
    )
}