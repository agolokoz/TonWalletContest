package org.ton.wallet

import android.app.Application
import android.content.Context
import org.ton.wallet.lib.core.*
import org.ton.wallet.lib.screen.viewmodel.ViewModelStore
import org.ton.wallet.lib.screen.viewmodel.ViewModelStoreHolder
import org.ton.wallet.util.NotificationUtils

class App : Application(), ViewModelStoreHolder {

    override val viewModelStore = ViewModelStore(this)

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Res.init(this, resources.configuration)
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(AppLifecycleDetector)

        L.init()
        SecurityUtils.init(this)
        FileUtils.init(this)
        AppComponentsProvider.init(this)
        NotificationUtils.init()

        ThreadUtils.postOnDefault {
            NetworkUtils.init(this)
        }
    }
}