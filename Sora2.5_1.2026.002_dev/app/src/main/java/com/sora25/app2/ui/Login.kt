package com.sora25.app2.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sora25.app2.R
import com.sora25.app2.data.Auth
import kotlinx.coroutines.launch

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** Вход и регистрация: почта + пароль или номер телефона (SMS). */
@Composable
fun LoginScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var tab by remember { mutableIntStateOf(0) }            // 0 = почта, 1 = телефон
    var signUp by remember { mutableStateOf(true) }          // почта: регистрация / вход
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun fail(msg: String) { error = msg; loading = false }

    Box(Modifier.fillMaxSize()) {
        // Обои на заднем плане: res/drawable/login_bg.* (замени на свои)
        Image(painterResource(R.drawable.login_bg), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(48.dp))
            Image(painterResource(R.drawable.logo_cloud), null, Modifier.size(110.dp))
            Spacer(Modifier.height(10.dp))
            Image(painterResource(R.drawable.logo_text), null, Modifier.width(200.dp))
            Spacer(Modifier.height(24.dp))

            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xCC0B0B14)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(tab == 0, { tab = 0; error = null }, label = { Text("Почта") })
                        FilterChip(tab == 1, { tab = 1; error = null }, label = { Text("Телефон") })
                    }

                    if (tab == 0) {
                        OutlinedTextField(
                            email, { email = it }, label = { Text("Почта") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            password, { password = it }, label = { Text("Пароль (от 6 символов)") }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        PrimaryButton(if (signUp) "Создать аккаунт" else "Войти", loading) {
                            when {
                                !email.contains("@") -> error = "Введи адрес почты"
                                password.length < 6 -> error = "Пароль: минимум 6 символов"
                                else -> {
                                    loading = true; error = null
                                    scope.launch {
                                        val r = if (signUp) Auth.signUpEmail(email, password) else Auth.signInEmail(email, password)
                                        r.onFailure { fail(Auth.readable(it)) }
                                        loading = false
                                    }
                                }
                            }
                        }
                        TextButton({ signUp = !signUp; error = null }, Modifier.fillMaxWidth()) {
                            Text(if (signUp) "Уже есть аккаунт? Войти" else "Нет аккаунта? Создать")
                        }
                    } else if (verificationId == null) {
                        OutlinedTextField(
                            phone, { phone = it }, label = { Text("Телефон, например +79001234567") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        PrimaryButton("Получить код", loading) {
                            val num = phone.filter { it.isDigit() || it == '+' }
                            val act = ctx.findActivity()
                            when {
                                num.length < 8 || !num.startsWith("+") -> error = "Номер в формате +79001234567"
                                act == null -> error = "Не удалось начать проверку"
                                else -> {
                                    loading = true; error = null
                                    Auth.startPhone(
                                        act, num,
                                        onCodeSent = { id -> verificationId = id; loading = false },
                                        onSignedIn = { loading = false },
                                        onError = { fail(it) },
                                    )
                                }
                            }
                        }
                    } else {
                        Text("Код отправлен по SMS на $phone", color = Color.White.copy(0.8f), fontSize = 13.sp)
                        OutlinedTextField(
                            code, { code = it }, label = { Text("Код из SMS") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        PrimaryButton("Подтвердить", loading) {
                            if (code.length < 4) error = "Введи код из SMS"
                            else {
                                loading = true; error = null
                                scope.launch {
                                    Auth.confirmPhone(verificationId!!, code).onFailure { fail(Auth.readable(it)) }
                                    loading = false
                                }
                            }
                        }
                        TextButton({ verificationId = null; code = ""; error = null }, Modifier.fillMaxWidth()) {
                            Text("Изменить номер")
                        }
                    }

                    error?.let {
                        Text(it, color = Color(0xFFFF9C9C), fontSize = 13.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PrimaryButton(text: String, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = !loading, shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.Black)
        else Text(text, fontSize = 16.sp)
    }
}
