package no.nav.helse.sparkel.aareg

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import java.io.File

fun main() {
    val app = createApp(System.getenv())
    app.start()
}

internal fun createApp(env: Map<String, String>): RapidsConnection {
    return RapidApplication.create(env)
}

private fun String.readFile() = File(this).readText(Charsets.UTF_8)
