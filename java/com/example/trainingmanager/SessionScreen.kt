package com.example.trainingmanager

import Stopwatch
import Utils
import WarningPopup
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.iterator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

class SessionScreen : AppCompatActivity() {
    private lateinit var exercises: MutableList<Utils.ExerciseProgram>
    private lateinit var program: String
    private lateinit var session: String
    private lateinit var sessionStartTime: String
    private lateinit var sessionTimer: Stopwatch
    private var db = DatabaseHelper(this)
    private lateinit var viewPager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_screen)
        //initialising action bars
        this.supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar!!.setDisplayShowCustomEnabled(true)
        supportActionBar!!.setCustomView(R.layout.session_action_bar)
        val view: View = supportActionBar!!.customView
        supportActionBar!!.elevation = 0f

        // Initialising views
        val stopTimerButton = findViewById<Button>(R.id.hide_timer)
        viewPager = findViewById<ViewPager>(R.id.viewpager2)
        val exerciseTimerLayout = findViewById<LinearLayout>(R.id.timer_layout)
        val finishedButton = view.findViewById<Button>(R.id.finished_button)
        val timerStartButton = view.findViewById<ImageButton>(R.id.bar_timer_button)

        // Initialising timers
        sessionTimer = Stopwatch("%02d:%02d:%02d")
        val exerciseTimer = Stopwatch("%02d:%02d")
        sessionTimer.initialise(supportActionBar!!.customView.findViewById(R.id.session_timer))
        exerciseTimer.initialise(findViewById(R.id.timer_text))


        // Importing data from intents
        session = intent.getStringExtra("session")!!
        program = intent.getStringExtra("program")!!
        exercises = db.getSessionExercises(program,session)

        //Extracting the start time
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        sessionStartTime = sdf.format(Date())

        sessionTimer.attachStartTime(sessionStartTime)

        val adapter = ExercisePagerAdapter(supportFragmentManager, exercises,this,exerciseTimer,program, session)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = exercises.count()
        val tabDots = findViewById<TabLayout>(R.id.tabDots)
        tabDots.setupWithViewPager(viewPager,true)
        // Initialising button commands
        finishedButton.setOnClickListener{
            fun error(viewPager: ViewPager): Boolean {
                for (i in 0 until viewPager.childCount) {
                    val setWrapper = viewPager.getChildAt(i).findViewById<RecyclerView>(R.id.sets_layout)
                    for (ii in 0 until setWrapper.childCount) {
                        val button = setWrapper.getChildAt(ii).findViewById<Button>(R.id.set_finished_button)
                        if (button.visibility == View.VISIBLE) {
                            viewPager.setCurrentItem(i)
                            button.requestFocus()
                            return true
                        }
                    }
                }
                return false
            }
            if (!error(viewPager)) {
                WarningPopup(this, "Are you sure you want to submit?") { (::submitted)(viewPager) }
            } else {
                    Toast.makeText(this,"A set was not completed", Toast.LENGTH_SHORT).show()
            }
        }
        stopTimerButton.setOnClickListener{
            exerciseTimer.stop()
            exerciseTimerLayout.visibility = View.INVISIBLE
        }
        timerStartButton.setOnClickListener{
            exerciseTimer.start()
            exerciseTimerLayout.visibility = View.VISIBLE
        }
        sessionTimer.start()

    }

    override fun onBackPressed() {
        fun handler() {
            this.finish()
        }
        WarningPopup(this,resources.getString(R.string.progress_will_be_lost),::handler)

    }

    private fun submitted(viewPager: ViewPager) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val endTime = sdf.format(Date())
        val db = DatabaseHelper(this)
        db.createExerciseActivityTable()
        for (i in 0 until viewPager.childCount) {
            val page = viewPager.getChildAt(i)
            val inputWrapper = page.findViewById<RecyclerView>(R.id.sets_layout)
            val comment = page.findViewWithTag<EditText>("this_comments").text.toString()
            if (comment.isNotEmpty()) {
                db.insertComment(
                    exercises[i].title,
                    comment,
                    sessionStartTime
                )
            }
            for (setBox in inputWrapper) {
                db.insertExerciseSet(exercises[i].title,
                    exercises[i].sets,
                    exercises[i].reps,
                    setBox.findViewWithTag<EditText>("weight_input").text.toString(),
                    setBox.findViewWithTag<EditText>("reps_input").text.toString(),
                    setBox.findViewWithTag<EditText>("RPE_input").text.toString(),
                    sessionStartTime
                )
            }
        }

        db.insertActivity(program,session,sessionStartTime,endTime)
        db.updateCurrentProgram(program)

        val intent = Intent(this, WorkoutEndScreen::class.java)
        intent.putExtra("program",program)
        intent.putExtra("session",session)
        intent.putExtra("start_time",sessionStartTime)

        startActivity(intent)
        this.finishAffinity()
    }

    class ExercisePagerAdapter(fragmentManager: FragmentManager, private val exercises: MutableList<Utils.ExerciseProgram>,
                               private val context: Context, private val exerciseTimer:Stopwatch, private val program:String, private val session:String
    ) :
        FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        private val db = DatabaseHelper(context)
        private val lastSessionData = db.getLastSessionEntry(program,session)

        // 2
        override fun getItem(position: Int): Fragment {
            return RecyclerFragment.newInstance(context, exercises[position],exerciseTimer,db,lastSessionData,program,session,position)
        }

        // 3
        override fun getCount(): Int {
            return exercises.size
        }
    }
}