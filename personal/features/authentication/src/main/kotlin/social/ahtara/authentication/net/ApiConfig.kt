package social.ahtara.authentication.net

import social.ahtara.authentication.BuildConfig

object ApiConfig {
    /**
     * Gateway root including `/api/v1`, no trailing slash. Set per build type from Gradle properties
     * `ahtara.apiBaseUrl` (debug) and `ahtara.apiBaseUrlRelease` (release); see client/gradle.properties.
     */
    val baseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/')
}
