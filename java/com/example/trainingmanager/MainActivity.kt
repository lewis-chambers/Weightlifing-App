package com.example.trainingmanager

import Utils
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View.GONE
import android.widget.Button
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.gson.Gson
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var MainLifecycleListener : MainLifecycleListener
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        makeMainLifecycleListener()
        val db: DatabaseHelper = DatabaseHelper(this)
        //addPrograms(db)
        //addExercises(db)
        //db.createCurrentProgramTable()
    }
    fun addPrograms(db: DatabaseHelper) {
        File(filesDir, "/programs/").walk().maxDepth(1).forEach {
            if (it.toString().contains("/programs/")) {
                val file = File(it,"workout.json")
                val program = Utils().parseProgram(file.readText())
                db.createProgram(program)
                db.createSessions(program)
                db.createComments(this,program)
                //for (session in program.program) {
                //    db.createSession(Utils().replacePunctuationCheck(program.title),session)
                //}
            }
        }
    }
    fun addExercises(db: DatabaseHelper) {
        File(filesDir, "/exercises/").walk().maxDepth(1).forEach {
            if (it.toString().contains("/exercises/")) {
                val file = File(it,"exercise.json")
                val exercise = Utils().parseExerciseStorage(file.readText())
                db.createExercise(exercise)
                db.createExerciseInfo(exercise)
            }
        }
    }
    private fun makeMainLifecycleListener() {
        MainLifecycleListener = MainLifecycleListener(this,findViewById(R.id.mainLayout),lifecycle)
    }
    override fun onBackPressed() {
        finishAffinity()
    }
}

class MainLifecycleListener(
    private var context : Context,
    private var baseLayout : LinearLayout,
    lifecycle : Lifecycle
) : LifecycleObserver {

    init {
        lifecycle.addObserver(this)
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun create() {
        initaliseFolders()
        setProgramsButton()
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun resume () {
        setRecentProgramButton()
    }
    private fun setProgramsButton() {
        val btn = baseLayout.findViewWithTag<Button>("programs")

        btn.setOnClickListener {
            val intent = Intent(context, Programmes::class.java)
            context.startActivity(intent)
        }
    }
    private fun setRecentProgramButton() {
        val db = DatabaseHelper(context)
        val currentProgramName = db.getProgramName(db.getCurrentProgram()!!)
        val btn = baseLayout.findViewWithTag<Button>("current_program")
        if (currentProgramName != null) {
            lateinit var intent:Intent
            val currentProgram = db.getSessions(currentProgramName)
            btn.setOnClickListener {
                when (currentProgram?.count()) {
                    1 -> {
                        intent = Intent(context,SessionEntryScreen::class.java)
                        intent.putExtra("session",currentProgram.last())
                    }
                    else -> {
                        intent = Intent(context, WorkoutLaunchScreen::class.java)
                    }
                }
                intent.putExtra("program",currentProgramName)
                context.startActivity(intent)
            }
        } else {
            btn.visibility = GONE
        }
    }
    private fun initaliseFolders() {
        val rootPath = context.filesDir
        val programsPath = File(rootPath,"/programs/")
        val exercisesPath = File(rootPath,"/exercises/")
        val stringsPath = File(rootPath,"/strings/")
        val trackingPath = File(rootPath,"/tracking/")

        if (!programsPath.isDirectory) {
            programsPath.mkdirs()
        }
        if (!exercisesPath.isDirectory) {
            exercisesPath.mkdirs()
        }
        if (!stringsPath.isDirectory) {
            stringsPath.mkdirs()
            val arr = context.resources.getStringArray(R.array.exercise_strings)
            val stringsFile = File(stringsPath,"exercise_strings.json")
            stringsFile.writeText(Gson().toJson(arr))
        }
        if (!trackingPath.isDirectory) {
            trackingPath.mkdirs()
            val file = File(trackingPath,"program_activity.json")
            val empty = Utils.ProgramTracking(
                mutableListOf()
            )
            file.writeText(Gson().toJson(empty))
        }

        Utils().deleteSharedPrefs(context,"your_prefs")
    }
}