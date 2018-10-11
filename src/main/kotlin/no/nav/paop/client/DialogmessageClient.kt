package no.nav.paop.client

import no.kith.xmlstds.XMLCV
import no.kith.xmlstds.base64container.XMLBase64Container
import no.kith.xmlstds.dialog._2006_10_11.XMLNotat
import no.kith.xmlstds.dialog._2006_10_11.XMLPerson
import no.kith.xmlstds.dialog._2006_10_11.XMLRollerRelatertNotat
import no.kith.xmlstds.msghead._2006_05_24.XMLCS
import no.kith.xmlstds.msghead._2006_05_24.XMLDocument
import no.kith.xmlstds.msghead._2006_05_24.XMLHealthcareProfessional
import no.kith.xmlstds.msghead._2006_05_24.XMLIdent
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgInfo
import no.kith.xmlstds.msghead._2006_05_24.XMLOrganisation
import no.kith.xmlstds.msghead._2006_05_24.XMLPatient
import no.kith.xmlstds.msghead._2006_05_24.XMLReceiver
import no.kith.xmlstds.msghead._2006_05_24.XMLRefDoc
import no.kith.xmlstds.msghead._2006_05_24.XMLSender
import no.kith.xmlstds.msghead._2006_05_24.XMLTS
import no.nav.emottak.schemas.PartnerInformasjon
import no.nav.paop.model.IncomingMetadata
import no.nav.paop.model.IncomingUserInfo
import no.trygdeetaten.xml.eiff._1.XMLEIFellesformat
import no.trygdeetaten.xml.eiff._1.XMLMottakenhetBlokk
import java.time.LocalDateTime
import java.util.UUID

fun createDialogmelding(
    incomingMetadata: IncomingMetadata,
    incomingPersonInfo: IncomingUserInfo,
    gpOrganizationName: String,
    gpOrganizationNumber: String,
    herIDAdresseregister: Int,
    fagmelding: ByteArray,
    canReceiveDialogMessage: PartnerInformasjon,
    gpfirstname: String,
    gpMiddelName: String?,
    gpLastname: String,
    gpHerIdFlr: Int,
    gpFnr: String,
    gpHprNumber: Int
): XMLEIFellesformat = XMLEIFellesformat().apply {
    any.add(XMLMsgHead().apply {
        msgInfo = XMLMsgInfo().apply {
            type = XMLCS().apply {
                v = "DIALOG_NOTAT"
                dn = "Notat"
            }
            miGversion = "v1.2 2006-05-24"
            genDate = LocalDateTime.now()
            msgId = UUID.randomUUID().toString()
            ack = XMLCS().apply {
                dn = "Ja"
                v = "J"
            }
            sender = XMLSender().apply {
                organisation = XMLOrganisation().apply {
                    organisationName = "NAV"
                    ident.add(XMLIdent().apply {
                        id = "889640782"
                        typeId = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                            dn = "Organisasjonsnummeret i Enhetsregisteret"
                            s = "2.16.578.1.12.4.1.1.9051"
                            v = "ENH"
                        }
                    })
                    ident.add(XMLIdent().apply {
                        id = "79768"
                        typeId = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                            dn = "Identifikator fra Helsetjenesteenhetsregisteret (HER-id)"
                            s = "2.16.578.1.12.4.1.1.9051"
                            v = "HER"
                        }
                    })
                }
            }
            receiver = XMLReceiver().apply {
                organisation = XMLOrganisation().apply {
                    organisationName = gpOrganizationName
                    ident.add(XMLIdent().apply {
                        id = herIDAdresseregister.toString()
                        typeId = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                            dn = "HER-id"
                            s = "2.16.578.1.12.4.1.1.9051"
                            v = "HER"
                        }
                    })
                    ident.add(XMLIdent().apply {
                        id = gpOrganizationNumber
                        typeId = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                            dn = "Organisasjonsnummeret i Enhetsregisteret"
                            s = "2.16.578.1.12.4.1.1.9051"
                            v = "ENH"
                        }
                    })
                    healthcareProfessional = XMLHealthcareProfessional().apply {
                        roleToPatient = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                            v = "6"
                            s = "2.16.578.1.12.4.1.1.9034"
                            dn = "Fastlege"
                        }
                        familyName = gpLastname
                        givenName = gpfirstname
                        if (gpMiddelName != null) {
                            middleName = gpMiddelName
                        }
                        ident.add(XMLIdent().apply {
                            id = gpHerIdFlr.toString()
                            typeId = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                                dn = "HER-id"
                                s = "2.16.578.1.12.4.1.1.8116"
                                v = "HER"
                            }
                        })
                        ident.add(XMLIdent().apply {
                            id = gpFnr
                            typeId = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                                dn = "Fødselsnummer Norsk fødselsnummer"
                                s = "2.16.578.1.12.4.1.1.8116"
                                v = "FNR"
                            }
                        })
                        ident.add(XMLIdent().apply {
                            id = gpHprNumber.toString()
                            typeId = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                                dn = "HPR-nummer"
                                s = "2.16.578.1.12.4.1.1.8116"
                                v = "HPR"
                            }
                        })
                    }
                }
            }
            patient = XMLPatient().apply {
                familyName = incomingPersonInfo.userFamilyName
                givenName = incomingPersonInfo.userGivenName
                ident.add(XMLIdent().apply {
                    id = incomingPersonInfo.userPersonNumber
                    typeId = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                        dn = "Fødselsnummer"
                        s = "2.16.578.1.12.4.1.1.8116"
                        v = "FNR"
                    }
                })
            }
        }
        document.add(XMLDocument().apply {
            documentConnection = XMLCS().apply {
                dn = "Hoveddokument"
                v = "H"
            }
            refDoc = XMLRefDoc().apply {
                issueDate = XMLTS().apply {
                    v = "${LocalDateTime.now().year}-${LocalDateTime.now().monthValue}-${LocalDateTime.now().dayOfMonth}"
                }
                msgType = XMLCS().apply {
                    dn = "XML-instans"
                    v = "XML"
                }
                mimeType = "text/xml"
                content = XMLRefDoc.Content().apply {
                    any.add(no.kith.xmlstds.dialog._2006_10_11.XMLDialogmelding().apply {
                        notat.add(XMLNotat().apply {
                            temaKodet = XMLCV().apply {
                                dn = "Oppfølgingsplan"
                                s = "2.16.578.1.12.4.1.1.8127"
                                v = "1"
                            }
                            tekstNotatInnhold = XMLNotat().apply {
                                tekstNotatInnhold = "Åpne PDF-vedlegg"
                                dokIdNotat = incomingMetadata.archiveReference
                            }
                            rollerRelatertNotat.add(XMLRollerRelatertNotat().apply {
                                rolleNotat = XMLCV().apply {
                                    v = "1"
                                    s = "2.16.578.1.12.4.1.1.9057"
                                }
                                person = XMLPerson()
                            })
                        })
                    })
                }
            }
        })

        document.add(XMLDocument().apply {
            documentConnection = XMLCS().apply {
                dn = "Vedlegg"
                v = "V"
            }
            refDoc = XMLRefDoc().apply {
                id = incomingMetadata.archiveReference
                issueDate = XMLTS().apply {
                    v = "${LocalDateTime.now().year}-${LocalDateTime.now().monthValue}-${LocalDateTime.now().dayOfMonth}"
                }
                msgType = XMLCS().apply {
                    dn = "Vedlegg"
                    v = "A"
                }
                mimeType = "application/pdf"
                content = XMLRefDoc.Content().apply {
                    any.add(XMLBase64Container().apply {
                        value = fagmelding
                    })
                }
            }
        })
    })
    any.add(XMLMottakenhetBlokk().apply {
        ebAction = "Plan"
        ebRole = "Saksbehandler"
        ebService = "Oppfolgingsplan"
        partnerReferanse = canReceiveDialogMessage.partnerID
    })
}
