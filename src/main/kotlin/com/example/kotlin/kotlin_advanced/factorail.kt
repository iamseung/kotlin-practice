package com.example.kotlin.kotlin_advanced

fun factorialV1(n: Int): Int {
    return if (n <= 1) {
        1
    } else {
        n * factorialV1(n - 1)
    }
}

fun factorialV2(n: Int, cur: Int = 1): Int {
    return if (n <= 1) {
        cur
    } else {
        // n * factorialV1(n - 1) 이러한 연산없이 factorialV2만 바로 호출
        factorialV2(n - 1, n * cur)
    }
}

fun main() {
    val key = Key("키")
    println(key)

    val inlineUserId = Id<User>(1L)
    val inlineBookId = Id<Book>(1L)
    handleV2(inlineUserId, inlineBookId)
}

@JvmInline
value class Key(val name: String)

@JvmInline
value class Id<T>(val id: Long)

class User(
    val id: Id<Long>,
    val name: String,
)

class Book(
    val id: Id<Long>,
    val author: String,
)

fun handle(userId: Long, bookId: Long) {

}

fun handleV2(userId: Id<User>, bookId: Id<Book>) {

}

@JvmInline
value class Number(val num: Long) {
    init {
        require(num in 1 .. 10)
    }
}