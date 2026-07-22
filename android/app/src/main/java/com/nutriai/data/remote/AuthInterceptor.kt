package com.nutriai.data.remote

import com.nutriai.data.local.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Attaches the Bearer access token to every request when one is stored. */
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // Device UTC offset in minutes (e.g. +330 for IST) so the server uses the phone's day.
        val offsetMin = java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000
        val builder = chain.request().newBuilder().addHeader("X-TZ-Offset", offsetMin.toString())
        val token = tokenStore.accessTokenBlocking()
        if (token != null) builder.addHeader("Authorization", "Bearer $token")
        return chain.proceed(builder.build())
    }
}
