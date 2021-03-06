package com.trainerapp.ui.fragments


import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.trainerapp.R
import com.trainerapp.base.BaseDialogFragment
import com.trainerapp.di.component.ActivityComponent
import com.trainerapp.extension.getViewModel
import com.trainerapp.ui.NavigationActivity
import com.trainerapp.ui.customui.InputFilterMinMax
import com.trainerapp.ui.viewmodel.EventViewModel
import kotlinx.android.synthetic.main.fragment_search.*
import javax.inject.Inject

class SearchFragment : BaseDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var eventViewModel: EventViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        eventViewModel = getViewModel(viewModelFactory)
        val userSharedPref = activity?.getSharedPreferences(context
                ?.getString(R.string.user_id_preferences), Context.MODE_PRIVATE)
        val editor = userSharedPref?.edit()
        val radiusValue = userSharedPref?.getString(context?.getString(R.string.user_prefered_distance_key), "30")
        events_radius_edit_text.text = SpannableStringBuilder(radiusValue)
        events_radius_edit_text.filters = arrayOf<InputFilter>(InputFilterMinMax("1", "10000"))
        search_fab.setOnClickListener {
            if (events_radius_edit_text.text != null) {
                editor?.putString(getString(R.string.user_prefered_distance_key), events_radius_edit_text.text.toString())
                editor?.commit()
                (activity as NavigationActivity).backOnStack()
            } else {
                Snackbar.make(it, "Some fields are missing", Snackbar.LENGTH_LONG)
                        .show()
            }
        }
    }

    override fun onInject(activityComponent: ActivityComponent) {
        super.onInject(activityComponent)
        activityComponent.inject(this)
    }

}
