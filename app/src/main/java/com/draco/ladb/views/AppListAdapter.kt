package com.draco.ladb.views

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.draco.ladb.R


class AppListAdapter(
    private val context: Activity,
    private val appList: List<AppRow>,
    private val appListImmutable: List<AppRow>,
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

    override fun getFilter(): Filter {
        return AppsFilter<AppRow>(appList, appListImmutable, this)
    }

//    override fun getFilter(): Filter {
//        return filter
//    }


    fun filterResults(searchText: String) {
       appList.filter { e -> searchText.lowercase() in e.title.lowercase() }
    }
}

class AppsFilter<T>(appList: List<AppRow>, appListImmutable: List<AppRow>, appListAdapter: AppListAdapter): Filter() {

    private val appList = appList
    var myListAdapter: AppListAdapter? = appListAdapter

    private var sourceObjects = appListImmutable


    override fun performFiltering(constraint: CharSequence?): FilterResults {
        val filterSeq: String = constraint.toString().lowercase()
        val result = FilterResults()
        if (filterSeq.isNotEmpty()) {
            val filter = ArrayList<AppRow>()
            for (i in appList.indices) {
                // the filtering itself:
                if (appList[i].title.lowercase().contains(filterSeq)) {
                    filter.add(appList[i])
                }
            }
            result.count = filter.size
            result.values = filter
        } else {
            // add all objects
            synchronized(this) {
                result.values = sourceObjects
                result.count = sourceObjects.size
            }
            result.values = sourceObjects
            result.count = sourceObjects.size
        }

        return result
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            val filtered = results!!.values as ArrayList<T>
            myListAdapter?.notifyDataSetChanged()
            myListAdapter?.clear()

            var i = 0
            val l: Int = filtered.size
            while (i < l) {
                myListAdapter?.add(filtered[i] as AppRow)
                i++
            }

            myListAdapter?.notifyDataSetInvalidated()
    }

}