package com.github.vharatian.refexpo.utils

fun Boolean?.isNullOrFalse(): Boolean {
    return this == null || !this
}