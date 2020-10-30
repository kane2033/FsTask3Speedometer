package com.fosusstart.fstask3speedometer.speedometer

import android.animation.*
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Parcelable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.fosusstart.fstask3speedometer.R
import kotlin.math.*


class SpeedometerView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attributeSet, defStyleAttr, defStyleRes) {

    private companion object {
        const val SPEED_KEY = "speed"
        const val LEVEL_KEY = "speed_level"
        const val COLOR_KEY = "speed_color"
        const val SUPER_STATE = "super_state"
    }

    private var size = 640

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)

    private val bgColor = -4663859 // Цвет круга по центру
    private var speedColor = Color.GREEN // Цвет спидометра (меняется в соотв. со скоростью)
    private var speed: Float // Текущая скорость спидометра
    private var maxSpeed: Float // Максимальная скорость спидометра

    private var nums = emptyArray<String>() // Массив цифр на спидометре
    private var animatorSet = AnimatorSet()

    private var lowSpeedRange: Float
    private var mediumSpeedRange: Float
    private var highSpeedRange: Float
    private var speedLevel: Speed

    init {
        val typedArray = context.obtainStyledAttributes(
            attributeSet,
            R.styleable.SpeedometerView,
            defStyleAttr,
            defStyleRes
        )

        try {
            speed = typedArray.getFloat(R.styleable.SpeedometerView_speed, 0f)
            maxSpeed = typedArray.getFloat(R.styleable.SpeedometerView_maxSpeed, 150f)

            if (speed !in 0f..maxSpeed) {
                throw SpeedOutOfBoundsException("Speed parameter must be in range of [0; maxSpeed]")
            }

            // Скоростные интервалы - на первом и последнем интервалах медленно набирается скорость,
            // между ними - быстрый набор скорости (эмуляция ускорения и замедления)
            lowSpeedRange = maxSpeed * 40f / 100f // От 0 до 40% от макс. скорости - низкая скорость
            mediumSpeedRange = maxSpeed * 80f / 100f // От 40% до 80% - средняя скорость
            highSpeedRange = maxSpeed // От 80% до 100% - максимальная скорость

            speedLevel = setSpeedState(speed) // Установка enum состояния скорости
            speedColor = setSpeedColor(speedLevel) // Установка цвета спидометра в соотв. с состоянием скорости

            /*
            * Формирование массива цифр на спидометре:
            * создается массив из 6 чисел от h до maxSpeed,
            * где h - одинаковый шаг
            * */
            val h = maxSpeed / 6
            nums = Array(6) { "%.2f".format(it * h + h) }
        } finally {
            typedArray.recycle()
        }
    }

    // Установка состояния спидометра в соответствии со значением скорости
    private fun setSpeedState(newSpeed: Float): Speed {
        return when (newSpeed) {
            in 0f..lowSpeedRange -> Speed.LOW // [0, 40%]
            in lowSpeedRange..mediumSpeedRange -> Speed.MEDIUM // [40%, 80%]
            in mediumSpeedRange..highSpeedRange -> Speed.HIGH // [80%, 100%]
            else -> Speed.LOW
        }
    }

    // Установка цвета спидометра в соответствии с уровнем скорости
    private fun setSpeedColor(speedLevel: Speed):Int {
        return when (speedLevel) {
            Speed.LOW -> Color.GREEN
            Speed.MEDIUM -> Color.YELLOW
            Speed.HIGH -> Color.RED
        }
    }

    // Метод изменения скорости на дельту без анимации
    fun changeSpeedByDelta(delta: Float) {
        val deltaSpeed = speed + delta
        speed = if (deltaSpeed in 0f..maxSpeed) deltaSpeed else speed
        invalidate()
    }

    // Метод изменения скорости на конкретное значение без анимации
    fun changeSpeed(speed: Float) {
        this.speed = if (speed in 0f..maxSpeed) speed else this.speed
        invalidate()
    }

    // Метод изменения скорости на дельту с анимацией
    fun changeSpeedByDeltaAnimated(delta: Float) {
        val deltaSpeed = speed + delta
        if (deltaSpeed in 0f..maxSpeed) { // Скорость должна быть в интервале спидометра
            speedLevel = setSpeedState(deltaSpeed) // Установка состояния скорости
            doAnimation(speed, deltaSpeed) // Отрисовка с анимацией
        }
    }

    private fun doAnimation(prevSpeed: Float, newSpeed: Float) {
        if (!animatorSet.isRunning) { // Если сейчас не запущены анимации
            // Анимация движения стрелки
            val arrowAnimator = ValueAnimator.ofFloat(prevSpeed, newSpeed).apply {
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { valueAnimator ->
                    speed = valueAnimator.animatedValue as Float
                    invalidate()
                }
            }

            // Расчет цвета, в который должен перейти спидометр
            val newColor = setSpeedColor(speedLevel)
            // Анимация цвета
            val colorAnimator = ValueAnimator().apply {
                setIntValues(
                    speedColor,
                    newColor
                )
                setEvaluator(ArgbEvaluator())
                addUpdateListener { _ ->
                    (this.animatedValue as? Int)?.let { speedColor = it }
                }
                interpolator = AccelerateDecelerateInterpolator()
            }

            // Запускаем две анимации одновременно
            animatorSet.apply {
                play(colorAnimator).with(arrowAnimator)
                duration = when (speedLevel) { // Расчет длительности обоих анимаций
                Speed.LOW -> 1800L
                Speed.MEDIUM -> 700L
                Speed.HIGH -> 2200L
                }
                start()
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable? =
        Bundle().apply {
            putFloat(SPEED_KEY, speed)
            putString(LEVEL_KEY, speedLevel.toString())
            putInt(COLOR_KEY, speedColor)
            putParcelable(SUPER_STATE, super.onSaveInstanceState())
        }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var superState = state

        if (state is Bundle) {
            speed = state.getFloat(SPEED_KEY)
            speedLevel = Speed.valueOf(state.getString(LEVEL_KEY)!!)
            speedColor = state.getInt(COLOR_KEY)
            superState = state.getParcelable(SUPER_STATE)
        }

        super.onRestoreInstanceState(superState)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = measureDimension(size, widthMeasureSpec)
        val height = measureDimension(size, heightMeasureSpec)
        size = min(width, height)
        setMeasuredDimension(width, height)
    }

    private fun measureDimension(minSize: Int, measureSpec: Int): Int {
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)

        return when (specMode) {
            MeasureSpec.EXACTLY -> specSize
            MeasureSpec.AT_MOST -> minSize.coerceAtMost(specSize)
            else -> minSize
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val percent = 270 / maxSpeed // 1%
        val speedAngle = speed * percent // Перевод в градусы (sweepAngle)

        val width = width.toFloat()
        val height = height.toFloat()

        // Дуга скорости (отрисовывает дугу с углом, соотв. скорости)
        paint.apply {
            style = Paint.Style.FILL
            color = speedColor
        }
        canvas.drawArc(0f, 0f, width, height, 135f, speedAngle, true, paint)

        val prWidth = width/8
        val prHeight = height/8

        // Круг по центру, котоырй меньше, чем дуга скорости
        paint.color = bgColor
        canvas.drawArc(
            prWidth, prHeight, width - prWidth, height - prHeight,
            135f, 360f, false, paint
        )

        // Стрелка спидометра
        paint.color = Color.RED
        canvas.drawCircle(width / 2, height / 2, size / 32f, paint) // Кружок в центре спидометра
        // Расчет координаты в соответствии с углом скорости
        val x = width/2 + width/2 * cos(Math.toRadians(135.00 + speedAngle)).toFloat()
        val y = height/2 + height/2 * sin(Math.toRadians(135.00 + speedAngle)).toFloat()
        paint.apply {
            style = Paint.Style.STROKE
            strokeWidth = size/64f
        }
        canvas.drawLine(width / 2, height / 2, x, y, paint) // Отрисовка линии спидометра

        // Обводка
        paint.apply {
            style = Paint.Style.STROKE
            strokeWidth = size/128f
            color = Color.BLACK
        }
        canvas.drawArc(0f, 0f, width, height, 135f, 270f, false, paint)
        canvas.drawArc(
            prWidth, prHeight, width - prWidth, height - prHeight,
            135f, 270f, false, paint
        )

        // Цифры спидометра
        textPaint.apply {
            color = Color.BLACK
            textSize = size/26f
        }
        val textMargin = textPaint.textSize
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("0", prWidth * 2 - textMargin, height - prHeight * 2 + textMargin, textPaint)
        canvas.drawText(nums[0], prWidth, prHeight * 4, textPaint)
        canvas.drawText(nums[1], prWidth * 2 - textMargin, prHeight * 2 - textMargin / 2, textPaint)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(nums[2], prWidth * 4, prHeight - textMargin, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(nums[3], width - prWidth * 2 + textMargin / 2, prHeight * 2 - textMargin / 2, textPaint)
        canvas.drawText(nums[4], width - prWidth, prHeight * 4, textPaint)
        canvas.drawText(nums[5], width - prWidth * 2 + textMargin / 2, height - prHeight * 2 + textMargin, textPaint)
    }
}