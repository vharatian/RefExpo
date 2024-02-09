package com.github.vharatian.refexpo.utils

fun Boolean?.checkNullOrFalse(): Boolean {
    return this == null || !this
}