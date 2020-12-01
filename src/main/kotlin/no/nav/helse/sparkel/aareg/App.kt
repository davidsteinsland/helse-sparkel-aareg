package no.nav.helse.sparkel.aareg

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.*

val httpTraceLog: Logger = LoggerFactory.getLogger("tjenestekall")

val objectMapper: ObjectMapper = jacksonObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .registerModule(JavaTimeModule())

fun main() {
    val environment = setUpEnvironment()
    val app = createApp(environment)
    app.start()
}

internal fun createApp(environment: Environment): RapidsConnection {
    val stsClientWs = stsClient(env.securityTokenServiceEndpointUrl,
        env.securityTokenUsername to env.securityTokenPassword)

    val stsClientRest = StsRestClient(
        env.stsRestUrl, env.securityTokenUsername, env.securityTokenPassword)

    val wsClients = WsClients(
        stsClientWs = stsClientWs,
        stsClientRest = stsClientRest,
        callIdGenerator = callIdGenerator::get
    )

    val arbeidsforholdClient = wsClients.arbeidsforhold(environment.aaregBaseUrl)

    val httpClient = HttpClient()
    val kodeverkClient = KodeverkClient(httpClient, environment)
    val arbeidsforholdClient = ArbeidsforholdClient(ArbeidsforholdV3())
    val organisasjonClient = OrganisasjonClient()
    val behovløser = Behovløser()
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
                call.respond(HttpStatusCode.OK)
            }
        }
    }.build()
}
