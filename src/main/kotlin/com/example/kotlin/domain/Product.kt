package com.example.kotlin.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany

@Entity
class Product(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long ?= null,
    var name: String?,
    var description: String?,
    var price: Int,
    var quantity: Int,

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var productOptions: MutableList<ProductOption> = mutableListOf()
) {

    val totalPrice: Int
        get() = (quantity * price)

    companion object {
        fun of(
            name: String?,
            description: String?,
            price: Int,
            quantity: Int,
        ): Product {
            return Product(
                name = name,
                description = description,
                price = price,
                quantity = quantity,
                productOptions = mutableListOf(),
            )
        }
    }
}