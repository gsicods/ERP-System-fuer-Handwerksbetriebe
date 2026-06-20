package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.Dokumenttyp
import org.example.kalkulationsprogramm.domain.FormularTemplateTextbausteinDefault
import org.example.kalkulationsprogramm.domain.TextbausteinPosition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface FormularTemplateTextbausteinDefaultRepository : JpaRepository<FormularTemplateTextbausteinDefault, Long> {
    fun findByTemplateNameIgnoreCaseOrderByDokumenttypAscPositionAscSortOrderAsc(
        templateName: String,
    ): List<FormularTemplateTextbausteinDefault>

    fun findByTemplateNameIgnoreCaseAndDokumenttypAndPositionOrderBySortOrderAsc(
        templateName: String,
        dokumenttyp: Dokumenttyp,
        position: TextbausteinPosition,
    ): List<FormularTemplateTextbausteinDefault>

    fun findByTemplateNameIgnoreCaseAndDokumenttypOrderByPositionAscSortOrderAsc(
        templateName: String,
        dokumenttyp: Dokumenttyp,
    ): List<FormularTemplateTextbausteinDefault>

    @Transactional
    fun deleteByTemplateNameIgnoreCaseAndDokumenttyp(templateName: String, dokumenttyp: Dokumenttyp)

    @Transactional
    fun deleteByTemplateNameIgnoreCase(templateName: String)
}
