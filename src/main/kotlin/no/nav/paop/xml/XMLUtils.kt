package no.nav.paop.xml

import java.io.StringWriter
import javax.xml.bind.Marshaller

fun Marshaller.toString(input: Any): String = StringWriter().use {
    marshal(input, it)
    it.toString()
}
