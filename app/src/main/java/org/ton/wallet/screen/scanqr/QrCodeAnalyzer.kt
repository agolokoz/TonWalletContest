package org.ton.wallet.screen.scanqr

import androidx.annotation.WorkerThread
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ton.wallet.lib.core.CoroutinesUtils
import org.ton.wallet.lib.core.L
import org.ton.wallet.util.await
import java.util.concurrent.Executors
import kotlin.math.sqrt

class QrCodeAnalyzer : ImageAnalysis.Analyzer {

    private val barCodeExecutor = Executors.newSingleThreadExecutor()

    private val barCodeScanner: BarcodeScanner by lazy {
        val builder = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .setExecutor(barCodeExecutor)
            .build()
        BarcodeScanning.getClient(builder)
    }

    private var callback: QrCodeCallback? = null

    override fun analyze(image: ImageProxy) {
        CoroutinesUtils.appCoroutinesScope.launch(Dispatchers.Default) {
            val androidImage = image.image
            if (androidImage != null) {
                val inputImage = InputImage.fromMediaImage(androidImage, image.imageInfo.rotationDegrees)
                try {
                    val qrCodeValue = getQrCodeValue(inputImage)
                    if (!qrCodeValue.isNullOrEmpty()) {
                        callback?.onQrCodeDetected(qrCodeValue)
                    }
                } catch (e: Exception) {
                    L.e(e)
                }
                image.close()
            }
        }
    }

    suspend fun getQrCodeValue(inputImage: InputImage): String? {
        try {
            val barCodes = barCodeScanner.process(inputImage).await()
            val imageCenterX = inputImage.width * 0.5f
            val imageCenterY = inputImage.height * 0.5f

            var nearestToCenterBarCodeIndex = -1
            for (i in 0 until barCodes.size) {
                if (barCodes[i].boundingBox == null || barCodes[i].displayValue.isNullOrEmpty()) {
                    continue
                }
                if (nearestToCenterBarCodeIndex == -1) {
                    nearestToCenterBarCodeIndex = i
                } else {
                    val nearestCenterX = barCodes[nearestToCenterBarCodeIndex].boundingBox!!.exactCenterX() - imageCenterX
                    val nearestCenterY = barCodes[nearestToCenterBarCodeIndex].boundingBox!!.exactCenterY() - imageCenterY
                    val nearestCenterDistance = sqrt(nearestCenterX * nearestCenterX + nearestCenterY * nearestCenterY)

                    val currentCenterX = barCodes[i].boundingBox!!.exactCenterX() - imageCenterX
                    val currentCenterY = barCodes[i].boundingBox!!.exactCenterY() - imageCenterY
                    val currentCenterDistance = sqrt(currentCenterX * currentCenterX + currentCenterY * currentCenterY)

                    if (currentCenterDistance < nearestCenterDistance) {
                        nearestToCenterBarCodeIndex = i
                    }
                }
            }
            if (nearestToCenterBarCodeIndex != -1) {
                return barCodes[nearestToCenterBarCodeIndex].displayValue
            }
        } catch (e: Exception) {
            L.e(e)
        }
        return null
    }

    fun setCallback(callback: QrCodeCallback) {
        this.callback = callback
    }

    fun destroy() {
        barCodeScanner.close()
        barCodeExecutor.shutdown()
    }

    interface QrCodeCallback {

        @WorkerThread
        fun onQrCodeDetected(value: String)
    }
}