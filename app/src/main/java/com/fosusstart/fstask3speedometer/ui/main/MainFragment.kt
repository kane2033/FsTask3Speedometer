package com.fosusstart.fstask3speedometer.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fosusstart.fstask3speedometer.R
import kotlinx.android.synthetic.main.main_fragment.*

class MainFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.main_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val delta = 45f
        increaseSpeed.setOnClickListener { speedometer.changeSpeedByDeltaAnimated(delta) }
        decreaseSpeed.setOnClickListener { speedometer.changeSpeedByDeltaAnimated(-delta) }
    }

}