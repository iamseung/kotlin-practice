package com.example.kotlin.config.datasource

import org.slf4j.LoggerFactory
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * 트랜잭션 컨텍스트에 따라 동적으로 DataSource를 라우팅하는 클래스
 *
 * Spring의 AbstractRoutingDataSource를 확장하여,
 * 현재 트랜잭션이 읽기 전용인지 여부에 따라 Master 또는 Slave DataSource를 선택합니다.
 *
 * ## 동작 원리
 * 1. @Transactional(readOnly = true) → SLAVE DataSource 선택
 * 2. @Transactional 또는 @Transactional(readOnly = false) → MASTER DataSource 선택
 * 3. 트랜잭션이 없는 경우 → MASTER DataSource 선택 (기본값)
 *
 * ## 사용 예시
 * ```kotlin
 * @Service
 * class UserService(private val userRepository: UserRepository) {
 *
 *     @Transactional(readOnly = true)  // ← SLAVE 사용
 *     fun findUser(id: Long) = userRepository.findById(id)
 *
 *     @Transactional  // ← MASTER 사용
 *     fun createUser(user: User) = userRepository.save(user)
 * }
 * ```
 *
 * @see AbstractRoutingDataSource
 * @see DataSourceType
 */
class RoutingDataSource : AbstractRoutingDataSource() {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * 현재 트랜잭션 컨텍스트를 기반으로 사용할 DataSource의 키를 결정합니다.
     *
     * @return DataSourceType.MASTER 또는 DataSourceType.SLAVE
     */
    override fun determineCurrentLookupKey(): Any {
        // 현재 트랜잭션이 읽기 전용인지 확인
        val isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly()

        val dataSourceType = if (isReadOnly) {
            DataSourceType.SLAVE
        } else {
            DataSourceType.MASTER
        }

        if (log.isDebugEnabled) {
            log.debug(
                "DataSource routing - readOnly: {}, selected: {}, transactionActive: {}",
                isReadOnly,
                dataSourceType,
                TransactionSynchronizationManager.isActualTransactionActive()
            )
        }

        return dataSourceType
    }

    /**
     * 트랜잭션이 활성화되어 있는지 확인하는 유틸리티 메서드
     */
    fun isTransactionActive(): Boolean {
        return TransactionSynchronizationManager.isActualTransactionActive()
    }

    /**
     * 현재 선택된 DataSource 타입을 반환하는 유틸리티 메서드
     */
    fun getCurrentDataSourceType(): DataSourceType {
        return determineCurrentLookupKey() as DataSourceType
    }
}
