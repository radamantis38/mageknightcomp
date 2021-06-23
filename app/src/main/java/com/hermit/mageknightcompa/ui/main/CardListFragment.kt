package com.hermit.mageknightcompa.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.hermit.mageknightcompa.R


/**
 * A placeholder fragment containing a simple view.
 */
class CardListFragment (_data1:ArrayList<CardModel>) : Fragment() {

    val data1 = _data1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        var mainView = inflater.inflate(R.layout.fragment_main, null);

        val concatAdapter = context?.let { CustomAdapter(data1, it) }


        val recyclerView: RecyclerView =mainView.findViewById(R.id.recycler_view)
        recyclerView.adapter = concatAdapter

        return mainView;
    }


}