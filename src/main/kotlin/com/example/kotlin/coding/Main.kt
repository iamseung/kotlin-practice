package com.example.kotlin.coding

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.StringTokenizer

class Main {
    private val sb = StringBuilder()
    private var N = 0
    private var M = 0
    private lateinit var selected: IntArray

    fun run() {
        val br = BufferedReader(InputStreamReader(System.`in`))
        val st = StringTokenizer(br.readLine())
        N = st.nextToken().toInt()
        M = st.nextToken().toInt()
        selected = IntArray(M + 1)

        recFunction(1)

        print(sb)
    }

    private fun recFunction(k: Int) {
        if (k == M + 1) {
            for (i in 1..M) {
                sb.append(selected[i]).append(" ")
            }
            sb.append("\n")
        } else {
            for (cand in 1..N) {
                val isUsed = (1 until k).any { selected[it] == cand }

                if (!isUsed) {
                    selected[k] = cand
                    recFunction(k + 1)
                    selected[k] = 0
                }
            }
        }
    }
}

fun main() {
    Main().run()
}
