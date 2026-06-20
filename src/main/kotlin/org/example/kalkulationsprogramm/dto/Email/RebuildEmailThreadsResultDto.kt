package org.example.kalkulationsprogramm.dto.Email

data class RebuildEmailThreadsResultDto(
    var processed: Int = 0,
    var relinked: Int = 0,
    var cleared: Int = 0,
)
