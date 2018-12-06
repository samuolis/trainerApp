package com.example.lukas.trainerapp.ui.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager

import androidx.fragment.app.Fragment

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast

import com.example.lukas.trainerapp.AppExecutors
import com.example.lukas.trainerapp.ui.LoginActivity
import com.example.lukas.trainerapp.R
import com.example.lukas.trainerapp.db.AppDatabase
import com.example.lukas.trainerapp.db.entity.User
import com.example.lukas.trainerapp.ui.viewmodel.UserViewModel
import com.example.lukas.trainerapp.model.UserData
import com.example.lukas.trainerapp.web.webservice.UserWebService
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.Gson
import com.google.gson.GsonBuilder

import java.io.IOException

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.ButterKnife
import com.example.lukas.trainerapp.ui.NavigationActivity
import com.facebook.AccessToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import com.facebook.accountkit.ui.SkinManager.Skin.CONTEMPORARY
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.fragment_login.*


class LoginFragment : Fragment() {

    private var callbackManager: CallbackManager? = null

    private var userViewModel: UserViewModel? = null
    private var mDb: AppDatabase? = null
    private var userWebService: UserWebService? = null
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    lateinit var userSharedPref: SharedPreferences
    lateinit var editor: SharedPreferences.Editor

    fun LoginFragment(){
        // Required empty public constructor
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        callbackManager = CallbackManager.Factory.create()

        val permissionsList = mutableListOf<String>("email", "public_profile")
        LoginManager.getInstance().logInWithReadPermissions(activity, permissionsList)
        LoginManager.getInstance().registerCallback(callbackManager,
                object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                Log.d(TAG, "facebook:onSuccess:$loginResult")
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                Log.d(TAG, "facebook:onCancel")
                // ...
            }

            override fun onError(error: FacebookException) {
                Log.d(TAG, "facebook:onError", error)
                // ...
            }

        })

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build()

        googleSignInClient = GoogleSignIn.getClient(activity!!, gso)


        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_login, container, false)
        userViewModel = ViewModelProviders.of(activity!!).get(UserViewModel::class.java)
        val gson = GsonBuilder()
                .setLenient()
                .create()

        val retrofit = Retrofit.Builder()
                .baseUrl(userViewModel!!.baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

        userWebService = retrofit.create(UserWebService::class.java)
        userSharedPref = activity!!.getSharedPreferences(context
                ?.getString(R.string.user_id_preferences), Context.MODE_PRIVATE)
        editor = userSharedPref?.edit()
        mDb = AppDatabase.getInstance(context)
        AppExecutors.getInstance().diskIO().execute {
            val user = mDb!!.userDao().simpleUser
            if (user != null) {
                (activity as LoginActivity).GoToNavigationActivity()
            }
        }

        rootView.post {

            login_email!!.setOnClickListener {
                showProgressBar()
                signIn()
            }

            login_email.setSize(SignInButton.SIZE_WIDE)
        }

        return rootView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager?.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
                hideProgressBar()

            }
        } else {
            hideProgressBar()
        }



    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)

        auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information

                        val user = auth.currentUser
                        Log.d(TAG, "signInWithCredential:success with user : " + user!!.email)
                        val sharedPref = activity!!.getSharedPreferences(getString(R.string.user_id_preferences), Context.MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putString(getString(R.string.user_id_key), user.uid)
                        editor.commit()
                        getUser(user.uid)

                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        Snackbar.make(login_layout, "Authentication Failed.", Snackbar.LENGTH_SHORT).show()
                        hideProgressBar()
                    }

                }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d(TAG, "handleFacebookAccessToken:$token")
        // [START_EXCLUDE silent]
        showProgressBar()
        // [END_EXCLUDE]

        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success")
                        val user = auth.currentUser
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        Toast.makeText(context, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()

                    }

                    hideProgressBar()
                }
    }

    fun getUser(userId: String) {
        userWebService!!.getExistantUser(userId)
                .enqueue(object : Callback<User> {
                    override fun onResponse(call: Call<User>, response: Response<User>) {
                        if (response.isSuccessful) {
                            FirebaseInstanceId.getInstance().instanceId
                                    .addOnCompleteListener { task ->
                                        if (!task.isSuccessful) {
                                            hideProgressBar()
                                            Log.w("LoginFragment", "getInstanceId failed", task.exception)
                                        } else {
                                            val user = response.body()

                                            // Get new Instance ID token
                                            val token = task.result!!.token
                                            if (response.body()!!.userFcmToken == null || response.body()!!.userFcmToken === user!!.userFcmToken) {
                                                user!!.userFcmToken = token
                                            }
                                            userWebService!!.postUser(user!!).enqueue(object : Callback<User> {
                                                override fun onResponse(call: Call<User>, response: Response<User>) {

                                                }

                                                override fun onFailure(call: Call<User>, t: Throwable) {

                                                }
                                            })
                                        }

                                    }
                            AppExecutors.getInstance().diskIO().execute {
                                mDb!!.userDao().insertUser(response.body()!!)
                                (activity as LoginActivity).GoToNavigationActivity()
                            }
                        } else {
                            (activity as LoginActivity).GoToRegisterFragment()
                        }
                    }

                    override fun onFailure(call: Call<User>, t: Throwable) {
                        (activity as LoginActivity).GoToRegisterFragment()
                    }
                })
    }


    private fun showProgressBar() {
        login_progress!!.visibility = View.VISIBLE
        progress_bar_background!!.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        login_progress!!.visibility = View.GONE
        progress_bar_background!!.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser?.uid != null){
            (activity as LoginActivity).GoToNavigationActivity()
        }
    }

    companion object {

        private val APP_REQUEST_CODE = 10

        private val RC_SIGN_IN = 11

        /**
         * Keep track of the login task to ensure we can cancel it if requested.
         */

        private val EMAIL = "email"

        private val TAG = LoginFragment::class.java.simpleName
    }

}// Required empty public constructor
