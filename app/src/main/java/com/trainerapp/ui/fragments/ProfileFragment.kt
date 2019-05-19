package com.trainerapp.ui.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.trainerapp.R
import com.trainerapp.enums.ProfilePicture
import com.trainerapp.models.User
import com.trainerapp.ui.NavigationActivity
import com.trainerapp.ui.adapters.UserEventsRecyclerViewAdapter
import com.trainerapp.ui.viewmodel.EventViewModel
import com.trainerapp.utils.DrawableUtils
import kotlinx.android.synthetic.main.fragment_profile.*


/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class ProfileFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInfo()
    }

    private fun setupInfo() {
        val eventViewModel = ViewModelProviders.of(activity!!).get(EventViewModel::class.java)
        profile_events_recycler_view.layoutManager = LinearLayoutManager(context) as RecyclerView.LayoutManager?
        eventViewModel.getStatus()?.observe(this, Observer {
            profile_swipe_container.isRefreshing = !(it == 0)
        })
        expired_event_layout.setOnClickListener {
            (activity as NavigationActivity).showArchivedEventsDialogFragment()
        }
        profile_swipe_container.setOnRefreshListener {
            eventViewModel.loadUserData()
        }
        profile_swipe_container.setColorSchemeResources(R.color.colorAccent)
        eventViewModel.getUserWeb()?.observe(this, Observer { user: User ->
            initials_image_view.post {
                if (user.profilePictureIndex == null || user.profilePictureIndex!! >= ProfilePicture.values().size) {
                    DrawableUtils.setupInitials(initials_image_view, user)
                } else {
                    initials_image_view.setImageResource(ProfilePicture.values()[user.profilePictureIndex!!].drawableId)
                }
            }
            user_full_name_text_view.text = user.fullName
            profile_linear_layout.visibility = View.VISIBLE

            eventViewModel.loadUserEventsByIds()

        })
        user_full_name_text_view.setOnClickListener {
            (activity as NavigationActivity).showAccountEditDialogFragment()
        }
        initials_image_view.setOnClickListener {
            (activity as NavigationActivity).showAccountEditDialogFragment()
        }

        eventViewModel.getUserEvents()?.observe(this, Observer { userEvents ->

            profile_events_recycler_view.adapter = UserEventsRecyclerViewAdapter(
                    userEvents,
                    context!!
            ) { position ->
                (activity as NavigationActivity)
                        .showEventDetailsDialogFragment(userEvents[position].eventId!!)
            }
        })

        eventViewModel.getArchivedEvents()?.observe(this, Observer {
            if (it != null) {
                expired_event_count.text = it.size.toString()
                expired_event_layout.visibility = View.VISIBLE
            }
        })

    }
}
