package org.ton.wallet.screen.connect.approve

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.math.MathUtils.clamp
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import coil.load
import coil.transform.RoundedCornersTransformation
import org.ton.wallet.R
import org.ton.wallet.lib.core.Formatter
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ext.setOnClickListenerWithLock
import org.ton.wallet.lib.screen.controller.BaseViewModelBottomSheetController
import org.ton.wallet.lib.screen.viewmodel.viewModels
import org.ton.wallet.uikit.drawable.EmptyDrawable
import org.ton.wallet.uikit.drawable.IndeterminateProgressDrawable
import org.ton.wallet.uikit.span.FontSpan
import org.ton.wallet.util.CubicBezierInterpolator

class TonConnectApproveController(args: Bundle) : BaseViewModelBottomSheetController<TonConnectApproveViewModel>(args) {

    override val viewModel by viewModels { TonConnectApproveViewModel(args) }

    private val drawableSize = Res.dp(20)
    private val emptyDrawable = EmptyDrawable(drawableSize, drawableSize)
    private val loadingDrawable = IndeterminateProgressDrawable(drawableSize)

    private lateinit var rootView: View
    private lateinit var loadingView: View
    private lateinit var contentLayout: ViewGroup
    private lateinit var imageView: ImageView
    private lateinit var titleText: TextView
    private lateinit var subTitleText: TextView
    private lateinit var connectButton: TextView
    private lateinit var doneImage: ImageView

    private var prevDataLoading = true

    override fun createBottomSheetView(inflater: LayoutInflater, container: ViewGroup?, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.screen_ton_connect_approve, container, false)
        view.findViewById<View>(R.id.tonConnectApproveCloseButton).setOnClickListenerWithLock(viewModel::onCloseClicked)

        loadingView = view.findViewById(R.id.tonConnectApproveLoadingView)
        val progressDrawable = IndeterminateProgressDrawable()
        progressDrawable.setColor(Res.color(R.color.blue))
        progressDrawable.setStrokeWidth(Res.dp(4f))
        loadingView.background = progressDrawable

        contentLayout = view.findViewById(R.id.tonConnectApproveContentLayout)
        imageView = view.findViewById(R.id.tonConnectApproveImageView)
        titleText = view.findViewById(R.id.tonConnectApproveTitleTextView)
        subTitleText = view.findViewById(R.id.tonConnectApproveSubTitleTextView)

        connectButton = view.findViewById(R.id.tonConnectApproveConnectButton)
        connectButton.setCompoundDrawablesWithIntrinsicBounds(emptyDrawable, null, emptyDrawable, null)
        connectButton.setOnClickListenerWithLock(viewModel::onConnectClicked)
        doneImage = view.findViewById(R.id.tonConnectApproveDoneImage)

        rootView = view
        return view
    }

    override fun onPostCreateView(view: View) {
        super.onPostCreateView(view)
        viewModel.stateFlow.launchInViewScope(::onStateChanged)
    }

    private fun onStateChanged(state: TonConnectApproveState) {
        titleText.text = Res.str(R.string.ton_connect_to, state.appName)

        // image
        if (state.appIconUrl.isNotEmpty()) {
            imageView.load(state.appIconUrl) {
                lifecycle(lifecycleOwner)
                transformations(RoundedCornersTransformation(Res.dp(20f)))
            }
        }
        imageView.isVisible = state.appIconUrl.isNotEmpty()

        // subtitle
        if (state.accountAddress.isNotEmpty()) {
            val shortAddress = Formatter.getShortAddress(state.accountAddress)
            val subtitleBuilder = SpannableStringBuilder(Res.str(R.string.ton_connect_requesting_access, state.appHost, shortAddress, state.accountVersion))
            val colorSpanStart = subtitleBuilder.indexOf(shortAddress)
            val colorSpanEnd = colorSpanStart + shortAddress.length
            subtitleBuilder.setSpan(ForegroundColorSpan(Res.color(R.color.text_secondary)), colorSpanStart, colorSpanEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            subtitleBuilder.setSpan(FontSpan(Res.font(R.font.robotomono_regular)), colorSpanStart, colorSpanStart + 4, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            subtitleBuilder.setSpan(FontSpan(Res.font(R.font.robotomono_regular)), colorSpanEnd - 4, colorSpanEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            subTitleText.text = subtitleBuilder
        }

        // button
        when (state.connectionState) {
            TonConnectApproveState.ConnectionDefault -> {
                connectButton.setCompoundDrawablesWithIntrinsicBounds(emptyDrawable, null, emptyDrawable, null)
            }
            TonConnectApproveState.ConnectionInProgress -> {
                connectButton.setCompoundDrawablesWithIntrinsicBounds(emptyDrawable, null, loadingDrawable, null)
            }
            TonConnectApproveState.ConnectionConnected -> {
                connectButton.animate().cancel()
                connectButton.animate()
                    .alpha(0f)
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(150L)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            doneImage.isVisible = true
                            doneImage.alpha = 0f
                            doneImage.scaleX = 0f
                            doneImage.scaleY = 0f
                            doneImage.animate().cancel()
                            doneImage.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(150L)
                                .start()
                        }
                    })
                    .start()
            }
        }

        if (!state.isDataLoading && prevDataLoading) {
            val widthSpec = View.MeasureSpec.makeMeasureSpec(rootView.width, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            contentLayout.measure(widthSpec, heightSpec)
            if (rootView.height < contentLayout.measuredHeight && !isShowHideAnimatorInProgress) {
                val currentTranslation = contentLayout.measuredHeight - rootView.height.toFloat()
                ValueAnimator.ofFloat(0f, 1f).apply {
                    addUpdateListener { animator ->
                        val progress = animator.animatedValue as Float
                        val loadingProgress = 1f - clamp(progress * 2f, 0f, 1f)
                        val contentProgress = clamp((progress - 0.5f) * 2f,0f, 1f)
                        setBottomSheetTranslation(currentTranslation * (1f - progress))
                        loadingView.alpha = loadingProgress
                        loadingView.scaleX = loadingProgress
                        loadingView.scaleY = loadingProgress
                        contentLayout.alpha = contentProgress
                        contentLayout.scaleX = contentProgress
                        contentLayout.scaleY = contentProgress
                    }
                    duration = 200L
                    interpolator = CubicBezierInterpolator.Default
                    start()
                }
            } else {
                loadingView.isVisible = false
            }
        }
        contentLayout.isInvisible = state.isDataLoading
        prevDataLoading = state.isDataLoading
    }

    companion object {

        const val ArgumentKeyUrl = "url"
    }
}