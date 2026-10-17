# 04 — Contrato da API REST

> Base: mesmo domínio do front (`/`). JSON em UTF-8, campos em camelCase.
> Datas-hora no formato ISO-8601 UTC (`"2026-10-20T15:00:00Z"`); datas como `"2026-10-25"`; `BigDecimal` como número JSON.
> **Este arquivo é o contrato.** Mudou algo aqui? Atualize o back, o front e os testes no mesmo PR.

## 1. Convenções

### 1.1 Autenticação e CSRF
- Sessão por cookie `JSESSIONID` (HttpOnly, SameSite=Lax, Secure em produção).
- CSRF: o cookie `XSRF-TOKEN` (legível pelo JS) deve ser enviado no cabeçalho `X-XSRF-TOKEN` em **todo** POST, PATCH, PUT e DELETE, inclusive login e cadastro.
- `GET /csrf` → `200 {"token":"...","headerName":"X-XSRF-TOKEN"}`. O front chama ao carregar cada página e **depois de login ou logout** (o token muda).
- Requisição sem sessão em rota protegida: **401 JSON**, nunca redirecionamento HTML.

### 1.2 Paginação
Parâmetro de query `pagina` (base 0, padrão 0). O tamanho é **fixo em 20**; outros parâmetros de tamanho são ignorados. Resposta:
```json
{ "conteudo": [ ... ], "pagina": 0, "tamanho": 20, "totalElementos": 57, "totalPaginas": 3 }
```
Não serializar `org.springframework.data.domain.Page` direto; converter para o record `Pagina<T>`.

## 2. Erros — formato único
```json
{
  "status": 400,
  "codigo": "VALIDACAO",
  "mensagem": "Verifique os campos destacados.",
  "campos": { "titulo": "O título deve ter de 3 a 100 caracteres." }
}
```
`campos` só aparece quando há erro ligado a campo. Nunca retornar stack trace, SQL ou nome de classe.

| HTTP | codigo | Quando |
|------|--------|--------|
| 400 | VALIDACAO | Bean Validation ou regra ligada a um campo (inclui as regras RN03–RN07) |
| 400 | PERFIL_SEM_CONTATO | Publicar sem WhatsApp ou sem aceite (RN02) |
| 400 | DENUNCIA_PROPRIO_ANUNCIO | RN10.2 |
| 400 | BLOQUEIO_NAO_PERMITIDO | RN10.6 / RN13 |
| 400 | ARQUIVO_INVALIDO | Foto que não é JPEG nem PNG |
| 401 | CREDENCIAIS_INVALIDAS | Login com e-mail ou senha incorretos, ou conta bloqueada (mensagem genérica) |
| 401 | NAO_AUTENTICADO | Sem sessão ou sessão de usuário bloqueado |
| 403 | ACESSO_NEGADO | Não é o dono, papel insuficiente ou CSRF inválido |
| 404 | NAO_ENCONTRADO | Não existe ou não está visível (RN08) |
| 409 | EMAIL_JA_CADASTRADO | Cadastro com e-mail existente |
| 409 | ESTADO_INVALIDO | Transição proibida (anúncio fora de ATIVO ou vencido; denúncia já resolvida). Inclui `"statusAtual"` no corpo |
| 409 | DENUNCIA_DUPLICADA | RN10.3 |
| 413 | ARQUIVO_MUITO_GRANDE | Foto acima de 2 MB |
| 429 | MUITAS_TENTATIVAS | 5 logins falhos para o mesmo e-mail em 15 min |
| 500 | ERRO_INTERNO | Inesperado. Mensagem: "Serviço indisponível. Tente novamente." O detalhe vai só para o log |

## 3. Endpoints

| # | Método e rota | Acesso | UC/RF |
|---|---------------|--------|-------|
| 1 | `GET /csrf` | Público | — |
| 2 | `POST /usuarios` | Público | UC01 |
| 3 | `POST /sessao` | Público | UC02 |
| 4 | `DELETE /sessao` | Autenticado | UC02 |
| 5 | `GET /me` | Autenticado | UC02/UC03 |
| 6 | `PATCH /me` | Autenticado | UC03 |
| 7 | `GET /me/anuncios` | Autenticado | UC07/UC08 |
| 7b | `GET /me/anuncios/{id}` | Dono | UC07 |
| 8 | `GET /categorias` | Público | RF03 |
| 9 | `GET /bairros` | Público | RF03 |
| 10 | `GET /anuncios` | Público | UC04 |
| 11 | `GET /anuncios/{id}` | Público | UC05 |
| 12 | `POST /anuncios` | Autenticado | UC06 |
| 13 | `PATCH /anuncios/{id}` | Dono | UC07 |
| 14 | `POST /anuncios/{id}/encerramento` | Dono | UC08 |
| 15 | `GET /anuncios/{id}/contato` | Autenticado | UC09 |
| 16 | `POST /anuncios/{id}/denuncias` | Autenticado | UC10 |
| 17 | `GET /admin/denuncias` | ADMIN | UC11 |
| 18 | `GET /admin/denuncias/{id}` | ADMIN | UC11 |
| 19 | `PATCH /admin/denuncias/{id}` | ADMIN | UC11 + UC12 |
| 20 | `GET /fotos/{id}` | Público | RF04 |

As subseções abaixo usam o número (`#`) da tabela. As referências "§11" e "§18" apontam para esses números.

---

### 2. `POST /usuarios` — cadastrar
```json
// request
{ "nome": "Ana Souza", "email": "ana@exemplo.com", "senha": "segredo123", "tipo": "MORADOR" }
// 201 Created
{ "id": "uuid", "nome": "Ana Souza", "email": "ana@exemplo.com", "tipo": "MORADOR" }
```
Cria a conta com `papel = USUARIO` e `ativo = true`. O e-mail é gravado em minúsculas. **Não** abre sessão: o front redireciona para o login (UC01, passo 5). Erros: 400 VALIDACAO, 409 EMAIL_JA_CADASTRADO.

### 3. `POST /sessao` — entrar
```json
// request
{ "email": "ana@exemplo.com", "senha": "segredo123" }
// 200 OK  → corpo igual ao de GET /me
```
Erros: 401 CREDENCIAIS_INVALIDAS (mensagem "E-mail ou senha inválidos."), 429 MUITAS_TENTATIVAS. Um login bem-sucedido zera o contador de falhas. Implementação em 05_ARQUITETURA §4.

### 4. `DELETE /sessao` — sair
`204`. Invalida a sessão e apaga o cookie.

### 5. `GET /me`
```json
{
  "id": "uuid", "nome": "Ana Souza", "email": "ana@exemplo.com",
  "tipo": "MORADOR", "papel": "USUARIO",
  "whatsapp": "5547999998888", "aceiteContatoEm": "2026-10-20T15:00:00Z",
  "podePublicar": true
}
```
O front usa este endpoint para saber se há sessão: 200 = logado, 401 = visitante.

### 6. `PATCH /me` — atualizar perfil (envia o formulário completo)
```json
{ "nome": "Ana Souza", "tipo": "PEQUENO_PRODUTOR", "whatsapp": "(47) 99999-8888", "aceiteContato": true }
```
`whatsapp` pode ser `null` ou `""` (remove o número e o aceite). Regras em RN02. Retorna `200` com o corpo de GET /me. E-mail e senha não podem ser alterados.

### 7. `GET /me/anuncios?pagina=0`
Página de **MeuAnuncio**, com todos os status, ordenada por `criadoEm DESC`:
```json
{
  "...": "todos os campos de AnuncioDetalhe (§11)",
  "status": "ATIVO", "resultado": null, "encerradoEm": null, "atualizadoEm": "...",
  "visivel": true,
  "podeEditar": true
}
```
`visivel` = RN08. `podeEditar` = `status == ATIVO && expiraEm > agora` (o mesmo vale para encerrar).

### 7b. `GET /me/anuncios/{id}`
Um MeuAnuncio, independente da visibilidade. Usado para carregar o formulário de edição. Erros: 404 (não existe), 403 (não é o dono).

### 8. `GET /categorias`
`200 [{ "id": 1, "nome": "Frutas", "origemAnimal": false }, ...]`, ordenado por id.

### 9. `GET /bairros`
`200 [{ "id": 1, "nome": "Água Verde" }, ...]`, ordenado por nome.

### 10. `GET /anuncios` — busca pública
Query (todos opcionais e cumulativos): `texto` (até 100 caracteres), `bairroId`, `categoriaId`, `modalidade` (DOACAO|VENDA), `pagina`.
Retorna uma página de **AnuncioResumo**, só com anúncios visíveis (RN08), ordenada por `criadoEm DESC, id DESC`:
```json
{
  "id": "uuid", "titulo": "Laranjas do quintal",
  "categoria": { "id": 1, "nome": "Frutas" },
  "bairro": { "id": 11, "nome": "Garcia" },
  "modalidade": "DOACAO", "preco": null,
  "quantidade": 5.000, "unidade": "KG",
  "fotoUrl": "/fotos/9b1d2c4e-7a3f-4c1b-9e2a-5d6f7a8b9c0d",
  "expiraEm": "2026-10-25T03:00:00Z", "criadoEm": "2026-10-20T15:00:00Z"
}
```

### 11. `GET /anuncios/{id}` — detalhe público
404 se não existe ou não está visível. Retorna **AnuncioDetalhe** = AnuncioResumo mais:
```json
{
  "descricao": "...", "conservacao": "Manter refrigerado",
  "validadeAlimento": "2026-10-30", "origemProduto": "Galinhas caipiras do quintal", "inspecao": "SIM 0123 (fictício)",
  "anunciante": { "nome": "Ana Souza", "tipo": "MORADOR" },
  "souDono": false
}
```
**Não incluir:** WhatsApp, e-mail, ID do anunciante. `souDono` é `true` quando a sessão pertence ao dono (o front esconde Contato/Denunciar e mostra Editar).

### 12. `POST /anuncios` — publicar (`multipart/form-data`)
| Parte | Tipo | Obrigatória |
|-------|------|-------------|
| `dados` | `application/json` (AnuncioForm) | sim |
| `foto` | arquivo | não |

**AnuncioForm**
```json
{
  "titulo": "Ovos caipiras", "descricao": "Uma dúzia de ovos de galinhas soltas.",
  "categoriaId": 3, "bairroId": 11,
  "quantidade": 2, "unidade": "DUZIA",
  "modalidade": "VENDA", "preco": 12.00,
  "conservacao": "Manter em local fresco",
  "validadeAlimento": "2026-10-30", "origemProduto": "Quintal próprio", "inspecao": "SIM 0123 (fictício)",
  "disponivelAte": "2026-10-24",
  "excedenteDeclarado": true
}
```
`201` + AnuncioDetalhe + cabeçalho `Location: /anuncios/{id}`. Erros: 400 VALIDACAO, 400 PERFIL_SEM_CONTATO, 400 ARQUIVO_INVALIDO, 413.
Atomicidade (UC06): anúncio e foto são gravados na mesma transação do banco (03_MODELO §3).

### 13. `PATCH /anuncios/{id}` — editar (`multipart/form-data`)
Mesmas partes do POST. `dados` = AnuncioForm **completo** mais `"removerFoto": false`.
- Com `foto` enviada: substitui a anterior (a linha antiga em `foto` é apagada na mesma transação; a nova ganha outro `id`, então a URL muda e o cache do navegador não mostra a foto velha).
- Com `removerFoto = true` e sem `foto`: remove a foto.
- Caso contrário: mantém a foto atual.

Erros: 403 (não é dono), 404, 409 ESTADO_INVALIDO, 400 (incluindo prazo ampliado — RN07). Retorna `200` + AnuncioDetalhe.

### 14. `POST /anuncios/{id}/encerramento`
```json
{ "resultado": "DOADO" }
```
`200` + MeuAnuncio. Erros: 403, 404, 409 ESTADO_INVALIDO, 400 VALIDACAO (resultado incompatível — RN09).

### 15. `GET /anuncios/{id}/contato`
Revalida RN08 **no momento da chamada**. 401 para visitante; 404 se não estiver visível.
```json
{
  "whatsapp": "5547999998888",
  "whatsappFormatado": "(47) 99999-8888",
  "link": "https://wa.me/5547999998888?text=Ol%C3%A1!%20Vi%20seu%20an%C3%BAncio%20%22Ovos%20caipiras%22%20no%20Colheita%20Compartilhada."
}
```
Texto da mensagem (codificado com `URLEncoder` e `+` trocado por `%20`): `Olá! Vi seu anúncio "{titulo}" no Colheita Compartilhada.` Nada é gravado (não há pedido nem reserva).

### 16. `POST /anuncios/{id}/denuncias`
```json
{ "motivo": "Anúncio de produto industrializado, fora do objetivo." }
// 201
{ "id": "uuid", "status": "ABERTA", "criadoEm": "..." }
```
Erros: 401, 404, 400 VALIDACAO, 400 DENUNCIA_PROPRIO_ANUNCIO, 409 DENUNCIA_DUPLICADA.

### 17. `GET /admin/denuncias?status=ABERTA&pagina=0`
`status` opcional (padrão ABERTA). Ordenação: `criadoEm ASC`. Item:
```json
{
  "id": "uuid", "motivo": "...", "status": "ABERTA", "criadoEm": "...",
  "autor": { "id": "uuid", "nome": "Bruno" },
  "anuncio": { "id": "uuid", "titulo": "...", "status": "ATIVO" },
  "anunciante": { "id": "uuid", "nome": "Ana", "papel": "USUARIO", "ativo": true }
}
```

### 18. `GET /admin/denuncias/{id}`
Item acima, mais:
- `"anuncioDetalhe"`: MeuAnuncio completo, **mesmo que o anúncio não esteja visível**;
- `"anunciante"`: inclui também `"email"`;
- `"decisao"`, `"moderador": {"id","nome"}`, `"resolvidaEm"`.

### 19. `PATCH /admin/denuncias/{id}` — resolver (transação única)
```json
{ "procedente": true, "decisao": "Produto industrializado. Removido.", "bloquearAnunciante": false }
```
Regras em RN10.6. Retorna `200` + o mesmo corpo de §18. Erros: 400 VALIDACAO, 400 BLOQUEIO_NAO_PERMITIDO, 404, 409 ESTADO_INVALIDO.

### 20. `GET /fotos/{id}`
`id` = UUID da linha em `foto` (UUID inválido → 404). Lê os bytes no banco e responde com `Content-Type` igual ao `foto.tipo`, `Content-Length` e `Cache-Control: public, max-age=31536000, immutable`. O cache pode ser longo porque uma foto nova sempre recebe um id novo.

## 4. Matriz de autorização (testar todas as linhas — T02, T07, T08)

| Rota | Visitante | USUARIO | USUARIO dono | ADMIN |
|------|-----------|---------|--------------|-------|
| GET /anuncios, /anuncios/{id}, /categorias, /bairros, /fotos/** , /csrf | ✅ | ✅ | ✅ | ✅ |
| POST /usuarios, POST /sessao | ✅ | ✅ | ✅ | ✅ |
| GET/PATCH /me, GET /me/anuncios, DELETE /sessao | 401 | ✅ | ✅ | ✅ |
| GET /me/anuncios/{id} | 401 | 403 | ✅ | 403 |
| POST /anuncios | 401 | ✅ (RN02) | — | ✅ (RN02) |
| PATCH /anuncios/{id}, POST .../encerramento | 401 | 403 | ✅ | 403 (admin não edita anúncio alheio) |
| GET /anuncios/{id}/contato | 401 | ✅ | ✅ | ✅ |
| POST /anuncios/{id}/denuncias | 401 | ✅ | 400 | ✅ |
| /admin/** | 401 | 403 | 403 | ✅ |
