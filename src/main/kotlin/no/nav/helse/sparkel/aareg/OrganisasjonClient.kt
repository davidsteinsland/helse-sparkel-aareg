package no.nav.helse.sparkel.aareg

import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentOrganisasjonRequest

class OrganisasjonClient(
    private val organisasjonV5: OrganisasjonV5,
    private val kodeverkClient: KodeverkClient
) {
    fun finnOrganisasjon(orgnr: String): OrganisasjonDto =
        organisasjonV5.hentOrganisasjon(hentOrganisasjonRequst(orgnr)).organisasjon.let { organisasjon ->
            OrganisasjonDto(
                navn = (organisasjon.navn as UstrukturertNavn).navnelinje.joinToString(),
                bransjer = organisasjon.organisasjonDetaljer.naering.map { næring ->
                    kodeverkClient.getNæring(næring.naeringskode.kodeRef)
                        .also { println("Kanskje naering: ${næring.naeringskode.value}") }
                }
            )
        }

    private fun hentOrganisasjonRequst(orgnr: String) =
        HentOrganisasjonRequest().apply {
            orgnummer = orgnr
            isInkluderHierarki = true
            isInkluderHistorikk = true
        }
}
