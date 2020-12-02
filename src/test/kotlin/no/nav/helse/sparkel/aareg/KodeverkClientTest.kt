package no.nav.helse.sparkel.aareg

import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class KodeverkClientTest {
    @Test
    fun `henter tekst fra kodeverksrespons`() {
        assertEquals("Engroshandel med innsatsvarer ikke nevnt annet sted", kodeverksrespons.hentTekst("46.769"))
    }

    private val kodeverksrespons: JsonNode = requireNotNull(
        objectMapper.valueToTree(
            mapOf(
                "betydninger" to mapOf(
                    "46.769" to listOf(
                        mapOf(
                            "gyldigFra" to "1995-01-01",
                            "gyldigTil" to "9999-12-31",
                            "beskrivelser" to mapOf(
                                "nb" to mapOf(
                                    "term" to "Engroshandel med innsatsvarer ikke nevnt annet sted",
                                    "tekst" to "Engroshandel med innsatsvarer ikke nevnt annet sted"
                                )
                            )
                        )
                    )
                )
            )
        )
    )
}
