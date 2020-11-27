package no.nav.helse.sparkel.aareg

import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput
import java.time.LocalDate

class ArbeidsforholdService(private val aaregClient: AaregClient) {
    suspend fun finnArbeidsforhold(aktørId: String, fom: LocalDate, tom: LocalDate) {
        try {
            aaregClient.finnArbeidsforhold(aktørId, fom, tom)
        } catch (e: FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning) {
            throw RuntimeException("Ikke tilgang til å hente arbeidsforhold", e)
        } catch (e: FinnArbeidsforholdPrArbeidstakerUgyldigInput) {
            throw RuntimeException("Ugyldig input til hent arbeidsforhold", e)
        }
    }
}
