package com.example.collectalogger2.util

/**
 * A class for numbers (ints, floats) to be compared with a sign.
 */
class Comparison(val number: Int, val operator: ComparisonOperator) {
    fun evaluate(num: Int): Boolean {
        return when (operator) {
            ComparisonOperator.EQUALS -> num == number
            ComparisonOperator.NOT_EQUALS -> num != number
            ComparisonOperator.GREATER -> num > number
            ComparisonOperator.GREATER_EQUAL -> num >= number
            ComparisonOperator.LESS -> num < number
            ComparisonOperator.LESS_EQUAL -> num <= number
        }
    }
}

enum class ComparisonOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER,
    GREATER_EQUAL,
    LESS,
    LESS_EQUAL
}