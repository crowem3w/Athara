package soc.athara.app.core.identityapi


data class PersonalSetup(
    
    val photoUri: String? = null,
    
    val coverUri: String? = null,
    val about: String = "",
    val location: String = "",
)