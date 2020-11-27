package no.nav.helse.sparkel.aareg

import arrow.core.left
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AaregClientTest {
    private val arbeidsforholdV3Mock = mockk<ArbeidsforholdV3>()
    private val aaregClient = AaregClient(arbeidsforholdV3Mock)

    @Test
    fun `håndterer sikkerhetsbegrensingfeil fra arbeidsforhold v3`() = runBlocking {
        val finnArbeidsforholdPrArbeidstakerSikkerhetsbegrensningException =
            FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning("", mockk())

        every { arbeidsforholdV3Mock.finnArbeidsforholdPrArbeidstaker(any()) } throws finnArbeidsforholdPrArbeidstakerSikkerhetsbegrensningException

        val resultat =
            aaregClient.finnArbeidsforhold(
                "aktørId",
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 2, 1)
            )

        assertEquals(finnArbeidsforholdPrArbeidstakerSikkerhetsbegrensningException.left(), resultat)
    }

    @Test
    fun `håndterer feil fra arbeidsforhold v3`() = runBlocking {
        val finnArbeidsforholdPrArbeidstakerUgyldigInputException =
            FinnArbeidsforholdPrArbeidstakerUgyldigInput("", mockk())

        every { arbeidsforholdV3Mock.finnArbeidsforholdPrArbeidstaker(any()) } throws finnArbeidsforholdPrArbeidstakerUgyldigInputException

        val resultat =
            aaregClient.finnArbeidsforhold(
                "aktørId",
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 2, 1)
            )

        assertEquals(finnArbeidsforholdPrArbeidstakerUgyldigInputException.left(), resultat)
    }

}
