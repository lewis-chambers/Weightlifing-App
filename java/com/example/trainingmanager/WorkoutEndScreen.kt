package com.example.trainingmanager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_workout_end_screen.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.floor

class WorkoutEndScreen : AppCompatActivity() {
    private lateinit var program: String
    private lateinit var session: String
    private lateinit var comment: EditText
    private lateinit var startTime: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_end_screen)

        comment = findViewById(R.id.comment_et)

        session = intent.getStringExtra("session")!!
        program = intent.getStringExtra("program")!!
        startTime = intent.getStringExtra("start_time")!!
        setTimeTaken()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this,MainActivity::class.java)
        startActivity(intent)
        this.finish()
    }
    private fun setTimeTaken() {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val endTime = sdf.format(Date())

        val first = sdf.parse(startTime)
        val second = sdf.parse(endTime)
        val diffInMillies = abs(second.time - first.time)
        val diff = java.util.concurrent.TimeUnit.MINUTES.convert(diffInMillies,
            java.util.concurrent.TimeUnit.MILLISECONDS
        ).toInt()
        var timeTaken = ""
        if (diff < 60) {
            timeTaken = if (diff < 10) {
                "00:0$diff"
            } else {
                "00:$diff"
            }
        } else if (diff >=60) {
            val hours = floor((diff / 60).toDouble()).toInt()
            val minutes = floor((diff % 60).toDouble()).toInt()

            var m = minutes.toString()
            var h = hours.toString()

            if (minutes < 10) {
                m ="0$minutes"
            }
            if (hours < 10) {
                h = "0$hours"
            }
            timeTaken = "$h:$m"
        }

        if (timeTaken.isNotEmpty()) {
            val tv = findViewById<TextView>(R.id.time_taken_tv)
            tv.text = "${tv.text}$timeTaken"
        }
    }

    fun doneOnClick(view: View) {
        val db = DatabaseHelper(this)
        val comment = comment_et.text.toString()
        if (comment.isNotEmpty()) {
            db.insertSessionComment(
                program,
                session,
                comment_et.text.toString(),
                startTime
            )
        }
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}