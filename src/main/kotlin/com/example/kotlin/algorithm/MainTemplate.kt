package com.example.kotlin.algorithm

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.LinkedList
import java.util.StringTokenizer

class MainTemplate {
    private val br = BufferedReader(InputStreamReader(System.`in`))
    private val bw = BufferedWriter(OutputStreamWriter(System.out))
    private val sb = StringBuilder()

    private lateinit var visited: BooleanArray
    private lateinit var adj: Array<MutableList<Int>>
    private var N = 0
    private var M = 0
    private var V = 0

    fun run() {
        br.use {
            bw.use {
                input()
                process()
                bw.write(sb.toString())
                bw.flush()
            }
        }
    }

    private fun input() {
        var st = StringTokenizer(br.readLine())
        N = st.nextToken().toInt()
        M = st.nextToken().toInt()
        V = st.nextToken().toInt()

        adj = Array(N + 1) { mutableListOf<Int>() }

        repeat(M) {
            st = StringTokenizer(br.readLine())
            val x = st.nextToken().toInt()
            val y = st.nextToken().toInt()
            adj[x].add(y)
            adj[y].add(x)
        }

        // 정점 번호가 작은 것을 먼저 방문한다.
        for (i in 1..N) adj[i].sort()
    }

    private fun process() {
        visited = BooleanArray(N + 1)
        dfs(V)
        sb.append("\n")

        visited.fill(false)
        bfs(V)
    }

    private fun dfs(start: Int) {
        sb.append("$start ")
        visited[start] = true

        for (k in adj[start]) {
            if (visited[k]) continue
            dfs(k)
        }
    }

    private fun bfs(start: Int) {
        val queue: LinkedList<Int> = LinkedList()
        queue.add(start)
        visited[start] = true

        while (queue.isNotEmpty()) {
            val x = queue.poll()
            sb.append("$x ")

            for (k in adj[x]) {
                if (visited[k]) continue
                queue.add(k)
                visited[k] = true
            }
        }
    }
}

fun main() {
    MainTemplate().run()
}