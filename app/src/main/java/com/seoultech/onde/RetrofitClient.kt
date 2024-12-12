package com.seoultech.onde

import android.telecom.Call
import android.util.Log
import org.jsoup.helper.HttpConnection
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private val BASE_URL = App.getInstance().getString(R.string.base_url).format(
        App.getInstance().getString(R.string.api_domain)
    )

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
