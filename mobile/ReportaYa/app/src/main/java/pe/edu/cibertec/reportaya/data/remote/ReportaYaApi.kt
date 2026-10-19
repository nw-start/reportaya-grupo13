package pe.edu.cibertec.reportaya.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ReportaYaApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @GET("api/auth/me")
    suspend fun me(): Response<UserDto>

    @GET("api/tipos-incidencia")
    suspend fun listIncidentTypes(): Response<List<IncidentTypeDto>>

    @GET("api/incidencias")
    suspend fun listIncidents(): Response<List<IncidentResponseDto>>

    @POST("api/incidencias")
    suspend fun createIncident(@Body request: IncidentCreateRequestDto): Response<IncidentResponseDto>

    @PATCH("api/incidencias/{id}")
    suspend fun updateIncident(
        @Path("id") id: Long,
        @Body request: IncidentUpdateRequestDto
    ): Response<IncidentResponseDto>

    @DELETE("api/incidencias/{id}")
    suspend fun deleteIncident(@Path("id") id: Long): Response<Unit>
}
