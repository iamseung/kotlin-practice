package com.example.kotlin.repository

import com.example.kotlin.entity.Post
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostRepository : JpaRepository<Post, Long>, PostRepositoryCustom {
    fun findByTitleContaining(title: String): List<Post>
}
