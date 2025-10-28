package com.example.kotlin.scope_test

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/**
 * also - 객체에 대한 부수 효과(side-effect) 작업에 특화된 함수
 *
 * 언제 사용하는가?
 * 1. 객체의 속성을 변경하지 않고 추가 작업(로깅, 검증 등)을 수행할 때
 * 2. 객체를 컬렉션에 추가하면서 동시에 다른 작업을 할 때
 * 3. 디버깅을 위해 중간 값을 출력할 때
 * 4. "이것을 하고 또한(also) 저것도 해라"는 의미
 *
 * 특징:
 * - 컨텍스트 객체: it (명시적으로 이름 변경 가능)
 * - 반환값: 컨텍스트 객체 자체
 * - 주 용도: 로깅, 검증, 디버깅 등의 부수 효과
 * - apply와 유사하지만 it을 사용 (더 명시적)
 */
class AlsoTest {

    // 실무 예제용 도메인 클래스들
    data class User(
        var id: Long = 0,
        var username: String = "",
        var email: String = "",
        var createdAt: LocalDateTime = LocalDateTime.now(),
        var lastModifiedAt: LocalDateTime = LocalDateTime.now()
    )

    data class AuditLog(
        val timestamp: LocalDateTime,
        val action: String,
        val userId: Long?,
        val details: String
    )

    data class Event(
        val id: String,
        val type: String,
        val data: Map<String, Any>,
        val timestamp: LocalDateTime
    )

    data class CacheEntry<T>(
        val key: String,
        val value: T,
        val cachedAt: LocalDateTime,
        var hitCount: Int = 0
    )

    /**
     * 예제 1: 기본 사용 - 로깅
     */
    @Test
    fun `also - 기본 로깅`() {
        val logs = mutableListOf<String>()

        val user = User(
            id = 1,
            username = "testuser",
            email = "test@example.com"
        ).also {
            // also는 객체를 그대로 반환하면서 부수 작업 수행
            logs.add("User created: ${it.username}")
        }

        assertEquals("testuser", user.username)
        assertEquals(1, logs.size)
        assertTrue(logs[0].contains("testuser"))

        // 체이닝 가능
        val modifiedUser = user.also {
            logs.add("User accessed: ${it.username}")
        }.also {
            logs.add("User email: ${it.email}")
        }

        assertEquals(user, modifiedUser)  // 같은 객체
        assertEquals(3, logs.size)
    }

    /**
     * 예제 2: 컬렉션에 추가하면서 로깅
     * 실무에서 매우 자주 사용하는 패턴
     */
    @Test
    fun `also - 컬렉션에 추가하면서 로깅`() {
        val users = mutableListOf<User>()
        val logs = mutableListOf<String>()

        fun createUser(username: String, email: String): User {
            return User().apply {
                id = (users.size + 1).toLong()
                this.username = username
                this.email = email
            }.also { newUser ->
                // 사용자를 생성하고 리스트에 추가하면서 로깅
                users.add(newUser)
                logs.add("[${LocalDateTime.now()}] Created user: ${newUser.username} (ID: ${newUser.id})")
            }
        }

        val user1 = createUser("alice", "alice@example.com")
        val user2 = createUser("bob", "bob@example.com")
        val user3 = createUser("charlie", "charlie@example.com")

        assertEquals(3, users.size)
        assertEquals(3, logs.size)
        assertEquals("alice", user1.username)
        assertTrue(users.contains(user1))
        assertTrue(logs.any { it.contains("alice") })
    }

    /**
     * 예제 3: 감사 로그(Audit Log) 기록
     */
    @Test
    fun `also - 감사 로그 기록`() {
        val auditLogs = mutableListOf<AuditLog>()

        fun logAudit(action: String, userId: Long?, details: String) {
            auditLogs.add(
                AuditLog(
                    timestamp = LocalDateTime.now(),
                    action = action,
                    userId = userId,
                    details = details
                )
            )
        }

        // 사용자 생성 시 감사 로그 기록
        val newUser = User().apply {
            id = 100
            username = "admin"
            email = "admin@company.com"
        }.also {
            logAudit("USER_CREATED", it.id, "Created user: ${it.username}")
        }

        // 사용자 수정 시 감사 로그 기록
        val updatedUser = newUser.apply {
            email = "admin.new@company.com"
            lastModifiedAt = LocalDateTime.now()
        }.also {
            logAudit("USER_UPDATED", it.id, "Updated email for user: ${it.username}")
        }

        // 사용자 삭제 시뮬레이션
        updatedUser.also {
            logAudit("USER_DELETED", it.id, "Deleted user: ${it.username}")
        }

        assertEquals(3, auditLogs.size)
        assertEquals("USER_CREATED", auditLogs[0].action)
        assertEquals("USER_UPDATED", auditLogs[1].action)
        assertEquals("USER_DELETED", auditLogs[2].action)
        assertTrue(auditLogs.all { it.userId == 100L })
    }

    /**
     * 예제 4: 디버깅 - 중간 값 확인
     */
    @Test
    fun `also - 파이프라인에서 디버깅`() {
        val debugLogs = mutableListOf<String>()

        fun debug(message: String) {
            debugLogs.add("[DEBUG] $message")
        }

        val result = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            .also { debug("Initial list: $it") }
            .filter { it % 2 == 0 }
            .also { debug("After filter (even numbers): $it") }
            .map { it * 2 }
            .also { debug("After map (doubled): $it") }
            .sum()
            .also { debug("Final sum: $it") }

        assertEquals(60, result)  // (2+4+6+8+10) * 2 = 60
        assertEquals(4, debugLogs.size)

        assertTrue(debugLogs[0].contains("Initial list"))
        assertTrue(debugLogs[1].contains("even numbers"))
        assertTrue(debugLogs[2].contains("doubled"))
        assertTrue(debugLogs[3].contains("Final sum: 60"))
    }

    /**
     * 예제 5: 이벤트 발행
     * 객체를 생성하거나 수정할 때 이벤트 발행
     */
    @Test
    fun `also - 이벤트 발행 시스템`() {
        val eventBus = mutableListOf<Event>()

        fun publishEvent(type: String, data: Map<String, Any>) {
            eventBus.add(
                Event(
                    id = "evt-${System.nanoTime()}",
                    type = type,
                    data = data,
                    timestamp = LocalDateTime.now()
                )
            )
        }

        // 주문 생성 시 이벤트 발행
        data class Order(
            val id: Long,
            val userId: Long,
            val amount: BigDecimal,
            val status: String
        )

        val order = Order(
            id = 1001,
            userId = 500,
            amount = BigDecimal("150000"),
            status = "PENDING"
        ).also { newOrder ->
            publishEvent(
                "ORDER_CREATED",
                mapOf(
                    "orderId" to newOrder.id,
                    "userId" to newOrder.userId,
                    "amount" to newOrder.amount,
                    "status" to newOrder.status
                )
            )
        }

        // 주문 승인 시 이벤트 발행
        val confirmedOrder = order.copy(status = "CONFIRMED")
            .also { confirmed ->
                publishEvent(
                    "ORDER_CONFIRMED",
                    mapOf(
                        "orderId" to confirmed.id,
                        "userId" to confirmed.userId,
                        "previousStatus" to order.status,
                        "newStatus" to confirmed.status
                    )
                )
            }

        assertEquals(2, eventBus.size)
        assertEquals("ORDER_CREATED", eventBus[0].type)
        assertEquals("ORDER_CONFIRMED", eventBus[1].type)
        assertEquals(1001L, eventBus[0].data["orderId"])
    }

    /**
     * 예제 6: 검증 및 유효성 체크
     */
    @Test
    fun `also - 객체 검증`() {
        val validationErrors = mutableListOf<String>()

        fun validateUser(user: User) {
            if (user.username.length < 3) {
                validationErrors.add("Username must be at least 3 characters")
            }
            if (!user.email.contains("@")) {
                validationErrors.add("Email must contain @")
            }
            if (user.id <= 0) {
                validationErrors.add("ID must be positive")
            }
        }

        // 유효한 사용자
        val validUser = User(
            id = 1,
            username = "validuser",
            email = "valid@example.com"
        ).also { validateUser(it) }

        assertEquals(0, validationErrors.size)

        // 유효하지 않은 사용자
        validationErrors.clear()
        val invalidUser = User(
            id = -1,
            username = "ab",
            email = "invalid-email"
        ).also { validateUser(it) }

        assertEquals(3, validationErrors.size)
        assertTrue(validationErrors.any { it.contains("Username") })
        assertTrue(validationErrors.any { it.contains("Email") })
        assertTrue(validationErrors.any { it.contains("ID") })
    }

    /**
     * 예제 7: 캐시 시스템
     * 캐시에 저장하면서 통계 기록
     */
    @Test
    fun `also - 캐시 시스템 with 통계`() {
        val cache = ConcurrentHashMap<String, CacheEntry<User>>()
        val cacheStats = mutableMapOf(
            "hits" to 0,
            "misses" to 0,
            "puts" to 0
        )

        fun putCache(key: String, user: User): User {
            return user.also {
                cache[key] = CacheEntry(
                    key = key,
                    value = it,
                    cachedAt = LocalDateTime.now()
                )
                cacheStats["puts"] = cacheStats["puts"]!! + 1
            }
        }

        fun getCache(key: String): User? {
            return cache[key]?.also {
                it.hitCount++
                cacheStats["hits"] = cacheStats["hits"]!! + 1
            }?.value ?: run {
                cacheStats["misses"] = cacheStats["misses"]!! + 1
                null
            }
        }

        // 사용자를 캐시에 저장
        val user1 = putCache(
            "user:1",
            User(1, "cached_user_1", "user1@example.com")
        )
        val user2 = putCache(
            "user:2",
            User(2, "cached_user_2", "user2@example.com")
        )

        assertEquals(2, cacheStats["puts"])
        assertEquals(2, cache.size)

        // 캐시에서 조회 (hit)
        val retrieved1 = getCache("user:1")
        assertNotNull(retrieved1)
        assertEquals("cached_user_1", retrieved1?.username)
        assertEquals(1, cacheStats["hits"])

        // 캐시에서 조회 (miss)
        val retrieved3 = getCache("user:3")
        assertNull(retrieved3)
        assertEquals(1, cacheStats["misses"])

        // 동일 키를 여러 번 조회
        getCache("user:1")
        getCache("user:1")
        assertEquals(3, cacheStats["hits"])
        assertEquals(3, cache["user:1"]?.hitCount)
    }

    /**
     * 예제 8: 메트릭 수집
     */
    @Test
    fun `also - 성능 메트릭 수집`() {
        data class OperationMetric(
            val operationName: String,
            val startTime: Long,
            val endTime: Long,
            val durationMs: Long,
            val success: Boolean
        )

        val metrics = mutableListOf<OperationMetric>()

        fun <T> measureOperation(operationName: String, block: () -> T): T {
            val startTime = System.currentTimeMillis()
            var success = true
            val result = try {
                block()
            } catch (e: Exception) {
                success = false
                throw e
            } finally {
                val endTime = System.currentTimeMillis()
                metrics.add(
                    OperationMetric(
                        operationName = operationName,
                        startTime = startTime,
                        endTime = endTime,
                        durationMs = endTime - startTime,
                        success = success
                    )
                )
            }
            return result
        }

        // 여러 작업 측정
        val user1 = measureOperation("createUser") {
            User(1, "user1", "user1@example.com")
                .also { Thread.sleep(10) }  // 작업 시뮬레이션
        }

        val user2 = measureOperation("createUser") {
            User(2, "user2", "user2@example.com")
                .also { Thread.sleep(20) }
        }

        val processedUsers = measureOperation("processUsers") {
            listOf(user1, user2)
                .map { it.username.uppercase() }
                .also { Thread.sleep(15) }
        }

        assertEquals(3, metrics.size)
        assertTrue(metrics.all { it.success })
        assertTrue(metrics[0].durationMs >= 10)
        assertTrue(metrics[1].durationMs >= 20)
        assertEquals("createUser", metrics[0].operationName)
        assertEquals("processUsers", metrics[2].operationName)
    }

    /**
     * 예제 9: 알림 발송
     */
    @Test
    fun `also - 알림 발송 시스템`() {
        data class Notification(
            val recipient: String,
            val message: String,
            val channel: String,
            val sentAt: LocalDateTime
        )

        val sentNotifications = mutableListOf<Notification>()

        fun sendNotification(recipient: String, message: String, channel: String) {
            sentNotifications.add(
                Notification(
                    recipient = recipient,
                    message = message,
                    channel = channel,
                    sentAt = LocalDateTime.now()
                )
            )
        }

        // 사용자 생성 시 환영 메일 발송
        val newUser = User().apply {
            id = 1
            username = "newbie"
            email = "newbie@example.com"
        }.also { user ->
            sendNotification(
                recipient = user.email,
                message = "환영합니다, ${user.username}님!",
                channel = "EMAIL"
            )
            sendNotification(
                recipient = user.email,
                message = "회원가입이 완료되었습니다.",
                channel = "SMS"
            )
        }

        assertEquals(2, sentNotifications.size)
        assertEquals("EMAIL", sentNotifications[0].channel)
        assertEquals("SMS", sentNotifications[1].channel)
        assertTrue(sentNotifications.all { it.recipient == "newbie@example.com" })

        // 비밀번호 변경 시 알림
        sentNotifications.clear()
        newUser.also { user ->
            sendNotification(
                recipient = user.email,
                message = "비밀번호가 변경되었습니다.",
                channel = "EMAIL"
            )
            sendNotification(
                recipient = user.email,
                message = "[보안] 비밀번호 변경 알림",
                channel = "PUSH"
            )
        }

        assertEquals(2, sentNotifications.size)
        assertTrue(sentNotifications.any { it.channel == "EMAIL" })
        assertTrue(sentNotifications.any { it.channel == "PUSH" })
    }

    /**
     * 예제 10: 실무 시나리오 - 복잡한 주문 처리 파이프라인
     */
    @Test
    fun `also - 실무 시나리오 주문 처리 파이프라인`() {
        data class Order(
            val id: Long,
            val userId: Long,
            var status: String,
            val amount: BigDecimal,
            val items: List<String>
        )

        data class ProcessLog(
            val timestamp: LocalDateTime,
            val orderId: Long,
            val stage: String,
            val message: String
        )

        val processLogs = mutableListOf<ProcessLog>()
        val notifications = mutableListOf<String>()
        val analytics = mutableMapOf<String, Int>()

        fun logProcess(orderId: Long, stage: String, message: String) {
            processLogs.add(
                ProcessLog(
                    timestamp = LocalDateTime.now(),
                    orderId = orderId,
                    stage = stage,
                    message = message
                )
            )
        }

        fun sendNotification(message: String) {
            notifications.add(message)
        }

        fun trackAnalytics(event: String) {
            analytics[event] = analytics.getOrDefault(event, 0) + 1
        }

        fun processOrder(order: Order): Order {
            return order
                // 1단계: 주문 검증
                .also {
                    logProcess(it.id, "VALIDATION", "Validating order ${it.id}")
                    trackAnalytics("order_validation_started")
                }
                // 2단계: 재고 확인
                .also {
                    logProcess(it.id, "INVENTORY_CHECK", "Checking inventory for ${it.items.size} items")
                    trackAnalytics("inventory_check")
                }
                // 3단계: 결제 처리
                .also {
                    logProcess(it.id, "PAYMENT", "Processing payment of ${it.amount}")
                    it.status = "PAYMENT_COMPLETED"
                    trackAnalytics("payment_completed")
                }
                // 4단계: 배송 준비
                .also {
                    logProcess(it.id, "SHIPPING", "Preparing shipment for order ${it.id}")
                    it.status = "SHIPPING_PREPARED"
                    trackAnalytics("shipping_prepared")
                }
                // 5단계: 알림 발송
                .also {
                    sendNotification("Order ${it.id} has been confirmed and is being prepared for shipping")
                    logProcess(it.id, "NOTIFICATION", "Sent confirmation notification")
                    trackAnalytics("notification_sent")
                }
                // 6단계: 주문 완료
                .also {
                    it.status = "PROCESSING"
                    logProcess(it.id, "COMPLETED", "Order ${it.id} processing completed")
                    trackAnalytics("order_processing_completed")
                }
        }

        // 주문 처리 실행
        val order = Order(
            id = 12345,
            userId = 100,
            status = "PENDING",
            amount = BigDecimal("250000"),
            items = listOf("노트북", "마우스", "키보드")
        )

        val processedOrder = processOrder(order)

        // 검증
        assertEquals("PROCESSING", processedOrder.status)

        // 6개의 단계가 모두 로깅되었는지 확인
        assertEquals(6, processLogs.size)
        assertEquals("VALIDATION", processLogs[0].stage)
        assertEquals("INVENTORY_CHECK", processLogs[1].stage)
        assertEquals("PAYMENT", processLogs[2].stage)
        assertEquals("SHIPPING", processLogs[3].stage)
        assertEquals("NOTIFICATION", processLogs[4].stage)
        assertEquals("COMPLETED", processLogs[5].stage)

        // 알림이 발송되었는지 확인
        assertEquals(1, notifications.size)
        assertTrue(notifications[0].contains("12345"))

        // 분석 이벤트가 기록되었는지 확인
        assertEquals(6, analytics.size)
        assertEquals(1, analytics["order_validation_started"])
        assertEquals(1, analytics["payment_completed"])
        assertEquals(1, analytics["notification_sent"])

        // 처리 로그 출력 (실제로는 로깅 시스템으로 전송)
        println("\n=== 주문 처리 로그 ===")
        processLogs.forEach { log ->
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
            println("[${log.timestamp.format(formatter)}] [${log.stage}] Order ${log.orderId}: ${log.message}")
        }
    }

    /**
     * 예제 11: 파일 업로드 시뮬레이션
     */
    @Test
    fun `also - 파일 업로드 진행 상황 추적`() {
        data class FileUpload(
            val fileName: String,
            val fileSize: Long,
            var uploadedBytes: Long = 0,
            var status: String = "PENDING"
        )

        val uploadLogs = mutableListOf<String>()
        val uploadStats = mutableMapOf<String, Any>()

        fun logUpload(message: String) {
            uploadLogs.add("[${LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}] $message")
        }

        fun uploadFile(file: FileUpload): FileUpload {
            return file
                .also {
                    logUpload("Starting upload: ${it.fileName} (${it.fileSize} bytes)")
                    it.status = "UPLOADING"
                }
                .also {
                    // 업로드 진행 시뮬레이션 (25%)
                    it.uploadedBytes = it.fileSize / 4
                    logUpload("Progress: ${it.fileName} - 25% (${it.uploadedBytes}/${it.fileSize} bytes)")
                }
                .also {
                    // 50%
                    it.uploadedBytes = it.fileSize / 2
                    logUpload("Progress: ${it.fileName} - 50% (${it.uploadedBytes}/${it.fileSize} bytes)")
                }
                .also {
                    // 75%
                    it.uploadedBytes = it.fileSize * 3 / 4
                    logUpload("Progress: ${it.fileName} - 75% (${it.uploadedBytes}/${it.fileSize} bytes)")
                }
                .also {
                    // 100% 완료
                    it.uploadedBytes = it.fileSize
                    it.status = "COMPLETED"
                    logUpload("Upload completed: ${it.fileName}")
                    uploadStats[it.fileName] = mapOf(
                        "size" to it.fileSize,
                        "status" to it.status,
                        "completedAt" to LocalDateTime.now()
                    )
                }
        }

        val file = FileUpload(
            fileName = "document.pdf",
            fileSize = 1024 * 1024  // 1MB
        )

        val uploaded = uploadFile(file)

        assertEquals("COMPLETED", uploaded.status)
        assertEquals(uploaded.fileSize, uploaded.uploadedBytes)
        assertEquals(5, uploadLogs.size)  // 시작 + 25% + 50% + 75% + 완료
        assertTrue(uploadStats.containsKey("document.pdf"))
    }

    /**
     * 예제 12: A/B 테스트 추적
     */
    @Test
    fun `also - AB 테스트 추적`() {
        data class Experiment(
            val experimentId: String,
            val variant: String,
            val userId: Long
        )

        val experimentData = mutableListOf<Experiment>()
        val variantCounts = mutableMapOf<String, Int>()

        fun assignVariant(userId: Long, experimentId: String): String {
            // 간단한 변형 할당 로직 (실제로는 더 복잡)
            return if (userId % 2 == 0L) "A" else "B"
        }

        fun trackExperiment(userId: Long, experimentId: String, variant: String) {
            experimentData.add(Experiment(experimentId, variant, userId))
            variantCounts[variant] = variantCounts.getOrDefault(variant, 0) + 1
        }

        // 여러 사용자에 대한 실험 할당
        val users = (1L..100L).map { userId ->
            User(userId, "user$userId", "user$userId@example.com")
                .also { user ->
                    val variant = assignVariant(user.id, "homepage_redesign")
                    trackExperiment(user.id, "homepage_redesign", variant)
                }
        }

        assertEquals(100, experimentData.size)
        assertEquals(50, variantCounts["A"])  // 짝수 ID
        assertEquals(50, variantCounts["B"])  // 홀수 ID
        assertTrue(experimentData.all { it.experimentId == "homepage_redesign" })
    }

    /**
     * 예제 13: return object.also { } 패턴의 의도
     *
     * 핵심 개념:
     * return order.also { } 는 다음을 의미합니다:
     * 1. order 객체를 반환하되
     * 2. 반환하기 전에 부수 작업(로깅, 이벤트 발행 등)을 수행
     * 3. 객체 자체는 변경할 수도, 안 할 수도 있음
     *
     * 왜 이렇게 사용하는가?
     * - "이 객체를 돌려주는데, 돌려주기 전에 이것도 해줘"
     * - 주로 로깅, 감사, 알림, 메트릭 수집 등의 관찰(observability) 목적
     * - 함수의 주요 로직과 부수 효과를 명확히 분리
     */
    @Test
    fun `also - return object_also 패턴의 의도 이해하기`() {
        val logs = mutableListOf<String>()

        // ============================================
        // 예시 1: also 없이 사용 (전통적인 방법)
        // ============================================
        fun createUserWithoutAlso(username: String): User {
            val user = User().apply {
                id = 1
                this.username = username
                email = "$username@example.com"
            }

            // 로깅 - 별도의 라인
            logs.add("User created: ${user.username}")

            // 반환 - 별도의 라인
            return user
        }

        // ============================================
        // 예시 2: also를 사용 (더 선언적)
        // ============================================
        fun createUserWithAlso(username: String): User {
            return User().apply {
                id = 1
                this.username = username
                email = "$username@example.com"
            }.also {
                // "이 객체를 반환하되, 반환하기 전에 로그도 남겨라"
                logs.add("User created: ${it.username}")
            }
        }

        logs.clear()
        val user1 = createUserWithoutAlso("alice")
        assertEquals(1, logs.size)

        logs.clear()
        val user2 = createUserWithAlso("bob")
        assertEquals(1, logs.size)

        // 두 방식 모두 동일한 결과, also가 더 선언적이고 의도가 명확
    }

    /**
     * 예제 14: 실무에서 return also를 사용하는 이유
     */
    @Test
    fun `also - 실무에서 return also 패턴이 유용한 경우들`() {
        val auditLogs = mutableListOf<String>()
        val eventBus = mutableListOf<String>()
        val metrics = mutableMapOf<String, Int>()

        data class Document(
            var id: Long = 0,
            var title: String = "",
            var content: String = "",
            var status: String = "DRAFT"
        )

        // ============================================
        // 케이스 1: 생성 + 감사 로그
        // ============================================
        fun createDocument(title: String, content: String): Document {
            return Document().apply {
                id = System.currentTimeMillis()
                this.title = title
                this.content = content
            }.also { doc ->
                // 문서를 반환하되, 반환하기 전에 감사 로그 기록
                auditLogs.add("Document created: ID=${doc.id}, Title=${doc.title}")
            }
        }

        val doc1 = createDocument("보고서", "내용입니다")
        assertEquals(1, auditLogs.size)
        assertTrue(auditLogs[0].contains("보고서"))

        // ============================================
        // 케이스 2: 수정 + 이벤트 발행
        // ============================================
        fun publishDocument(document: Document): Document {
            return document.apply {
                status = "PUBLISHED"
            }.also { doc ->
                // 문서를 반환하되, 반환하기 전에 이벤트 발행
                eventBus.add("DOCUMENT_PUBLISHED: ${doc.id}")
                auditLogs.add("Document published: ID=${doc.id}")
            }
        }

        val published = publishDocument(doc1)
        assertEquals("PUBLISHED", published.status)
        assertEquals(1, eventBus.size)

        // ============================================
        // 케이스 3: 조회 + 메트릭 수집
        // ============================================
        fun getDocument(id: Long): Document? {
            // DB 조회 시뮬레이션
            val found = if (id == doc1.id) doc1 else null

            return found?.also { doc ->
                // 문서를 반환하되, 반환하기 전에 조회 메트릭 기록
                val key = "document_accessed_${doc.id}"
                metrics[key] = metrics.getOrDefault(key, 0) + 1
            }
        }

        val retrieved1 = getDocument(doc1.id)
        assertNotNull(retrieved1)
        assertEquals(1, metrics.size)

        val retrieved2 = getDocument(doc1.id)
        assertEquals(2, metrics.values.first())  // 2번 조회됨

        val notFound = getDocument(999L)
        assertNull(notFound)
        assertEquals(2, metrics.values.first())  // null이므로 also 블록 실행 안됨
    }

    /**
     * 예제 15: return also 패턴 - 체이닝의 힘
     */
    @Test
    fun `also - 체이닝으로 여러 부수 효과 수행`() {
        data class Order(
            var id: Long = 0,
            var userId: Long = 0,
            var status: String = "PENDING",
            var amount: BigDecimal = BigDecimal.ZERO
        )

        val logs = mutableListOf<String>()
        val notifications = mutableListOf<String>()
        val analytics = mutableListOf<String>()

        fun createOrder(userId: Long, amount: BigDecimal): Order {
            return Order().apply {
                id = System.currentTimeMillis()
                this.userId = userId
                this.amount = amount
            }
                // 첫 번째 also: 로그 기록
                .also { order ->
                    logs.add("Order created: ID=${order.id}, Amount=${order.amount}")
                }
                // 두 번째 also: 알림 발송
                .also { order ->
                    notifications.add("User ${order.userId}: Your order ${order.id} has been created")
                }
                // 세 번째 also: 분석 데이터 수집
                .also { order ->
                    analytics.add("order_created|user=${order.userId}|amount=${order.amount}")
                }
            // 결과: 로그도 남고, 알림도 가고, 분석도 되고, Order도 반환됨!
        }

        val order = createOrder(100, BigDecimal("50000"))

        assertEquals(1, logs.size)
        assertEquals(1, notifications.size)
        assertEquals(1, analytics.size)
        assertNotNull(order)
        assertEquals(BigDecimal("50000"), order.amount)

        println("=== 체이닝의 결과 ===")
        println("반환된 객체: $order")
        println("로그: ${logs[0]}")
        println("알림: ${notifications[0]}")
        println("분석: ${analytics[0]}")
    }

    /**
     * 예제 16: return also vs return apply 비교
     */
    @Test
    fun `also - return also와 return apply의 차이점`() {
        data class Product(
            var id: Long = 0,
            var name: String = "",
            var price: BigDecimal = BigDecimal.ZERO
        )

        val logs = mutableListOf<String>()

        // ============================================
        // apply 사용: 객체 설정에 집중
        // ============================================
        fun createProductWithApply(name: String, price: BigDecimal): Product {
            return Product().apply {
                id = 1
                this.name = name
                this.price = price
                // apply 안에서는 주로 객체의 속성 설정
                // this로 접근하므로 객체의 멤버처럼 느껴짐
            }
        }

        // ============================================
        // also 사용: 부수 효과에 집중
        // ============================================
        fun createProductWithAlso(name: String, price: BigDecimal): Product {
            return Product().apply {
                id = 1
                this.name = name
                this.price = price
            }.also { product ->
                // also는 객체 설정 후 추가 작업 수행
                // it (또는 명시적 이름)으로 접근하므로 외부 작업처럼 느껴짐
                logs.add("Product created: ${product.name} - ${product.price}원")
            }
        }

        // ============================================
        // 혼합 사용: 가장 일반적인 실무 패턴
        // ============================================
        fun createProductBest(name: String, price: BigDecimal): Product {
            return Product()
                .apply {
                    // apply: 객체 초기화
                    id = 1
                    this.name = name
                    this.price = price
                }
                .also {
                    // also: 부수 효과
                    logs.add("Created: ${it.name}")
                }
        }

        logs.clear()
        val p1 = createProductWithApply("상품A", BigDecimal("10000"))
        assertEquals(0, logs.size)  // 로그 없음

        logs.clear()
        val p2 = createProductWithAlso("상품B", BigDecimal("20000"))
        assertEquals(1, logs.size)  // 로그 있음

        logs.clear()
        val p3 = createProductBest("상품C", BigDecimal("30000"))
        assertEquals(1, logs.size)  // 로그 있음
    }

    /**
     * 예제 17: 실무 시나리오 - 왜 return also를 선호하는가?
     */
    @Test
    fun `also - return also 패턴을 선호하는 실무적 이유`() {
        data class Payment(
            var id: String = "",
            var amount: BigDecimal = BigDecimal.ZERO,
            var status: String = "PENDING"
        )

        val auditTrail = mutableListOf<String>()
        val notifications = mutableListOf<String>()

        // ============================================
        // 나쁜 예: 모든 것을 apply 안에
        // ============================================
        fun processPaymentBad(amount: BigDecimal): Payment {
            return Payment().apply {
                id = "PAY-${System.currentTimeMillis()}"
                this.amount = amount
                status = "COMPLETED"

                // apply 안에서 부수 효과도 수행 - 혼란스러움!
                auditTrail.add("Payment processed: $id")
                notifications.add("Payment completed: ${this.amount}")
            }
            // 문제: apply는 객체 설정용인데 부수 효과까지 하니까 의도가 불명확
        }

        // ============================================
        // 좋은 예: apply는 설정, also는 부수 효과
        // ============================================
        fun processPaymentGood(amount: BigDecimal): Payment {
            return Payment()
                .apply {
                    // 명확: 여기는 객체 설정만!
                    id = "PAY-${System.currentTimeMillis()}"
                    this.amount = amount
                    status = "COMPLETED"
                }
                .also { payment ->
                    // 명확: 여기는 부수 효과만!
                    auditTrail.add("Payment processed: ${payment.id}")
                    notifications.add("Payment completed: ${payment.amount}")
                }
            // 장점: 코드를 읽는 사람이 의도를 바로 이해 가능
            // - apply 블록: "아, 객체 초기화하는구나"
            // - also 블록: "아, 로깅하는구나"
        }

        auditTrail.clear()
        notifications.clear()

        val payment1 = processPaymentBad(BigDecimal("10000"))
        assertEquals(1, auditTrail.size)

        auditTrail.clear()
        notifications.clear()

        val payment2 = processPaymentGood(BigDecimal("20000"))
        assertEquals(1, auditTrail.size)
        assertEquals(1, notifications.size)

        // 결론:
        // return object.also { } 패턴은
        // "객체는 반환하되, 반환하기 전에 관찰/기록/알림 등의 부수 효과를 수행"
        // 하는 의도를 명확하게 표현하는 코틀린의 관용구(idiom)입니다.
    }
}
