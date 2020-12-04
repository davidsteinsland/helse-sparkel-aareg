package no.nav.helse.sparkel.aareg

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.LocalDate

class Behovløser(
    private val rapidsConnection: RapidsConnection,
    private val arbeidsforholdClient: ArbeidsforholdClient,
    private val organisasjonClient: OrganisasjonClient
) : River.PacketListener {
    companion object {
        internal const val behov = "Arbeidsforhold"
    }

    init {
        River(rapidsConnection).apply {
            validate { it.requireContains("@behov", behov) }
            validate { it.forbid("@løsning") }
            validate { it.requireKey("@id") }
            validate { it.requireKey("aktørId") }
            validate { it.requireKey("vedtaksperiodeId") }
            validate { it.requireKey("organisasjonsnummer") }
            validate { it.require("fom", JsonNode::asLocalDate) }
            validate { it.require("tom", JsonNode::asLocalDate) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        sikkerlogg.info("mottok melding: ${packet.toJson()}")

        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val aktørId = packet["aktørId"].asText()
        val fom = packet["fom"].asLocalDate()
        val tom = packet["tom"].asLocalDate()

        løsBehov(
            aktørId = aktørId,
            fom = fom,
            tom = tom,
            organisasjonsnummer = organisasjonsnummer
        ).also {
            packet.setLøsning(behov, it)
        }

        sikkerlogg.info(
            "løser behov {} for {}",
            keyValue("id", packet["@id"].asText()),
            keyValue("vedtaksperiodeId", packet["vedtaksperiodeId"].asText())
        )

        context.send(packet.toJson())
    }

    internal fun løsBehov(
        aktørId: String,
        fom: LocalDate,
        tom: LocalDate,
        organisasjonsnummer: String
    ): List<LøsningDto> {
        val organisasjon = organisasjonClient.finnOrganisasjon(organisasjonsnummer)
        return arbeidsforholdClient.finnArbeidsforhold(organisasjonsnummer, aktørId, fom, tom).map { arbeidsforhold ->
            LøsningDto(
                arbeidsgivernavn = organisasjon.navn,
                bransjer = organisasjon.bransjer,
                stillingstittel = arbeidsforhold.stillingstittel,
                stillingsprosent = arbeidsforhold.stillingsprosent,
                startdato = arbeidsforhold.startdato,
                sluttdato = arbeidsforhold.sluttdato
            )
        }
    }

    private fun JsonMessage.setLøsning(nøkkel: String, data: Any) {
        this["@løsning"] = mapOf(
            nøkkel to data
        )
    }
}

data class LøsningDto(
    val arbeidsgivernavn: String,
    val bransjer: List<String>,
    val stillingstittel: String,
    val stillingsprosent: Int,
    val startdato: LocalDate,
    val sluttdato: LocalDate?
)
