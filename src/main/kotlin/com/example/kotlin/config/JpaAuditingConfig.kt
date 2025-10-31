package com.example.kotlin.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/**
 * JPA Auditing 설정
 *
 * @CreatedDate, @LastModifiedDate 어노테이션이 동작하도록 설정
 */
@Configuration
@EnableJpaAuditing
class JpaAuditingConfig
