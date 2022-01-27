package com.example.trainingmanager

import Utils
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.io.File
import java.util.*
import kotlin.properties.Delegates

class ExerciseFragment : Fragment() {
    private lateinit var exerciseStorage: Utils.ExerciseStorage
    private var variantInt by Delegates.notNull<Int>()
    private var lastInstance by Delegates.notNull<Utils.ExerciseInstance>()
    private lateinit var variant: Utils.Exercise
    private lateinit var inputWrapper : LinearLayout
    private var running: Boolean = false
    private var seconds: Int = 0
    private lateinit var titleTextView: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState:
        Bundle?
    ): View? {
        val args = arguments!!
        // Creates the view controlled by the fragment
        val view = inflater.inflate(R.layout.fragment_exercise, container, false)
        titleTextView = view.findViewById(R.id.exercise_title)
        val restButton = view.findViewById<Button>(R.id.rest_timer)
        val addSetButton = view.findViewById<Button>(R.id.add_set)
        inputWrapper = view.findViewById(R.id.input_wrapper)
        val timerLayout = activity!!.findViewById<LinearLayout>(R.id.timer_layout)
        val hideTimerButton = activity!!.findViewById<Button>(R.id.hide_timer)
        val lastComment = view.findViewById<TextView>(R.id.last_comments)
        val setsReps = view.findViewById<TextView>(R.id.rep_range)

        // Retrieve and display the movie data from the Bundle
        exerciseStorage = args.getParcelable("exercise_storage")!!
        variantInt = args.getInt("variant_index")
        variant = exerciseStorage.variants[variantInt]
        lastInstance = variant.instances.last()

        titleTextView.text = exerciseStorage.exerciseTitle
        lastComment.text = lastInstance.comment
        setsReps.text = "${variant.sets} sets x ${variant.reps} reps"

        for (i in 0 until variant.sets.toInt()) {
            initialiseLine(i)
        }
        // Button Listeners
        addSetButton.setOnClickListener{
            //initialiseLine(inputWrapper.childCount)
        }
        stopwatch()
        restButton.setOnClickListener{
            running = true
            seconds = 0
            timerLayout.visibility = VISIBLE
        }
        hideTimerButton.setOnClickListener{
            //timerLayout.visibility = INVISIBLE
            running = false
            seconds = 0
        }
        return view
    }
    private fun initialiseLine(setNumber: Int) {
        var i = setNumber
        if (i > lastInstance.sets.count()) {i = lastInstance.sets.count()-1}
        val line = View.inflate(context,R.layout.exercise_input_row,null)
        val set_tv = line.findViewWithTag<TextView>("set")
        val lastWeight = line.findViewWithTag<TextView>("previous")
        val thisWeight = line.findViewWithTag<EditText>("weight")
        val reps = line.findViewWithTag<EditText>("reps")
        val rpe = line.findViewWithTag<EditText>("difficulty")

        set_tv.text = (i+1).toString()
        lastWeight.text = (lastInstance.sets[i].weight.toString()+
                " x "
                +lastInstance.sets[i].repsCompleted.toString())
        thisWeight.setText(lastInstance.sets[i].weight.toString())
        reps.setText(lastInstance.sets[i].repsCompleted.toString())
        rpe.setText(lastInstance.sets[i].difficult.toString())

        inputWrapper.addView(line)
    }

    private fun stopwatch() {
        runTimer()

    }
    private fun runTimer() {
        val timeView = inputWrapper.getChildAt(0).findViewWithTag<TextView>("set")
        val handler = Handler()

        handler.post(object : Runnable {
            override fun run() {
                val minutes = seconds % 3600 / 60
                val secs = seconds % 60

                val time = java.lang.String
                    .format(
                        Locale.getDefault(),
                        "%02d:%02d",minutes, secs
                    )

                //Set the text view text.
                timeView.text = time

                //If running is true, increment the
                // seconds variable.
                if (running) {
                    seconds++
                    println(seconds)
                }

                // Post the code again
                // with a delay of 1 second.
                handler.postDelayed(this, 1000)
            }
        })
    }
    fun startTimer() {
        running = true
    }
    companion object {

        // Method for creating new instances of the fragment
        fun newInstance(context: Context, progExercise: Utils.ExerciseProgram): ExerciseFragment {
            val args = Bundle()
            val file = File(context.filesDir,"/exercises/"+
            Utils().removePunctuationCheck(progExercise.title)+
            "/exercise.json")
            val exercise = Utils().parseExerciseStorage(file.readText())
            for (i in 0 until exercise.variants.count()) {
                if (exercise.variants[i].sets == progExercise.sets &&
                    exercise.variants[i].reps == progExercise.reps) {
                    args.putParcelable("exercise_storage",exercise as Parcelable)
                    args.putInt("variant_index",i)
                }
            }

            // Create a new MovieFragment and set the Bundle as the arguments
            // to be retrieved and displayed when the view is created
            val fragment = ExerciseFragment()
            fragment.arguments = args
            return fragment
        }
        fun titleReturn(fragment: ExerciseFragment) {
            println(fragment.titleTextView)
        }
    }
}