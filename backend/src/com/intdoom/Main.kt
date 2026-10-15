// Main.kt
// ---------------------------------------------------------------------------
// This is the front door of the backend. It:
//   1. creates the HTTP server (JDK built-in, no framework),
//   2. points every request at the router in Routes.kt,
//   3. starts the background scheduler.
//
// Run it with:  run.bat     (from the backend folder)
// ---------------------------------------------------------------------------

package com.intdoom

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.Executors

const val PORT = 4000

fun main() {
    val server = HttpServer.create(InetSocketAddress(PORT), 0)

    // Every request runs on its own (virtual) thread, so one slow scrape
    // never blocks the other visitors.
    server.executor = Executors.newVirtualThreadPerTaskExecutor()

    // The "/" context catches every path; Routes.handle picks the handler.
    server.createContext("/", ::handle)

    server.start()
    startScheduler()

    println("\n[server] IntelliDoom backend running at http://localhost:$PORT")
}
