package com.example.kotlin.domain.item

import com.example.kotlin.domain.common.BaseEntity
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "items")
class Item(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 200)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(nullable = false, precision = 10, scale = 0)
    val basePrice: BigDecimal,

    @Column(nullable = false)
    var stockQuantity: Int = 0
) : BaseEntity() {
    @OneToMany(mappedBy = "item", cascade = [CascadeType.ALL], orphanRemoval = true)
    val options: MutableList<ItemOption> = mutableListOf()

    fun decreaseStock(quantity: Int) {
        require(this.stockQuantity >= quantity) { "재고가 부족합니다. 현재: ${this.stockQuantity}, 요청: $quantity" }
        this.stockQuantity -= quantity
        // updatedAt는 JPA Auditing이 자동으로 업데이트
    }

    fun increaseStock(quantity: Int) {
        this.stockQuantity += quantity
        // updatedAt는 JPA Auditing이 자동으로 업데이트
    }

    fun addOption(itemOption: ItemOption) {
        this.options.add(itemOption)
    }

    fun removeOption(itemOption: ItemOption) {
        this.options.remove(itemOption)
    }

    override fun toString(): String {
        return "Item(id=$id, name='$name', basePrice=$basePrice, stockQuantity=$stockQuantity, optionsCount=${options.size})"
    }
}
