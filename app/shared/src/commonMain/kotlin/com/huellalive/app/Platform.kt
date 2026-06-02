package com.huellalive.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform