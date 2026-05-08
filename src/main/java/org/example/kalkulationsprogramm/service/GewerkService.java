package org.example.kalkulationsprogramm.service;

import lombok.RequiredArgsConstructor;
import org.example.kalkulationsprogramm.domain.Gewerk;
import org.example.kalkulationsprogramm.dto.GewerkDto;
import org.example.kalkulationsprogramm.repository.GewerkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GewerkService {

    private final GewerkRepository repository;

    @Transactional(readOnly = true)
    public List<GewerkDto> findAll() {
        return repository.findAllByOrderByNameAsc().stream().map(GewerkService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<GewerkDto> findAktiv() {
        return repository.findByAktivTrueOrderByNameAsc().stream().map(GewerkService::toDto).toList();
    }

    @Transactional
    public GewerkDto save(GewerkDto dto) {
        Gewerk entity = dto.getId() != null
                ? repository.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Gewerk nicht gefunden: " + dto.getId()))
                : new Gewerk();
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Name darf nicht leer sein.");
        }
        if (dto.getBgName() == null || dto.getBgName().isBlank()) {
            throw new IllegalArgumentException("BG-Name darf nicht leer sein.");
        }
        if (dto.getBgSatzProzent() == null) {
            throw new IllegalArgumentException("BG-Satz (Prozent) ist Pflicht.");
        }
        entity.setName(dto.getName().trim());
        entity.setBgName(dto.getBgName().trim());
        entity.setBgSatzProzent(dto.getBgSatzProzent());
        entity.setAktiv(dto.getAktiv() == null ? Boolean.TRUE : dto.getAktiv());
        entity.setBemerkung(dto.getBemerkung());
        return toDto(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public static GewerkDto toDto(Gewerk e) {
        GewerkDto dto = new GewerkDto();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setBgName(e.getBgName());
        dto.setBgSatzProzent(e.getBgSatzProzent());
        dto.setAktiv(e.getAktiv());
        dto.setBemerkung(e.getBemerkung());
        return dto;
    }
}
