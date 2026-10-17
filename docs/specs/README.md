# Colheita Compartilhada — Specs de desenvolvimento

Specs de implementação derivadas da documentação do projeto: requisitos, regras de negócio, casos de uso, diagramas de atividades e de classes, modelo relacional e `01_schema.sql`.
Objetivo: permitir que qualquer integrante, **com qualquer IA ou sem IA**, implemente **e hospede** o MVP de forma consistente até **27/11/2026**.

## Arquitetura em uma linha
**Um container Docker** (Spring Boot servindo a API e o front estático) no **Render**, banco **PostgreSQL no Neon**, deploy automático a cada merge na `main`. Os dois serviços têm plano gratuito. Nada gravado em disco, tudo configurado por variável de ambiente. Detalhes em 05 §10–11.

## Ordem de leitura
| Arquivo | Para quê | Quem lê |
|---------|----------|---------|
| [00_DECISOES.md](00_DECISOES.md) | Especificações complementares e decisões técnicas, incluindo hospedagem | Todos, primeiro |
| [01_VISAO_E_ESCOPO.md](01_VISAO_E_ESCOPO.md) | O que entra e o que **não** entra, atores e glossário | Todos |
| [02_REGRAS_DE_NEGOCIO.md](02_REGRAS_DE_NEGOCIO.md) | RN01–RN14 precisas + tabela de validação de campos | Back e front |
| [03_MODELO_DE_DADOS.md](03_MODELO_DE_DADOS.md) | Tabelas, enums, JPA, métodos das entidades, máquina de estados, consultas | Back |
| [04_API.md](04_API.md) | Contrato REST completo (JSON, erros, matriz de autorização) | Back e front |
| [05_ARQUITETURA.md](05_ARQUITETURA.md) | Stack, pacotes, segurança, foto, configuração, testes, CI/CD e **hospedagem passo a passo** | Back / quem faz o deploy |
| [06_FRONTEND.md](06_FRONTEND.md) | Páginas, estados, mensagens, acessibilidade | Front |
| [07_CRITERIOS_DE_ACEITE.md](07_CRITERIOS_DE_ACEITE.md) | Cenários Dado/Quando/Então por UC, ligados a T01–T10, e DoD | Todos |
| [08_PLANO_DE_ENTREGAS.md](08_PLANO_DE_ENTREGAS.md) | Marcos S5–S10, 21 issues com dono e revisor, fluxo Git/deploy, checklist da entrega | Todos |

## Arquivos de base já no repositório
| Arquivo | Para quê |
|---------|----------|
| `AGENTS.md` (+ `CLAUDE.md`, `.github/copilot-instructions.md`) | Regras para qualquer assistente de IA |
| `Dockerfile`, `.dockerignore` | Imagem de produção (Render) |
| `.gitattributes` | Fim de linha LF no `mvnw` (build Linux a partir do Windows) |
| `.env.example`, `.gitignore` | Configuração local sem segredos no Git |
| `src/main/resources/db/migration/V1–V3` | Schema, bairros e tabela de fotos (Flyway) |
| `docs/diagramas/` | Diagramas draw.io do projeto |

O issue 1 completa a base com `pom.xml`, Maven wrapper, código inicial e CI.

> **Windows:** commite o `.gitattributes` **antes** do `mvnw`. Sem ele, o `mvnw` vai com fim de linha CRLF e o build Docker falha com `/bin/sh^M: not found`.

## Como usar com qualquer IA (fluxo recomendado)
1. Abra o issue do backlog (08 §2). Ele cita as seções de spec que se aplicam.
2. Dê à IA: **AGENTS.md + o texto do issue + as seções de spec citadas**. Ferramentas com acesso ao repositório leem os arquivos sozinhas.
3. Use um prompt como este:
   > Implemente o issue #N seguindo AGENTS.md e as specs citadas. Antes de codar, liste os arquivos que vai criar ou alterar e os critérios de aceite que vai cobrir. Escreva os testes dos cenários de 07_CRITERIOS_DE_ACEITE.md. Não adicione nada fora do escopo do issue. Respeite as regras de hospedagem do AGENTS.md.
4. Revise o código como se fosse de um colega: as regras estão na entidade? A autorização está no service? O JSON confere com 04_API? Os testes cobrem os cenários? Algo grava em disco ou depende de configuração fixa no código?
5. `./mvnw verify` verde → PR → revisão de um colega → merge → **conferir na URL pública**.

**Só faça commit de código que você sabe explicar.** Na revisão, o revisor pode perguntar "por que isso?"; se não houver resposta, o PR volta.

**Regra de ouro:** a IA não decide escopo, contrato, schema nem infraestrutura. Se ela "sugerir uma melhoria" fora da spec (outro serviço, outra biblioteca, outro provedor), **recuse** ou registre como trabalho futuro em 00_DECISOES.

## Pendência
Definir quem é dono das contas do Render e do Neon (00_DECISOES, P01).
