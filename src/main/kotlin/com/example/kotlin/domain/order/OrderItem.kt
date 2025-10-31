package com.example.kotlin.domain.order

import com.example.kotlin.domain.common.BaseEntity
import com.example.kotlin.domain.item.Item
import com.example.kotlin.domain.item.ItemOption
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "order_items")
class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    val item: Item,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_option_id")
    val itemOption: ItemOption? = null,

    @Column(nullable = false)
    val quantity: Int,

    @Column(nullable = false, precision = 10, scale = 0)
    val price: BigDecimal
) : BaseEntity() {
    fun getTotalPrice(): BigDecimal {
        return price.multiply(BigDecimal(quantity))
    }

    override fun toString(): String {
        return "OrderItem(id=$id, orderId=${order.id}, itemId=${item.id}, itemOptionId=${itemOption?.id}, quantity=$quantity, price=$price)"
    }
}
