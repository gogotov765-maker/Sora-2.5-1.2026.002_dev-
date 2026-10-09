package com.sora25.app2.data

import com.google.firebase.auth.FirebaseAuth
import com.sora25.app2.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Клиент твоего сервера (папка server/). */
object Backend {
    private val http = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Запускает генерацию. Сервер сразу создаёт черновик (статус queued) и возвращает id.
     * mode: "text" | "character" | "image"
     */
    suspend fun generate(
        mode: String,
        prompt: String,
        durationSec: Int,
        aspect: String,
        image: ByteArray?,
        characterName: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val token = FirebaseAuth.getInstance().currentUser!!.getIdToken(false).await().token!!
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("mode", mode)
                .addFormDataPart("prompt", prompt)
                .addFormDataPart("duration", durationSec.toString())
                .addFormDataPart("aspect", aspect)
                .apply {
                    if (!characterName.isNullOrBlank()) addFormDataPart("character", characterName)
                    if (image != null) addFormDataPart(
                        "image", "ref.jpg", image.toRequestBody("image/jpeg".toMediaType())
                    )
                }.build()
            val req = Request.Builder()
                .url(BuildConfig.BACKEND_URL.trimEnd('/') + "/generate")
                .header("Authorization", "Bearer $token")
                .post(body).build()
            http.newCall(req).execute().use { r ->
                val text = r.body?.string().orEmpty()
                check(r.isSuccessful) { "Сервер: ${r.code} $text" }
                org.json.JSONObject(text).getString("id")
            }
        }
    }
}
