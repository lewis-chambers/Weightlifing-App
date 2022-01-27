package com.example.trainingmanager

import Utils
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class session_entry_exercise : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState:
        Bundle?
    ): View? {
        val exerciseName = view!!.findViewById<TextView>(R.id.exercise_name)
        val exerciseSetsReps = view!!.findViewById<TextView>(R.id.exercise_sets_reps)
        // Creates the view controlled by the fragment
        val view = inflater.inflate(R.layout.fragment_exercise, container, false)
        // Retrieve and display the movie data from the Bundle
        val args = arguments!!
        exerciseName.text = args.getString("title")
        exerciseSetsReps.text = args.getString("reps")
        return view
    }

    companion object {

        // Method for creating new instances of the fragment
        fun newInstance(context: Context, progExercise: Utils.ExerciseProgram): ExerciseFragment {
            val args = Bundle()
            args.putString("title", progExercise.title)
            args.putString("sets", progExercise.sets)

            // Create a new MovieFragment and set the Bundle as the arguments
            // to be retrieved and displayed when the view is created
            val fragment = ExerciseFragment()
            fragment.arguments = args
            return fragment
        }
    }
}