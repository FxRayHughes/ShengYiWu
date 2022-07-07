package ray.mintcat.shengyiwu

import kotlinx.serialization.Serializable

@Serializable
data class Datas(
    var hitokoto: String = "null",
    val level: Long,
    val main_item: MainItem,
    val name: String,
    val star: Long,
    val sub_item: List<SubItem>,
)
@Serializable
data class MainItem(
    val name: String,
    val type: String,
    val value: String,
)
@Serializable
data class SubItem(
    val name: String,
    val type: String,
    val value: String,
)
