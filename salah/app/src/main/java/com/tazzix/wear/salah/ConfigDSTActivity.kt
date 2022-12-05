package com.tazzix.wear.salah

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity

import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView


class ConfigDSTActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        val recyclerView: WearableRecyclerView = findViewById(R.id.config_menu_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.isEdgeItemsCenteringEnabled = true
        recyclerView.layoutManager = WearableLinearLayoutManager(this)

        val menuItems: ArrayList<MenuItem> = ArrayList()
        val methods = resources.getStringArray(R.array.array_dst_offsets)

        for (method in methods) {
            menuItems.add(MenuItem(android.R.drawable.ic_menu_info_details, "", method))
        }

        recyclerView.adapter = MenuAdapter(this, R.layout.notitle_item, menuItems, object: MenuAdapter.AdapterCallback {
            override fun onItemClicked(menuPosition: Int?) {
                val sharedPreferences =  getSharedPreferences("SALAH_LOCATION", Context.MODE_PRIVATE)
                sharedPreferences.edit().putInt("DST_OFFSET", menuPosition!!).apply()
                finish()
            }
        })
    }
}
