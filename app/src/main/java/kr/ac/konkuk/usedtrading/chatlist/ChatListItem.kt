package kr.ac.konkuk.usedtrading.chatlist

data class ChatListItem(
    val buyerId: String,
    val sellerId: String,
    val itemTitle: String,
    val key: Long
){
    //파이어베이스 realtine database애서 사용하는 data class이기때문에 빈 생성자 필요
    constructor(): this("", "", "", 0)
}
