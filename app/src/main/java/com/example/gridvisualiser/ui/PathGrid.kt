package com.example.gridvisualiser.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.gridvisualiser.R
import com.example.gridvisualiser.model.PathFinder
import com.example.gridvisualiser.model.PathFinder.Companion.EMPTY_CELL_CODE
import com.example.gridvisualiser.model.PathFinder.Companion.END_CELL_CODE
import com.example.gridvisualiser.model.PathFinder.Companion.EXPLORE_CELL_CODE
import com.example.gridvisualiser.model.PathFinder.Companion.EXPLORE_HEAD_CELL_CODE
import com.example.gridvisualiser.model.PathFinder.Companion.FINAL_PATH_CELL_CODE
import com.example.gridvisualiser.model.PathFinder.Companion.OBSTACLE_CELL_CODE
import com.example.gridvisualiser.model.PathFinder.Companion.START_CELL_CODE
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min


class PathGrid(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val pathColor: Int
    private val obstacleColor: Int
    private val startColor: Int
    private val endColor: Int
    private val finalPathColor: Int
    private val exploreHeadColor: Int
    private val exploreColor: Int

    private val exploreHeadPaintColor: Paint = Paint()
    private val pathPaintColor: Paint = Paint()
    private val obstaclePaintColor: Paint = Paint()
    private val startPaintColor: Paint = Paint()
    private val finalPathPaintColor: Paint = Paint()
    private val endPaintColor: Paint = Paint()
    private val explorePaintColor: Paint = Paint()

    private var cellSize: Int = 0
    private var finder = PathFinder()
    private var turn = 0
    private var isSolving = false

    companion object {
        const val OBSTACLE_BLOCK_TURN = 0
        const val START_BLOCK_TURN = -1
        const val END_BLOCK_TURN = 1

    }

    init {
        val typedArray: TypedArray =
            context!!.theme.obtainStyledAttributes(attrs, R.styleable.PathGrid, 0, 0)
        try {
            pathColor = typedArray.getInteger(R.styleable.PathGrid_gridColor, 0)
            obstacleColor = typedArray.getInteger(R.styleable.PathGrid_obstacleColor, 0)
            startColor = typedArray.getInteger(R.styleable.PathGrid_startColor, 0)
            endColor = typedArray.getInteger(R.styleable.PathGrid_endColor, 0)
            exploreColor = typedArray.getInteger(R.styleable.PathGrid_exploreColor, 0)
            exploreHeadColor = typedArray.getInteger(R.styleable.PathGrid_exploreHeadColor, 0)
            finalPathColor = typedArray.getInteger(R.styleable.PathGrid_finalPathColor, 0)
        } finally {
            typedArray.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val dimension = min(height, width)
        cellSize = dimension / 15
        setMeasuredDimension(dimension, dimension)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        setPaint(pathPaintColor, pathColor)
        drawGrid(canvas!!)

        setPaint(obstaclePaintColor, obstacleColor)
        colorObstacleCell(canvas, finder.obstacleY, finder.obstacleX)

        for (i in 1..15) {
            for (j in 1..15) {
                if (finder.board[i][j] == OBSTACLE_CELL_CODE) {
                    setPaint(obstaclePaintColor, obstacleColor)
                    colorObstacleCell(canvas, i, j)
                }
                if (finder.board[i][j] == EXPLORE_CELL_CODE) {
                    setPaint(explorePaintColor, exploreColor)
                    colorCell(canvas, i, j, 12F, explorePaintColor)
                }
                if (finder.board[i][j] == EXPLORE_HEAD_CELL_CODE) {
                    setPaint(exploreHeadPaintColor, exploreHeadColor)
                    colorCell(canvas, i, j, 0F, exploreHeadPaintColor)
                }
                if (finder.board[i][j] == FINAL_PATH_CELL_CODE) {
                    setPaint(finalPathPaintColor, finalPathColor)
                    colorCell(canvas, i, j, 12F, finalPathPaintColor)
                }
            }
        }

        setPaint(startPaintColor, startColor)
        colorCell(canvas, finder.startY, finder.startX, 12F, startPaintColor)

        setPaint(endPaintColor, endColor)
        colorCell(canvas, finder.endY, finder.endX, 12F, endPaintColor)

    }

    private fun colorCell(canvas: Canvas, r: Int, c: Int, radius: Float, paintColor: Paint) {
        val rectF = RectF(
            ((c - 1) * cellSize).toFloat(),
            ((r - 1) * cellSize).toFloat(),
            (c * cellSize).toFloat(),
            (r * cellSize).toFloat()
        )

        canvas.drawRoundRect(rectF, radius, radius, paintColor)
        invalidate()
    }

    private fun setPaint(paintColor: Paint, color: Int) {
        paintColor.apply {
            style = Paint.Style.FILL
            this.color = color
            isAntiAlias = true
        }
    }

    fun getFinder() = this.finder

    fun isValid(x: Int, y: Int): Boolean {
        return x in 1..15 && y in 1..15//y>=1 && y<=15
    }

    // Create Grid
    private fun drawGrid(canvas: Canvas) {
        for (i in 1..15) {
            for (j in 1..15) {
                val rectF = RectF(
                    (((j - 1) * cellSize) + 5.toFloat()),
                    (((i - 1) * cellSize) + 5.toFloat()),
                    ((j * cellSize) - 5.toFloat()),
                    ((i * cellSize) - 5.toFloat())
                )

                val cornersRadius = 15
                canvas.drawRoundRect(rectF, cornersRadius.toFloat(), cornersRadius.toFloat(), pathPaintColor)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Get the coordinates of the touch event.
        val eventX = event.x
        val eventY = event.y
        if (!isSolving) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val y = ceil(eventY / cellSize).toInt()
                    val x = ceil(eventX / cellSize).toInt()

                    turn =  if (x == finder.endX && y == finder.endY) { END_BLOCK_TURN }
                            else if (x == finder.startX && y == finder.startY) { START_BLOCK_TURN }
                            else { OBSTACLE_BLOCK_TURN }

                    if (turn == OBSTACLE_BLOCK_TURN) moveCell(x, y, turn, 1)
                    else moveCell(x, y, turn, 0)
                }
                MotionEvent.ACTION_MOVE -> {
                    val y = ceil(eventY / cellSize).toInt()
                    val x = ceil(eventX / cellSize).toInt()
                    moveCell(x, y, turn, 0)
                }
            }
            invalidate()
        }
        return true
    }

    fun setSolving(flag: Boolean) {
        isSolving = flag
    }

    // Determine color and type
    private fun moveCell(x: Int, y: Int, turn: Int, firstTouch: Int) {
        if (turn == START_BLOCK_TURN) {
            if (isValid(x,y) && (finder.board[y][x] == EMPTY_CELL_CODE)) {
                finder.board[finder.startY][finder.startX] = EMPTY_CELL_CODE
                finder.startX = x
                finder.startY = y
                finder.board[finder.startY][finder.startX] = START_CELL_CODE
            }
        }
        else if (turn == END_BLOCK_TURN) {
            if (isValid(x,y) && (finder.board[y][x] == EMPTY_CELL_CODE)) {
                finder.board[finder.endY][finder.endX] = EMPTY_CELL_CODE
                finder.endX = x
                finder.endY = y
                finder.board[finder.endY][finder.endX] = END_CELL_CODE
            }
        }
        else {
            if (isValid(x,y) &&
                !(x == finder.startX && y == finder.startY) &&
                !(x == finder.endX && y == finder.endY) &&
                (abs(x - finder.obstacleX) >= 1 || abs(y - finder.obstacleY) >= 1 || firstTouch == 1)
            ) {
                if (finder.board[y][x] == EMPTY_CELL_CODE) {
                    finder.obstacleX = x
                    finder.obstacleY = y
                    finder.board[y][x] = OBSTACLE_CELL_CODE
                } else if (finder.board[y][x] == OBSTACLE_CELL_CODE) {
                    finder.obstacleX = x
                    finder.obstacleY = y
                    finder.board[y][x] = EMPTY_CELL_CODE
                }
            }
        }
    }

    private fun colorObstacleCell(canvas: Canvas, r: Int, c: Int) {
        if (r != -1 && c != -1 && finder.board[r][c] == OBSTACLE_CELL_CODE) {
            val rectF = RectF(
                ((c - 1) * cellSize).toFloat(),
                ((r - 1) * cellSize).toFloat(),
                (c * cellSize).toFloat(),
                (r * cellSize).toFloat()
            )

            val cornersRadius = 15

            canvas.drawRoundRect(
                rectF,  // rect
                cornersRadius.toFloat(),  // rx
                cornersRadius.toFloat(),  // ry
                obstaclePaintColor // Paint
            )
        }
        invalidate()
    }

}