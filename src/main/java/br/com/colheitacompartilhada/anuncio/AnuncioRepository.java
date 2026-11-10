package br.com.colheitacompartilhada.anuncio;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnuncioRepository extends JpaRepository<Anuncio, UUID>, JpaSpecificationExecutor<Anuncio> {

    @EntityGraph(attributePaths = {"categoria", "bairro", "usuario"})
    Page<Anuncio> findByUsuarioIdOrderByCriadoEmDesc(UUID usuarioId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Anuncio a where a.id = :id")
    Optional<Anuncio> findByIdParaAtualizar(@Param("id") UUID id);

    @Modifying
    @Query("update Anuncio a set a.status = EXPIRADO, a.atualizadoEm = :agora where a.status = ATIVO and a.expiraEm <= :agora")
    int expirarVencidos(@Param("agora") Instant agora);
}
