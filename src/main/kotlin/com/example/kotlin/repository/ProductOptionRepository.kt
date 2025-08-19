package com.example.kotlin.repository

import com.example.kotlin.domain.ProductOption
import org.springframework.data.jpa.repository.JpaRepository

interface ProductOptionRepository : JpaRepository<ProductOption, Long>