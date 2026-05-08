package org.example.kalkulationsprogramm.service;

import lombok.RequiredArgsConstructor;
import org.example.kalkulationsprogramm.domain.SvSatz;
import org.example.kalkulationsprogramm.domain.SvSatzTyp;
import org.example.kalkulationsprogramm.dto.SvSatzDto;
import org.example.kalkulationsprogramm.repository.SvSatzRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SvSatzService {

    private final SvSatzRepository repository;

    @Transactional(readOnly = true)
    public List<SvSatzDto> findAll() {
        return repository.findAllByOrderBySatzTypAscGueltigAbDesc().stream().map(SvSatzService::toDto).toList();
    }

    @Transactional
    public SvSatzDto save(SvSatzDto dto) {
        SvSatz entity = dto.getId() != null
                ? repository.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("SV-Satz nicht gefunden: " + dto.getId()))
                : new SvSatz();
        if (dto.getSatzTyp() == null || dto.getSatzTyp().isBlank()) {
            throw new IllegalArgumentException("Satz-Typ ist Pflicht.");
        }
        if (dto.getProzent() == null) {
            throw new IllegalArgumentException("Prozent ist Pflicht.");
        }
        if (dto.getGueltigAb() == null) {
            throw new IllegalArgumentException("Gueltig-ab-Datum ist Pflicht.");
        }
        SvSatzTyp typ;
        try {
            typ = SvSatzTyp.valueOf(dto.getSatzTyp());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unbekannter SV-Satz-Typ: " + dto.getSatzTyp());
        }
        entity.setSatzTyp(typ);
        entity.setProzent(dto.getProzent());
        entity.setGueltigAb(dto.getGueltigAb());
        entity.setBeschreibung(dto.getBeschreibung());
        try {
            return toDto(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(
                    "Fuer den Typ " + typ + " existiert bereits ein Satz mit dem Datum " + dto.getGueltigAb()
                            + ". Bitte den bestehenden Eintrag bearbeiten oder ein anderes Gueltig-ab-Datum waehlen.");
        }
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public static SvSatzDto toDto(SvSatz e) {
        SvSatzDto dto = new SvSatzDto();
        dto.setId(e.getId());
        dto.setSatzTyp(e.getSatzTyp().name());
        dto.setProzent(e.getProzent());
        dto.setGueltigAb(e.getGueltigAb());
        dto.setBeschreibung(e.getBeschreibung());
        return dto;
    }
}
