package com.example.kotlin.product

import com.example.kotlin.domain.Product
import com.example.kotlin.domain.ProductOption
import com.example.kotlin.repository.ProductOptionRepository
import com.example.kotlin.repository.ProductRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest
class productTest @Autowired constructor(
    private val productRepository: ProductRepository,
    private val productOptionRepository: ProductOptionRepository,
) {

    @Test
    fun `test`() {
        // given
        val product = Product.of(
            "상품A",
            "상품A 입니다",
            10000,
            100
        ).apply(productRepository::save)
        (1..10).map {
            ProductOption.of(
                name = "productOption${it}",
                extraPrice = it,
                quantity = it,
                product = product,
            )
        }.apply(productOptionRepository::saveAll)

        productRepository.save(product)
        // when
        val first = productRepository.findAll().last()

        // then
        println(first.id)
    }

}