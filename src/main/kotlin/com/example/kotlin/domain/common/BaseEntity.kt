package com.example.kotlin.domain.common

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * JPA Auditing을 적용한 Base Entity
 *
 * 모든 엔티티에서 공통으로 사용하는 필드를 정의
 * - createdAt: 생성 시간 (자동 설정)
 * - updatedAt: 수정 시간 (자동 업데이트)
 * - isActive: 활성화 상태
 * - isDeleted: 소프트 삭제 플래그
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity(
    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime? = null,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val isDeleted: Boolean = false
)
