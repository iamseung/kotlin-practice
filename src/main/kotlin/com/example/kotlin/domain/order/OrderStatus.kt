package com.example.kotlin.domain.order

enum class OrderStatus {
    PENDING,      // 대기
    CONFIRMED,    // 확정
    SHIPPED,      // 배송 중
    COMPLETED,    // 완료
    CANCELLED     // 취소
}