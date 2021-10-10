package com.tazzix.wear.salah

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity

import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView


class ConfigActivity : FragmentActivity() {

    override fun onResume() {
        super.onResume()
        drawUI(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        drawUI(this)
    }

    private fun drawUI(context: Context) {
        val recyclerView: WearableRecyclerView = findViewById(R.id.config_menu_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.isEdgeItemsCenteringEnabled = true
        recyclerView.layoutManager = WearableLinearLayoutManager(context)

        val menuItems: ArrayList<MenuItem> = ArrayList()
        val sharedPreferences =  getSharedPreferences("SALAH_LOCATION", Context.MODE_PRIVATE)
        val curMethod = resources.getStringArray(R.array.array_methods)[sharedPreferences.getInt("METHOD", 0)]
        val curAsrCalc = resources.getStringArray(R.array.array_asrcalcs)[sharedPreferences.getInt("ASR_CALC", 0)]
        menuItems.add(MenuItem(android.R.drawable.ic_menu_compass, getString(R.string.method), curMethod))
        menuItems.add(MenuItem(android.R.drawable.ic_menu_info_details, getString(R.string.asr_calc), curAsrCalc))

        recyclerView.adapter = MenuAdapter(context, R.layout.recview_item, menuItems, object: MenuAdapter.AdapterCallback {
            override fun onItemClicked(menuPosition: Int?) {
                Log.d("SALAH", "Item: $menuPosition")
                when (menuPosition) {
                    0 -> {
                        val intent = Intent(context, ConfigMethodActivity::class.java)
                        startActivity(intent)
                    }
                    1 -> {
                        val intent = Intent(context, ConfigAsrCalcActivity::class.java)
                        startActivity(intent)
                    }
                    else -> {
                        finish()
                    }
                }
            }
        })
    }
}
