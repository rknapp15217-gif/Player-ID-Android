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
        const val AUTH_TOKEN_URL = "https://auth.teamsnap.com/oauth/token"
    }
    
    /**
     * Authentication Endpoints
     */
    @FormUrlEncoded
    @POST(AUTH_TOKEN_URL)
    suspend fun authenticate(
        @Field("grant_type") grantType: String,
        @Field("username") email: String,
        @Field("password") password: String
    ): Response<TeamSnapAuthResponse>
    
    @FormUrlEncoded
    @POST(AUTH_TOKEN_URL)
    suspend fun refreshToken(
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token"
    ): Response<TeamSnapAuthResponse>

    @FormUrlEncoded
    @POST(AUTH_TOKEN_URL)
    suspend fun exchangeAuthorizationCode(
        @Field("grant_type") grantType: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("client_id") clientId: String,
        @Field("code_verifier") codeVerifier: String,
        @Field("client_secret") clientSecret: String? = null
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