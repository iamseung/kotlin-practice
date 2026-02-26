package com.example.kotlin.algorithm

import kotlin.test.Test
import kotlin.test.assertContentEquals

class Solution {

    @Test
    fun test1() {
        assertContentEquals(intArrayOf(0, 1), twoSum(intArrayOf(2, 7, 11, 15), 9))
    }

    @Test
    fun test2() {
        assertContentEquals(intArrayOf(1, 2), twoSum(intArrayOf(3, 2, 4), 6))
    }

    @Test
    fun test3() {
        assertContentEquals(intArrayOf(0, 1), twoSum(intArrayOf(3, 3), 6))
    }

    fun twoSum(nums: IntArray, target: Int): IntArray {
        val map = mutableMapOf<Int, Int>() // value -> index

        for (i in nums.indices) {
            val complement = target - nums[i]
            if (map.containsKey(complement)) {
                return intArrayOf(map[complement]!!, i)
            }
            map[nums[i]] = i
        }

        throw IllegalArgumentException("No two sum solution")
    }
}
