package com.example.barberrecods.util

class PhoneUtils {

    static String normalize(String phone) {
        if (!phone?.trim()) {
            return ''
        }
        String digits = phone.replaceAll(/\D/, '')
        if (digits.length() == 11 && digits.startsWith('8')) {
            digits = '7' + digits.substring(1)
        }
        if (digits.length() == 10) {
            digits = '7' + digits
        }
        digits
    }
}
