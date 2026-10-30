package social.ahtara.authentication.data






internal class AccountDirectory(private val daos: List<UserDao>) {

    suspend fun byAnyId(key: String): UserEntity? = first { it.findByAnyId(key) }

    suspend fun byIdentifier(identifier: String): UserEntity? = first { it.findByIdentifier(identifier) }

    suspend fun byUsername(username: String): UserEntity? = first { it.findByUsername(username) }

    suspend fun byEmail(email: String): UserEntity? = first { it.findByEmail(email) }

    suspend fun byProvider(provider: String, providerId: String): UserEntity? =
        first { it.findByProvider(provider, providerId) }

    private suspend fun first(query: suspend (UserDao) -> UserEntity?): UserEntity? {
        for (dao in daos) query(dao)?.let { return it }
        return null
    }
}