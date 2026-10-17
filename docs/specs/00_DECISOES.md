# 00 — Decisões de implementação

> **Leia primeiro.** Este arquivo registra como a documentação do projeto (requisitos, regras de negócio, casos de uso, UML e SQL) é traduzida em decisões concretas de implementação e hospedagem.
> Em qualquer dúvida de implementação, vale o que está registrado aqui e nas specs 01–08.

## 1. Especificações complementares
Detalhes que a documentação deixa a cargo da implementação, definidos a partir das regras e casos de uso do projeto.

| ID | Tema | Base na documentação | Especificação adotada |
|----|------|----------------------|------------------------|
| E01 | Categorias de alimento | RN04, SQL (`categoria`) | **4 categorias fixas**, cadastradas pela migração: Frutas, Verduras e legumes, Ovos, Mel. Sem CRUD no MVP. |
| E02 | Campos de origem animal | RN05, T04, SQL (regra 3) | Para Ovos e Mel são obrigatórios: **validade, origem e identificação da inspeção**, além da conservação (exigida para todas as categorias). Os dados são declaratórios e não certificam o alimento. |
| E03 | Frontend | 3.10 (escolha entre Angular e HTML/CSS/JS), cap. 6 | **HTML + CSS + JavaScript**, servido pelo próprio Spring Boot (D01). |
| E04 | Prazo do anúncio | RN07, SQL (regra 4) | O usuário informa a **data** `disponivelAte`; o servidor calcula `expira_em` (fórmula exata em 02_REGRAS, RN07). |
| E05 | Formato do WhatsApp | RN02, CHECK do SQL | Armazenado **só com dígitos e DDI**: `55` + DDD + número (12 ou 13 dígitos). A tela aceita a máscara `(47) 99999-9999` e normaliza. |
| E06 | Denúncia | UC10 (pré-condição: anúncio visível de outro anunciante) | Só é possível denunciar **anúncio visível** de outro usuário. |
| E07 | Criação do ADMIN | UC01 ("configuração controlada, nunca pelo formulário público") | **Bootstrap por variáveis de ambiente** na inicialização (05 §7). Nenhuma senha em SQL ou no Git. |
| E08 | Moderação e bloqueio | UC11, UC12, SQL (regra 7: remoção e resolução na mesma transação); 3.10 (bloqueio vinculado à denúncia) | A resolução da denúncia, a remoção do anúncio e o bloqueio do anunciante acontecem em **uma única operação transacional**: `PATCH /admin/denuncias/{id}` com `procedente`, `decisao` e `bloquearAnunciante`. |
| E09 | Armazenamento da foto | RN06, 3.10 ("armazenamento persistente, com referência no banco") | A foto é persistida **no PostgreSQL** (tabela `foto`, migração V3); `anuncio.foto_url` guarda a referência `/fotos/{fotoId}`. Mantém a aplicação sem estado em disco (D13). |
| E10 | Edição de anúncio oculto | UC07, 3.10 (`GET /me/anuncios` inclui anúncios indisponíveis do dono) | Complemento ao contrato: `GET /me/anuncios/{id}` (somente o dono) para carregar o formulário de edição. |

## 2. Decisões técnicas

| ID | Decisão | Motivo | Alternativa descartada |
|----|---------|--------|------------------------|
| D01 | Frontend em **HTML/CSS/JS puro**, multi-página, em `src/main/resources/static`. | Sem build e sem segundo projeto. **Um único deploy e uma única URL**, sem CORS. Mesmo domínio (cookie + CSRF simples). | Angular: exige CLI, build e um segundo deploy ou proxy. |
| D02 | **Monólito Spring Boot** em camadas, com pacotes **por funcionalidade**. | RNF06; 5 entidades; time pequeno; um único artefato para hospedar. | Microsserviços, hexagonal completo. |
| D03 | **Flyway** para migrações; Hibernate só valida (`ddl-auto=validate`). | RNF05. O banco é migrado **automaticamente a cada deploy**. | `ddl-auto=update`. |
| D04 | **Sessão por cookie** (`JSESSIONID`, HttpOnly, SameSite=Lax, Secure em produção) + **CSRF** por cookie e cabeçalho. | Definido em 3.10; mais simples e seguro que JWT no mesmo domínio. | JWT. |
| D05 | Senha com **BCrypt** (`PasswordEncoder` do Spring Security). | RNF01 (hash adaptativo). | — |
| D06 | DTOs como **`record`**; **sem Lombok e sem MapStruct**. | Código explícito e legível por toda a equipe; menos dependências. | Lombok. |
| D07 | Busca com **JPA Specification** (filtros opcionais). | Evita o erro do PostgreSQL com parâmetros nulos em JPQL. | `@Query` com `:param IS NULL OR ...`. |
| D08 | Transições de estado com **lock pessimista** (`SELECT ... FOR UPDATE`). | Conflito de encerramento ou moderação concorrente (UC07/UC08). | `@Version`. |
| D09 | `Clock` injetado como bean. | Testes determinísticos de prazo e expiração. | — |
| D10 | Expiração por `@Scheduled` **a cada 1 h** **e** filtro de visibilidade em toda consulta. | RF10: o anúncio vencido some na hora pela consulta, e a rotina só atualiza o status. Um intervalo maior deixa o banco gratuito suspender (D14). | Rodar a cada poucos minutos. |
| D11 | Rate limit de login **em memória**. | Uma instância (D15). | Bucket4j/Redis. |
| D12 | Testes de integração com **Testcontainers (PostgreSQL 16)**; fallback para PostgreSQL de teste (`test-local`). | Testar contra o banco real (plano de testes, 7.2). | H2. |
| D13 | **Aplicação sem estado em disco**: foto no banco (`bytea`), configuração só por variáveis de ambiente, porta por `PORT`. | Hospedar em qualquer PaaS de container **sem volume persistente**. | Foto em disco/volume; S3/R2 (mais uma conta, credenciais e SDK). |
| D14 | **Hospedagem: app no Render (Web Service, Docker, plano gratuito) + banco no Neon (PostgreSQL gerenciado, plano gratuito).** Deploy automático a cada merge na `main`. | Plano gratuito, integração com GitHub e HTTPS automático. O Dockerfile é padrão: trocar de provedor (Koyeb, Railway, Fly.io) não muda o código. Conferir os limites dos planos ao criar as contas. | VM própria (manutenção); Heroku (pago). |
| D15 | **Uma única instância** da aplicação. | Sessão e rate limit em memória; volume do MVP. Reiniciar desloga os usuários, o que é aceitável. | Spring Session + Redis (YAGNI). |
| D16 | **Dockerfile multi-stage** na raiz, com JVM ajustada para 512 MB de RAM. | Funciona igual em qualquer provedor e localmente. | Buildpack específico do provedor. |
| D17 | **Bairros**: os 35 bairros de `V2__seed_bairros.sql`. | Lista definida pela equipe. | — |

## 3. Pendência operacional

| ID | Pergunta | Padrão adotado |
|----|----------|----------------|
| P01 | Quem é dono das contas do Render e do Neon? | A **conta do projeto** no GitHub (a mesma dona do repositório) cria as duas. Credenciais compartilhadas fora do Git (gerenciador de senhas da equipe). |
