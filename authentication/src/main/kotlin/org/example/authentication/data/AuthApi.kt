package org.example.authentication.data

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * The REST surface of this module: auth and profile endpoints. Contract: see docs/API_CONTRACT.md.
 * Token refresh is handled by :network (TokenAuthenticator), so it is not listed here.
 */
internal interface AuthApi {
    // -- auth (no bearer token is attached to /v1/auth/**) --
    @POST("v1/auth/signup") suspend fun signUp(@Body body: SignUpRequest): Response<TokenResponse>
    @POST("v1/auth/login") suspend fun signIn(@Body body: SignInRequest): Response<TokenResponse>
    @POST("v1/auth/social/login") suspend fun socialLogin(@Body body: SocialLoginRequest): Response<TokenResponse>
    @POST("v1/auth/social/signup") suspend fun socialSignUp(@Body body: SocialSignUpRequest): Response<TokenResponse>
    @POST("v1/auth/password/reset") suspend fun resetPassword(@Body body: ResetPasswordRequest): Response<UserDto>
    @POST("v1/auth/phone/send-code") suspend fun sendCode(@Body body: SendCodeRequest): Response<Unit>
    @POST("v1/auth/phone/verify") suspend fun verifyCode(@Body body: VerifyCodeRequest): Response<VerifyCodeResponse>
    @POST("v1/auth/logout") suspend fun logout(@Body body: LogoutRequest): Response<Unit>

    // -- profile --
    @GET("v1/me") suspend fun me(): Response<UserDto>
    @PATCH("v1/me") suspend fun updateMe(@Body body: ProfileUpdateRequest): Response<UserDto>
    @DELETE("v1/me") suspend fun deleteMe(): Response<Unit>

    /** [kind] is "profile" or "cover". */
    @Multipart
    @PUT("v1/me/photo/{kind}")
    suspend fun uploadPhoto(@Path("kind") kind: String, @Part file: MultipartBody.Part): Response<UserDto>

    @DELETE("v1/me/photo/{kind}") suspend fun deletePhoto(@Path("kind") kind: String): Response<UserDto>
}
