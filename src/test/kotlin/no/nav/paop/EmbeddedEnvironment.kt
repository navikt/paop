package no.nav.paop

import io.ktor.application.call
import io.ktor.content.PartData
import io.ktor.http.ContentType
import io.ktor.request.receiveMultipart
import io.ktor.request.receiveText
import io.ktor.response.respondBytes
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.runBlocking
import net.logstash.logback.argument.StructuredArguments
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.common.KafkaEnvironment
import no.nav.emottak.schemas.HentPartnerIDViaOrgnummerResponse
import no.nav.emottak.schemas.PartnerResource
import no.nav.paop.client.PdfClient
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentResponse
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonResponse
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.LagreDokumentOgOpprettJournalpostResponse
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.v1.binding.Journalbehandling
import no.nhn.adresseregisteret.ICommunicationPartyService
import no.nhn.adresseregisteret.OrganizationPerson
import no.nhn.schemas.reg.flr.IFlrReadOperations
import no.nhn.schemas.reg.flr.PatientToGPContractAssociation
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl
import org.apache.activemq.artemis.core.server.ActiveMQServer
import org.apache.activemq.artemis.core.server.ActiveMQServers
import org.apache.commons.io.IOUtils
import org.apache.cxf.BusFactory
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.transport.http.HTTPConduit
import org.apache.cxf.transport.servlet.CXFNonSpringServlet
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy
import org.apache.cxf.ws.addressing.WSAddressingFeature
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import javax.jms.Connection
import javax.jms.ConnectionFactory
import javax.jms.MessageProducer
import javax.jms.Session
import javax.naming.InitialContext
import javax.xml.ws.Endpoint

interface PdfProvider {
    fun getPDF(postBody: String): ByteArray
}

class EmbeddedEnvironment {
    val fastlegeregisterMock: IFlrReadOperations = mock(IFlrReadOperations::class.java)
    val organisasjonV4Mock: OrganisasjonV4 = mock(OrganisasjonV4::class.java)
    val journalbehandlingMock: Journalbehandling = mock(Journalbehandling::class.java)
    val dokumentProduksjonV3Mock: DokumentproduksjonV3 = mock(DokumentproduksjonV3::class.java)
    val adresseRegisterV1Mock: ICommunicationPartyService = mock(ICommunicationPartyService::class.java)
    val partnerEmottakMock: PartnerResource = mock(PartnerResource::class.java)
    val pdfGenMock: PdfProvider = mock(PdfProvider::class.java)
    val diagnosisWebServerPort = randomPort()
    val diagnosisWebServerUrl = "http://localhost:$diagnosisWebServerPort"

    private val wsMockPort = randomPort()
    val wsBaseUrl = "http://localhost:$wsMockPort"
    private val mockHttpServerPort = randomPort()
    private val mockHttpServerUrl = "http://localhost:$mockHttpServerPort"

    private lateinit var activeMQServer: ActiveMQServer
    private lateinit var connectionFactory: ConnectionFactory
    private lateinit var queueConnection: Connection
    private lateinit var initialContext: InitialContext

    private lateinit var server: Server

    private lateinit var diagnosisWebserver: ApplicationEngine
    private lateinit var mockWebserver: ApplicationEngine

    private lateinit var producer: MessageProducer
    private lateinit var job: Job

    private lateinit var session: Session

    private val embeddedKafkaEnvironment: KafkaEnvironment = KafkaEnvironment(
            autoStart = false,
            topics = listOf("aapen-kafka-topic")
    )

    fun start() {
        activeMQServer = ActiveMQServers.newActiveMQServer(ConfigurationImpl()
                .setPersistenceEnabled(false)
                .setJournalDirectory("target/data/journal")
                .setSecurityEnabled(false)
                .addAcceptorConfiguration("invm", "vm://0"))
        activeMQServer.start()
        initialContext = InitialContext()
        connectionFactory = initialContext.lookup("ConnectionFactory") as ConnectionFactory

        server = createJettyServer()
        mockWebserver = createHttpMock()

        val env = Environment(
                kafkaBootstrapServersURL = embeddedKafkaEnvironment.brokersURL
        )

        embeddedKafkaEnvironment.start()
        val consumer = KafkaConsumer<String, ExternalAttachment>(readConsumerConfig(env).apply {
            remove("security.protocol")
            remove("sasl.mechanism")
        })

        val fastlegeregisteretClient = JaxWsProxyFactoryBean().apply {
            address = "$wsBaseUrl/ws/flr"
            features.add(LoggingFeature())
            serviceClass = IFlrReadOperations::class.java
        }.create() as IFlrReadOperations
        configureTimeout(fastlegeregisteretClient)

        val organisasjonV4Client = JaxWsProxyFactoryBean().apply {
            address = "$wsBaseUrl/ws/org"
            features.add(LoggingFeature())
            serviceClass = OrganisasjonV4::class.java
        }.create() as OrganisasjonV4
        configureTimeout(organisasjonV4Client)

        val journalbehandlingClient = JaxWsProxyFactoryBean().apply {
            address = "$wsBaseUrl/ws/jor"
            features.add(LoggingFeature())
            serviceClass = Journalbehandling::class.java
        }.create() as Journalbehandling
        configureTimeout(journalbehandlingClient)

        val dokumentProduksjonV3Client = JaxWsProxyFactoryBean().apply {
            address = "$wsBaseUrl/ws/dok"
            features.add(LoggingFeature())
            serviceClass = DokumentproduksjonV3::class.java
        }.create() as DokumentproduksjonV3
        configureTimeout(dokumentProduksjonV3Client)

        val adresseRegisterV1Client = JaxWsProxyFactoryBean().apply {
            address = "$wsBaseUrl/ws/adr"
            features.add(LoggingFeature())
            serviceClass = ICommunicationPartyService::class.java
        }.create() as ICommunicationPartyService
        configureTimeout(adresseRegisterV1Client)

        val partnerEmottakClient = JaxWsProxyFactoryBean().apply {
            address = "$wsBaseUrl/ws/emo"
            features.add(LoggingFeature())
            serviceClass = PartnerResource::class.java
        }.create() as PartnerResource
        configureTimeout(partnerEmottakClient)

        val iCorrespondenceAgencyExternalBasic = JaxWsProxyFactoryBean().apply {
            address = env.adresseregisteretV1EmottakEndpointURL
            features.add(LoggingFeature())
            features.add(WSAddressingFeature())
            serviceClass = ICorrespondenceAgencyExternalBasic::class.java
        }.create() as ICorrespondenceAgencyExternalBasic
        configureTimeout(iCorrespondenceAgencyExternalBasic)

        val pdfClient = PdfClient("$mockHttpServerUrl/create_pdf")

        queueConnection = connectionFactory.createConnection()
        queueConnection.start()
        session = queueConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        val arenaQueue = session.createQueue("arena_queue")
        val receiptQueue = session.createQueue("emottak_queue")

        diagnosisWebserver = createHttpServer(diagnosisWebServerPort, "TEST")

        val arenaProducer = session.createProducer(arenaQueue)
        val receiptProducer = session.createProducer(receiptQueue)

        job = listen(pdfClient, journalbehandlingClient, fastlegeregisteretClient, organisasjonV4Client,
                dokumentProduksjonV3Client, adresseRegisterV1Client, partnerEmottakClient, iCorrespondenceAgencyExternalBasic, arenaProducer, receiptProducer, session, consumer, consumer, "brukernavn", "passord")
    }

    private fun configureTimeout(service: Any) {
        val client = ClientProxy.getClient(service)
        val conduit = client.getConduit() as HTTPConduit
        val httpClientPolicy = HTTPClientPolicy()
        httpClientPolicy.connectionTimeout = 1000
        httpClientPolicy.receiveTimeout = 1000
        conduit.client = httpClientPolicy
    }

    fun resetMocks() {
        Mockito.reset(fastlegeregisterMock, organisasjonV4Mock, journalbehandlingMock, dokumentProduksjonV3Mock, adresseRegisterV1Mock, partnerEmottakMock, pdfGenMock)
    }

    fun shutdown() {
        embeddedKafkaEnvironment.stop()
        activeMQServer.stop(true)
        runBlocking {
            job.cancel()
            job.join()
        }
        CollectorRegistry.defaultRegistry.clear()
    }

    fun defaultMocks() {
        log.info("Setting up mocks")
        Mockito.`when`(fastlegeregisterMock.getPatientGPDetails(ArgumentMatchers.any())).thenReturn(PatientToGPContractAssociation())

        Mockito.`when`(organisasjonV4Mock.hentOrganisasjon(ArgumentMatchers.any())).thenReturn(HentOrganisasjonResponse())

        Mockito.`when`(journalbehandlingMock.lagreDokumentOgOpprettJournalpost(ArgumentMatchers.any())).thenReturn(LagreDokumentOgOpprettJournalpostResponse())

        Mockito.`when`(dokumentProduksjonV3Mock.produserIkkeredigerbartDokument(ArgumentMatchers.any())).thenReturn(ProduserIkkeredigerbartDokumentResponse())

        Mockito.`when`(adresseRegisterV1Mock.getOrganizationPersonDetails(ArgumentMatchers.any())).thenReturn(OrganizationPerson())

        Mockito.`when`(partnerEmottakMock.hentPartnerIDViaOrgnummer(ArgumentMatchers.any())).thenReturn(HentPartnerIDViaOrgnummerResponse())

        Mockito.`when`(pdfGenMock.getPDF(ArgumentMatchers.anyString())).thenReturn("Sample PDF".toByteArray(Charsets.UTF_8))
    }

    fun produceMessage(message: String) {
        val textMessage = session.createTextMessage(message)
        producer.send(textMessage)
        log.info("Sending: {}", StructuredArguments.keyValue("message", message))
        log.info("Pushed message to queue")
    }

    private fun createHttpMock(): ApplicationEngine = embeddedServer(Netty, mockHttpServerPort) {
            routing {
                post("/create_pdf/v1/genpdf/pale/{pdfType}") {
                    call.respondBytes(pdfGenMock.getPDF(call.receiveText()), ContentType.parse("application/pdf"))
                }

                post("/input") {
                    val multipart = call.receiveMultipart()
                    log.info("Received input {}", multipart)
                    while (true) {
                        val part = multipart.readPart() ?: break
                        when (part) {
                            is PartData.FileItem -> {
                                val stream = part.streamProvider()
                                val string = IOUtils.toString(stream, Charsets.ISO_8859_1)
                                produceMessage(string)
                            }
                        }
                    }
                }
            }
        }.start()

    private fun createJettyServer(): Server = Server(wsMockPort).apply {
        val soapServlet = CXFNonSpringServlet()

        val servletHandler = ServletContextHandler()
        servletHandler.addServlet(ServletHolder(soapServlet), "/ws/*")
        handler = servletHandler
        start()

        BusFactory.setDefaultBus(soapServlet.bus)
        Endpoint.publish("/flr", fastlegeregisterMock)
        Endpoint.publish("/org", organisasjonV4Mock)
        Endpoint.publish("/jor", journalbehandlingMock)
        Endpoint.publish("/dok", dokumentProduksjonV3Mock)
        Endpoint.publish("/adr", adresseRegisterV1Mock)
        Endpoint.publish("/emo", partnerEmottakMock)
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger("paop-it")

        @JvmStatic
        fun main(args: Array<String>) {
            val embeddedEnvironment = EmbeddedEnvironment()

            log.info("Diagnosis available at: {}", embeddedEnvironment.diagnosisWebServerUrl)
            log.info("Mock and input available at: {}", embeddedEnvironment.mockHttpServerUrl)

            Runtime.getRuntime().addShutdownHook(Thread {
                embeddedEnvironment.shutdown()
            })
        }
    }
    fun createKafkaMessage(topicName: String, message: String) {
        val embeddedEnvironment = KafkaEnvironment(
                autoStart = false,
                topics = listOf(topicName)
        )
        val env = Environment(
                kafkaBootstrapServersURL = embeddedEnvironment.brokersURL
        )
        val producer = KafkaProducer<String, String>(readProducerConfig(env, StringSerializer::class).apply {
            remove("security.protocol")
            remove("sasl.mechanism")
        })

        producer.send(ProducerRecord(topicName, message))
        log.info("Pushed message to kafka topic ")
    }

    fun readKafkaMessage(topicName: String): List<ConsumerRecord<String, String>> {
        val embeddedEnvironment = KafkaEnvironment(
                autoStart = false,
                topics = listOf(topicName)
        )
        val env = Environment(
                kafkaBootstrapServersURL = embeddedEnvironment.brokersURL
        )
        val consumer = KafkaConsumer<String, String>(readConsumerConfig(env).apply {
            remove("security.protocol")
            remove("sasl.mechanism")
        })
        consumer.subscribe(listOf(topicName))
        return consumer.poll(Duration.ofMillis(10000)).toList()
    }
}