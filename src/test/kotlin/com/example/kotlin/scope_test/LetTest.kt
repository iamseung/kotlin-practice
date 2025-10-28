package com.example.kotlin.scope_test

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * let - null 안전성과 스코프 제한, 값 변환에 특화된 함수
 *
 * 언제 사용하는가?
 * 1. nullable 객체를 안전하게 처리할 때 (?. 연산자와 함께)
 * 2. 객체를 다른 타입으로 변환할 때
 * 3. 변수의 스코프를 제한하고 싶을 때
 * 4. 체이닝 중간에 값을 변환하거나 검증할 때
 *
 * 특징:
 * - 컨텍스트 객체: it (명시적으로 이름 변경 가능)
 * - 반환값: 람다의 마지막 표현식 결과
 * - 주 용도: null 체크, 값 변환, 스코프 제한
 */
class LetTest {

    // 실무 예제용 도메인 클래스들
    data class User(
        val id: Long,
        val username: String,
        val email: String,
        val phoneNumber: String?,
        val address: Address?,
        val profileImage: String?,
        val settings: UserSettings?
    )

    data class Address(
        val street: String,
        val city: String,
        val zipCode: String
    )

    data class UserSettings(
        val language: String,
        val timezone: String,
        val emailNotifications: Boolean
    )

    data class ApiResponse<T>(
        val success: Boolean,
        val data: T?,
        val error: String?,
        val timestamp: LocalDateTime
    )

    data class Product(
        val id: Long,
        val name: String,
        val price: BigDecimal,
        val stock: Int?
    )

    /**
     * 예제 1: 기본 사용 - nullable 객체 처리
     */
    @Test
    fun `let - 기본 nullable 처리`() {
        val nullableString: String? = "Hello, Kotlin"

        // let 없이 처리
        val lengthWithout = if (nullableString != null) {
            nullableString.length
        } else {
            0
        }

        // let 사용 - 더 간결하고 안전
        val lengthWith = nullableString?.let {
            it.length  // it은 null이 아닌 String
        } ?: 0

        assertEquals(lengthWithout, lengthWith)

        // null인 경우
        val nullString: String? = null
        val result = nullString?.let {
            it.length
        } ?: 0

        assertEquals(0, result)
    }

    /**
     * 예제 2: 복잡한 nullable 체인 처리
     * 중첩된 nullable 객체를 안전하게 처리
     */
    @Test
    fun `let - 중첩된 nullable 객체 처리`() {
        val user = User(
            id = 1,
            username = "john_doe",
            email = "john@example.com",
            phoneNumber = "010-1234-5678",
            address = Address("강남대로 123", "서울", "06000"),
            profileImage = "https://example.com/profile.jpg",
            settings = UserSettings("ko", "Asia/Seoul", true)
        )

        // 중첩된 nullable 접근을 let으로 안전하게 처리
        val addressInfo = user.address?.let { address ->
            // address는 non-null Address
            "${address.city} ${address.street} (${address.zipCode})"
        } ?: "주소 정보 없음"

        assertEquals("서울 강남대로 123 (06000)", addressInfo)

        // 더 깊은 중첩
        val emailNotificationEnabled = user.settings?.let { settings ->
            settings.emailNotifications
        } ?: false

        assertTrue(emailNotificationEnabled)

        // null인 경우
        val userWithoutAddress = user.copy(address = null)
        val noAddress = userWithoutAddress.address?.let { address ->
            "${address.city} ${address.street}"
        } ?: "주소 정보 없음"

        assertEquals("주소 정보 없음", noAddress)
    }

    /**
     * 예제 3: 값 변환
     * 객체를 다른 형태로 변환할 때 let 사용
     */
    @Test
    fun `let - 객체를 DTO로 변환`() {
        data class UserDTO(
            val id: Long,
            val displayName: String,
            val contactEmail: String,
            val hasAddress: Boolean,
            val hasProfileImage: Boolean
        )

        val user = User(
            id = 100,
            username = "jane_doe",
            email = "jane@example.com",
            phoneNumber = null,
            address = Address("테헤란로 427", "서울", "06158"),
            profileImage = null,
            settings = null
        )

        // let을 사용해서 User를 UserDTO로 변환
        val userDTO = user.let { u ->
            UserDTO(
                id = u.id,
                displayName = u.username.uppercase(),
                contactEmail = u.email,
                hasAddress = u.address != null,
                hasProfileImage = u.profileImage != null
            )
        }

        assertEquals(100L, userDTO.id)
        assertEquals("JANE_DOE", userDTO.displayName)
        assertTrue(userDTO.hasAddress)
        assertFalse(userDTO.hasProfileImage)
    }

    /**
     * 예제 4: 체이닝 중간에 값 변환
     */
    @Test
    fun `let - 체이닝 중간에 값 변환`() {
        val input = "  hello world  "

        val result = input
            .trim()
            .let { trimmed ->
                // 공백 제거 후 단어 분리
                trimmed.split(" ")
            }
            .let { words ->
                // 각 단어의 첫 글자를 대문자로
                words.map { it.capitalize() }
            }
            .let { capitalizedWords ->
                // 다시 합치기
                capitalizedWords.joinToString(" ")
            }

        assertEquals("Hello World", result)

        // 더 실용적인 예: 데이터 파이프라인
        val prices = listOf("1,000", "2,500", "3,750", "10,000")

        val total = prices
            .let { stringPrices ->
                // 문자열을 숫자로 변환
                stringPrices.map { it.replace(",", "").toInt() }
            }
            .let { numbers ->
                // 합계 계산
                numbers.sum()
            }
            .let { sum ->
                // 포맷팅
                String.format("%,d원", sum)
            }

        assertEquals("17,250원", total)
    }

    /**
     * 예제 5: API 응답 처리
     * 실무에서 자주 사용하는 패턴
     */
    @Test
    fun `let - API 응답 안전하게 처리`() {
        // 성공 케이스
        val successResponse = ApiResponse(
            success = true,
            data = User(
                id = 1,
                username = "api_user",
                email = "api@example.com",
                phoneNumber = null,
                address = null,
                profileImage = null,
                settings = null
            ),
            error = null,
            timestamp = LocalDateTime.now()
        )

        val username = successResponse.data?.let { user ->
            user.username
        } ?: "Unknown"

        assertEquals("api_user", username)

        // 실패 케이스
        val errorResponse = ApiResponse<User>(
            success = false,
            data = null,
            error = "User not found",
            timestamp = LocalDateTime.now()
        )

        val errorUsername = errorResponse.data?.let { user ->
            user.username
        } ?: "Unknown"

        assertEquals("Unknown", errorUsername)

        // 복잡한 처리
        val result = successResponse.data?.let { user ->
            // 사용자가 있으면 상세 정보 생성
            mapOf(
                "id" to user.id,
                "name" to user.username,
                "contact" to (user.email ?: "N/A"),
                "hasPhone" to (user.phoneNumber != null)
            )
        }?.let { userInfo ->
            // 정보를 JSON 형식 문자열로 변환
            userInfo.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        } ?: "No user data"

        assertTrue(result.contains("api_user"))
    }

    /**
     * 예제 6: 조건부 처리와 let
     */
    @Test
    fun `let - 조건부 처리`() {
        fun processUser(userId: Long?): String {
            return userId?.let { id ->
                // id가 null이 아닐 때만 실행
                when {
                    id <= 0 -> "Invalid user ID"
                    id < 100 -> "Regular user: $id"
                    id < 1000 -> "Premium user: $id"
                    else -> "VIP user: $id"
                }
            } ?: "No user ID provided"
        }

        assertEquals("Regular user: 50", processUser(50))
        assertEquals("Premium user: 500", processUser(500))
        assertEquals("VIP user: 5000", processUser(5000))
        assertEquals("No user ID provided", processUser(null))
        assertEquals("Invalid user ID", processUser(-1))
    }

    /**
     * 예제 7: 스코프 제한
     * 임시 변수의 범위를 제한할 때
     */
    @Test
    fun `let - 변수 스코프 제한`() {
        data class Report(
            val title: String,
            val content: String,
            val generatedAt: String
        )

        val data = listOf(1, 2, 3, 4, 5)

        // let을 사용해서 중간 계산 변수의 스코프를 제한
        val report = data
            .let { numbers ->
                // numbers는 이 블록 내에서만 유효
                val sum = numbers.sum()
                val avg = numbers.average()
                val max = numbers.maxOrNull() ?: 0
                val min = numbers.minOrNull() ?: 0

                // Report 객체 반환
                Report(
                    title = "숫자 통계 리포트",
                    content = "합계: $sum, 평균: $avg, 최대: $max, 최소: $min",
                    generatedAt = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )
            }

        assertTrue(report.content.contains("합계: 15"))
        assertTrue(report.content.contains("평균: 3.0"))

        // sum, avg 등의 변수는 여기서 접근 불가 (스코프 제한됨)
    }

    /**
     * 예제 8: 파라미터 이름 변경
     * it 대신 의미있는 이름 사용
     */
    @Test
    fun `let - 파라미터 이름을 명시적으로 지정`() {
        val user = User(
            id = 1,
            username = "developer",
            email = "dev@example.com",
            phoneNumber = "010-9876-5432",
            address = null,
            profileImage = null,
            settings = null
        )

        // 기본: it 사용
        val info1 = user.let {
            "${it.username} (${it.email})"
        }

        // 명시적 이름 사용 - 더 읽기 쉬움
        val info2 = user.let { currentUser ->
            "${currentUser.username} (${currentUser.email})"
        }

        assertEquals(info1, info2)

        // 복잡한 경우 명시적 이름이 훨씬 유용
        val complexInfo = user.phoneNumber?.let { phone ->
            // phone이라는 이름이 it보다 의미가 명확
            val formatted = phone.replace("-", "")
            val countryCode = "+82"
            val localNumber = formatted.substring(1)  // 0 제거
            "$countryCode-$localNumber"
        } ?: "No phone number"

        assertEquals("+82-1098765432", complexInfo)
    }

    /**
     * 예제 9: 여러 nullable 값 조합
     */
    @Test
    fun `let - 여러 nullable 값 조합 처리`() {
        data class ContactInfo(
            val email: String,
            val phone: String,
            val address: String
        )

        val user1 = User(
            id = 1,
            username = "user1",
            email = "user1@example.com",
            phoneNumber = "010-1111-2222",
            address = Address("주소1", "서울", "12345"),
            profileImage = null,
            settings = null
        )

        // 모든 값이 있을 때만 ContactInfo 생성
        val contactInfo1 = user1.phoneNumber?.let { phone ->
            user1.address?.let { address ->
                ContactInfo(
                    email = user1.email,
                    phone = phone,
                    address = "${address.city} ${address.street}"
                )
            }
        }

        assertNotNull(contactInfo1)
        assertEquals("010-1111-2222", contactInfo1?.phone)

        // 하나라도 null이면 null 반환
        val user2 = user1.copy(phoneNumber = null)
        val contactInfo2 = user2.phoneNumber?.let { phone ->
            user2.address?.let { address ->
                ContactInfo(
                    email = user2.email,
                    phone = phone,
                    address = "${address.city} ${address.street}"
                )
            }
        }

        assertNull(contactInfo2)
    }

    /**
     * 예제 10: 실무 시나리오 - 결제 처리
     */
    @Test
    fun `let - 실무 시나리오 결제 처리`() {
        data class PaymentRequest(
            val userId: Long,
            val amount: BigDecimal,
            val paymentMethod: String?,
            val couponCode: String?,
            val shippingAddress: Address?
        )

        data class PaymentResult(
            val success: Boolean,
            val transactionId: String?,
            val finalAmount: BigDecimal,
            val message: String
        )

        fun processPayment(request: PaymentRequest): PaymentResult {
            // 결제 수단 검증
            val paymentMethod = request.paymentMethod?.let { method ->
                when (method.uppercase()) {
                    "CARD", "BANK", "MOBILE" -> method
                    else -> null
                }
            }

            if (paymentMethod == null) {
                return PaymentResult(
                    success = false,
                    transactionId = null,
                    finalAmount = request.amount,
                    message = "유효하지 않은 결제 수단"
                )
            }

            // 쿠폰 할인 적용
            val discountAmount = request.couponCode?.let { code ->
                when (code) {
                    "WELCOME10" -> request.amount * BigDecimal("0.1")
                    "SAVE5000" -> BigDecimal("5000")
                    "VIP20" -> request.amount * BigDecimal("0.2")
                    else -> null
                }
            }

            val finalAmount = discountAmount?.let { discount ->
                (request.amount - discount).max(BigDecimal.ZERO)
            } ?: request.amount

            // 배송지 검증
            val hasValidAddress = request.shippingAddress?.let { address ->
                address.street.isNotBlank() &&
                        address.city.isNotBlank() &&
                        address.zipCode.isNotBlank()
            } ?: false

            if (!hasValidAddress) {
                return PaymentResult(
                    success = false,
                    transactionId = null,
                    finalAmount = finalAmount,
                    message = "유효한 배송지가 필요합니다"
                )
            }

            // 결제 처리 성공
            return PaymentResult(
                success = true,
                transactionId = "TXN-${System.currentTimeMillis()}",
                finalAmount = finalAmount,
                message = "결제가 완료되었습니다"
            )
        }

        // 성공 케이스
        val successRequest = PaymentRequest(
            userId = 1,
            amount = BigDecimal("50000"),
            paymentMethod = "CARD",
            couponCode = "SAVE5000",
            shippingAddress = Address("강남대로 123", "서울", "06000")
        )

        val successResult = processPayment(successRequest)
        assertTrue(successResult.success)
        assertEquals(BigDecimal("45000"), successResult.finalAmount)
        assertNotNull(successResult.transactionId)

        // 실패 케이스 1: 잘못된 결제 수단
        val invalidMethodRequest = successRequest.copy(paymentMethod = "INVALID")
        val invalidMethodResult = processPayment(invalidMethodRequest)
        assertFalse(invalidMethodResult.success)
        assertEquals("유효하지 않은 결제 수단", invalidMethodResult.message)

        // 실패 케이스 2: 배송지 없음
        val noAddressRequest = successRequest.copy(shippingAddress = null)
        val noAddressResult = processPayment(noAddressRequest)
        assertFalse(noAddressResult.success)
        assertEquals("유효한 배송지가 필요합니다", noAddressResult.message)

        // 쿠폰 없는 경우
        val noCouponRequest = successRequest.copy(couponCode = null)
        val noCouponResult = processPayment(noCouponRequest)
        assertTrue(noCouponResult.success)
        assertEquals(BigDecimal("50000"), noCouponResult.finalAmount)
    }

    /**
     * 예제 11: takeIf/takeUnless와 함께 사용
     */
    @Test
    fun `let - takeIf, takeUnless와 조합`() {
        val products = listOf(
            Product(1, "재고 충분", BigDecimal("10000"), 100),
            Product(2, "재고 부족", BigDecimal("20000"), 5),
            Product(3, "재고 없음", BigDecimal("15000"), 0),
            Product(4, "재고 정보 없음", BigDecimal("30000"), null)
        )

        // takeIf와 let 조합: 조건을 만족할 때만 처리
        val lowStockProducts = products.mapNotNull { product ->
            product.stock
                ?.takeIf { it > 0 && it < 10 }  // 재고가 1~9 사이일 때만
                ?.let { stock ->
                    "${product.name}: 재고 ${stock}개 (긴급 발주 필요)"
                }
        }

        assertEquals(1, lowStockProducts.size)
        assertTrue(lowStockProducts[0].contains("재고 부족"))

        // takeUnless와 let 조합: 조건을 만족하지 않을 때만 처리
        val availableProducts = products.mapNotNull { product ->
            product.stock
                ?.takeUnless { it == 0 }  // 재고가 0이 아닐 때
                ?.let { stock ->
                    "${product.name}: ${stock}개 판매 가능"
                }
        }

        assertEquals(2, availableProducts.size)
    }

    /**
     * 예제 12: 복잡한 데이터 파싱
     */
    @Test
    fun `let - 복잡한 데이터 파싱 및 검증`() {
        // JSON 형식의 문자열을 파싱하는 상황 시뮬레이션
        data class ParsedData(
            val userId: Long,
            val email: String,
            val metadata: Map<String, String>
        )

        fun parseUserData(input: String?): ParsedData? {
            return input
                ?.takeIf { it.isNotBlank() }
                ?.let { data ->
                    // 데이터 분리
                    val parts = data.split("|")
                    if (parts.size < 3) return@let null

                    // 각 부분 파싱
                    val userId = parts[0].toLongOrNull() ?: return@let null
                    val email = parts[1].takeIf { it.contains("@") } ?: return@let null
                    val metadataStr = parts.getOrNull(2) ?: ""

                    // 메타데이터 파싱
                    val metadata = metadataStr
                        .split(",")
                        .mapNotNull { pair ->
                            pair.split("=")
                                .takeIf { it.size == 2 }
                                ?.let { it[0] to it[1] }
                        }
                        .toMap()

                    ParsedData(userId, email, metadata)
                }
        }

        // 성공 케이스
        val validData = "123|user@example.com|role=admin,status=active"
        val parsed = parseUserData(validData)

        assertNotNull(parsed)
        assertEquals(123L, parsed?.userId)
        assertEquals("user@example.com", parsed?.email)
        assertEquals("admin", parsed?.metadata?.get("role"))
        assertEquals("active", parsed?.metadata?.get("status"))

        // 실패 케이스들
        assertNull(parseUserData(null))
        assertNull(parseUserData(""))
        assertNull(parseUserData("invalid"))
        assertNull(parseUserData("123|invalid-email"))
        assertNull(parseUserData("not-a-number|user@example.com|"))
    }
}
