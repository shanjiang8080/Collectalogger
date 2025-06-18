package com.example.collectalogger2.util

// Exceptions that generate an HTML Status code like 429 Too Many Requests
class APIStatusException(message: String, val statusCode: Int) : APIException(message) {
}