package com.seoultech.onde

import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET("/crawl")
    fun getCrawledData(): Call<List<String>>
}
