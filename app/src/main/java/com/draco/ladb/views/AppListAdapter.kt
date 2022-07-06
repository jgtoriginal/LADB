package com.draco.ladb.views

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.draco.ladb.R

class AppListAdapter(
    private val context: Activity,
    private val appList: Array<AppRow>,
    private val deleteApp: (input: String, position: Int) -> Unit
)
    : ArrayAdapter<AppRow>(context, R.layout.list_row, appList) {

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.list_row, null, true)

        val titleText = rowView.findViewById(R.id.app_name) as TextView
        val imageView = rowView.findViewById(R.id.app_icon) as ImageView
        val subtitleText = rowView.findViewById(R.id.app_package) as TextView

        titleText.text = appList[position].title

        imageView.setImageDrawable(appList[position].imageId)
        subtitleText.text = appList[position].description

        val deleteButton = rowView.findViewById(R.id.delete_button) as Button

        deleteButton.setOnClickListener {
            deleteApp(appList[position].packageId, position)
        }

        return rowView
    }

    fun filter(searchText: String) {
        appList.filter { e -> searchText.lowercase() in e.title.lowercase() }
    }
}