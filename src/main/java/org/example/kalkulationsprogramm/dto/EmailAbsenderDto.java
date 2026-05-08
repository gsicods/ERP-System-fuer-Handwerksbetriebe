package org.example.kalkulationsprogramm.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.kalkulationsprogramm.domain.EmailAbsender;

@Data
@NoArgsConstructor
public class EmailAbsenderDto {

    private Long id;
    private String emailAdresse;
    private String anzeigename;
    private boolean aktiv = true;
    private int sortierung = 0;

    public static EmailAbsenderDto fromEntity(EmailAbsender entity) {
        if (entity == null) {
            return null;
        }
        EmailAbsenderDto dto = new EmailAbsenderDto();
        dto.setId(entity.getId());
        dto.setEmailAdresse(entity.getEmailAdresse());
        dto.setAnzeigename(entity.getAnzeigename());
        dto.setAktiv(entity.isAktiv());
        dto.setSortierung(entity.getSortierung());
        return dto;
    }
}
