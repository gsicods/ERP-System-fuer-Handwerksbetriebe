package org.example.kalkulationsprogramm.service;

import lombok.RequiredArgsConstructor;
import org.example.kalkulationsprogramm.domain.Krankenkasse;
import org.example.kalkulationsprogramm.dto.KrankenkasseDto;
import org.example.kalkulationsprogramm.repository.KrankenkasseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KrankenkasseService {

    private final KrankenkasseRepository repository;

    @Transactional(readOnly = true)
    public List<KrankenkasseDto> findAll() {
        return repository.findAllByOrderByNameAsc().stream().map(KrankenkasseService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<KrankenkasseDto> findAktiv() {
        return repository.findByAktivTrueOrderByNameAsc().stream().map(KrankenkasseService::toDto).toList();
    }

    @Transactional
    public KrankenkasseDto save(KrankenkasseDto dto) {
        Krankenkasse entity = dto.getId() != null
                ? repository.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Krankenkasse nicht gefunden: " + dto.getId()))
                : new Krankenkasse();
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Name darf nicht leer sein.");
        }
        if (dto.getZusatzbeitragProzent() == null) {
            throw new IllegalArgumentException("Zusatzbeitrag (Prozent) ist Pflicht.");
        }
        entity.setName(dto.getName().trim());
        entity.setKuerzel(dto.getKuerzel());
        entity.setZusatzbeitragProzent(dto.getZusatzbeitragProzent());
        entity.setAktiv(dto.getAktiv() == null ? Boolean.TRUE : dto.getAktiv());
        entity.setGueltigAb(dto.getGueltigAb());
        entity.setBemerkung(dto.getBemerkung());
        return toDto(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public static KrankenkasseDto toDto(Krankenkasse e) {
        KrankenkasseDto dto = new KrankenkasseDto();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setKuerzel(e.getKuerzel());
        dto.setZusatzbeitragProzent(e.getZusatzbeitragProzent());
        dto.setAktiv(e.getAktiv());
        dto.setGueltigAb(e.getGueltigAb());
        dto.setBemerkung(e.getBemerkung());
        return dto;
    }
}
