package no.nav.helse.sparkel.aareg

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.CallId
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.callIdMdc
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.request.path
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import org.apache.cxf.ws.security.trust.STSClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.time.LocalDate
import java.util.UUID
import javax.xml.namespace.QName

val httpTraceLog: Logger = LoggerFactory.getLogger("tjenestekall")

val objectMapper: ObjectMapper = jacksonObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .registerModule(JavaTimeModule())

fun main() {
    val environment = setUpEnvironment()
    val serviceUser = readServiceUserCredentials()
    val app = createApp(environment, serviceUser)
    app.start()
}

internal fun createApp(environment: Environment, serviceUser: ServiceUser): RapidsConnection {
    val stsClientWs = stsClient(environment.stsSoapBaseUrl, serviceUser)

    val organisasjonV5 = setupOrganisasjonV5(environment.organisasjonBaseUrl, stsClientWs)
    val arbeidsforholdV3 = setupArbeidsforholdV3(environment.aaregBaseUrl, stsClientWs)

    val httpClient = HttpClient {
        install(JsonFeature) { serializer = JacksonSerializer() }
    }
    val kodeverkClient = KodeverkClient(
        httpClient = httpClient,
        kodeverkBaseUrl = environment.kodeverkBaseUrl,
        appName = environment.appName
    )

    val arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3, kodeverkClient)
    val organisasjonClient = OrganisasjonClient(organisasjonV5, kodeverkClient)

    val behovløser = Behovløser(arbeidsforholdClient, organisasjonClient)

    return RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(environment.raw)).withKtorModule {
        install(CallId) {
            generate {
                UUID.randomUUID().toString()
            }
        }
        install(CallLogging) {
            logger = httpTraceLog
            level = Level.INFO
            callIdMdc("callId")
            filter { call -> call.request.path().startsWith("/api/") }
        }
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }
        routing {
            get("/api/test") {
                val behov = call.receive<Behov>()
                val message = behovløser.løsBehov(
                    aktørId = behov.aktørId,
                    fom = behov.fom,
                    tom = behov.tom,
                    organisasjonsnummer = behov.organisasjonsnummer
                )
                call.respond(message)
            }
        }
    }.build()
}

data class Behov(
    val aktørId: String, val fom: LocalDate, val tom: LocalDate, val organisasjonsnummer: String
)

private val callIdGenerator: ThreadLocal<String> = ThreadLocal.withInitial {
    UUID.randomUUID().toString()
}

fun setupOrganisasjonV5(organisasjonBaseUrl: String, stsClientWs: STSClient): OrganisasjonV5 =
    JaxWsProxyFactoryBean().apply {
        address = organisasjonBaseUrl
        wsdlURL = "wsdl/no/nav/tjeneste/virksomhet/organisasjon/v5/Binding.wsdl"
        serviceName = QName("http://nav.no/tjeneste/virksomhet/organisasjon/v5/Binding", "Organisasjon_v5")
        endpointName = QName("http://nav.no/tjeneste/virksomhet/organisasjon/v5/Binding", "Organisasjon_v5Port")
        serviceClass = OrganisasjonV5::class.java
        this.features.addAll(listOf(WSAddressingFeature(), LoggingFeature()))
        this.outInterceptors.addAll(listOf(CallIdInterceptor(callIdGenerator::get)))
    }.create(OrganisasjonV5::class.java).apply { stsClientWs.configureFor(this) }

fun setupArbeidsforholdV3(arbeidsforholdBaseUrl: String, stsClientWs: STSClient): ArbeidsforholdV3 =
    JaxWsProxyFactoryBean().apply {
        address = arbeidsforholdBaseUrl
        wsdlURL = "wsdl/no/nav/tjeneste/virksomhet/arbeidsforhold/v3/Binding.wsdl"
        serviceName = QName("http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/Binding", "Arbeidsforhold_v3")
        endpointName = QName("http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/Binding", "Arbeidsforhold_v3Port")
        serviceClass = ArbeidsforholdV3::class.java
        this.features.addAll(listOf(WSAddressingFeature(), LoggingFeature()))
        this.outInterceptors.addAll(listOf(CallIdInterceptor(callIdGenerator::get)))
    }.create(ArbeidsforholdV3::class.java).apply { stsClientWs.configureFor(this) }
