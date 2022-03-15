package com.draco.ladb.views

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.draco.ladb.R

class AppListAdapter(
    private val context: Activity,
    private val title: Array<String>,
    private val description: Array<String>,
    private val imgId: Array<Drawable>,
    deleteApp: (input: String) -> Unit
)
    : ArrayAdapter<String>(context, R.layout.list_row, title) {
    val deleteApp = deleteApp

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.list_row, null, true)

        val titleText = rowView.findViewById(R.id.app_name) as TextView
        val imageView = rowView.findViewById(R.id.app_icon) as ImageView
        val subtitleText = rowView.findViewById(R.id.app_package) as TextView

        titleText.text = title[position]

        imageView.setImageDrawable(imgId[position])
        subtitleText.text = description[position]

        val deleteButton = rowView.findViewById(R.id.delete_button) as Button

        deleteButton.setOnClickListener {
            deleteApp(title[position])
        }

        return rowView
    }
}