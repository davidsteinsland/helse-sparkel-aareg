package no.nav.helse.sparkel.aareg

import arrow.core.Either
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.NorskIdent
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Periode
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Regelverker
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory

class AaregClient(private val arbeidsforholdV3: ArbeidsforholdV3) {
    suspend fun finnArbeidsforhold(
        aktørId: String,
        fom: LocalDate,
        tom: LocalDate
    ): Either<Throwable, List<Arbeidsforhold>> = Either.catch {
        arbeidsforholdV3.finnArbeidsforholdPrArbeidstaker(
            hentArbeidsforholdRequest(aktørId, fom, tom)
        ).arbeidsforhold.toList()
    }


    private fun hentArbeidsforholdRequest(aktørId: String, fom: LocalDate, tom: LocalDate) =
        FinnArbeidsforholdPrArbeidstakerRequest().apply {
            ident = NorskIdent().apply {
                ident = aktørId
            }
            arbeidsforholdIPeriode = Periode().apply {
                this.fom = fom.toXmlGregorianCalendar()
                this.tom = tom.toXmlGregorianCalendar()
            }
            rapportertSomRegelverk = Regelverker().apply {
                value = RegelverkerValues.A_ORDNINGEN.name
                kodeRef = RegelverkerValues.A_ORDNINGEN.name
            }
        }

    private val datatypeFactory = DatatypeFactory.newInstance()

    private fun LocalDate.toXmlGregorianCalendar() = this.let {
        val gcal = GregorianCalendar.from(this.atStartOfDay(ZoneOffset.UTC))
        datatypeFactory.newXMLGregorianCalendar(gcal)
    }

    private enum class RegelverkerValues {
        FOER_A_ORDNINGEN, A_ORDNINGEN, ALLE
    }
}
