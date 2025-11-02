package com.example.kotlin.annotation

/**
 * 강제로 Master DataSource를 사용하도록 지정하는 어노테이션
 *
 * ## 사용 목적
 * - Replication Lag 문제 해결: Master에 쓴 직후 바로 읽어야 하는 경우
 * - 일관성이 중요한 읽기 작업: 최신 데이터가 반드시 필요한 경우
 * - 쓰기 후 즉시 읽기 패턴 (Write-After-Read)
 *
 * ## 사용 예시
 * ```kotlin
 * @Service
 * class UserService(private val userRepository: UserRepository) {
 *
 *     @Transactional
 *     fun createUser(user: User): User {
 *         return userRepository.save(user)  // Master 사용
 *     }
 *
 *     // 일반적인 readOnly는 Slave를 사용하지만,
 *     // 방금 생성한 유저를 조회해야 하므로 Master를 강제 사용
 *     @ForceMaster
 *     @Transactional(readOnly = true)
 *     fun findUserImmediately(id: Long): User? {
 *         return userRepository.findById(id).orElse(null)  // Master 사용
 *     }
 * }
 * ```
 *
 * ## 주의사항
 * - 남용 시 Master DB 부하 증가
 * - 꼭 필요한 경우에만 사용
 * - 대부분의 경우 @Transactional(readOnly = true)로 Slave 사용 권장
 *
 * @see com.example.kotlin.aspect.DataSourceAspect
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ForceMaster
