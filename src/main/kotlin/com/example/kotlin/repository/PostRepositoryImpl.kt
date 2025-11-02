package com.example.kotlin.repository

import com.example.kotlin.entity.Post
import com.example.kotlin.entity.QPost
import com.example.kotlin.entity.QUser
import com.example.kotlin.entity.QCategory
import com.example.kotlin.entity.QTag
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class PostRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : PostRepositoryCustom {

    private val post = QPost.post
    private val user = QUser.user
    private val category = QCategory.category
    private val tag = QTag.tag

    override fun findPostsByAuthorUsername(username: String): List<Post> {
        return queryFactory
            .selectFrom(post)
            .join(post.author, user).fetchJoin()
            .where(user.username.eq(username))
            .orderBy(post.createdAt.desc())
            .fetch()
    }

    override fun findPostsByCategoryName(categoryName: String): List<Post> {
        return queryFactory
            .selectFrom(post)
            .join(post.category, category).fetchJoin()
            .where(category.name.eq(categoryName))
            .orderBy(post.createdAt.desc())
            .fetch()
    }

    override fun findPostsByTagName(tagName: String): List<Post> {
        return queryFactory
            .selectFrom(post)
            .join(post.tags, tag)
            .where(tag.name.eq(tagName))
            .distinct()
            .orderBy(post.createdAt.desc())
            .fetch()
    }

    override fun searchPostsByKeyword(keyword: String): List<Post> {
        return queryFactory
            .selectFrom(post)
            .where(
                post.title.containsIgnoreCase(keyword)
                    .or(post.content.containsIgnoreCase(keyword))
            )
            .orderBy(post.createdAt.desc())
            .fetch()
    }
}
