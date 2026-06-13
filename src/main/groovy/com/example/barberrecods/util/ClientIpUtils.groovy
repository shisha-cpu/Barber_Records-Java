package com.example.barberrecods.util

import jakarta.servlet.http.HttpServletRequest

class ClientIpUtils {

    static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader('X-Forwarded-For')
        if (forwarded?.trim()) {
            return forwarded.split(',')[0].trim()
        }
        request.remoteAddr ?: ''
    }
}
