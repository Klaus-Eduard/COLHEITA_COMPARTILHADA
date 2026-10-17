# 07 — Critérios de aceite e plano de testes

> Um requisito só é **aceito** quando todos os cenários dele passam e há evidência (teste automatizado verde no CI ou print/vídeo do roteiro manual).
> Os cenários usam o formato **Dado / Quando / Então**. O código `[T0x]` liga cada cenário ao plano de testes da doc (seção 7.2). `[A]` = teste automatizado; `[M]` = roteiro manual.

## 1. Cenários por caso de uso

### UC01 Cadastrar conta — RF01 [T01]
1. [A] Dado um e-mail novo, quando faço POST /usuarios com dados válidos, então recebo 201, a conta tem `papel = USUARIO` e `ativo = true`, o `senha_hash` começa com `{bcrypt}` e **a resposta não contém senha nem hash**.
2. [A] Dado um usuário existente `ana@x.com`, quando tento cadastrar `ANA@x.com`, então recebo 409 EMAIL_JA_CADASTRADO e o banco continua com uma única conta.
3. [A] Quando envio nome com 1 caractere, senha com 7 caracteres e e-mail inválido, então recebo 400 com `campos.nome`, `campos.senha` e `campos.email`.
4. [A] Quando envio `"papel":"ADMIN"` no corpo, então a conta é criada como USUARIO (o campo é ignorado).

### UC02 Autenticar e sair — RF01, RNF01 [T01]
1. [A] Com credenciais válidas, POST /sessao → 200 e GET /me → 200.
2. [A] Com senha errada → 401 CREDENCIAIS_INVALIDAS. Com e-mail inexistente → **a mesma** resposta.
3. [A] Usuário bloqueado com senha correta → 401 CREDENCIAIS_INVALIDAS.
4. [A] Na 6ª tentativa falha em 15 min para o mesmo e-mail → 429. Com o `Clock` avançado 15 min → volta a aceitar.
5. [A] Depois de DELETE /sessao, GET /me → 401.
6. [A] POST sem cabeçalho CSRF → 403.
7. [A] O ID da sessão muda após o login (fixação de sessão).

### UC03 Atualizar perfil — RF02 [T07]
1. [A] PATCH /me com `whatsapp = "(47) 99999-8888"` e `aceiteContato = true` → grava `5547999998888` e preenche `aceite_contato_em`.
2. [A] Aceite existente, mesmo número, `aceiteContato = true` → a data do aceite **não muda**.
3. [A] Número alterado com `aceiteContato = false` → `aceite_contato_em = null`.
4. [A] `aceiteContato = true` sem WhatsApp → 400 `campos.whatsapp`.
5. [A] Depois de revogar o aceite, o anúncio ATIVO do usuário some de GET /anuncios e GET /anuncios/{id} → 404. Ao renovar o aceite, ele volta a aparecer.

### UC04 Buscar anúncios — RF03, RN08 [T05]
1. [A] Com 25 anúncios visíveis, `pagina=0` traz 20 e `pagina=1` traz 5, sem repetição, ordenados por `criadoEm` decrescente.
2. [A] Filtros cumulativos: `bairroId + categoriaId + modalidade + texto` retornam somente a interseção. O texto não diferencia maiúsculas e procura em título e descrição.
3. [A] Não aparecem anúncios: ENCERRADO, REMOVIDO, EXPIRADO, ATIVO com `expira_em` no passado (antes da rotina), ATIVO com validade vencida, ATIVO de usuário bloqueado, ATIVO de usuário sem aceite.
4. [A] `texto = "%"` não retorna todos os anúncios (o curinga é escapado).
5. [M] Sem resultados → aparece o estado vazio com "Limpar filtros".

### UC05 Consultar detalhes — RF04 [T05]
1. [A] Para cada caso do UC04.3, GET /anuncios/{id} → 404.
2. [A] O JSON do detalhe **não contém** as chaves `whatsapp`, `email` nem `senha`.
3. [M] Anúncio de Ovos mostra origem, validade e o aviso da RN05.

### UC06 Publicar anúncio — RF05, RN02–RN07 [T03, T04]
1. [A] Usuário sem WhatsApp ou sem aceite → 400 PERFIL_SEM_CONTATO.
2. [A] DOACAO com preço → 400 `campos.preco`. VENDA com preço 0 ou −1 → 400. VENDA sem preço → 201, e a tela mostra "a combinar". VENDA com 12.50 → 201. **[T03]**
3. [A] Categoria Ovos sem validade, sem origem ou sem inspeção → 400 no campo correspondente. Com os três → 201. Categoria Frutas sem esses campos → 201. **[T04]**
4. [A] Categoria inexistente (por exemplo, id 99, que representaria "leite") → 400 `campos.categoriaId`. **[T04]**
5. [A] Unidade DUZIA com quantidade 1.5 → 400. KG com 1.5 → 201.
6. [A] `disponivelAte` = hoje + 8 → 400. `disponivelAte` depois da validade → 400. Validade ontem → 400.
7. [A] Com `Clock` fixo em 2026-10-20T15:00Z e `disponivelAte = 2026-10-27` → `expira_em = 2026-10-27T15:00Z` (limite de 7 dias corridos). Com `disponivelAte = 2026-10-22` → `expira_em = 2026-10-23T03:00Z` (fim do dia em São Paulo).
8. [A] `excedenteDeclarado = false` → 400.
9. [A] Foto PNG de 1 MB → 201 e `fotoUrl` acessível. Arquivo `.png` com conteúdo de texto → 400 ARQUIVO_INVALIDO. Arquivo de 3 MB → 413.
10. [A] Se a gravação do anúncio falhar (simular), nenhuma linha nova fica na tabela `foto`.
11. [A] Ao trocar a foto na edição, `fotoUrl` muda e a tabela `foto` continua com uma única linha para o anúncio.

### UC07 Editar anúncio próprio — RF06 [T02]
1. [A] O usuário B faz PATCH no anúncio de A → 403, e o anúncio fica inalterado. **[T02]**
2. [A] O dono edita o título → 200 e `atualizado_em` muda.
3. [A] A edição que tenta aumentar o prazo → 400 `campos.disponivelAte`. Reduzir → 200.
4. [A] Editar anúncio ENCERRADO, EXPIRADO, REMOVIDO ou vencido → 409 ESTADO_INVALIDO.
5. [A] Trocar a categoria para Mel sem origem → 400 (as validações de publicação são reaplicadas).

### UC08 Encerrar anúncio próprio — RF06, RN09 [T06]
1. [A] DOACAO + DOADO → 200, `status = ENCERRADO`, `encerrado_em` e `resultado` preenchidos.
2. [A] DOACAO + VENDIDO → 400.
3. [A] Encerrar duas vezes → a 2ª chamada retorna 409, e `encerrado_em` não muda.
4. [A] O usuário B encerra o anúncio de A → 403. **[T02]**
5. [A] Pedir contato não altera `status` nem `quantidade`. **[T06]**

### UC09 Obter contato externo — RF07 [T07]
1. [A] Visitante → 401, e o número não aparece em nenhuma resposta pública.
2. [A] Usuário logado em anúncio visível → 200 com `whatsapp` e um `link` que começa com `https://wa.me/55`.
3. [A] Anunciante que revogou o aceite → 404.
4. [M] No celular, "Abrir WhatsApp" abre o app; "Copiar número" copia.

### UC10 Denunciar anúncio — RF08 [T08]
1. [A] Denunciar o próprio anúncio → 400 DENUNCIA_PROPRIO_ANUNCIO.
2. [A] Segunda denúncia aberta do mesmo autor para o mesmo anúncio → 409. Depois que a primeira é resolvida, uma nova denúncia é aceita.
3. [A] Motivo com 9 caracteres → 400.
4. [A] Depois de denunciar, o anúncio continua visível.

### UC11 Moderar denúncias — RF09 [T08]
1. [A] USUARIO em GET /admin/denuncias → 403. Visitante → 401.
2. [A] Improcedente → denúncia RESOLVIDA com `moderador_id`, `decisao` e `resolvida_em`; o anúncio não muda.
3. [A] Procedente em anúncio ATIVO → anúncio REMOVIDO e denúncia RESOLVIDA (mesma transação).
4. [A] Procedente em anúncio ENCERRADO → REMOVIDO, com `resultado` e `encerrado_em` nulos.
5. [A] Resolver denúncia já RESOLVIDA → 409.
6. [A] Atomicidade: se a resolução falhar (simular exceção depois de remover), o anúncio continua no estado anterior.

### UC12 Bloquear usuário — RF09, RN13 [T08]
1. [A] Procedente + bloquear → anunciante com `ativo = false`. Todos os anúncios dele somem da busca, e o status deles **não muda**.
2. [A] Sessão aberta do usuário bloqueado: a próxima requisição → 401.
3. [A] Bloquear um ADMIN → 400 BLOQUEIO_NAO_PERMITIDO. `bloquearAnunciante = true` com `procedente = false` → 400.
4. [A] Bloquear quem já está bloqueado → 200, sem efeito adicional.

### RF10 Expiração [T09]
1. [A] Anúncio com `expira_em` no passado e status ATIVO não aparece na busca **antes** da rotina rodar.
2. [A] Rodar `expirarVencidos` → status EXPIRADO. Rodar de novo → 0 linhas afetadas e nenhum erro.
3. [A] Anúncio com `expira_em` no futuro não é afetado.

### RNF05 Persistência e hospedagem [T10]
1. [M] Criar dados (com foto) na URL de produção → fazer um novo deploy no Render → dados **e fotos** continuam lá.
2. [M] A URL pública abre por HTTPS; o cookie de sessão tem `Secure` e `HttpOnly` (DevTools → Application → Cookies).
3. [M] Depois de o app hibernar, o primeiro acesso volta a funcionar sozinho (cold start), sem erro de conexão com o banco.
4. [M] `pg_dump` → `pg_restore` em uma branch de teste do Neon (05 §11) → contagens de linhas iguais e FKs válidas. Guardar print ou log.

## 2. Matriz de autorização
Um teste parametrizado (`@ParameterizedTest`) cobre **todas as linhas** de 04_API §4.

## 3. Roteiro manual de interface (RNF03 — usabilidade, 5 participantes)
Tarefas: (1) achar uma doação de frutas no Garcia; (2) criar conta e autorizar o contato; (3) publicar uma dúzia de ovos à venda "a combinar"; (4) encerrar o anúncio como "Vendido"; (5) denunciar um anúncio.
Para cada tarefa, registrar: concluiu (sim/não), tempo e dificuldade observada. Meta da doc: 5/5 depois das correções.
Checklist técnico em 360 px e 1440 px: sem rolagem horizontal; navegação completa só com Tab, Enter e Esc; erros ao lado dos campos; foco visível.

## 4. Definição de pronto (DoD) — vale para todo PR
- [ ] Os critérios de aceite do issue estão cobertos por teste (A) ou por evidência (M) anexada ao PR.
- [ ] `./mvnw verify` verde no CI.
- [ ] O contrato de 04_API foi respeitado; se mudou, 04_API foi atualizado no mesmo PR.
- [ ] Não há segredo, `System.out`, código comentado, TODO sem issue nem dependência nova não aprovada.
- [ ] Revisão aprovada por outro integrante.
- [ ] Telas testadas em 360 px e com teclado (quando houver front).
