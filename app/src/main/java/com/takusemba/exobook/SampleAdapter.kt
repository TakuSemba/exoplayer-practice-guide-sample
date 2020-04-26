package com.takusemba.exobook

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView

class SampleAdapter : BaseExpandableListAdapter() {

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_expandable_list_item_1, parent, false)
        val (title, _) = getGroup(groupPosition)
        (view as TextView).text = title
        return view
    }

    override fun getGroup(groupPosition: Int): Pair<String, Array<String>> {
        return SAMPLE_GROUP[groupPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getGroupCount(): Int {
        return SAMPLE_GROUP.size
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        val title = getChild(groupPosition, childPosition)
        view.findViewById<TextView>(android.R.id.text1).text = title
        return view
    }

    override fun getChild(groupPosition: Int, childPosition: Int): String {
        val (_, samples) = getGroup(groupPosition)
        return samples[childPosition]
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        val (_, samples) = getGroup(groupPosition)
        return samples.size
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    companion object {

        private val CORE_SAMPLES = arrayOf(
            "Chapter 1",
            "Chapter 2",
            "Chapter 3",
            "Chapter 4",
            "Chapter 5",
            "Chapter 6",
            "Chapter 7",
            "Chapter 8",
            "Chapter 9"
        )

        private val EXTENSION_SAMPLES = arrayOf(
            "Chapter 1 (IMA)",
            "Chapter 2 (Cast)",
            "Chapter 3 (MediaSession)",
            "Chapter 4 (Network)",
            "Chapter 5 (Codec)",
            "Chapter 6 (Scheduler)",
            "Chapter 7 (RTMP)",
            "Chapter 8 (Leanback)"
        )

        private val SAMPLE_GROUP = arrayOf(
            Pair("Core Library", CORE_SAMPLES),
            Pair("Extension Library", EXTENSION_SAMPLES)
        )
    }
}