package no.nav.paop

import java.net.ServerSocket

fun randomPort(): Int = ServerSocket(0).use { it.localPort }
