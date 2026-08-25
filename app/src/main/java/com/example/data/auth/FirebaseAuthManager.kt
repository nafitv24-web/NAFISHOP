package com.example.data.auth

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class AuthResult {
    data class Success(val user: FirebaseUser, val message: String) : AuthResult()
    data class Error(val errorMessage: String, val rawException: Throwable? = null) : AuthResult()
}

suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exception ->
        if (cont.isActive) cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        if (cont.isActive) cont.cancel()
    }
}

class FirebaseAuthManager {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val currentUser: FirebaseUser?
        get() = try {
            auth.currentUser
        } catch (e: Exception) {
            null
        }

    val isUserLoggedIn: Boolean
        get() = currentUser != null

    suspend fun signUpWithEmail(
        email: String,
        pass: String,
        displayName: String? = null
    ): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).awaitTask()
            val user = result.user
            if (user != null) {
                if (!displayName.isNullOrBlank()) {
                    try {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build()
                        user.updateProfile(profileUpdates).awaitTask()
                    } catch (_: Exception) {
                        // ignore display name update failure if user is created
                    }
                }
                AuthResult.Success(user, "Firebase-এ অ্যাকাউন্ট সফলভাবে তৈরি হয়েছে!")
            } else {
                AuthResult.Error("অ্যাকাউন্ট তৈরিতে সমস্যা হয়েছে।")
            }
        } catch (e: Exception) {
            AuthResult.Error(getReadableErrorMessage(e), e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).awaitTask()
            val user = result.user
            if (user != null) {
                AuthResult.Success(user, "Firebase-এ লগইন সফল হয়েছে!")
            } else {
                AuthResult.Error("লগইন ব্যর্থ হয়েছে।")
            }
        } catch (e: Exception) {
            AuthResult.Error(getReadableErrorMessage(e), e)
        }
    }

    suspend fun sendPasswordReset(email: String): AuthResult {
        return try {
            auth.sendPasswordResetEmail(email).awaitTask()
            val dummyUser = currentUser
            if (dummyUser != null) {
                AuthResult.Success(dummyUser, "পাসওয়ার্ড রিসেট লিংক আপনার জিমেইলে পাঠানো হয়েছে।")
            } else {
                AuthResult.Success(
                    user = auth.currentUser ?: throw Exception("Reset sent"),
                    message = "পাসওয়ার্ড রিসেট লিংক আপনার জিমেইলে পাঠানো হয়েছে।"
                )
            }
        } catch (e: Exception) {
            if (e.message == "Reset sent") {
                AuthResult.Success(auth.currentUser!!, "পাসওয়ার্ড রিসেট লিংক আপনার জিমেইলে পাঠানো হয়েছে।")
            } else {
                AuthResult.Error(getReadableErrorMessage(e), e)
            }
        }
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (_: Exception) {}
    }

    private fun getReadableErrorMessage(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("The email address is already in use", ignoreCase = true) ||
            msg.contains("email-already-in-use", ignoreCase = true) ->
                "এই জিমেইল আইডি দিয়ে ইতিমধ্যে একটি অ্যাকাউন্ট তৈরি আছে! অনুগ্রহ করে 'লগইন করুন' বাটনে চাপুন।"
            msg.contains("There is no user record", ignoreCase = true) ||
            msg.contains("user-not-found", ignoreCase = true) ||
            msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
            msg.contains("wrong-password", ignoreCase = true) ||
            msg.contains("INVALID_PASSWORD", ignoreCase = true) ->
                "জিমেইল বা পাসওয়ার্ড ভুল হয়েছে! সঠিক পাসওয়ার্ড দিন অথবা 'নতুন অ্যাকাউন্ট তৈরি' করুন।"
            msg.contains("Password should be at least 6 characters", ignoreCase = true) ||
            msg.contains("weak-password", ignoreCase = true) ->
                "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে!"
            msg.contains("badly formatted", ignoreCase = true) ||
            msg.contains("invalid-email", ignoreCase = true) ->
                "সঠিক জিমেইল ঠিকানা দিন (যেমন: name@gmail.com)!"
            msg.contains("network error", ignoreCase = true) ||
            msg.contains("A network error", ignoreCase = true) ->
                "ইন্টারনেট সংযোগ বিচ্ছিন্ন অথবা ধীরগতির। ডাটা বা ওয়াইফাই সংযোগ চেক করুন।"
            msg.contains("Too many unsuccessful login attempts", ignoreCase = true) ||
            msg.contains("too-many-requests", ignoreCase = true) ->
                "অনেকবার ভুল পাসওয়ার্ড দেওয়া হয়েছে। কিছুক্ষণ পর চেষ্টা করুন অথবা পাসওয়ার্ড রিসেট করুন।"
            else -> "Firebase ত্রুটি: ${e.localizedMessage ?: "কিছু সমস্যা হয়েছে"}"
        }
    }
}
