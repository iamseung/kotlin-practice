package com.example.kotlin.algorithm

import java.io.*
import java.util.*

class Main {

    private val br = BufferedReader(InputStreamReader(System.`in`))
    private val bw = BufferedWriter(OutputStreamWriter(System.out))
    private var N = 0
    private var count = 0
    private val dir = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, 1), Pair(0, -1))
    private val list = mutableListOf<Int>()
    private lateinit var visited: Array<BooleanArray>
    private lateinit var arr: Array<IntArray>

    fun run() {
        br.use {
            bw.use {
                input()
                process()

                list.sort()
                bw.write("${list.size}\n")
                    for (v in list) bw.write("$v\n")

                bw.flush()
            }
        }
    }

    fun input() {
        N = br.readLine().toInt()
        visited = Array(N) { BooleanArray(N) }
        arr = Array(N) { IntArray(N) }

        for(i in 0 until N) {
            val input = br.readLine()
            input.toCharArray().mapIndexed { idx, ch ->
                arr[i][idx] = ch - '0'
            }
        }
    }

    fun process() {
        for(y in 0 until N) {
            for (x in 0 until N) {
                if (isCanDFS(x, y)) {
                    count = 0
                    dfs(x, y)
                    list.add(count)
                }
            }
        }
    }

    fun isCanDFS(x: Int, y: Int): Boolean {
        if (x < 0 || y < 0 || x >= N || y >= N) {
            return false
        }

        return arr[y][x] == 1 && !visited[y][x]
    }

    fun dfs(x: Int, y: Int) {
        count++
        visited[y][x] = true

        for(pair in dir) run {
            val dx = x + pair.first
            val dy = y + pair.second

            if (isCanDFS(dx, dy)) {
                dfs(dx, dy)
            }
        }
    }
}

fun main() {
    Main().run()
}