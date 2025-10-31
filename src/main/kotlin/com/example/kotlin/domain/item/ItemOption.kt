package com.example.kotlin.domain.item

import com.example.kotlin.domain.common.BaseEntity
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "item_options")
class ItemOption(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    val item: Item,

    @Column(nullable = false, length = 50)
    val name: String,

    @Column(nullable = false, length = 100)
    val value: String,

    @Column(nullable = false, precision = 10, scale = 0)
    val additionalPrice: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var stockQuantity: Int = 0
) : BaseEntity() {
    fun getTotalPrice(): BigDecimal {
        return item.basePrice.add(additionalPrice)
    }

    fun decreaseStock(quantity: Int) {
        require(this.stockQuantity >= quantity) { "옵션 재고가 부족합니다. 현재: ${this.stockQuantity}, 요청: $quantity" }
        this.stockQuantity -= quantity
        // updatedAt는 JPA Auditing이 자동으로 업데이트
    }

    fun increaseStock(quantity: Int) {
        this.stockQuantity += quantity
        // updatedAt는 JPA Auditing이 자동으로 업데이트
    }

    override fun toString(): String {
        return "ItemOption(id=$id, itemId=${item.id}, name='$name', value='$value', additionalPrice=$additionalPrice, stockQuantity=$stockQuantity)"
    }
}
