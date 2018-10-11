package no.nav.paop.client

import no.nav.model.arena.brev.AktivitetsType
import no.nav.model.arena.brev.BesoksadresseType
import no.nav.model.arena.brev.FagType
import no.nav.model.arena.brev.FellesType
import no.nav.model.arena.brev.KontaktInformasjonType
import no.nav.model.arena.brev.MoteInfoType
import no.nav.model.arena.brev.MottakerAdresseType
import no.nav.model.arena.brev.MottakerType
import no.nav.model.arena.brev.MottakerTypeKode
import no.nav.model.arena.brev.PostadresseType
import no.nav.model.arena.brev.ReturadresseType
import no.nav.model.arena.brev.SakspartType
import no.nav.model.arena.brev.SakspartTypeKode
import no.nav.model.arena.brev.SignerendeSaksbehandlerType
import no.nav.model.arena.brevdata.Brevdata
import no.nav.paop.log
import no.nav.paop.model.IncomingMetadata
import no.nav.paop.newInstance
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.util.GregorianCalendar
import javax.jms.MessageProducer
import javax.jms.Session
import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

fun createPhysicalLetter(
    dokumentProduksjonV3: DokumentproduksjonV3,
    arenaProducer: MessageProducer,
    session: Session,
    incomingMetadata: IncomingMetadata,
    receiverOrgNumber: String,
    gpName: String,
    postnummer: String,
    poststed: String,
    xmlContent: String
) {
    val brevrequest = createProduserIkkeredigerbartDokumentRequest(incomingMetadata, receiverOrgNumber, gpName, postnummer, poststed, xmlContent)
    try {
        dokumentProduksjonV3.produserIkkeredigerbartDokument(brevrequest)
        // TODO do we need to tell ARENA that the letter is sendt?
        letterSentNotificationToArena(arenaProducer, session, incomingMetadata)
    } catch (e: Exception) {
        log.error("Call to dokprod returned Exception", e)
    }
}

val documentBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().let {
    it.isNamespaceAware = true
    it.newDocumentBuilder()
}
fun wrapFormData(formData: String): Element = documentBuilder.parse(InputSource(StringReader(formData))).documentElement

fun createArenaBrevdata(): Brevdata = Brevdata().apply {
    // TODO this is only TMP
    content.add(JAXBElement(QName("felles"), FellesType::class.java, FellesType().apply {
        spraakkode = "NB"
        fagsaksnummer = "2014122950"
        signerendeSaksbehandler = SignerendeSaksbehandlerType().apply {
            signerendeSaksbehandlerNavn = "Sagne Sakbehandler"
        }
        sakspart = SakspartType().apply {
            sakspartId = "01010112345".toLong()
            sakspartTypeKode = SakspartTypeKode.PERSON
            sakspartNavn = "Liv Mona Olsen"
        }
        mottaker = MottakerType().apply {
            mottakerId = "01010112345".toLong()
            mottakerTypeKode = MottakerTypeKode.PERSON
            mottakerNavn = "Liv Mona Olsen"
            mottakerAdresse = MottakerAdresseType().apply {
                adresselinje1 = "Rolfsbuktalleen 7"
                adresselinje2 = "Oslo"
                adresselinje3 = "Moss"
                postNr = 1364
                poststed = "FORNEBU"
                land = "Norge"
            }
        }
        navnAvsenderEnhet = "Dagpenger Inn"
        kontaktInformasjon = KontaktInformasjonType().apply {
            kontaktTelefonnummer = "55 55 33 33"
            returadresse = ReturadresseType().apply {
                navEnhetsNavn = "Dagpenger Inn"
                adresselinje = "Postboks 6944 St.Olavs plass"
                postNr = "0130".toShort()
                poststed = "OSLO"
            }
            postadresse = PostadresseType().apply {
                navEnhetsNavn = "Dagpenger Inn"
                adresselinje = "Postboks 6944 St.Olavs plass"
                postNr = "0130".toShort()
                poststed = "OSLO"
            }
            besoksadresse = BesoksadresseType().apply {
                adresselinje = "Adresselinje"
                postNr = "0130"
                poststed = "OSLO"
            }
        }
    }))

    content.add(JAXBElement(QName("fag"), FagType::class.java, FagType().apply {
        aktivitetsNavn = "aktivitet"
        aktivitetsType = AktivitetsType.VGINT
        isSvarslipp = true
        moteInfo = MoteInfoType().apply {
            moteKontakt = "Kurt Kursholder"
            dato = newInstance.newXMLGregorianCalendar(GregorianCalendar())
            klokkeslett = "10:59"
            sted = "Oslo"
            fristDato = newInstance.newXMLGregorianCalendar(GregorianCalendar())
            brevTekst = "Brevtekst"
        }
        isVisReaksjon = true
    }))
}
