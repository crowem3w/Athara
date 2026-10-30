package social.ahtara.authentication.net

/**
 * A non-2xx answer from the gateway (`{"error":{"code","message",...}}`), or a transport failure
 * (`status == 0`, `code == "NETWORK"`).
 */
class ApiException(
    val status: Int,
    val code: String,
    override val message: String,
    /** `existing_type` on 409 conflicts: the account type that already owns the identifier. */
    val existingType: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    val isNetwork: Boolean get() = status == 0
}
