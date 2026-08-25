package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class ShopApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ensureFirebaseInitialized(this)
    }

    companion object {
        private const val TAG = "ShopApplication"

        fun ensureFirebaseInitialized(context: Context) {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:652224811291:android:23f864c558df82113603be")
                        .setApiKey("AIzaSyAruRSCBucfPlKVgSN6rBC6H8Pajq8rGdM")
                        .setProjectId("nafishop-54e99")
                        .setDatabaseUrl("https://nafishop-54e99-default-rtdb.firebaseio.com/")
                        .setStorageBucket("nafishop-54e99.firebasestorage.app")
                        .setGcmSenderId("652224811291")
                        .build()
                    FirebaseApp.initializeApp(context.applicationContext, options)
                    Log.d(TAG, "Firebase initialized with custom options successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Firebase with custom options, trying default", e)
                try {
                    FirebaseApp.initializeApp(context.applicationContext)
                } catch (fallbackEx: Exception) {
                    Log.e(TAG, "Default Firebase initialization also failed", fallbackEx)
                }
            }
        }
    }
}
