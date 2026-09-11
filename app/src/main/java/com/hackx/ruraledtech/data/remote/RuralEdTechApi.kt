package com.hackx.ruraledtech.data.remote

import com.hackx.ruraledtech.data.remote.dto.ContentPackageDto
import com.hackx.ruraledtech.data.remote.dto.SyncRequest
import com.hackx.ruraledtech.data.remote.dto.SyncResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface RuralEdTechApi {
    
    @POST("api/v1/sync/events")
    suspend fun syncEvents(@Body request: SyncRequest): Response<SyncResponse>

    @GET("api/v1/content")
    suspend fun getAvailableContent(
        @Query("status") status: String = "PUBLISHED"
    ): Response<List<ContentPackageDto>>

    @Streaming
    @GET("api/v1/content/{package_id}/{version}/download")
    suspend fun downloadContent(
        @Path("package_id") packageId: String,
        @Path("version") version: Int
    ): Response<ResponseBody>

    @GET("api/v1/classes")
    suspend fun getClasses(): Response<List<com.hackx.ruraledtech.data.remote.dto.ClassGroupDto>>
    
    @GET("api/v1/teacher/classes/{class_id}/analytics")
    suspend fun getClassAnalytics(
        @Path("class_id") classId: String
    ): Response<com.hackx.ruraledtech.data.remote.dto.ClassAnalyticsDto>
}
