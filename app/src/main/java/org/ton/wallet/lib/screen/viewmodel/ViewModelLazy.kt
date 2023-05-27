package org.ton.wallet.lib.screen.viewmodel

class ViewModelLazy<VM : BaseViewModel>(
    private val key: String,
    private val factory: () -> VM,
    private val retainType: ViewModelRetainType,
    private val store: ViewModelStore? = null,
) : Lazy<VM> {

    private var _value: VM? = null

    override val value: VM
        get() {
            if (_value == null) {
                _value = store?.get(key, factory) ?: factory.invoke().apply { setRetainType(retainType) }
            }
            return _value!!
        }

    override fun isInitialized(): Boolean {
        return _value != null
    }
}