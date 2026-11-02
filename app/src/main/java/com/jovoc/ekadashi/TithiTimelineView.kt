package com.jovoc.ekadashi

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import java.time.format.DateTimeFormatter
import java.util.Locale

class TithiTimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var schedule: DayTithiSchedule? = null
    private var langCode: String = "en"

    private val density = context.resources.displayMetrics.density
    private val barHeightPx = 30f * density
    private val cornerRadiusPx = 6f * density
    private val labelSpacingPx = 8f * density
    private val labelTextSizePx = 13f * density
    private val timeTextSizePx = 11f * density

    // Paints
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val timeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = timeTextSizePx
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = labelTextSizePx
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val clipPath = Path()
    private val barRect = RectF()

    // Colors matching user mockup: Black for past, White for active, Green for next
    private val colorPast = Color.parseColor("#141416")
    private val colorActive = Color.parseColor("#FFFFFF")
    private val colorFuture = Color.parseColor("#00C853")

    fun setSchedule(schedule: DayTithiSchedule?, langCode: String = "en") {
        this.schedule = schedule
        this.langCode = langCode
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (barHeightPx + labelSpacingPx + labelTextSizePx + 8f * density).toInt()
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sched = schedule ?: return
        val segments = sched.segments
        if (segments.isEmpty()) return

        val totalWidth = width.toFloat()
        if (totalWidth <= 0f) return

        // 1. Draw rounded outer bar track
        barRect.set(0f, 0f, totalWidth, barHeightPx)
        clipPath.reset()
        clipPath.addRoundRect(barRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)

        canvas.save()
        canvas.clipPath(clipPath)

        var curX = 0f
        val timeFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

        // Find index of active segment
        val activeIndex = segments.indexOfFirst { it.isActive }.let { if (it == -1) 0 else it }

        segments.forEachIndexed { index, seg ->
            val segWidth = totalWidth * seg.visualPercent
            val segRight = if (index == segments.size - 1) totalWidth else curX + segWidth

            // Assign color based on active/past/future relative to active segment
            val segColor = when {
                index < activeIndex -> colorPast
                index == activeIndex -> colorActive
                else -> colorFuture
            }

            barPaint.color = segColor
            canvas.drawRect(curX, 0f, segRight, barHeightPx, barPaint)

            // Draw transition timestamp at the boundary between segments
            if (index < segments.size - 1) {
                val boundaryTime = seg.segmentEnd.format(timeFmt)
                if (segWidth > 55f * density) {
                    timeTextPaint.color = if (segColor == colorActive) Color.parseColor("#141416") else Color.WHITE
                    val textY = (barHeightPx / 2f) - ((timeTextPaint.descent() + timeTextPaint.ascent()) / 2f)
                    canvas.drawText(boundaryTime, curX + (segWidth / 2f), textY, timeTextPaint)
                }
            }

            curX = segRight
        }
        canvas.restore()

        // 2. Draw Tithi Labels below the bar
        curX = 0f
        val labelY = barHeightPx + labelSpacingPx + labelTextSizePx

        segments.forEachIndexed { index, seg ->
            val segWidth = totalWidth * seg.visualPercent
            val centerX = curX + (segWidth / 2f)

            val displayName = TithiHelper.getTithiDisplayName(seg.tithi.type, langCode, seg.tithi.name)

            if (seg.isActive) {
                labelPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                labelPaint.color = Color.WHITE
            } else {
                labelPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                labelPaint.color = Color.parseColor("#CCCCCC")
            }

            // Keep label within view bounds
            val textWidth = labelPaint.measureText(displayName)
            val clampedCenterX = centerX.coerceIn(textWidth / 2f, totalWidth - (textWidth / 2f))

            canvas.drawText(displayName, clampedCenterX, labelY, labelPaint)
            curX += segWidth
        }
    }
}
