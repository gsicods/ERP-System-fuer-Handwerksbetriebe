package org.example.kalkulationsprogramm.dto.Kunde

/**
 * Update-Request entspricht aktuell den gleichen Validierungsregeln wie der Create-Request,
 * wird aber bewusst getrennt gehalten, damit sich zukünftige Unterschiede leichter abbilden lassen.
 */
class KundeUpdateRequestDto : KundeCreateRequestDto()
