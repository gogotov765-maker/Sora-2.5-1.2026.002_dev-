package com.sora25.app2.data

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/** Регистрация и вход: почта + пароль или номер телефона (SMS-код). Аккаунты хранит Firebase. */
object Auth {
    private val auth get() = FirebaseAuth.getInstance()

    suspend fun signUpEmail(email: String, password: String): Result<Unit> = runCatching {
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
        runCatching { user?.sendEmailVerification()?.await() } // письмо с подтверждением (не блокирует вход)
        Unit
    }

    suspend fun signInEmail(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    /** Шаг 1: отправить SMS. onCodeSent вернёт verificationId для шага 2. */
    fun startPhone(
        activity: Activity,
        phone: String,
        onCodeSent: (verificationId: String) -> Unit,
        onSignedIn: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val cb = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(c: PhoneAuthCredential) {
                // Автоподстановка кода: вход без ввода
                auth.signInWithCredential(c)
                    .addOnSuccessListener { onSignedIn() }
                    .addOnFailureListener { onError(readable(it)) }
            }
            override fun onVerificationFailed(e: FirebaseException) = onError(readable(e))
            override fun onCodeSent(id: String, t: PhoneAuthProvider.ForceResendingToken) = onCodeSent(id)
        }
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(cb)
                .build()
        )
    }

    /** Шаг 2: ввести код из SMS. */
    suspend fun confirmPhone(verificationId: String, code: String): Result<Unit> = runCatching {
        auth.signInWithCredential(PhoneAuthProvider.getCredential(verificationId, code.trim())).await()
        Unit
    }

    fun signOut() = auth.signOut()

    fun readable(e: Throwable): String = when ((e as? FirebaseAuthException)?.errorCode) {
        "ERROR_EMAIL_ALREADY_IN_USE" -> "Эта почта уже зарегистрирована"
        "ERROR_INVALID_EMAIL" -> "Неверный адрес почты"
        "ERROR_WEAK_PASSWORD" -> "Слишком простой пароль (минимум 6 символов)"
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_USER_NOT_FOUND" -> "Неверная почта или пароль"
        "ERROR_INVALID_VERIFICATION_CODE" -> "Неверный код из SMS"
        "ERROR_INVALID_PHONE_NUMBER" -> "Неверный номер. Пример: +79001234567"
        "ERROR_TOO_MANY_REQUESTS" -> "Слишком много попыток, попробуй позже"
        "ERROR_OPERATION_NOT_ALLOWED" -> "Этот способ входа не включён в Firebase"
        else -> e.message ?: "Что-то пошло не так"
    }
}
