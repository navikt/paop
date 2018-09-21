package no.nav.paop

import io.ktor.content.PartData
import io.ktor.http.ContentType
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.runBlocking
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.emottak.schemas.PartnerResource
import no.nav.paop.client.PdfClient
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.v1.binding.Journalbehandling
import no.nhn.adresseregisteret.ICommunicationPartyService
import no.nhn.schemas.reg.flr.IFlrReadOperations
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
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.StringReader
import javax.jms.Connection
import javax.jms.ConnectionFactory
import javax.jms.MessageConsumer
import javax.jms.MessageProducer
import javax.jms.Session
import javax.jms.TextMessage
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


        val pdfClient = PdfClient("$mockHttpServerUrl/create_pdf")


        queueConnection = connectionFactory.createConnection()
        queueConnection.start()
        session = queueConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        val inputQueue = session.createQueue("input_queue")
        val arenaQueue = session.createQueue("arena_queue")
        val apprecQueue = session.createQueue("apprec_queue")
        val backoutQueue = session.createQueue("backout_queue")

        diagnosisWebserver = createHttpServer(diagnosisWebServerPort, "TEST")

        producer = session.createProducer(inputQueue)

        job =  listen(PdfClient(env.pdfGeneratorURL), journalbehandling, fastlegeregisteret, organisasjonV4,
                dokumentProduksjonV3, adresseRegisterV1, partnerEmottak, arenaProducer, receiptProducer, session, consumer).join()

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
        reset(personV3Mock, organisasjonEnhetV2Mock, pdfGenMock)
    }


    fun shutdown() {
        activeMQServer.stop(true)
        runBlocking {
            job.cancel()
            job.join()
        }
        CollectorRegistry.defaultRegistry.clear()
    }

    fun defaultMocks(
        person: Person,
        doctor: Person? = defaultPerson(),
        navOffice: Organisasjonsenhet? = defaultNavOffice(),
        geografiskTilknytning: GeografiskTilknytning? = Kommune().withGeografiskTilknytning("navkontor"),
        samhandlerList: List<Samhandler> = listOfNotNull(doctor?.toSamhandler())
    ) {
        log.info("Setting up mocks")
        `when`(personV3Mock.hentPerson(any())).thenReturn(HentPersonResponse().withPerson(person))

        `when`(personV3Mock.hentGeografiskTilknytning(any())).thenReturn(HentGeografiskTilknytningResponse()
                .withAktoer(person.aktoer)
                .withNavn(person.personnavn)
                .withGeografiskTilknytning(geografiskTilknytning))

        `when`(organisasjonEnhetV2Mock.finnNAVKontor(any()))
                .thenReturn(FinnNAVKontorResponse().apply {
                    navKontor = navOffice
                })
        `when`(samhandlerMock.getSamhandlerList()).thenReturn(samhandlerList)
        `when`(pdfGenMock.getPDF(anyString())).thenReturn("Sample PDF".toByteArray(Charsets.UTF_8))
    }

    fun consumeMessage(consumer: MessageConsumer): String? = consumer.receive(2000).run {
        if (this == null) return null
        if (this !is TextMessage) throw RuntimeException("Got unexpected message type")
        println(this.text)
        this.text
    }

    fun produceMessage(message: String) {
        val textMessage = session.createTextMessage(message)
        producer.send(textMessage)
        log.info("Sending: {}", keyValue("message", message))
        log.info("Pushed message to queue")
    }

    fun produceMessage(fellesformat: EIFellesformat) =
            produceMessage(fellesformatJaxBContext.createMarshaller().toString(fellesformat))

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

                get("/rest/sar/samh") {
                    val ident = call.request.queryParameters["ident"]
                    call.respondJson {
                        samhandlerMock.getSamhandlerList()
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
        Endpoint.publish("/tps", personV3Mock)
        Endpoint.publish("/norg2", organisasjonEnhetV2Mock)
        Endpoint.publish("/joark", journalbehandlingMock)
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger("pale-it")

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
}
