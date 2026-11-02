package com.menash.myself.feature.money.domain

/** Temporary function used only to verify the changed-lines CI gate. */
fun coverageGateProof(value: Int): Int {
    val one = value + 1
    val two = one + 2
    val three = two + 3
    val four = three + 4
    val five = four + 5
    val six = five + 6
    val seven = six + 7
    val eight = seven + 8
    val nine = eight + 9
    val ten = nine + 10
    return if (value > 0) ten else -ten
}
