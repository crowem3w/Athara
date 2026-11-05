package soc.athara.app.shared.authentication.data

import kotlinx.serialization.Serializable



@Serializable
internal data class DetailsDto(val industry: String? = null, val phone: String? = null, val registrationId: String? = null)

@Serializable
internal data class SignUpRequest(
    val name: String,
    val identifier: String? = null,
    val password: String,
    val accountType: String,
    val details: DetailsDto? = null,
    val username: String? = null,
    val email: String? = null,
    val birthday: String? = null,
    val phoneVerificationToken: String? = null,
)

@Serializable
internal data class SignInRequest(val identifier: String, val password: String)

@Serializable
internal data class SocialLoginRequest(val provider: String, val idToken: String)

@Serializable
internal data class SocialSignUpRequest(
    val provider: String,
    val idToken: String,
    val identifier: String,
    val accountType: String,
    val phoneVerificationToken: String? = null,
)

@Serializable
internal data class ResetPasswordRequest(val identifier: String, val newPassword: String, val phoneVerificationToken: String? = null)

@Serializable
internal data class ValidateRequest(val field: String, val value: String)

@Serializable
internal data class ValidateResponse(val valid: Boolean, val code: String? = null, val message: String? = null)

@Serializable
internal data class LogoutRequest(val refreshToken: String)

@Serializable
internal data class SendCodeRequest(val phone: String)

@Serializable
internal data class VerifyCodeRequest(val phone: String, val code: String)

@Serializable
internal data class VerifyCodeResponse(val verificationToken: String)

@Serializable
internal data class ProfileUpdateRequest(val about: String? = null, val location: String? = null)

@Serializable
internal data class UserDto(
    val id: String,
    val identifier: String? = null,
    val displayName: String,
    val provider: String = "password",
    val accountType: String = "PERSONAL",
    val industry: String? = null,
    val phone: String? = null,
    val registrationId: String? = null,
    val username: String? = null,
    val email: String? = null,
    val birthday: String? = null,
    val about: String? = null,
    val location: String? = null,
    val photoUrl: String? = null,
    val coverUrl: String? = null,
)


@Serializable
internal data class TokenResponse(val accessToken: String, val refreshToken: String, val user: UserDto? = null)