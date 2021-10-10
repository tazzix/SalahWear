package com.tazzix.wear.salah

import android.content.Context
import android.widget.TextView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView.ViewHolder


class MenuAdapter(private val context: Context, itemLayout: Int, dataArgs: ArrayList<MenuItem>, callback: AdapterCallback?) :
RecyclerView.Adapter<MenuAdapter.RecyclerViewHolder>() {
    private var dataSource = ArrayList<MenuItem>()
    private var itemLayout: Int

    interface AdapterCallback {
        fun onItemClicked(menuPosition: Int?)
    }

    private val callback: AdapterCallback?

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerViewHolder {
        val view: View =
            LayoutInflater.from(context).inflate(itemLayout, parent, false)
        return RecyclerViewHolder(view)
    }

    class RecyclerViewHolder(view: View) : ViewHolder(view) {
        var menuContainer: RelativeLayout = view.findViewById(R.id.menu_container)
        var menuTitle: TextView = view.findViewById(R.id.menu_title)
        var menuDetail: TextView = view.findViewById(R.id.menu_detail)
        var menuIcon: ImageView = view.findViewById(R.id.menu_icon)

    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val dataProvider = dataSource[position]
        holder.menuTitle.text = dataProvider.text
        holder.menuDetail.text = dataProvider.detail
        holder.menuIcon.setImageResource(dataProvider.image)
        holder.menuContainer.setOnClickListener {
            callback?.onItemClicked(position)
        }
    }

    override fun getItemCount(): Int {
        return dataSource.size
    }

    init {
        dataSource = dataArgs
        this.callback = callback
        this.itemLayout = itemLayout
    }
}

class MenuItem(val image: Int, val text: String, val detail: String)
