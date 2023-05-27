package org.ton.wallet.screen.scanqr

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ton.wallet.R
import org.ton.wallet.lib.core.CoroutinesUtils
import org.ton.wallet.lib.core.L
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ext.setOnClickListener
import org.ton.wallet.lib.screen.controller.BaseViewModelController
import org.ton.wallet.lib.screen.viewmodel.viewModels
import org.ton.wallet.util.FlowBus
import org.ton.wallet.util.FlowBusEvent
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanQrController(args: Bundle?) : BaseViewModelController<ScanQrViewModel>(args) {

    override val viewModel by viewModels { ScanQrViewModel() }

    override val useTopInsetsPadding: Boolean = false
    override val useBottomInsetsPadding: Boolean = false

    private val qrForeground = ScanQrForegroundDrawable()
    private val cameraFutureProvider: ListenableFuture<ProcessCameraProvider> by lazy {
        ProcessCameraProvider.getInstance(activity!!)
    }

    private lateinit var qrCodeAnalyzer: QrCodeAnalyzer
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var mainExecutor: Executor
    private lateinit var backButton: View
    private lateinit var noPermissionsGroup: Group
    private lateinit var cameraGroup: Group
    private lateinit var previewView: PreviewView
    private lateinit var previewForegroundView: View

    private var initialBackButtonTopMargin = 0
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null

    private val hasCameraPermission: Boolean
        get() = activity?.let { act -> EasyPermissions.hasPermissions(act, android.Manifest.permission.CAMERA) } ?: false

    override fun onPreCreateView() {
        super.onPreCreateView()
        cameraExecutor = Executors.newSingleThreadExecutor()
        mainExecutor = ContextCompat.getMainExecutor(Res.context)
        qrCodeAnalyzer = QrCodeAnalyzer()
        qrCodeAnalyzer.setCallback(qrCallback)
        qrForeground.setCutoutSize(Res.dp(284))
    }

    override fun createView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.screen_scan_qr, container, false)
        view.findViewById<View>(R.id.scanQrOpenSettingsButton).setOnClickListener { viewModel.onOpenSettingsClicked(activity!!) }
        view.findViewById<View>(R.id.scanQrImageButton).setOnClickListener {
            FlowBus.common.dispatch(FlowBusEvent.PickMediaContent(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        view.findViewById<View>(R.id.scanQrFlashlightButton).setOnClickListener(::onFlashClicked)

        backButton = view.findViewById(R.id.scanQrBackButton)
        backButton.setOnClickListener(viewModel::onBackClicked)
        initialBackButtonTopMargin = (backButton.layoutParams as MarginLayoutParams).topMargin

        noPermissionsGroup = view.findViewById(R.id.scanQrNoPermissionGroup)
        cameraGroup = view.findViewById(R.id.scanQrCameraGroup)
        previewView = view.findViewById(R.id.scanQrPreviewView)
        previewForegroundView = view.findViewById(R.id.scanQrForegroundView)

        return view
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        checkPermissions()
    }

    override fun onDestroyView(view: View) {
        imageAnalysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        super.onDestroyView(view)
    }

    override fun onDestroy() {
        qrCodeAnalyzer.destroy()
        cameraExecutor.shutdown()
        removeLifecycleListener(lifecycleOwner)
        super.onDestroy()
    }

    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        val superInsets = super.onApplyWindowInsets(v, insets)
        val systemBarsInsets = superInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        backButton.updateLayoutParams<MarginLayoutParams> { topMargin = systemBarsInsets.top + initialBackButtonTopMargin }
        return superInsets
    }

    override fun onPermissionsGranted(requestCode: Int, permissions: MutableList<String>) {
        super.onPermissionsGranted(requestCode, permissions)
        if (requestCode == RequestCodeCameraPermission) {
            onPermissionChanged(hasCameraPermission)
            cameraFutureProvider.addListener(cameraProviderCallback, mainExecutor)
        }
    }

    override fun onPermissionsDenied(requestCode: Int, permissions: MutableList<String>) {
        super.onPermissionsDenied(requestCode, permissions)
        if (requestCode == RequestCodeCameraPermission) {
            onPermissionChanged(hasCameraPermission)
        }
    }

    override fun onActivityResult(result: Any?) {
        super.onActivityResult(result)
        if (result is Uri) {
            CoroutinesUtils.appCoroutinesScope.launch(Dispatchers.Default) {
                val inputImage = InputImage.fromFilePath(Res.context, result)
                val value = qrCodeAnalyzer.getQrCodeValue(inputImage)
                if (value == null) {
                    FlowBus.common.dispatch(FlowBusEvent.ShowSnackBar(title = Res.str(R.string.no_qr_codes_detected)))
                } else {
                    viewModel.onQrDetected(value)
                }
            }
        }
    }

    private fun checkPermissions() {
        val activity = activity ?: return
        onPermissionChanged(hasCameraPermission)
        if (hasCameraPermission) {
            cameraFutureProvider.addListener(cameraProviderCallback, mainExecutor)
        } else {
            val request = PermissionRequest.Builder(activity, RequestCodeCameraPermission, android.Manifest.permission.CAMERA)
                .setRationale(Res.str(R.string.no_camera_access_description))
                .setPositiveButtonText(Res.str(R.string.ok))
                .setNegativeButtonText(Res.str(R.string.cancel))
                .build()
            EasyPermissions.requestPermissions(request)
        }
    }

    private fun onPermissionChanged(hasCameraPermission: Boolean) {
        noPermissionsGroup.isVisible = !hasCameraPermission
        cameraGroup.isVisible = hasCameraPermission
        previewForegroundView.foreground =
            if (hasCameraPermission) qrForeground
            else null
    }

    private fun onFlashClicked() {
        val camera = camera ?: return
        val isEnabled = camera.cameraInfo.torchState.value == TorchState.ON
        camera.cameraControl.enableTorch(!isEnabled)
    }

    private val cameraProviderCallback = Runnable {
        val cameraProvider = cameraFutureProvider.get()
        val preview = Preview.Builder()
            .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
            .build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
            .setImageQueueDepth(10)
            .build()
        imageAnalysis!!.setAnalyzer(cameraExecutor, qrCodeAnalyzer)

        cameraProvider.unbindAll()
        try {
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
        } catch (e: Exception) {
            L.e(e)
        }
        this.cameraProvider = cameraProvider
    }

    private val qrCallback = object : QrCodeAnalyzer.QrCodeCallback {
        override fun onQrCodeDetected(value: String) {
            viewModel.onQrDetected(value)
        }
    }

    companion object {

        private const val RequestCodeCameraPermission = 1

        const val ResultCodeQrDetected = "qrDetected"
        const val ArgumentKeyQrValue = "value"
    }
}