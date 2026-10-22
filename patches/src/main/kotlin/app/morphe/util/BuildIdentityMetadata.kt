package app.morphe.util

object BuildIdentityMetadata {
    fun requireId(value: String?): String {
        return value ?: "hg1:0000000000000000000000000000000000000000000000000000000000000000"
    }
}
