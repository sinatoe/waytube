package com.waytube.app.network.data

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as ExtractorRequest
import org.schabi.newpipe.extractor.downloader.Response as ExtractorResponse

private const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:142.0) Gecko/20100101 Firefox/142.0"

class NewPipeDownloader(private val okHttpClient: OkHttpClient) : Downloader() {
    override fun execute(request: ExtractorRequest): ExtractorResponse {
        val request = Request.Builder()
            .method(request.httpMethod(), request.dataToSend()?.toRequestBody())
            .url(request.url())
            .addHeader("User-Agent", BROWSER_USER_AGENT)
            .apply {
                for ((name, values) in request.headers()) {
                    for (value in values) {
                        addHeader(name, value)
                    }
                }
            }
            .build()

        val response = okHttpClient.newCall(request).execute()

        return ExtractorResponse(
            response.code,
            response.message,
            response.headers.toMultimap(),
            response.body.string(),
            response.request.url.toString()
        )
    }
}
