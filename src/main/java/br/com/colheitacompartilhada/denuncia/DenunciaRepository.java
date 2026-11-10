package br.com.colheitacompartilhada.denuncia;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DenunciaRepository extends JpaRepository<Denuncia, UUID> {

    Page<Denuncia> findByStatusOrderByCriadoEmAsc(StatusDenuncia status, Pageable pageable);

    boolean existsByAnuncioIdAndAutorIdAndStatus(UUID anuncioId, UUID autorId, StatusDenuncia status);
}
