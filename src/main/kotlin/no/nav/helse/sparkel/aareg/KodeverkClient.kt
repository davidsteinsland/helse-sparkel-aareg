package no.nav.helse.sparkel.aareg

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.util.*

class KodeverkClient(private val httpClient: HttpClient, private val environment: Environment) {
    private var cachedNæringResponse: String? = null
    private var cachedYrkerResponse: String? = null

    fun getNæring(kode: String): String = runBlocking {
        if (cachedNæringResponse === null) {
            cachedNæringResponse = httpClient.get<HttpResponse>("${environment.kodeverkBaseUrl}/api/v1/kodeverk/Næringskoder/koder/betydninger") {
                    setup(UUID.randomUUID().toString())
                }.receive<JsonNode>().hentTekst(kode)
        }
        requireNotNull(cachedNæringResponse)
    }

    fun getYrke(kode: String) = runBlocking {
        if (cachedYrkerResponse === null) {
            cachedYrkerResponse = httpClient.get<HttpResponse>("${environment.kodeverkBaseUrl}/api/v1/kodeverk/Yrker/koder/betydninger") {
                setup(UUID.randomUUID().toString())
            }.receive<JsonNode>().hentTekst(kode)
        }
        requireNotNull(cachedYrkerResponse)
    }

    private fun HttpRequestBuilder.setup(callId: String) {
        header("Nav-Call-Id", callId)
        header("Nav-Consumer-Id", environment.appName)
        parameter("spraak", "nb")
        parameter("ekskluderUgyldige", true)
        parameter("oppslagsdato", LocalDate.now())
    }

    private fun JsonNode.hentTekst(kode: String): String =
        path("betydninger").path(kode).path("beskrivelser").path("nb").path("tekst").asText()
}
