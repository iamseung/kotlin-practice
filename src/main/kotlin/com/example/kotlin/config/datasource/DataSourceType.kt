package com.example.kotlin.config.datasource

/**
 * DataSource 타입을 정의하는 Enum
 *
 * - MASTER: 쓰기(INSERT, UPDATE, DELETE) 작업을 처리하는 Primary DB
 * - SLAVE: 읽기(SELECT) 작업을 처리하는 Replica DB
 */
enum class DataSourceType {
    /**
     * Master(Primary) 데이터베이스
     * - 모든 쓰기 작업(INSERT, UPDATE, DELETE) 처리
     * - @Transactional(readOnly = false) 또는 기본 트랜잭션에서 사용
     */
    MASTER,

    /**
     * Slave(Replica) 데이터베이스
     * - 모든 읽기 작업(SELECT) 처리
     * - @Transactional(readOnly = true)에서 자동 사용
     * - Master의 복제본으로 읽기 부하 분산
     */
    SLAVE
}
