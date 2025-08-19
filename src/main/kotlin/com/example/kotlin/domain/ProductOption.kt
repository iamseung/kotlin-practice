package com.example.kotlin.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne

@Entity
class ProductOption(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?= null,
    var name: String?,
    var extraPrice: Int,
    var quantity: Int,

    @ManyToOne
    val product: Product,
) {

    companion object {
        fun of(
            name: String?,
            extraPrice: Int,
            quantity: Int,
            product: Product,
        ): ProductOption {
            return ProductOption(
                name = name,
                extraPrice = extraPrice,
                quantity = quantity,
                product = product,
            )
        }
    }
}