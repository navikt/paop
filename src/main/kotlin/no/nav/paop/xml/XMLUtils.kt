package no.nav.paop.xml

import no.kith.xmlstds.dialog._2006_10_11.XMLDialogmelding
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.model.arena.brev.FagType
import no.nav.model.arena.brev.FellesType
import no.nav.model.arena.brevdata.Brevdata
import no.nav.model.arenaBrevTilArbeidsgiver.ArenaBrevTilArbeidsgiver
import no.nav.model.arenaOppfolging.ArenaOppfolgingPlan
import no.nav.model.dataBatch.DataBatch
import no.nav.model.navOppfPlan.OppfolgingsplanMetadata
import no.trygdeetaten.xml.eiff._1.XMLEIFellesformat
import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import javax.xml.datatype.DatatypeFactory

val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance()

val dataBatchJaxBContext: JAXBContext = JAXBContext.newInstance(DataBatch::class.java)
val dataBatchUnmarshaller: Unmarshaller = dataBatchJaxBContext.createUnmarshaller()

val oppfolgingsplanMetadataJaxBContext: JAXBContext = JAXBContext.newInstance(OppfolgingsplanMetadata::class.java)
val oppfolgingsplanMetadataUnmarshaller: Unmarshaller = oppfolgingsplanMetadataJaxBContext.createUnmarshaller()

val arenaEiaInfoJaxBContext: JAXBContext = JAXBContext.newInstance(ArenaOppfolgingPlan::class.java)
val arenaMarshaller: Marshaller = arenaEiaInfoJaxBContext.createMarshaller()

val arenabrevnfoJaxBContext: JAXBContext = JAXBContext.newInstance(ArenaBrevTilArbeidsgiver::class.java)
val arenabrevMarshaller: Marshaller = arenabrevnfoJaxBContext.createMarshaller()

val arenabrevdataJaxBContext: JAXBContext = JAXBContext.newInstance(Brevdata::class.java, FellesType::class.java, FagType::class.java)
val arenabrevdataMarshaller: Marshaller = arenabrevdataJaxBContext.createMarshaller()

val eIFellesformatJaxBContext: JAXBContext = JAXBContext.newInstance(XMLEIFellesformat::class.java, XMLMsgHead::class.java, XMLDialogmelding::class.java)
val eIFellesformatMarshaller: Marshaller = eIFellesformatJaxBContext.createMarshaller()

fun extractDataBatch(dataBatchString: String): DataBatch =
        dataBatchUnmarshaller.unmarshal(StringReader(dataBatchString)) as DataBatch

fun extractNavOppfPlan(formdataString: String): OppfolgingsplanMetadata {
    return oppfolgingsplanMetadataUnmarshaller.unmarshal(StringReader(formdataString)) as OppfolgingsplanMetadata
}

fun Marshaller.toString(input: Any): String = StringWriter().use {
    marshal(input, it)
    it.toString()
}
