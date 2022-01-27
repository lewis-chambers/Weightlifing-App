package com.example.trainingmanager

import Utils
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class WorkoutLaunchScreen : AppCompatActivity() {
    private lateinit var baseLayout: LinearLayout

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_launch_screen)
        baseLayout = findViewById(R.id.mainLayout)
        val program = intent.getStringExtra("program")!!
        buildPage(program)
    }
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        this.finish()
        return true
    }
    override fun onBackPressed() {
        this.finish()
    }
    @RequiresApi(Build.VERSION_CODES.M)
    fun buildPage(program:String) {
        val title = baseLayout.findViewWithTag<TextView>("title_et")
        val buttonLayout = baseLayout.findViewById<LinearLayout>(R.id.session_button_wrapper)
        title.text = program
        val db = DatabaseHelper(this)
        val sessionNames= db.getSessions(program)!!
        for (session in sessionNames) {
            val btn = Button(this)
            btn.background = this.resources.getDrawable(R.drawable.button)
            btn.setTextAppearance(R.style.button_text)
            btn.text = session
            val lp = Utils().widthHeight(MATCH_PARENT, WRAP_CONTENT)
            lp.setMargins(0,20,0,20)
            btn.layoutParams = lp

            btn.setOnClickListener{
                val intent = Intent(this,SessionEntryScreen::class.java)
                intent.putExtra("program",program)
                intent.putExtra("session",session)
                this.startActivity(intent)
            }
            buttonLayout.addView(btn)
        }
    }
}