package no.nav.helse.sparkel.aareg

import java.time.LocalDate

class Behovløser(
    private val arbeidsforholdClient: ArbeidsforholdClient,
    private val organisasjonClient: OrganisasjonClient
) {
    fun løsBehov(aktørId: String, fom: LocalDate, tom: LocalDate, organisasjonsnummer: String): List<LøsningDto> {
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
}

data class LøsningDto(
    val arbeidsgivernavn: String,
    val bransjer: List<String>,
    val stillingstittel: String,
    val stillingsprosent: Int,
    val startdato: LocalDate,
    val sluttdato: LocalDate?
)
