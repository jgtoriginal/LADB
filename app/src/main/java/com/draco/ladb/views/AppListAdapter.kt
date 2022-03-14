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
    private val imgid: Array<Drawable>
)
    : ArrayAdapter<String>(context, R.layout.snippet_list_row, title) {

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.snippet_list_row, null, true)

        val titleText = rowView.findViewById(R.id.app_name) as TextView
        val imageView = rowView.findViewById(R.id.app_icon) as ImageView
        val subtitleText = rowView.findViewById(R.id.app_package) as TextView

        titleText.text = title[position]

        imageView.setImageDrawable(imgid[position])
        subtitleText.text = description[position]

        return rowView
    }
}