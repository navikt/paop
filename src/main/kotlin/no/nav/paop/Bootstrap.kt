package no.nav.paop

import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import com.ibm.msg.client.wmq.compat.base.internal.MQC
import io.ktor.application.Application
import io.ktor.client.HttpClient
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.emottak.schemas.PartnerResource
import no.nav.paop.client.createHttpClient
import no.nav.paop.model.ReceivedOppfolginsplan
import no.nav.paop.routes.handleAltinnFollowupPlan
import no.nav.paop.ws.configureBasicAuthFor
import no.nav.paop.ws.configureSTSFor
import no.nav.syfo.api.registerNaisApi
import no.nav.tjeneste.virksomhet.behandlejournal.v2.binding.BehandleJournalV2
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.binding.OrganisasjonEnhetV2
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nhn.adresseregisteret.ICommunicationPartyService
import no.nhn.schemas.reg.flr.IFlrReadOperations
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.jms.MessageProducer
import javax.jms.Session

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

val log: Logger = LoggerFactory.getLogger("nav.paop-application")

fun main(args: Array<String>) = runBlocking(Executors.newFixedThreadPool(2).asCoroutineDispatcher()) {
    DefaultExports.initialize()
    val env = Environment()
    val applicationState = ApplicationState()

    val applicationServer = embeddedServer(Netty, env.applicationPort) {
        initRouting(applicationState)
    }.start(wait = false)

    connectionFactory(env).createConnection(env.mqUsername, env.mqPassword).use { connection ->
        connection.start()
        try {
            val listeners = (1..env.applicationThreads).map {
                launch {
                    val session = connection.createSession()
                    val receiptQueue = session.createQueue(env.receiptQueueName)
                    val consumerProperties = readConsumerConfig(env)
                    val altinnConsumer = KafkaConsumer<String, ExternalAttachment>(consumerProperties)
                    altinnConsumer.subscribe(listOf(env.kafkaIncommingTopicOppfolginsplan))

                    val producerProperties = readProducerConfig(env, valueSerializer = JacksonKafkaSerializer::class)
                    val kafkaproducer = KafkaProducer<String, ReceivedOppfolginsplan>(producerProperties)

                    val fastlegeregisteret = JaxWsProxyFactoryBean().apply {
                        address = env.fastlegeregiserHdirURL
                        features.add(LoggingFeature())
                        features.add(WSAddressingFeature())
                        serviceClass = IFlrReadOperations::class.java
                    }.create() as IFlrReadOperations
                    configureSTSFor(fastlegeregisteret, env.srvPaopUsername,
                            env.srvPaopPassword, env.securityTokenServiceUrl)

                    val organisasjonV4 = JaxWsProxyFactoryBean().apply {
                        address = env.organisasjonV4EndpointURL
                        features.add(LoggingFeature())
                        features.add(WSAddressingFeature())
                        serviceClass = OrganisasjonV4::class.java
                    }.create() as OrganisasjonV4
                    configureSTSFor(organisasjonV4, env.srvPaopUsername,
                            env.srvPaopPassword, env.securityTokenServiceUrl)

                    val behandleJournalV2 = JaxWsProxyFactoryBean().apply {
                        address = env.behandleJournalV2EndpointURL
                        features.add(LoggingFeature())
                        serviceClass = BehandleJournalV2::class.java
                    }.create() as BehandleJournalV2
                    configureSTSFor(behandleJournalV2, env.srvPaopUsername,
                            env.srvPaopPassword, env.securityTokenServiceUrl)

                    val adresseRegisterV1 = JaxWsProxyFactoryBean().apply {
                        address = env.adresseregisteretV1EmottakEndpointURL
                        features.add(LoggingFeature())
                        features.add(WSAddressingFeature())
                        serviceClass = ICommunicationPartyService::class.java
                    }.create() as ICommunicationPartyService
                    configureSTSFor(adresseRegisterV1, env.srvPaopUsername,
                            env.srvPaopPassword, env.securityTokenServiceUrl)

                    val partnerEmottak = JaxWsProxyFactoryBean().apply {
                        address = env.partnerEmottakEndpointURL
                        features.add(LoggingFeature())
                        features.add(WSAddressingFeature())
                        serviceClass = PartnerResource::class.java
                    }.create() as PartnerResource
                    configureBasicAuthFor(partnerEmottak, env.srvPaopUsername, env.srvPaopPassword)

                    val personV3 = JaxWsProxyFactoryBean().apply {
                        address = env.personV3EndpointURL
                        serviceClass = PersonV3::class.java
                    }.create() as PersonV3
                    configureSTSFor(personV3, env.srvPaopUsername,
                            env.srvPaopPassword, env.securityTokenServiceUrl)

                    val orgnaisasjonEnhet = JaxWsProxyFactoryBean().apply {
                        address = env.organisasjonEnhetV2EndpointURL
                        serviceClass = OrganisasjonEnhetV2::class.java
                    }.create() as OrganisasjonEnhetV2
                    configureSTSFor(orgnaisasjonEnhet, env.srvPaopUsername,
                            env.srvPaopPassword, env.securityTokenServiceUrl)

                    val receiptProducer = session.createProducer(receiptQueue)
                    val httpClient = createHttpClient()

                    blockingApplicationLogic(env, applicationState, httpClient, behandleJournalV2, fastlegeregisteret,
                            organisasjonV4, adresseRegisterV1, partnerEmottak, receiptProducer, session, altinnConsumer,
                            personV3, orgnaisasjonEnhet, kafkaproducer)
                }
            }.toList()

            applicationState.initialized = true

            Runtime.getRuntime().addShutdownHook(Thread {
                applicationServer.stop(10, 10, TimeUnit.SECONDS)
            })
            runBlocking { listeners.forEach { it.join() } }
        } finally {
            applicationState.running = false
        }
    }
}

fun connectionFactory(environment: Environment) = MQConnectionFactory().apply {
    hostName = environment.mqHostname
    port = environment.mqPort
    queueManager = environment.mqQueueManagerName
    transportType = WMQConstants.WMQ_CM_CLIENT
    // TODO mq crypo
    // sslCipherSuite = "TLS_RSA_WITH_AES_256_CBC_SHA"
    channel = environment.mqChannelName
    ccsid = 1208
    setIntProperty(WMQConstants.JMS_IBM_ENCODING, MQC.MQENC_NATIVE)
    setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208)
}

suspend fun blockingApplicationLogic(
    env: Environment,
    applicationState: ApplicationState,
    httpClient: HttpClient,
    behandleJournalV2: BehandleJournalV2,
    fastlegeregisteret: IFlrReadOperations,
    organisasjonV4: OrganisasjonV4,
    adresseRegisterV1: ICommunicationPartyService,
    partnerEmottak: PartnerResource,
    receiptProducer: MessageProducer,
    session: Session,
    consumer: KafkaConsumer<String, ExternalAttachment>,
    personV3: PersonV3,
    organisasjonEnhetV2: OrganisasjonEnhetV2,
    kafkaproducer: KafkaProducer<String, ReceivedOppfolginsplan>
) {
    while (applicationState.running) {
        consumer.poll(Duration.ofMillis(0)).forEach {
            handleAltinnFollowupPlan(env, it, httpClient, behandleJournalV2, fastlegeregisteret, organisasjonV4,
                    adresseRegisterV1, partnerEmottak, receiptProducer, session, personV3, organisasjonEnhetV2, kafkaproducer)
        }
        delay(100)
    }
}

fun Application.initRouting(applicationState: ApplicationState) {
    routing {
        registerNaisApi(
                readynessCheck = {
                    applicationState.initialized
                },
                livenessCheck = {
                    applicationState.running
                }
        )
    }
}