package com.playerid.app.data.teamsnap

import retrofit2.Response
import retrofit2.http.*

/**
 * TeamSnap API v3 Service Interface
 * Based on the official TeamSnap API documentation
 */
interface TeamSnapApiService {
    
    companion object {
        const val BASE_URL = "https://api.teamsnap.com/v3/"
        const val AUTH_URL = "https://auth.teamsnap.com/"
    }
    
    /**
     * Authentication Endpoints
     */
    @POST("oauth/access_token")
    suspend fun authenticate(
        @Body authRequest: TeamSnapAuthRequest
    ): Response<TeamSnapAuthResponse>
    
    @POST("oauth/refresh_token")
    suspend fun refreshToken(
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token"
    ): Response<TeamSnapAuthResponse>
    
    /**
     * Team Endpoints
     */
    @GET("teams")
    suspend fun getUserTeams(
        @Header("Authorization") token: String
    ): Response<TeamSnapResponse<TeamSnapTeam>>
    
    @GET("teams/{team_id}")
    suspend fun getTeam(
        @Header("Authorization") token: String,
        @Path("team_id") teamId: Long
    ): Response<TeamSnapResponse<TeamSnapTeam>>
    
    /**
     * Member Endpoints
     */
    @GET("teams/{team_id}/members")
    suspend fun getTeamMembers(
        @Header("Authorization") token: String,
        @Path("team_id") teamId: Long
    ): Response<TeamSnapResponse<TeamSnapMember>>
    
    @GET("members/{member_id}")
    suspend fun getMember(
        @Header("Authorization") token: String,
        @Path("member_id") memberId: Long
    ): Response<TeamSnapResponse<TeamSnapMember>>
    
    /**
     * Bulk Loading Endpoint
     * This is very efficient for loading team + members in one call
     */
    @GET("bulk_load")
    suspend fun bulkLoad(
        @Header("Authorization") token: String,
        @Query("team_id") teamId: Long,
        @Query("types") types: String = "team,member"
    ): Response<TeamSnapResponse<Any>>
    
    /**
     * Search Teams (if available)
     */
    @GET("teams/search")
    suspend fun searchTeams(
        @Header("Authorization") token: String,
        @Query("search") query: String
    ): Response<TeamSnapResponse<TeamSnapTeam>>
}