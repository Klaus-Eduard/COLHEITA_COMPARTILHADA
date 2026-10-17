# 08 — Plano de entregas, backlog e fluxo de trabalho

> Hoje é **17/10/2026, fim da S4**. **Entrega e apresentação: sexta-feira, 27/11/2026.** Restam cerca de 5,5 semanas (S5–S10) de trabalho, aproximadamente 130 h de equipe. Foco: **implementar e hospedar**.
> Se parte da fundação já estiver pronta, marque os issues como concluídos e antecipe os seguintes. **Não acrescente escopo.**
> Estratégia:
> - **Fatias verticais**: back + front + teste de cada funcionalidade.
> - **Em produção desde a primeira semana** (*walking skeleton*): o app vai para a URL pública na S5 e cada merge na `main` faz deploy. Assim, problemas de hospedagem aparecem cedo, quando ainda são baratos de resolver.

## 1. Marcos

| Semana | Datas | Marco verificável (demo de 5 min **na URL pública**) |
|--------|-------|------------------------------------------------------|
| S5 | 19–25/10 | Render + Neon configurados; deploy automático da `main`; app no ar com HTTPS; CI verde; cadastro, login e logout funcionando **em produção** |
| S6 | 26/10–01/11 | Perfil com aceite; publicar anúncio com foto (API); busca e detalhe (API) — tudo em produção |
| S7 | 02–08/11 | Fluxo do usuário completo no navegador: buscar → detalhe → contato; publicar, editar e encerrar; expiração ativa |
| S8 | 09–15/11 | Denúncia + moderação + bloqueio no navegador; matriz de autorização testada |
| S9 | 16–20/11 | T01–T10 com evidência; acessibilidade; usabilidade com 5 pessoas; backup e restauração ensaiados; dados de demonstração. **Congelamento de funcionalidades: sexta, 20/11** |
| S10 | 23–27/11 | 23–25/11: somente correções, README final e prints das telas. **26/11: ensaio geral na URL pública.** **27/11: entrega e apresentação** |

## 2. Backlog (cada linha = 1 issue no GitHub)
Responsáveis **sugeridos** (validar na reunião). Todos implementam, todos revisam.

| # | Issue | Specs | Depende de | Sem. | Resp. | Revisor |
|---|-------|-------|------------|------|-------|---------|
| 1 | **Setup + walking skeleton em produção:** Spring Boot 3.5 + Java 21, `pom` (`finalName app`, Actuator), Flyway V1–V3, `application.yml`, `ClockConfig`, `Dockerfile`, `.dockerignore`, `.gitattributes`, `.env.example`, `.gitignore`, README, AGENTS.md, `docs/`, CI com smoke test em Testcontainers, **contas Neon + Render, deploy automático, `/actuator/health/liveness` respondendo na URL pública** | 05 §1–2, §7, §10–11 | — | S5 | Klaus | Luan |
| 2 | Entidades + enums + repositórios + pacote `comum` (exceções, `ApiExceptionHandler`, `ErroResposta`, `Pagina`) | 03, 04 §2, 05 §3, §5 | 1 | S5 | Luan | Klaus |
| 3 | Segurança e acesso: `SecurityConfig`, CSRF, `POST /usuarios`, `POST/DELETE /sessao`, `GET /me`, `GET /csrf`, `LoginRateLimiter`, `AtivoCheckFilter`, `AdminBootstrap` | 04 #1–5, 05 §4, 07 UC01–02 | 2 | S5 | Klaus | Isabele |
| 4 | Front base: `estilo.css`, `api.js`, `sessao.js`, `layout.js`, `formatos.js`, `form-erros.js`, `entrar.html`, `cadastro.html` | 06 §1–3.5, §4–5 | 3 (contrato) | S5 | Isabele | Klaus |
| 5 | Perfil: `PATCH /me` (RN02, normalização do WhatsApp) + `perfil.html` | 02 RN02, 04 #6, 06 §3.6, 07 UC03 | 3, 4 | S6 | Isabele | Luan |
| 6 | Catálogo: `GET /categorias`, `GET /bairros` | 04 #8–9 | 2 | S5 | Luan | Isabele |
| 7 | Domínio do anúncio: `publicar`, `editar`, `encerrar`, `remover`, `estaVisivel`, cálculo de `expira_em` + **testes unitários** | 02 RN03–RN11, 03 §4–5 | 2 | S6 | Luan | Klaus |
| 8 | Foto no banco: entidade `Foto`, `FotoService`, `GET /fotos/{id}` | 02 RN06, 03 §3, 05 §6, 04 #20 | 2 | S6 | Klaus | Luan |
| 9 | API do dono: `POST /anuncios`, `PATCH /anuncios/{id}`, `GET /me/anuncios`, `GET /me/anuncios/{id}`, `POST .../encerramento` | 04 #7, 7b, 12–14, 07 UC06–08 | 7, 8 | S6 | Luan | Klaus |
| 10 | Busca e detalhe: `AnuncioSpecs`, `GET /anuncios`, `GET /anuncios/{id}` | 02 RN08, 03 §7, 04 #10–11, 07 UC04–05 | 7 | S6 | Klaus | Luan |
| 11 | Front: `index.html` (busca) + `anuncio.html` (detalhe) | 06 §3.2–3.3 | 4, 10 | S7 | Isabele | Luan |
| 12 | Front: `anuncio-form.html` + `meus-anuncios.html` | 06 §3.7–3.8 | 4, 9 | S7 | Isabele | Klaus |
| 13 | Contato: `GET /anuncios/{id}/contato` + botões no detalhe | 04 #15, 07 UC09 | 10, 11 | S7 | Luan | Isabele |
| 14 | Expiração: `ExpiracaoJob` + teste de idempotência | 02 RN14, 05 §8, 07 RF10 | 7 | S7 | Luan | Klaus |
| 15 | Denúncia: `Denuncia.abrir`, `POST /anuncios/{id}/denuncias` + diálogo no detalhe | 02 RN10, 04 #16, 07 UC10 | 10, 11 | S8 | Klaus | Isabele |
| 16 | Moderação: `GET /admin/denuncias`, `GET/PATCH /admin/denuncias/{id}` (remover + bloquear na mesma transação) + `admin.html` | 02 RN10–RN13, 04 #17–19, 06 §3.9, 07 UC11–12 | 15 | S8 | Klaus (back) / Isabele (front) | Luan |
| 17 | Teste parametrizado da matriz de autorização + revisão da suíte T01–T10 | 04 §4, 07 §2 | 16 | S8 | Luan | Klaus |
| 18 | Revisão de acessibilidade e responsividade + roteiro de usabilidade com 5 pessoas (na URL pública) | 06 §4, 07 §3 | 11, 12, 16 | S9 | Isabele | todos |
| 19 | Produção pronta para a apresentação: perfil `demo` com dados fictícios, conta ADMIN de demonstração, backup + restauração ensaiados (T10), teste de redeploy e cold start, envio de URL e credenciais ao professor por canal restrito | 05 §7, §11, 07 RNF05 | 16 | S9 | Klaus | Luan |
| 20 | README final (como rodar, variáveis, URL, contas de teste) + prints das telas do sistema em execução para a apresentação | 08 §6 | 18, 19 | S10 | Todos | Todos |
| 21 | Sessão de conhecimento compartilhado (26/11, junto com o ensaio geral): cada integrante apresenta à equipe um fluxo **que não implementou** (busca, publicação, moderação, segurança, deploy) | 08 §6.3 | 20 | S10 | Todos | Todos |

**Caminho crítico prático:** 1 → 2 → 7 → 9/10 → 11/12 → 15 → 16 → 19. Atraso em qualquer um destes issues vai para a reunião semanal da mesma semana.

**Se faltar tempo, corte nesta ordem** (sem violar RF01–RF10): prévia da foto no formulário → aba "Resolvidas" no admin → perfil `demo` (cadastrar os dados de demonstração à mão).

## 3. Modelo de issue (copie no GitHub; serve de prompt para qualquer IA)
```markdown
## Objetivo
<uma frase>

## Specs a seguir
- docs/specs/0X_...md §Y
- docs/specs/04_API.md #N

## Critérios de aceite
- [ ] <copiar os cenários de 07_CRITERIOS_DE_ACEITE.md>
- [ ] Funciona na URL de produção depois do merge

## Fora do escopo deste issue
- <o que NÃO fazer>

## Arquivos esperados
- src/main/java/.../X.java
- src/test/java/.../XTest.java
```

## 4. Fluxo Git e deploy
- Branches permanentes: `main` (sempre estável; **cada merge vai para produção**), `klaus`, `luan`, `isabele`.
- Antes de começar um issue: `git switch <minha-branch>` → `git pull origin main`.
- Commits pequenos, em português, no formato *Conventional Commits*: `feat(anuncio): publicar anúncio com foto (#9)`, `fix(auth): ...`, `test(...)`, `docs(...)`.
- PR `<minha-branch> → main` **por issue**, com `Closes #N`, revisão de 1 colega e CI verde. Merge com **"Create a merge commit"** (preserva a autoria).
- Proteger a `main`: PR obrigatório, CI obrigatório, sem push direto.
- **Depois do merge, quem abriu o PR confere a funcionalidade na URL pública.** Se o deploy quebrar: reverter o merge (`git revert -m 1 <commit>` via PR) primeiro e investigar depois.
- **Nunca apagar** as branches pessoais: elas preservam o histórico de trabalho de cada integrante.
- Cada integrante usa a **própria conta e o próprio e-mail** no `git config`. A conta do projeto é dona do repositório e das contas Render e Neon (P01); ninguém faz commits por ela.

## 5. Rotina semanal (20 min)
1. Demo do marco da semana **na URL pública** (§1). 2. Horas realizadas por pessoa. 3. Bloqueios. 4. Riscos e decisões novas em `00_DECISOES.md`. 5. Ata curta em `docs/atas/AAAA-MM-DD.md`.

## 6. Checklist da entrega final

### 6.1 Sistema no ar
- [ ] URL pública HTTPS funcionando, com deploy automático da `main`.
- [ ] RF01–RF10 demonstráveis em produção, com dados no PostgreSQL remoto (Neon); nenhum defeito crítico aberto.
- [ ] Sistema decomposto em classes, interfaces (repositórios Spring Data), enumerações e interfaces gráficas.
- [ ] Evidências T01–T10: CI verde + `docs/evidencias/`.
- [ ] Redeploy mantém dados e fotos; cold start funciona (07 RNF05).
- [ ] Backup feito e restauração ensaiada (T10).
- [ ] Dados de demonstração carregados; URL e credenciais (ADMIN e usuário comum) enviadas ao professor por canal restrito.
- [ ] **No dia da apresentação: abrir a URL 5 minutos antes** (o plano gratuito hiberna).

### 6.2 Repositório
- [ ] Privado, com o professor como colaborador.
- [ ] Branches `main`, `klaus`, `luan` e `isabele`, com commits identificados de cada integrante.
- [ ] Nenhum segredo no histórico (`.env` no `.gitignore`; procurar por "password" e "DB_" antes da entrega).
- [ ] README: o que é, URL de produção, como rodar localmente, variáveis (`.env.example`), como rodar os testes, como fazer deploy, equipe.

### 6.3 Conhecimento compartilhado
- [ ] Todo integrante conhece todas as partes do sistema, inclusive o deploy; a revisão cruzada de PR é o principal mecanismo.
- [ ] Nenhum código entra na `main` sem que o autor e o revisor o entendam.
- [ ] Issue 21: sessão de conhecimento compartilhado em 26/11.
