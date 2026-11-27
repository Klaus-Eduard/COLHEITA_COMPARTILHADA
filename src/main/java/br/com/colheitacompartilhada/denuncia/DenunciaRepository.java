package br.com.colheitacompartilhada.denuncia;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DenunciaRepository extends JpaRepository<Denuncia, UUID> {

    @EntityGraph(attributePaths = {"anuncio", "anuncio.usuario", "autor"})
    Page<Denuncia> findByStatusOrderByCriadoEmAsc(StatusDenuncia status, Pageable pageable);

    @EntityGraph(attributePaths = {"anuncio", "anuncio.usuario", "anuncio.categoria", "anuncio.bairro", "autor",
            "moderador"})
    Optional<Denuncia> findComAssociacoesById(UUID id);

    // Sem EntityGraph: o PostgreSQL não permite FOR UPDATE no lado opcional de um join (moderador é nulo).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Denuncia d where d.id = :id")
    Optional<Denuncia> findByIdParaAtualizar(@Param("id") UUID id);

    boolean existsByAnuncioIdAndAutorIdAndStatus(UUID anuncioId, UUID autorId, StatusDenuncia status);
}
