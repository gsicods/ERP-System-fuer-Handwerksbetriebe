package org.example.kalkulationsprogramm.dto.Artikel

data class ArtikelInProjektUpdateDto(
    var schnittForm: String? = null,
    var anschnittWinkelLinks: String? = null,
    var anschnittWinkelRechts: String? = null,
    var kommentar: String? = null,
)
