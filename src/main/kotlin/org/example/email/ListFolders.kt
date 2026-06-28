package org.example.email

import jakarta.mail.Folder
import jakarta.mail.MessagingException
import jakarta.mail.Session
import java.util.Properties

object ListFolders {
    @JvmStatic
    fun main(args: Array<String>) {
        val username = "info-bauschlosserei-kuhn@t-online.de"
        val password = "Lini+marviTkom"

        val props = Properties()
        props["mail.store.protocol"] = "imaps"

        try {
            val session = Session.getInstance(props, null)
            val store = session.store
            store.connect("secureimap.t-online.de", username, password)

            println("Erfolgreich verbunden. Verfuegbare Ordner:")
            listFolders(store.defaultFolder, "")
            store.close()
        } catch (e: MessagingException) {
            System.err.println("Fehler beim Auflisten der Ordner: ${e.message}")
            e.printStackTrace()
        }
    }

    @Throws(MessagingException::class)
    fun listFolders(folder: Folder, indent: String) {
        println("$indent-> ${folder.fullName}")

        if (folder.type and Folder.HOLDS_FOLDERS != 0) {
            for (subFolder in folder.list()) {
                listFolders(subFolder, "$indent  ")
            }
        }
    }
}
