package ray.mintcat.shengyiwu

import kotlinx.serialization.Serializable

@Serializable
data class Root(
    val hitokoto: String,
    val level: Long,
    val mainItem: PMainItem,
    val name: String,
    val star: Long,
    val subItem: List<PSubItem>,
)
@Serializable
data class PMainItem(
    val name: String,
    val type: String,
    val value: String,
)
@Serializable
data class PSubItem(
    val name: String,
    val type: String,
    val value: String,
)
