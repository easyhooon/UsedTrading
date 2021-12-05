package kr.ac.konkuk.usedtrading.chatdetail

data class ChatItem (
    val senderId: String,
    val message: String
) {
    constructor(): this("","")
}