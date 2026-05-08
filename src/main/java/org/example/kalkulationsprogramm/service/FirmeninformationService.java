package org.example.kalkulationsprogramm.service;

import lombok.RequiredArgsConstructor;
import org.example.kalkulationsprogramm.domain.Firmeninformation;
import org.example.kalkulationsprogramm.domain.Gewerk;
import org.example.kalkulationsprogramm.dto.FirmeninformationDto;
import org.example.kalkulationsprogramm.repository.FirmeninformationRepository;
import org.example.kalkulationsprogramm.repository.GewerkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FirmeninformationService {

    private final FirmeninformationRepository repository;
    private final GewerkRepository gewerkRepository;

    @Transactional(readOnly = true)
    public FirmeninformationDto getFirmeninformation() {
        Firmeninformation fi = repository.getOrCreate();
        return toDto(fi);
    }

    @Transactional
    public FirmeninformationDto speichern(FirmeninformationDto dto) {
        Firmeninformation fi = repository.getOrCreate();
        
        fi.setFirmenname(dto.getFirmenname());
        fi.setStrasse(dto.getStrasse());
        fi.setPlz(dto.getPlz());
        fi.setOrt(dto.getOrt());
        fi.setTelefon(dto.getTelefon());
        fi.setFax(dto.getFax());
        fi.setEmail(dto.getEmail());
        fi.setWebsite(dto.getWebsite());
        fi.setSteuernummer(dto.getSteuernummer());
        fi.setUstIdNr(dto.getUstIdNr());
        fi.setHandelsregister(dto.getHandelsregister());
        fi.setHandelsregisterNummer(dto.getHandelsregisterNummer());
        fi.setBankName(dto.getBankName());
        fi.setIban(dto.getIban());
        fi.setBic(dto.getBic());
        fi.setLogoDateiname(dto.getLogoDateiname());
        fi.setGeschaeftsfuehrer(dto.getGeschaeftsfuehrer());
        fi.setFusszeileText(dto.getFusszeileText());
        fi.setGoogleBewertungsLink(normalizeUrl(dto.getGoogleBewertungsLink()));

        fi.setMahnverfahrenAktiv(dto.isMahnverfahrenAktiv());
        fi.setTageBisZahlungserinnerung(positivOrDefault(dto.getTageBisZahlungserinnerung(), 7));
        fi.setTageBisErsteMahnung(positivOrDefault(dto.getTageBisErsteMahnung(), 14));
        fi.setTageBisZweiteMahnung(positivOrDefault(dto.getTageBisZweiteMahnung(), 21));
        fi.setMahnverfahrenNeuesZahlungszielTage(positivOrDefault(dto.getMahnverfahrenNeuesZahlungszielTage(), 7));

        if (dto.getGewerkId() != null) {
            Gewerk g = gewerkRepository.findById(dto.getGewerkId())
                    .orElseThrow(() -> new IllegalArgumentException("Gewerk nicht gefunden: " + dto.getGewerkId()));
            fi.setGewerk(g);
        } else {
            fi.setGewerk(null);
        }
        fi.setBgSatzOverride(dto.getBgSatzOverride());

        fi = repository.save(fi);
        return toDto(fi);
    }

    private static int positivOrDefault(int wert, int fallback) {
        return wert > 0 ? wert : fallback;
    }

    private static String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private FirmeninformationDto toDto(Firmeninformation fi) {
        FirmeninformationDto dto = new FirmeninformationDto();
        dto.setId(fi.getId());
        dto.setFirmenname(fi.getFirmenname());
        dto.setStrasse(fi.getStrasse());
        dto.setPlz(fi.getPlz());
        dto.setOrt(fi.getOrt());
        dto.setTelefon(fi.getTelefon());
        dto.setFax(fi.getFax());
        dto.setEmail(fi.getEmail());
        dto.setWebsite(fi.getWebsite());
        dto.setSteuernummer(fi.getSteuernummer());
        dto.setUstIdNr(fi.getUstIdNr());
        dto.setHandelsregister(fi.getHandelsregister());
        dto.setHandelsregisterNummer(fi.getHandelsregisterNummer());
        dto.setBankName(fi.getBankName());
        dto.setIban(fi.getIban());
        dto.setBic(fi.getBic());
        dto.setLogoDateiname(fi.getLogoDateiname());
        dto.setGeschaeftsfuehrer(fi.getGeschaeftsfuehrer());
        dto.setFusszeileText(fi.getFusszeileText());
        dto.setGoogleBewertungsLink(fi.getGoogleBewertungsLink());
        dto.setMahnverfahrenAktiv(fi.isMahnverfahrenAktiv());
        dto.setTageBisZahlungserinnerung(fi.getTageBisZahlungserinnerung());
        dto.setTageBisErsteMahnung(fi.getTageBisErsteMahnung());
        dto.setTageBisZweiteMahnung(fi.getTageBisZweiteMahnung());
        dto.setMahnverfahrenNeuesZahlungszielTage(fi.getMahnverfahrenNeuesZahlungszielTage());
        if (fi.getGewerk() != null) {
            Gewerk g = fi.getGewerk();
            dto.setGewerkId(g.getId());
            dto.setGewerkName(g.getName());
            dto.setBgName(g.getBgName());
            dto.setBgSatzVorschlag(g.getBgSatzProzent());
        }
        dto.setBgSatzOverride(fi.getBgSatzOverride());
        if (fi.getBgSatzOverride() != null) {
            dto.setBgSatzEffektiv(fi.getBgSatzOverride());
        } else if (fi.getGewerk() != null) {
            dto.setBgSatzEffektiv(fi.getGewerk().getBgSatzProzent());
        }
        return dto;
    }
}
