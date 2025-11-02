package com.example.kotlin.repository

import com.example.kotlin.entity.Post

interface PostRepositoryCustom {
    fun findPostsByAuthorUsername(username: String): List<Post>
    fun findPostsByCategoryName(categoryName: String): List<Post>
    fun findPostsByTagName(tagName: String): List<Post>
    fun searchPostsByKeyword(keyword: String): List<Post>
}
