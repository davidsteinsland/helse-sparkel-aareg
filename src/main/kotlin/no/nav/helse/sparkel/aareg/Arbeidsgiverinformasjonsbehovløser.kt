package no.nav.helse.sparkel.aareg

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate

class Arbeidsgiverinformasjonsbehovløser(
    rapidsConnection: RapidsConnection,
    private val organisasjonClient: OrganisasjonClient
) : River.PacketListener {
    companion object {
        internal const val behov = "Arbeidsgiverinformasjon"
    }

    init {
        River(rapidsConnection).apply {
            validate { it.requireContains("@behov", behov) }
            validate { it.forbid("@løsning") }
            validate { it.requireKey("@id") }
            validate { it.requireKey("$behov.organisasjonsnummer") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        sikkerlogg.info("mottok melding: ${packet.toJson()}")

        val organisasjonsnummer = packet["$behov.organisasjonsnummer"].asText()

        løsBehov(organisasjonsnummer = organisasjonsnummer).also { packet.setLøsning(behov, it) }

        sikkerlogg.info(
            "løser $behov-behov {}",
            keyValue("id", packet["@id"].asText()),
        )

        context.send(packet.toJson())
    }

    private fun løsBehov(organisasjonsnummer: String) =
        organisasjonClient.finnOrganisasjon(organisasjonsnummer)
            .let { organisasjon ->
                LøsningDto(
                    arbeidsgivernavn = organisasjon.navn,
                    bransjer = organisasjon.bransjer,
                )
            }

    private fun JsonMessage.setLøsning(nøkkel: String, data: Any) {
        this["@løsning"] = mapOf(
            nøkkel to data
        )
    }

    data class LøsningDto(
        val arbeidsgivernavn: String,
        val bransjer: List<String>
    )
}
