package com.example.kotlin.scope_test

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * apply - 객체 초기화 및 설정에 특화된 함수
 *
 * 언제 사용하는가?
 * 1. 객체 생성 직후 여러 속성을 설정할 때
 * 2. Builder 패턴처럼 사용할 때
 * 3. 객체 설정 후 그 객체를 바로 사용하거나 반환해야 할 때
 *
 * 특징:
 * - 컨텍스트 객체: this (생략 가능)
 * - 반환값: 컨텍스트 객체 자체
 * - 주 용도: 객체 초기화 및 설정
 */
class ApplyTest {

    // 실무 예제용 도메인 클래스들
    data class User(
        var id: Long = 0,
        var username: String = "",
        var email: String = "",
        var firstName: String = "",
        var lastName: String = "",
        var age: Int = 0,
        var roles: MutableList<String> = mutableListOf(),
        var metadata: MutableMap<String, Any> = mutableMapOf(),
        var createdAt: LocalDateTime? = null,
        var isActive: Boolean = true
    )

    data class ApiRequest(
        var url: String = "",
        var method: String = "GET",
        var headers: MutableMap<String, String> = mutableMapOf(),
        var queryParams: MutableMap<String, String> = mutableMapOf(),
        var body: Any? = null,
        var timeout: Long = 30000,
        var retryCount: Int = 3
    )

    data class EmailBuilder(
        var from: String = "",
        var to: MutableList<String> = mutableListOf(),
        var cc: MutableList<String> = mutableListOf(),
        var bcc: MutableList<String> = mutableListOf(),
        var subject: String = "",
        var body: String = "",
        var attachments: MutableList<String> = mutableListOf(),
        var isHtml: Boolean = false,
        var priority: String = "NORMAL"
    )

    data class DatabaseConfig(
        var host: String = "localhost",
        var port: Int = 5432,
        var database: String = "",
        var username: String = "",
        var password: String = "",
        var maxPoolSize: Int = 10,
        var minPoolSize: Int = 2,
        var connectionTimeout: Long = 30000,
        var idleTimeout: Long = 600000,
        var properties: MutableMap<String, String> = mutableMapOf()
    )

    /**
     * 예제 1: 기본 사용 - 객체 초기화
     * 가장 간단한 apply 사용법
     */
    @Test
    fun `apply - 기본 객체 초기화`() {
        // apply 없이 사용
        val userWithout = User()
        userWithout.username = "johndoe"
        userWithout.email = "john@example.com"
        userWithout.age = 30

        // apply 사용 - 더 간결하고 읽기 쉬움
        val userWith = User().apply {
            username = "johndoe"
            email = "john@example.com"
            age = 30
        }

        assertEquals(userWithout.username, userWith.username)
        assertEquals(userWithout.email, userWith.email)
    }

    /**
     * 예제 2: 복잡한 객체 초기화
     * 실무에서 DTO나 Request 객체를 만들 때 자주 사용
     */
    @Test
    fun `apply - 복잡한 API 요청 객체 생성`() {
        // REST API 호출을 위한 복잡한 요청 객체 생성
        val apiRequest = ApiRequest().apply {
            url = "https://api.example.com/users"
            method = "POST"

            // 여러 헤더 설정
            headers.apply {
                put("Content-Type", "application/json")
                put("Authorization", "Bearer token123")
                put("X-API-Version", "v1")
                put("X-Request-ID", "req-${System.currentTimeMillis()}")
            }

            // 쿼리 파라미터 설정
            queryParams.apply {
                put("page", "1")
                put("size", "20")
                put("sort", "createdAt,desc")
            }

            // 요청 바디 설정
            body = mapOf(
                "username" to "newuser",
                "email" to "newuser@example.com"
            )

            timeout = 60000  // 1분
            retryCount = 5
        }

        assertEquals("POST", apiRequest.method)
        assertEquals(4, apiRequest.headers.size)
        assertEquals(3, apiRequest.queryParams.size)
        assertEquals("Bearer token123", apiRequest.headers["Authorization"])
    }

    /**
     * 예제 3: 이메일 빌더 패턴
     * apply를 사용한 Fluent API 스타일의 객체 생성
     */
    @Test
    fun `apply - 이메일 빌더 패턴 구현`() {
        // 복잡한 이메일 객체를 apply로 깔끔하게 생성
        val email = EmailBuilder().apply {
            from = "admin@company.com"

            to.apply {
                add("user1@example.com")
                add("user2@example.com")
                add("user3@example.com")
            }

            cc.apply {
                add("manager@company.com")
                add("team-lead@company.com")
            }

            subject = "[긴급] 시스템 점검 안내"

            body = """
                안녕하세요,

                다음과 같이 시스템 점검이 예정되어 있습니다.
                - 일시: 2025-10-25 02:00 ~ 04:00
                - 대상: 전체 시스템

                점검 시간 동안 서비스 이용이 제한될 수 있습니다.
                양해 부탁드립니다.
            """.trimIndent()

            isHtml = false
            priority = "HIGH"

            attachments.apply {
                add("/files/maintenance-schedule.pdf")
                add("/files/backup-plan.pdf")
            }
        }

        assertEquals(3, email.to.size)
        assertEquals(2, email.cc.size)
        assertEquals("HIGH", email.priority)
        assertEquals(2, email.attachments.size)
    }

    /**
     * 예제 4: 데이터베이스 설정 객체
     * 실무에서 설정(Configuration) 객체를 만들 때 유용
     */
    @Test
    fun `apply - 데이터베이스 설정 객체 생성`() {
        val dbConfig = DatabaseConfig().apply {
            host = "prod-db.example.com"
            port = 5432
            database = "production_db"
            username = "app_user"
            password = "encrypted_password"

            // 커넥션 풀 설정
            maxPoolSize = 50
            minPoolSize = 10
            connectionTimeout = 20000
            idleTimeout = 300000

            // 추가 속성 설정
            properties.apply {
                put("ssl", "true")
                put("sslmode", "require")
                put("characterEncoding", "UTF-8")
                put("useUnicode", "true")
                put("serverTimezone", "Asia/Seoul")
            }
        }

        assertEquals("prod-db.example.com", dbConfig.host)
        assertEquals(50, dbConfig.maxPoolSize)
        assertEquals(5, dbConfig.properties.size)
        assertEquals("true", dbConfig.properties["ssl"])
    }

    /**
     * 예제 5: 컬렉션 초기화
     * apply를 사용해 컬렉션을 생성하면서 초기 데이터 추가
     */
    @Test
    fun `apply - 컬렉션 초기화 및 설정`() {
        // MutableList 생성과 동시에 데이터 추가
        val adminUsers = mutableListOf<User>().apply {
            add(User().apply {
                id = 1
                username = "admin1"
                email = "admin1@company.com"
                roles.addAll(listOf("ADMIN", "USER"))
            })

            add(User().apply {
                id = 2
                username = "admin2"
                email = "admin2@company.com"
                roles.addAll(listOf("ADMIN", "USER", "SUPER_ADMIN"))
            })

            add(User().apply {
                id = 3
                username = "admin3"
                email = "admin3@company.com"
                roles.addAll(listOf("ADMIN", "USER"))
            })
        }

        assertEquals(3, adminUsers.size)
        assertTrue(adminUsers.all { it.roles.contains("ADMIN") })

        // ConcurrentHashMap 초기화
        val cache = ConcurrentHashMap<String, User>().apply {
            adminUsers.forEach { user ->
                put(user.username, user)
            }
            put("guest", User().apply {
                username = "guest"
                email = "guest@example.com"
                roles.add("GUEST")
            })
        }

        assertEquals(4, cache.size)
        assertNotNull(cache["admin1"])
    }

    /**
     * 예제 6: 중첩된 apply 사용
     * 복잡한 계층 구조의 객체를 만들 때
     */
    @Test
    fun `apply - 중첩된 객체 초기화`() {
        val superUser = User().apply {
            id = 100
            username = "superadmin"
            email = "super@company.com"
            firstName = "Super"
            lastName = "Admin"
            age = 35
            isActive = true
            createdAt = LocalDateTime.now()

            // 역할 설정
            roles.apply {
                add("SUPER_ADMIN")
                add("ADMIN")
                add("USER")
                add("DEVELOPER")
            }

            // 메타데이터 설정 - 중첩된 맵 구조
            metadata.apply {
                put("department", "IT")
                put("level", 10)
                put("permissions", mutableListOf<String>().apply {
                    add("READ")
                    add("WRITE")
                    add("DELETE")
                    add("EXECUTE")
                })
                put("settings", mutableMapOf<String, Any>().apply {
                    put("theme", "dark")
                    put("language", "ko")
                    put("notifications", mutableMapOf<String, Boolean>().apply {
                        put("email", true)
                        put("sms", false)
                        put("push", true)
                    })
                })
            }
        }

        assertEquals("superadmin", superUser.username)
        assertEquals(4, superUser.roles.size)
        assertEquals("IT", superUser.metadata["department"])

        @Suppress("UNCHECKED_CAST")
        val permissions = superUser.metadata["permissions"] as List<String>
        assertEquals(4, permissions.size)

        @Suppress("UNCHECKED_CAST")
        val settings = superUser.metadata["settings"] as Map<String, Any>
        assertEquals("dark", settings["theme"])
    }

    /**
     * 예제 7: apply와 함께 함수 체이닝
     * 객체를 생성하고 설정한 후 바로 다른 작업 수행
     */
    @Test
    fun `apply - 체이닝과 함께 사용`() {
        val userDatabase = mutableListOf<User>()

        // apply로 객체를 설정하고 바로 리스트에 추가
        val newUser = User().apply {
            username = "chained_user"
            email = "chained@example.com"
            roles.add("USER")
        }.also { user ->
            // also를 체이닝해서 추가 작업
            userDatabase.add(user)
            println("User ${user.username} added to database")
        }

        assertEquals("chained_user", newUser.username)
        assertEquals(1, userDatabase.size)
        assertTrue(userDatabase.contains(newUser))
    }

    /**
     * 예제 8: 조건부 속성 설정
     * apply 블록 내에서 조건에 따라 다르게 설정
     */
    @Test
    fun `apply - 조건부 속성 설정`() {
        fun createUser(isPremium: Boolean, isAdmin: Boolean): User {
            return User().apply {
                username = "conditional_user"
                email = "conditional@example.com"

                // 조건에 따른 역할 부여
                when {
                    isAdmin -> {
                        roles.addAll(listOf("ADMIN", "USER", "PREMIUM"))
                        metadata["accessLevel"] = "FULL"
                    }
                    isPremium -> {
                        roles.addAll(listOf("PREMIUM", "USER"))
                        metadata["accessLevel"] = "PREMIUM"
                    }
                    else -> {
                        roles.add("USER")
                        metadata["accessLevel"] = "BASIC"
                    }
                }

                // 프리미엄 사용자 추가 설정
                if (isPremium || isAdmin) {
                    metadata.apply {
                        put("maxStorage", "100GB")
                        put("supportPriority", "HIGH")
                        put("features", listOf("advanced_search", "export", "api_access"))
                    }
                } else {
                    metadata.apply {
                        put("maxStorage", "5GB")
                        put("supportPriority", "NORMAL")
                        put("features", listOf("basic_search"))
                    }
                }
            }
        }

        val adminUser = createUser(isPremium = false, isAdmin = true)
        assertEquals("FULL", adminUser.metadata["accessLevel"])
        assertEquals(3, adminUser.roles.size)

        val premiumUser = createUser(isPremium = true, isAdmin = false)
        assertEquals("PREMIUM", premiumUser.metadata["accessLevel"])
        assertEquals("100GB", premiumUser.metadata["maxStorage"])

        val basicUser = createUser(isPremium = false, isAdmin = false)
        assertEquals("BASIC", basicUser.metadata["accessLevel"])
        assertEquals("5GB", basicUser.metadata["maxStorage"])
    }

    /**
     * 예제 9: StringBuilder와 함께 사용
     * 문자열 생성 시 apply 활용
     */
    @Test
    fun `apply - StringBuilder로 복잡한 문자열 생성`() {
        val users = listOf(
            User().apply { id = 1; username = "user1"; email = "user1@example.com" },
            User().apply { id = 2; username = "user2"; email = "user2@example.com" },
            User().apply { id = 3; username = "user3"; email = "user3@example.com" }
        )

        val report = StringBuilder().apply {
            appendLine("=== 사용자 리포트 ===")
            appendLine("생성 시각: ${LocalDateTime.now()}")
            appendLine("총 사용자 수: ${users.size}")
            appendLine()
            appendLine("상세 정보:")

            users.forEachIndexed { index, user ->
                appendLine("${index + 1}. ${user.username} (${user.email})")
            }

            appendLine()
            appendLine("=".repeat(30))
        }.toString()

        assertTrue(report.contains("총 사용자 수: 3"))
        assertTrue(report.contains("user1@example.com"))
        println(report)
    }

    /**
     * 예제 10: 실무 시나리오 - 테스트 데이터 생성
     * 통합 테스트나 E2E 테스트에서 복잡한 테스트 데이터를 만들 때
     */
    @Test
    fun `apply - 실무 시나리오 테스트 데이터 생성`() {
        // 테스트용 사용자 목록 생성
        val testUsers = (1..10).map { index ->
            User().apply {
                id = index.toLong()
                username = "testuser$index"
                email = "testuser$index@test.com"
                firstName = "Test"
                lastName = "User$index"
                age = 20 + index
                isActive = index % 2 == 0  // 짝수 ID만 활성화

                roles.apply {
                    add("USER")
                    if (index <= 3) add("ADMIN")
                    if (index == 1) add("SUPER_ADMIN")
                }

                metadata.apply {
                    put("testData", true)
                    put("batchNumber", index / 5 + 1)  // 5명씩 배치로 나눔
                    put("createdBy", "test-automation")
                    put("tags", listOf("test", "batch-${index / 5 + 1}"))
                }

                createdAt = LocalDateTime.now().minusDays(index.toLong())
            }
        }

        assertEquals(10, testUsers.size)

        val adminUsers = testUsers.filter { it.roles.contains("ADMIN") }
        assertEquals(3, adminUsers.size)

        val activeUsers = testUsers.filter { it.isActive }
        assertEquals(5, activeUsers.size)

        val superAdmin = testUsers.find { it.roles.contains("SUPER_ADMIN") }
        assertNotNull(superAdmin)
        assertEquals("testuser1", superAdmin?.username)
    }
}
