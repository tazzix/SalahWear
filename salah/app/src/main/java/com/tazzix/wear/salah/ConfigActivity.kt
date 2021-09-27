package com.tazzix.wear.salah

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import androidx.fragment.app.FragmentActivity

class ConfigActivity : FragmentActivity() {

    private lateinit var methodSpinner: Spinner
    private lateinit var asrCalcSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        methodSpinner = findViewById(R.id.method_choice)
        asrCalcSpinner = findViewById(R.id.asrcalc_choice)

        // TODO: show initial chosen values as per sharedprefs
        val sharedPreference =  getSharedPreferences("SALAH_LOCATION", Context.MODE_PRIVATE)
        asrCalcSpinner.setSelection(sharedPreference.getInt("ASR_CALC", 0))
        methodSpinner.setSelection(sharedPreference.getInt("METHOD", 0))

        // TODO: get selection changes
        asrCalcSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val sharedPreference =  getSharedPreferences("SALAH_LOCATION", Context.MODE_PRIVATE)
                sharedPreference.edit().putInt("ASR_CALC", position).apply()
            }
        }

        methodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val sharedPreference =  getSharedPreferences("SALAH_LOCATION", Context.MODE_PRIVATE)
                sharedPreference.edit().putInt("METHOD", position).apply()
            }
        }
    }
}
