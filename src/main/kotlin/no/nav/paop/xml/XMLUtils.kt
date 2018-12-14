package no.nav.paop.xml

import no.kith.xmlstds.base64container.XMLBase64Container
import no.kith.xmlstds.dialog._2006_10_11.XMLDialogmelding
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.model.dataBatch.DataBatch
import no.trygdeetaten.xml.eiff._1.XMLEIFellesformat
import no.trygdeetaten.xml.eiff._1.XMLMottakenhetBlokk
import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import javax.xml.datatype.DatatypeFactory

val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance()

val dataBatchJaxBContext: JAXBContext = JAXBContext.newInstance(DataBatch::class.java)
val dataBatchUnmarshaller: Unmarshaller = dataBatchJaxBContext.createUnmarshaller()

val eIFellesformatJaxBContext: JAXBContext = JAXBContext.newInstance(XMLEIFellesformat::class.java, XMLMsgHead::class.java, XMLDialogmelding::class.java, XMLBase64Container::class.java, XMLMottakenhetBlokk::class.java)
val eIFellesformatMarshaller: Marshaller = eIFellesformatJaxBContext.createMarshaller()

fun extractDataBatch(dataBatchString: String): DataBatch =
        dataBatchUnmarshaller.unmarshal(StringReader(dataBatchString)) as DataBatch

fun Marshaller.toString(input: Any): String = StringWriter().use {
    marshal(input, it)
    it.toString()
}
