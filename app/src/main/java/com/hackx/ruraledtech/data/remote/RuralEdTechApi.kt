package com.hackx.ruraledtech.data.remote

import com.hackx.ruraledtech.data.remote.dto.ClassAnalyticsDto
import com.hackx.ruraledtech.data.remote.dto.ClassGroupCreateDto
import com.hackx.ruraledtech.data.remote.dto.ClassGroupDto
import com.hackx.ruraledtech.data.remote.dto.ContentPackageDto
import com.hackx.ruraledtech.data.remote.dto.SyncRequest
import com.hackx.ruraledtech.data.remote.dto.SyncResponse
import com.hackx.ruraledtech.data.remote.dto.TeacherCreateDto
import com.hackx.ruraledtech.data.remote.dto.TeacherDashboardDto
import com.hackx.ruraledtech.data.remote.dto.TeacherResponseDto
import com.hackx.ruraledtech.data.remote.dto.TokenDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface RuralEdTechApi {

    @POST("api/v1/sync/events")
    suspend fun syncEvents(@Body request: SyncRequest): Response<SyncResponse>

    @GET("api/v1/content")
    suspend fun getAvailableContent(
        @Query("status") status: String = "PUBLISHED",
    ): Response<List<ContentPackageDto>>

    @Streaming
    @GET("api/v1/content/{package_id}/{version}/download")
    suspend fun downloadContent(
        @Path("package_id") packageId: String,
        @Path("version") version: Int,
    ): Response<ResponseBody>

    @Multipart
    @POST("api/v1/content")
    suspend fun uploadContent(
        @Part("package_id") packageId: RequestBody,
        @Part("version") version: RequestBody,
        @Part("subject") subject: RequestBody,
        @Part("grade") grade: RequestBody,
        @Part("language") language: RequestBody,
        @Part("checksum") checksum: RequestBody,
        @Part("manifest") manifest: RequestBody,
        @Part file: MultipartBody.Part,
    ): Response<ContentPackageDto>

    @POST("api/v1/auth/register")
    suspend fun registerTeacher(@Body request: TeacherCreateDto): Response<TeacherResponseDto>

    @FormUrlEncoded
    @POST("api/v1/auth/login")
    suspend fun loginTeacher(
        @Field("username") email: String,
        @Field("password") password: String,
    ): Response<TokenDto>

    @GET("api/v1/auth/me")
    suspend fun getCurrentTeacher(): Response<TeacherResponseDto>

    @GET("api/v1/classes")
    suspend fun getClasses(): Response<List<ClassGroupDto>>

    @POST("api/v1/classes")
    suspend fun createClass(@Body request: ClassGroupCreateDto): Response<ClassGroupDto>

    @POST("api/v1/classes/{class_id}/learners/{learner_id}")
    suspend fun addLearnerToClass(
        @Path("class_id") classId: String,
        @Path("learner_id") learnerId: String,
    ): Response<Unit>

    @GET("api/v1/teacher/dashboard")
    suspend fun getTeacherDashboard(): Response<TeacherDashboardDto>

    @GET("api/v1/teacher/classes/{class_id}/analytics")
    suspend fun getClassAnalytics(@Path("class_id") classId: String): Response<ClassAnalyticsDto>
}
