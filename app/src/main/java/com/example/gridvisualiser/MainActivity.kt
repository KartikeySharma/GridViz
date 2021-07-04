package com.example.gridvisualiser

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.gridvisualiser.ui.PathGrid
import com.example.gridvisualiser.model.PathFinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var pathGrid: PathGrid
    private var finder = PathFinder()
    private var speed: Long = 0
    private var algorithmToApply: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSpeedSpinner()
        setAlgoSpinner()

        pathGrid = findViewById(R.id.pathGrid)
        finder = pathGrid.getFinder()

        val btnSolve = findViewById<Button>(R.id.btn_solve)
        val btnReset = findViewById<Button>(R.id.btn_reset)
        val btnExit = findViewById<Button>(R.id.btn_exit)

        btnSolve.setOnClickListener {

            pathGrid.setSolving(true)

            GlobalScope.launch(Dispatchers.Main) {

                btnSolve.isEnabled = false
                btnReset.isEnabled = false
                btnExit.isEnabled = false
                btnSolve.setTextColor(Color.parseColor("#000000"))

                when (algorithmToApply) {
                    BFS_ALGO -> {
                        val found = finder.solveBFS(speed)
                        makeToast(found)
                    }
                    else -> {
                        val found = finder.solveDFS(speed)
                        makeToast(found)
                    }
                }

                btnSolve.isEnabled = true
                btnSolve.setBackgroundResource(R.drawable.rounded_border_black)//==(Color.parseColor("#FFFFFF"))
                btnSolve.setTextColor(Color.parseColor("#FFFFFF"))
                btnReset.isEnabled = true
                btnReset.setBackgroundResource(R.drawable.rounded_border_black)
                btnExit.isEnabled = true
                btnExit.setBackgroundResource(R.drawable.rounded_border_black)

            }
        }

        btnReset.setOnClickListener {
            pathGrid.setSolving(false)
            finder.resetGrid()
            pathGrid.invalidate()
        }

        btnExit.setOnClickListener {
            finish()
        }
    }

    private fun makeToast(found: Boolean) {
        if (found) {
            val toast = Toast.makeText(this@MainActivity, "Path Found.", Toast.LENGTH_SHORT)
            toast.show()
        } else {
            val toast = Toast.makeText(this@MainActivity, "No Path Found.", Toast.LENGTH_SHORT)
            toast.show()
        }
    }

    private fun setAlgoSpinner() {
        val algoSpinner: Spinner = findViewById(R.id.algo_spinner)
        algoSpinner.onItemSelectedListener = this
        ArrayAdapter.createFromResource(
            this,
            R.array.algo_array,
            R.layout.spinner_list_text
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_style)
            algoSpinner.adapter = adapter
        }
    }

    private fun setSpeedSpinner() {
        val speedSpinner: Spinner = findViewById(R.id.speed_spinner)
        speedSpinner.onItemSelectedListener = this
        ArrayAdapter.createFromResource(
            this,
            R.array.speed_array,
            R.layout.spinner_list_text
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_style)
            speedSpinner.adapter = adapter
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (parent!!.id) {
            R.id.speed_spinner -> {
                speed = when (parent.selectedItem.toString()) {
                    "Fast" -> {
                        FAST
                    }
                    "Average" -> {
                        AVG
                    }
                    else -> {
                        SLOW
                    }
                }
            }

            R.id.algo_spinner -> {
                algorithmToApply = when (parent.selectedItem.toString()) {
                    "BFS" -> {
                        BFS_ALGO
                    }
                    else -> {
                        DFS_ALGO
                    }

                }
            }
        }

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    companion object {
        const val FAST: Long = 30
        const val AVG: Long = 60
        const val SLOW: Long = 120

        const val BFS_ALGO = 1
        const val DFS_ALGO = 2
    }
}