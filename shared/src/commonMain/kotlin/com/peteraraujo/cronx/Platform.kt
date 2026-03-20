package com.peteraraujo.cronx

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform