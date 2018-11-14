package com.example.lukas.trainerapp.ui.fragments


import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*

import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.lukas.trainerapp.R
import com.example.lukas.trainerapp.db.entity.User
import com.example.lukas.trainerapp.db.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.fragment_account_edit.*
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.widget.ImageView
import android.widget.Toast
import com.example.lukas.trainerapp.AppExecutors
import com.example.lukas.trainerapp.db.AppDatabase
import com.example.lukas.trainerapp.server.service.UserWebService
import com.example.lukas.trainerapp.ui.NavigationActivity
import com.example.lukas.trainerapp.utils.DrawableUtils
import com.google.gson.GsonBuilder
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.util.*

class AccountEditDialogFragment : DialogFragment() {

    lateinit var userViewModel : UserViewModel
    lateinit var mDb: AppDatabase
    var userId: String? = null
    var uriToImage: Uri? = null
    var databaseId: Long = 0
    val PICK_IMAGE = 1
    var bArray: ByteArray? = null
    val maxSize: Double = 300000.0

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View {
        // Inflate the layout to use as dialog or embedded fragment
        var rootView = inflater.inflate(R.layout.fragment_account_edit, container, false)
        mDb = AppDatabase.getInstance(activity)
        rootView.post({
            userViewModel = ViewModelProviders.of(activity!!).get(UserViewModel::class.java)
            userViewModel.user.observe(this, Observer { user: User ->
                if (user.imageArray == null) {
                    DrawableUtils.setupInitials(initials_image_view_fragment_edit, user)
                } else{
                    bArray = user.imageArray
                    var gotBitmap = DrawableUtils.convertByteToBitmap(bArray)
                    initials_image_view_fragment_edit.setImageBitmap(gotBitmap)
                }
                name_edit_text.text = SpannableStringBuilder(user.fullName)
                phone_edit_text.text = SpannableStringBuilder(user.phoneNumber)
                email_edit_text.text = SpannableStringBuilder(user.email)
                userId = user.userId
                databaseId = user.id
                edit_profile_submit_text_view.setOnClickListener {
                    submitEdit()
                }
            })
            initials_image_view_fragment_edit.setOnClickListener {
                var intent = Intent()
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
            }
        })
        return rootView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            uriToImage = data?.data
            var bitmap = MediaStore.Images.Media.getBitmap(activity?.contentResolver, uriToImage)
            var newBitmap = bitmap
            var sizeBeforeResize = bitmap.byteCount
            if (sizeBeforeResize > maxSize) {
                var divideBy : Double = sizeBeforeResize.div(maxSize)
                newBitmap = DrawableUtils.resizeBitmapByScale(bitmap, divideBy)
            }
            var sizeAfterResize = newBitmap.byteCount
            bArray = DrawableUtils.convertBitmapToByte(newBitmap)
            initials_image_view_fragment_edit.setImageBitmap(newBitmap)
        }
    }

    /** The system calls this only when creating the layout in a dialog. */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // The only reason you might override this method when using onCreateView() is
        // to modify any dialog characteristics. For example, the dialog includes a
        // title by default, but your custom layout might not need it. So here you can
        // remove the dialog title, but you must call the superclass to get the Dialog.
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.actionBar.setDisplayHomeAsUpEnabled(true)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
    }

    fun submitEdit(){
        // Reset errors.
        name_edit_text.setError(null)
        email_edit_text.setError(null)
        phone_edit_text.setError(null)

        // Store values at the time of the login attempt.
        val email = email_edit_text.getText().toString()
        val phoneNumber = phone_edit_text.getText().toString()
        val fullName = name_edit_text.getText().toString()

        var cancel = false
        var focusView: View? = null


        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            email_edit_text.setError(getString(R.string.error_field_required))
            focusView = email_edit_text
            cancel = true
        } else if (!isEmailValid(email)) {
            email_edit_text.setError(getString(R.string.error_invalid_email))
            focusView = email_edit_text
            cancel = true
        }
        if (TextUtils.isEmpty(phoneNumber)) {
            phone_edit_text.setError(getString(R.string.error_field_required))
            focusView = phone_edit_text
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView!!.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            val currentTime = Calendar.getInstance().time
            val user = User(databaseId, userId, fullName, email, phoneNumber, currentTime, uriToImage, bArray)

            val gson = GsonBuilder()
                    .setLenient()
                    .create()

            val retrofit = Retrofit.Builder()
                    .baseUrl(userViewModel.baseUrl)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()

            val userWebService = retrofit.create(UserWebService::class.java)

            userWebService.postUser(user).enqueue(object : Callback<User> {
                override fun onResponse(call: Call<User>, response: Response<User>) {
                    AppExecutors.getInstance().diskIO().execute {
                        mDb.userDao().insertUser(user)
                        (activity as NavigationActivity).backOnStack()
                    }
                }

                override fun onFailure(call: Call<User>, t: Throwable) {
                    Toast.makeText(activity, t.localizedMessage, Toast.LENGTH_LONG).show()
                }
            })

        }
    }

    private fun isEmailValid(email: String): Boolean {
        //TODO: Replace this with your own logic
        return email.contains("@")
    }
}
