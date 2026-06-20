package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.Dokumenttyp
import org.example.kalkulationsprogramm.domain.FormularTemplateAssignment
import org.springframework.data.jpa.repository.JpaRepository

interface FormularTemplateAssignmentRepository : JpaRepository<FormularTemplateAssignment, Long> {
    fun findByTemplateNameIgnoreCaseAndUser_Id(templateName: String, userId: Long?): List<FormularTemplateAssignment>

    fun findByTemplateNameIgnoreCaseAndUserIsNull(templateName: String): List<FormularTemplateAssignment>

    fun findByTemplateNameIgnoreCase(templateName: String): List<FormularTemplateAssignment>

    fun deleteByTemplateNameIgnoreCase(templateName: String)

    fun deleteByTemplateNameIgnoreCaseAndUser_Id(templateName: String, userId: Long?)

    fun deleteByTemplateNameIgnoreCaseAndUserIsNull(templateName: String)

    fun findFirstByDokumenttypAndUser_IdOrderByIdDesc(
        dokumenttyp: Dokumenttyp,
        userId: Long?,
    ): Optional<FormularTemplateAssignment>

    fun findFirstByDokumenttypAndUserIsNullOrderByIdDesc(
        dokumenttyp: Dokumenttyp,
    ): Optional<FormularTemplateAssignment>

    fun deleteByDokumenttypAndUser_Id(dokumenttyp: Dokumenttyp, userId: Long?)

    fun deleteByDokumenttypAndUserIsNull(dokumenttyp: Dokumenttyp)

    fun deleteByDokumenttyp(dokumenttyp: Dokumenttyp)

    fun findByDokumenttyp(dokumenttyp: Dokumenttyp): List<FormularTemplateAssignment>
}
