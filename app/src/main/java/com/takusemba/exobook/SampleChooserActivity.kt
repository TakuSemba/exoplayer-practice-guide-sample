package com.takusemba.exobook

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ExpandableListView
import androidx.appcompat.app.AppCompatActivity

class SampleChooserActivity : AppCompatActivity(), ExpandableListView.OnChildClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_chooser)
        val sampleAdapter = SampleAdapter()
        val sampleListView = findViewById<ExpandableListView>(R.id.sample_list)
        sampleListView.setAdapter(sampleAdapter)
        sampleListView.setOnChildClickListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        item.isChecked = !item.isChecked
        return true
    }

    override fun onChildClick(
        parent: ExpandableListView,
        view: View,
        groupPosition: Int,
        childPosition: Int,
        id: Long
    ): Boolean {

        return true
    }

    companion object {

        fun createIntent(context: Context): Intent {
            return Intent(context, SampleChooserActivity::class.java)
        }
    }
}
