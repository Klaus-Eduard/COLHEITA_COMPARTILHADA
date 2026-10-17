# AGENTS.md — Regras para qualquer assistente de IA neste repositório

> Vale para Claude Code, Cursor, Copilot, Codex, Gemini, ChatGPT ou qualquer outra ferramenta.
> Ferramentas que procuram outro nome de arquivo devem apontar para este:
> `CLAUDE.md` e `.github/copilot-instructions.md` contêm apenas a linha `Siga integralmente o arquivo AGENTS.md na raiz do repositório.`

## Projeto
**Colheita Compartilhada**: site para divulgar excedentes de alimentos em Blumenau (doação ou venda, com contato por WhatsApp fora do sistema). Equipe de 3 pessoas, entrega em 27/11/2026.

## Fonte de verdade (ler antes de codar)
1. `docs/specs/00_DECISOES.md` — vence qualquer outro documento em caso de conflito.
2. `docs/specs/02_REGRAS_DE_NEGOCIO.md` — regras RN01–RN14 e tabela de validações.
3. `docs/specs/04_API.md` — contrato HTTP (rotas, JSON, códigos de erro). **Não inventar rotas nem campos.**
4. `docs/specs/03_MODELO_DE_DADOS.md`, `05_ARQUITETURA.md`, `06_FRONTEND.md`, `07_CRITERIOS_DE_ACEITE.md`.

Se a tarefa pedir algo que contradiz as specs ou que está em "Fora do escopo" (`01_VISAO_E_ESCOPO.md` §3): **pare e pergunte** em vez de implementar.

## Stack fixa
Java 21 · Spring Boot 3.5.x (Web, Security, Data JPA, Validation, Actuator) · PostgreSQL 16 (Neon) · Flyway · Maven wrapper · JUnit 5 + MockMvc + Testcontainers · Front em HTML/CSS/JS puro em `src/main/resources/static` · Docker → Render.

## O sistema roda hospedado (regras inegociáveis)
Produção = **um container no Render + PostgreSQL no Neon**, com deploy automático a cada merge na `main` (`05_ARQUITETURA.md` §10–11). Todo código precisa funcionar nesse ambiente:
- **Nada é gravado em disco local** (sem uploads, arquivos temporários persistentes nem SQLite). O container é descartável. Arquivos do usuário vão para o banco (ex.: tabela `foto`).
- **Toda configuração vem de variável de ambiente.** Nada de URL, senha ou host fixo no código. Porta = `${PORT:8080}`.
- **Uma única instância, sem estado compartilhado externo.** Não introduzir Redis, filas, S3, e-mail nem outro serviço.
- **O app hiberna e acorda** (plano gratuito): nada pode depender de rodar continuamente. Rotinas agendadas devem ser idempotentes e a regra de negócio não pode depender delas (ver RN08/RN14).
- Health check = `/actuator/health/liveness`, que **não consulta o banco**.
- Datas e horas: o servidor roda em UTC. Use sempre `Clock` + `America/Sao_Paulo` explícito para "hoje".
- O Dockerfile pula testes; o CI é quem testa. Não mexer no Dockerfile sem necessidade.
- Mudanças que exigem nova variável de ambiente devem atualizar `.env.example`, `05_ARQUITETURA.md` §7 e o README no mesmo PR, e avisar a equipe para cadastrá-la no Render.

## Regras de código
- Pacote por funcionalidade: `br.com.colheitacompartilhada.{config,comum,usuario,catalogo,anuncio,denuncia}`.
- Domínio, API e banco em português **sem acento** nos identificadores; sufixos técnicos em inglês (`AnuncioService`).
- **Regras de transição ficam na entidade** (`Anuncio.encerrar`, `Denuncia.resolver`...). Services carregam, autorizam, chamam a entidade e salvam. Controllers apenas mapeiam HTTP.
- DTOs são `record`. Nunca expor entidade nem `senhaHash` na API.
- Toda leitura de "agora" usa o bean `Clock`. Fuso de negócio: `America/Sao_Paulo`.
- A visibilidade pública (RN08) existe em **um único lugar** (`AnuncioSpecs.visivel` + `Anuncio.estaVisivel`). Não duplicar.
- Autorização de dono ou papel é verificada **no service**, sempre, com o ID vindo da sessão.
- Erros: lançar as exceções de `comum`; somente o `ApiExceptionHandler` converte para HTTP.
- Schema: só por **nova** migração Flyway `V{n}__descricao.sql`. Nunca editar uma migração já aplicada. Nunca usar `ddl-auto=update`.
- Front: `textContent` para dados (nunca `innerHTML` com dados), todas as chamadas via `js/api.js`, labels e `aria-describedby` em todos os campos.
- Proibido sem aprovação registrada em `00_DECISOES.md`: Lombok, MapStruct, JWT, Redis, frameworks JS ou CSS, novas dependências em geral.
- Nada de segredos no código. Configuração por variáveis de ambiente (`05_ARQUITETURA.md` §7).
- Nunca registrar em log: senha, hash, WhatsApp, tokens ou corpos de requisição.
- Clean code e YAGNI: métodos curtos, nomes claros, sem abstração "para o futuro", sem código comentado, sem TODO solto.

## Testes
- Toda regra nova ou alterada vem com teste. Regras de entidade: teste unitário com `Clock.fixed`. Endpoints: teste de integração (MockMvc + Testcontainers).
- Os cenários de aceite estão em `07_CRITERIOS_DE_ACEITE.md`; use os mesmos nomes de caso.
- Comando: `./mvnw verify` (Windows: `mvnw.cmd verify`). Só entregue a tarefa com a suíte verde.

## Código simples e compartilhado
Todo o código é mantido pela equipe inteira: qualquer integrante precisa conseguir entender e alterar qualquer parte. Por isso:
- Prefira a solução mais simples e convencional do Spring a uma solução engenhosa.
- Ao entregar, explique **o porquê** de cada decisão não óbvia, em linguagem simples.
- Não gere grandes volumes de código de uma vez: um issue por vez, em passos pequenos e revisáveis.

## Como trabalhar uma tarefa
1. Ler o issue e as seções de spec citadas nele.
2. Listar os arquivos que vai criar ou alterar **antes** de escrever código.
3. Implementar em passos pequenos, rodando os testes.
4. Ao terminar: resumir o que mudou, quais critérios de aceite foram cobertos e o que ficou pendente.
5. Se mudar o contrato (04_API) ou uma regra, atualizar a spec correspondente no mesmo PR.

## Git
Branches `main`, `klaus`, `luan`, `isabele`. Commits no formato `tipo(escopo): descrição (#issue)`, em português. Não fazer push na `main`, não reescrever histórico compartilhado, não commitar `.env`. Cada merge na `main` vai para produção: depois do merge, conferir a funcionalidade na URL pública.
