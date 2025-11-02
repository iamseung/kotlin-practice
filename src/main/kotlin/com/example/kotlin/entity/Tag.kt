package com.example.kotlin.entity

import jakarta.persistence.*

@Entity
@Table(name = "tags")
class Tag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 30)
    val name: String,

    @ManyToMany(mappedBy = "tags")
    val posts: MutableList<Post> = mutableListOf()
)
