package org.ton.wallet.lib.navigator

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import org.ton.wallet.lib.core.L
import org.ton.wallet.lib.core.ext.weak
import java.lang.ref.WeakReference

interface Navigator {

    @MainThread
    fun attach(activity: Activity, container: ViewGroup, savedInstanceState: Bundle?)

    @MainThread
    fun detach()

    @MainThread
    fun onBackPressed(): Boolean

    @MainThread
    fun forEachScreen(action: (Controller) -> Unit)

    @MainThread
    fun getBackStack(): List<RouterTransaction>

    @MainThread
    fun setBackStack(backStack: List<RouterTransaction>)

    @MainThread
    fun closeApp()


    @AnyThread
    fun add(screenParams: ScreenParams)

    @AnyThread
    fun replace(screenParams: ScreenParams, setRoot: Boolean = false)

    @AnyThread
    fun back()

    @AnyThread
    fun popTo(screenParams: ScreenParams?, excludeTop: Boolean = false)

    @AnyThread
    fun setCurrentScreenRoot()
}

internal class NavigatorImpl(
    private val controllerFactory: ControllerTransactionsFactory
) : Navigator {

    private val commandBuffer = mutableListOf<NavigatorCommand>()
    private val handler = Handler(Looper.getMainLooper())
    private var router: Router? = null
    private var activityRef: WeakReference<Activity?> = weak(null)

    @MainThread
    override fun attach(activity: Activity, container: ViewGroup, savedInstanceState: Bundle?) {
        activityRef = weak(activity)
        router = Conductor.attachRouter(activity, container, savedInstanceState)
        router!!.setPopRootControllerMode(Router.PopRootControllerMode.NEVER)
        commandBuffer.forEach(::executeCommand)
        commandBuffer.clear()
    }

    @MainThread
    override fun detach() {
        router = null
    }

    @MainThread
    override fun onBackPressed(): Boolean {
        return router?.handleBack() ?: false
    }

    @MainThread
    override fun forEachScreen(action: (Controller) -> Unit) {
        router?.backstack?.forEach { transaction ->
            action.invoke(transaction.controller)
        }
    }

    @MainThread
    override fun getBackStack(): List<RouterTransaction> {
        return router?.backstack ?: emptyList()
    }

    @MainThread
    override fun setBackStack(backStack: List<RouterTransaction>) {
        router?.setBackstack(backStack, null)
    }

    @MainThread
    override fun closeApp() {
        activityRef.get()?.finish()
    }


    @AnyThread
    override fun add(screenParams: ScreenParams) {
        executeCommand(NavigatorCommand.Add(screenParams))
    }

    @AnyThread
    override fun replace(screenParams: ScreenParams, setRoot: Boolean) {
        executeCommand(NavigatorCommand.Replace(screenParams, setRoot))
    }

    @AnyThread
    override fun back() {
        executeCommand(NavigatorCommand.Back)
    }

    @AnyThread
    override fun popTo(screenParams: ScreenParams?, excludeTop: Boolean) {
        executeCommand(NavigatorCommand.BackTo(screenParams, excludeTop))
    }

    @AnyThread
    override fun setCurrentScreenRoot() {
        executeCommand(NavigatorCommand.SetCurrentScreenRoot)
    }

    @AnyThread
    private fun executeCommand(command: NavigatorCommand) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            executeCommandInternal(command)
        } else {
            handler.post { executeCommandInternal(command) }
        }
    }

    private fun executeCommandInternal(command: NavigatorCommand) {
        if (router == null) {
            commandBuffer.add(command)
        } else {
            runCommand(command)
        }
    }

    @MainThread
    private fun runCommand(command: NavigatorCommand) {
        val router = router ?: return
        when (command) {
            is NavigatorCommand.Add -> {
                addScreen(command.screen, isReplace = false, setRoot = false)
            }
            is NavigatorCommand.Replace -> {
                addScreen(command.screen, true, command.setRoot)
            }
            is NavigatorCommand.Back -> {
                router.popCurrentController()
            }
            is NavigatorCommand.BackTo -> {
                backTo(command.screen?.screen, command.excludeTop)
            }
            is NavigatorCommand.SetCurrentScreenRoot -> {
                router.setRoot(router.backstack.last())
            }
        }
    }

    @MainThread
    private fun addScreen(screenParams: ScreenParams, isReplace: Boolean, setRoot: Boolean) {
        val router = router ?: return
        val transaction = try {
            controllerFactory.createTransaction(screenParams)
        } catch (e: Exception) {
            L.e(e)
            return
        }
        transaction.tag(screenParams.screen.name)

        val isSetRoot = !router.hasRootController() || setRoot
        if (isSetRoot) {
            router.setRoot(transaction)
        } else {
            val targetController: Controller?
            if (isReplace) {
                targetController = router.backstack.getOrNull(router.backstackSize - 2)?.controller
                router.replaceTopController(transaction)
            } else {
                targetController = router.backstack.last().controller
                router.pushController(transaction)
            }
            transaction.controller.targetController = targetController
        }
    }

    @MainThread
    private fun backTo(screen: Screen?, excludeTop: Boolean) {
        val router = router ?: return
        if (screen == null) {
            if (excludeTop && router.backstack.size >= 2) {
                val newBackStack = ArrayList<RouterTransaction>()
                newBackStack.add(router.backstack.first())
                newBackStack.add(router.backstack.last())
                router.setBackstack(newBackStack, null)
            } else {
                router.popToRoot()
            }
        } else {
            if (excludeTop && router.backstack.size >= 2) {
                val newBackStack = ArrayList<RouterTransaction>()
                for (i in 0 until router.backstackSize - 2) {
                    newBackStack.add(router.backstack[i])
                    if (router.backstack[i].tag() == screen.name) {
                        break
                    }
                }
                newBackStack.add(router.backstack.last())
                router.setBackstack(newBackStack, null)
            } else {
                router.popToTag(screen.name)
            }
        }
    }


    private sealed class NavigatorCommand {

        class Add(val screen: ScreenParams) : NavigatorCommand()

        class Replace(
            val screen: ScreenParams,
            val setRoot: Boolean
        ) : NavigatorCommand()

        object Back : NavigatorCommand()

        class BackTo(
            val screen: ScreenParams?,
            val excludeTop: Boolean
        ) : NavigatorCommand()

        object SetCurrentScreenRoot : NavigatorCommand()
    }
}