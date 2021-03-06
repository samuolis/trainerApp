package com.trainerapp.ui.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.trainerapp.R
import com.trainerapp.base.BaseFragment
import com.trainerapp.di.component.ActivityComponent
import com.trainerapp.enums.EventDetailScreen
import com.trainerapp.extension.getViewModel
import com.trainerapp.navigation.NavigationController
import com.trainerapp.ui.adapters.UserEventsRecyclerViewAdapter
import com.trainerapp.ui.viewmodel.EventViewModel
import kotlinx.android.synthetic.main.fragment_archived_events_dialog.*
import javax.inject.Inject

class ArchivedEventsDialogFragment : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var navigationController: NavigationController

    lateinit var eventViewModel: EventViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_archived_events_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        eventViewModel = getViewModel(viewModelFactory)
        archived_events_swipe_container.setOnRefreshListener {
            eventViewModel.loadEvents()
        }
        archived_events_swipe_container.setColorSchemeResources(R.color.colorAccent)
        archived_events_recyclerview.layoutManager = LinearLayoutManager(context)
        eventViewModel.archivedEvents.observe(this, Observer {
            archived_events_recyclerview.adapter = null
            val list = it
            archived_events_recyclerview.adapter = UserEventsRecyclerViewAdapter(
                    list,
                    context!!
            ) { position ->
                navigationController.showEventDetailsDialogFragment(
                        eventId = it[position].eventId!!,
                        eventDetailScreen = EventDetailScreen.HOME
                )
            }
        })

        eventViewModel.refreshStatus.observe(this, Observer {
            archived_events_swipe_container.isRefreshing = it != 0
        })

    }

    override fun onInject(activityComponent: ActivityComponent) {
        super.onInject(activityComponent)
        activityComponent.inject(this)
    }
}
