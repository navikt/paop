package no.nav.paop.extractor

import java.io.StringReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader


class DataBatchExtractor(xmlFile: String) {

    private val xRDBatch: XMLStreamReader = XMLInputFactory
            .newFactory().createXMLStreamReader(StringReader(xmlFile))
    private enum class XType { ELEM, CDATA, ATTACH }


}