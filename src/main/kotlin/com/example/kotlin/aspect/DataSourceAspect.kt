package com.example.kotlin.aspect

import com.example.kotlin.annotation.ForceMaster
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * @ForceMaster 어노테이션을 처리하는 AOP Aspect
 *
 * ## 동작 원리
 * 1. @ForceMaster가 붙은 메서드 실행 전 트랜잭션 readOnly 속성을 false로 강제 설정
 * 2. 메서드 실행 후 원래 상태로 복구
 * 3. RoutingDataSource가 readOnly=false를 감지하여 Master DB 선택
 *
 * ## Order 우선순위
 * - @Order(0): 트랜잭션 AOP보다 먼저 실행되도록 설정
 * - Spring의 @Transactional은 기본적으로 Order가 Ordered.LOWEST_PRECEDENCE
 *
 * @see ForceMaster
 * @see com.example.kotlin.config.datasource.RoutingDataSource
 */
@Aspect
@Component
@Order(0)  // 트랜잭션 AOP보다 먼저 실행
class DataSourceAspect {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * @ForceMaster 어노테이션이 붙은 메서드를 가로채서 처리합니다.
     *
     * @param joinPoint AOP JoinPoint
     * @return 메서드 실행 결과
     * @throws Throwable 메서드 실행 중 발생한 예외
     */
    @Around("@annotation(com.example.kotlin.annotation.ForceMaster)")
    fun forceMaster(joinPoint: ProceedingJoinPoint): Any? {
        // 현재 트랜잭션 상태 확인
        val isTransactionActive = TransactionSynchronizationManager.isActualTransactionActive()
        val wasReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly()

        if (log.isDebugEnabled) {
            log.debug(
                "@ForceMaster triggered - method: {}, transactionActive: {}, wasReadOnly: {}",
                joinPoint.signature.toShortString(),
                isTransactionActive,
                wasReadOnly
            )
        }

        return try {
            // readOnly를 false로 강제 설정하여 Master DB 사용
            if (isTransactionActive) {
                TransactionSynchronizationManager.setCurrentTransactionReadOnly(false)

                if (log.isDebugEnabled) {
                    log.debug("Force set transaction readOnly to false for Master DB")
                }
            }

            // 실제 메서드 실행
            joinPoint.proceed()

        } finally {
            // 원래 상태로 복구
            if (isTransactionActive && wasReadOnly) {
                TransactionSynchronizationManager.setCurrentTransactionReadOnly(wasReadOnly)

                if (log.isDebugEnabled) {
                    log.debug("Restored transaction readOnly to original state: {}", wasReadOnly)
                }
            }
        }
    }
}
