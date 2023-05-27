package org.ton.wallet.util

import android.graphics.PointF
import android.view.animation.Interpolator
import kotlin.math.abs

class CubicBezierInterpolator(start: PointF, end: PointF) : Interpolator {

    private val start: PointF = start
    private val end: PointF = end
    private var a = PointF()
    private var b = PointF()
    private var c = PointF()

    init {
        require(!(start.x < 0 || start.x > 1)) { "startX value must be in the range [0, 1]" }
        require(!(end.x < 0 || end.x > 1)) { "endX value must be in the range [0, 1]" }
    }

    constructor(startX: Float, startY: Float, endX: Float, endY: Float) : this(
        PointF(startX, startY), PointF(endX, endY)
    )

    constructor(startX: Double, startY: Double, endX: Double, endY: Double) : this(
        startX.toFloat(),
        startY.toFloat(),
        endX.toFloat(),
        endY.toFloat()
    )

    override fun getInterpolation(time: Float): Float {
        return getBezierCoordinateY(getXForTime(time))
    }

    private fun getBezierCoordinateY(time: Float): Float {
        c.y = 3 * start.y
        b.y = 3 * (end.y - start.y) - c.y
        a.y = 1 - c.y - b.y
        return time * (c.y + time * (b.y + time * a.y))
    }

    private fun getXForTime(time: Float): Float {
        var x = time
        var z: Float
        for (i in 1..13) {
            z = getBezierCoordinateX(x) - time
            if (abs(z) < 1e-3) {
                break
            }
            x -= z / getXDerivate(x)
        }
        return x
    }

    private fun getXDerivate(t: Float): Float {
        return c.x + t * (2 * b.x + 3 * a.x * t)
    }

    private fun getBezierCoordinateX(time: Float): Float {
        c.x = 3 * start.x
        b.x = 3 * (end.x - start.x) - c.x
        a.x = 1 - c.x - b.x
        return time * (c.x + time * (b.x + time * a.x))
    }

    companion object {

        val Default = CubicBezierInterpolator(0.25, 0.1, 0.25, 1.0)
        val EaseOut = CubicBezierInterpolator(0.0, 0.0, .58, 1.0)
        val EaseOutQuint = CubicBezierInterpolator(.23, 1.0, .32, 1.0)
        val EaseIn = CubicBezierInterpolator(.42, 0.0, 1.0, 1.0)
        val EaseBoth = CubicBezierInterpolator(.42, 0.0, .58, 1.0)
    }
}