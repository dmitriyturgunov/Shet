package com.example.schet

object ProblemGenerator {

    fun buildAdd(maxSum: Int): List<Problem> {
        val list = mutableListOf<Problem>()
        if (maxSum == 100) {
            for (a in 10..89) {
                for (b in 10..(100 - a)) {
                    list.add(Problem(a, b, a + b, "a${a}+${b}"))
                }
            }
        } else {
            for (a in 1 until maxSum) {
                for (b in 1..(maxSum - a)) {
                    list.add(Problem(a, b, a + b, "a${a}+${b}"))
                }
            }
        }
        return list
    }

    fun buildSub(maxSum: Int): List<Problem> {
        val list = mutableListOf<Problem>()
        if (maxSum == 100) {
            for (a in 20..100) {
                for (b in 10 until a) {
                    val r = a - b
                    if (r >= 10) list.add(Problem(a, b, r, "s${a}-${b}"))
                }
            }
        } else {
            for (a in 2..maxSum) {
                for (b in 1 until a) {
                    list.add(Problem(a, b, a - b, "s${a}-${b}"))
                }
            }
        }
        return list
    }

    fun build(op: String, maxSum: Int): List<Problem> =
        if (op == "add") buildAdd(maxSum) else buildSub(maxSum)
}
