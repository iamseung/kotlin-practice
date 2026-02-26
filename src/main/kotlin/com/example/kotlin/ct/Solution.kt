package com.example.kotlin.ct

class Solution {

    private lateinit var visited : BooleanArray

    fun main() {
        val nums = intArrayOf(2,7,11,15)
        val target = 9

        val ans = twoSumV2(nums, target)
        println(ans)
    }

    fun twoSum(nums: IntArray, target: Int): IntArray {
        visited = BooleanArray(nums.size)

        return IntArray(nums.size) { i -> nums[i] + target }
    }

    fun twoSumV2(nums: IntArray, target: Int): IntArray {
        for (i in nums.indices) {
            println(i)
        }

        return IntArray(nums.size) { i -> nums[i] + target }
    }
}

fun main() {
    val solution = Solution()
    solution.main()
}