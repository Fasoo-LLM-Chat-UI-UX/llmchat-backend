package kr.ac.kau.llmchat.service.reader

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface JinaReaderApi {
    @GET("reader/v1/extract")
    fun extractContent(
        @Query("url") url: String,
    ): Call<JinaReaderResponse>
}

data class JinaReaderResponse(
    val content: String,
    val title: String,
    val url: String,
)
