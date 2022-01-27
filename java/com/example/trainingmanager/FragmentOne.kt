package com.example.trainingmanager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_one.*

class FragmentOne : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_one, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tv_fragment_name.text = "Fragment One"
    }
    companion object {

        // Method for creating new instances of the fragment
        fun getInstance(position:Int): FragmentOne {

            // to be retrieved and displayed when the view is created
            val fragment = FragmentOne()
            return fragment
        }
    }
}