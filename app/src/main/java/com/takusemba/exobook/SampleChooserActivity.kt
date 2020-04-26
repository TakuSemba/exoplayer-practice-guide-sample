package com.takusemba.exobook

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ExpandableListView
import androidx.appcompat.app.AppCompatActivity
import com.takusemba.exobook.SampleAdapter.Companion.SAMPLE_GROUPS

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
        val sample = SAMPLE_GROUPS[groupPosition].samples[childPosition]
        val intent = Intent(this, sample.destination)
        startActivity(intent)
        return true
    }
}
