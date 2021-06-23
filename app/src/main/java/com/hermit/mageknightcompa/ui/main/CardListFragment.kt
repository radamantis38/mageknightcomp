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
import com.hermit.mageknightcompa.databinding.FragmentMainBinding

/**
 * A placeholder fragment containing a simple view.
 */
class CardListFragment (_data1:ArrayList<CardModel>) : Fragment() {

    val data1 = _data1




    private var _binding: FragmentMainBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        var mainView = inflater.inflate(R.layout.fragment_main, null);

        val concatAdapter = CustomAdapter(data1)


        val recyclerView: RecyclerView =mainView.findViewById(R.id.recycler_view)
        recyclerView.adapter = concatAdapter

        return mainView;
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}