package br.com.colheitacompartilhada.anuncio;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FotoRepository extends JpaRepository<Foto, UUID> {

    void deleteByAnuncioId(UUID anuncioId);
}
