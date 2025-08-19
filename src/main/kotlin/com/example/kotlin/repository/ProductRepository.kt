package com.example.kotlin.repository

import com.example.kotlin.domain.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<Product, Long>