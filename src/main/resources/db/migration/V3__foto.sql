-- Colheita Compartilhada | V3 - foto do anúncio armazenada no banco (E09/D13)
-- Mantém a aplicação sem estado em disco: qualquer hospedagem de container funciona sem volume.
-- anuncio.foto_url continua existindo e guarda '/fotos/{foto.id}' (um novo id a cada troca evita cache antigo).

CREATE TABLE foto (
 id uuid PRIMARY KEY,
 anuncio_id uuid NOT NULL UNIQUE REFERENCES anuncio(id),
 tipo varchar(10) NOT NULL CHECK (tipo IN ('image/jpeg','image/png')),
 tamanho integer NOT NULL CHECK (tamanho > 0 AND tamanho <= 2097152),
 conteudo bytea NOT NULL,
 criado_em timestamptz NOT NULL DEFAULT now()
);
