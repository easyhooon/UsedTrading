package kr.ac.konkuk.usedtrading.home

data class ArticleModel(
    val sellerId: String,
    val title: String,
    val createdAt: Long,
    val content: String,
    val imageUrl: String
) {

    //firebase realtime DB에서 그대로 MODEL 클래스를 사용하려면 빈 생성자가 필수로 있어야야 함
    constructor(): this("", "", 0, "", "")

}