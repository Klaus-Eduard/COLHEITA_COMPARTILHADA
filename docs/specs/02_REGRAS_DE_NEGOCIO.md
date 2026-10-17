# 02 — Regras de negócio e validações

> Toda regra é validada **no servidor**. O front pode repetir validações para dar retorno rápido, mas o servidor nunca confia nele.
> Fuso para datas de calendário: **`America/Sao_Paulo`**. Instantes trafegam e são gravados em UTC (`timestamptz`).
> "Agora" = `Instant.now(clock)` (bean `Clock`, ver D09).

## RN01 — Público e território
Atendemos apenas Blumenau (todo bairro pertence a Blumenau; o CHECK está no banco). Não existe vitrine de loja, importação de catálogo nem publicidade.

## RN02 — Conta e contato
1. Para **publicar** são exigidos: `ativo = true`, `whatsapp` preenchido e `aceite_contato_em` preenchido. Se faltar algo, a resposta é 400 `PERFIL_SEM_CONTATO`.
2. `PATCH /me` com `aceiteContato = true` exige `whatsapp`. Se faltar: 400 `VALIDACAO`, campo `whatsapp`.
3. Quando o `whatsapp` é **alterado**:
   - com `aceiteContato = true` na mesma requisição, `aceite_contato_em = agora`;
   - sem isso, `aceite_contato_em = null`.
4. Quando `aceiteContato = true` e o número não mudou: se já havia aceite, **mantém a data original**; se não havia, grava `agora`.
5. `aceiteContato = false` grava `aceite_contato_em = null`. Os anúncios **continuam ATIVO**, porém deixam de ser visíveis (RN08). Ao renovar o aceite, voltam a aparecer, desde que não tenham vencido.
6. Remover o WhatsApp força `aceite_contato_em = null` (há um CHECK no banco).

## RN03 — Modalidade e preço
| modalidade | preco |
|------------|-------|
| DOACAO | Deve ser `null`. Se vier preenchido: 400, campo `preco`. |
| VENDA | Opcional. Se informado, deve ser `> 0` e ter no máximo 2 casas decimais. Refere-se à unidade escolhida. Sem preço, a tela mostra **"a combinar"**. |

## RN04 — Categorias aceitas
Existem apenas 4 categorias fixas, criadas por migração: **Frutas**, **Verduras e legumes**, **Ovos**, **Mel**. Qualquer outro alimento (leite, carne, pescado, refeição pronta etc.) não pode ser anunciado porque não há categoria para ele. `categoriaId` inexistente: 400, campo `categoriaId`.

## RN05 — Origem animal (Ovos, Mel)
| Campo | Categoria animal | Categoria vegetal |
|-------|------------------|-------------------|
| `validadeAlimento` | **Obrigatório** | Opcional |
| `origemProduto` | **Obrigatório** (1–200 caracteres) | Opcional |
| `inspecao` | **Obrigatório** (1–200 caracteres) — identificação da inspeção declarada | Opcional |
| `conservacao` | Obrigatório (todas) | Obrigatório (todas) |

A tela mostra o aviso: *"As informações são declaradas pelo anunciante e não certificam o alimento."*

## RN06 — Quantidade e foto
- `quantidade > 0`, no máximo 3 casas decimais e no máximo 9.999.999,999 (`numeric(10,3)`).
- `unidade` ∈ {KG, LITRO, UNIDADE, MACO, DUZIA}. Para **UNIDADE, MACO e DUZIA a quantidade deve ser inteira** (`quantidade.stripTrailingZeros().scale() <= 0`).
- Foto: no máximo **1**, opcional, **JPEG ou PNG**, **≤ 2 MB**, armazenada **no PostgreSQL** (tabela `foto`, D13). O tipo é validado pelos **bytes iniciais** (JPEG `FF D8 FF`; PNG `89 50 4E 47 0D 0A 1A 0A`), nunca pela extensão nem pelo `Content-Type` enviado. Arquivo inválido: 400 `ARQUIVO_INVALIDO`. Arquivo grande demais: 413 `ARQUIVO_MUITO_GRANDE`.

## RN07 — Prazo (`expira_em`)
O usuário informa `disponivelAte` (LocalDate). Define-se:
- `hoje` = data atual em America/Sao_Paulo;
- `fimDoDia(d)` = início do dia `d + 1` em America/Sao_Paulo, convertido para Instant (limite exclusivo).

**Na publicação:**
1. `hoje <= disponivelAte <= hoje + 7 dias`. Fora desse intervalo: 400, campo `disponivelAte`.
2. Se houver `validadeAlimento`: `validadeAlimento >= hoje` (caso contrário: 400, campo `validadeAlimento`) e `disponivelAte <= validadeAlimento` (caso contrário: 400, campo `disponivelAte`).
3. `expira_em = min(fimDoDia(disponivelAte), criado_em + 7 dias)`.

**Na edição** (a edição nunca amplia o prazo):
1. Aplicam-se as mesmas validações, usando `hoje` do momento da edição.
2. `novoExpiraEm = min(fimDoDia(disponivelAte), criado_em + 7 dias)`.
3. Se `novoExpiraEm > expira_em atual`: 400, campo `disponivelAte`, mensagem *"A edição não pode ampliar o prazo do anúncio."*

## RN08 — Visibilidade (regra única, usada em busca, detalhe, contato e denúncia)
Um anúncio é **visível** quando **todas** as condições abaixo valem:
```
anuncio.status = 'ATIVO'
AND anuncio.expira_em > agora
AND (anuncio.validade_alimento IS NULL OR anuncio.validade_alimento >= hoje)
AND usuario.ativo = true
AND usuario.aceite_contato_em IS NOT NULL
```
- Esta regra é implementada **em um único lugar** (Specification `AnuncioSpecs.visivel(agora, hoje)` e método `Anuncio.estaVisivel(agora, hoje)`). Duplicar a lógica é proibido.
- Anúncio não visível é tratado como **inexistente** para o público: 404 `NAO_ENCONTRADO`, inclusive no acesso direto pela URL.
- O dono vê os próprios anúncios em `GET /me/anuncios`, com qualquer status. O ADMIN vê qualquer anúncio pela denúncia.

## RN09 — Propriedade, edição e encerramento
1. Somente o **dono** edita ou encerra. Se não for o dono: 403 `ACESSO_NEGADO`, mesmo trocando o ID na URL (T02).
2. Só é permitido editar ou encerrar quando `status = ATIVO` **e** `expira_em > agora`. Caso contrário: 409 `ESTADO_INVALIDO`, com o status atual no corpo.
3. A compatibilidade do resultado com a modalidade é obrigatória:

| modalidade | resultados aceitos |
|------------|--------------------|
| DOACAO | DOADO, NAO_INFORMADO |
| VENDA | VENDIDO, NAO_INFORMADO |

4. Ao encerrar: `status = ENCERRADO`, `encerrado_em = agora`, `resultado` = informado, `atualizado_em = agora`.
5. Repetir o encerramento retorna 409 `ESTADO_INVALIDO` e não altera nada.
6. Pedir o contato **não** encerra o anúncio nem reduz a quantidade.

## RN10 — Denúncia e moderação
1. Somente usuário autenticado e ativo pode denunciar, e só **anúncio visível** (RN08). Se o anúncio não estiver visível: 404.
2. Denunciar o próprio anúncio: 400 `DENUNCIA_PROPRIO_ANUNCIO`.
3. Já existe denúncia ABERTA do mesmo autor para o mesmo anúncio: 409 `DENUNCIA_DUPLICADA` (há também índice único parcial no banco).
4. `motivo`: entre 10 e 500 caracteres, contados sem os espaços das pontas.
5. Denunciar **não** remove o anúncio.
6. Ao resolver (somente ADMIN, somente denúncia ABERTA; caso contrário 409 `ESTADO_INVALIDO`):
   - `decisao`: obrigatória, entre 1 e 500 caracteres;
   - se `procedente = true` e o anúncio não estiver REMOVIDO, executa `anuncio.remover()` (RN11);
   - se `bloquearAnunciante = true`, exige `procedente = true`. O anunciante precisa ter `papel = USUARIO` e não pode ser o próprio moderador; caso contrário, 400 `BLOQUEIO_NAO_PERMITIDO`. Se já estiver bloqueado, nada muda (sem erro);
   - a denúncia passa a `status = RESOLVIDA`, com `moderador_id`, `decisao` e `resolvida_em = agora`;
   - **tudo acontece em uma única transação.** Se algo falhar, nada é gravado.
7. Outras denúncias abertas do mesmo anúncio continuam na fila; o ADMIN resolve cada uma.

## RN11 — Estados finais
- ENCERRADO, EXPIRADO e REMOVIDO **nunca voltam** a ATIVO. Para um novo excedente, cria-se um novo anúncio.
- `remover()` funciona a partir de ATIVO, ENCERRADO ou EXPIRADO. Ao remover um anúncio ENCERRADO, `resultado` e `encerrado_em` voltam a `null` (CHECK do banco). A justificativa fica registrada na `decisao` da denúncia.
- Bloquear um usuário (`ativo = false`) **não muda o status** dos anúncios dele: eles somem pela RN08.

## RN12 — Limite de responsabilidade
Retirada, negociação, pagamento e entrega acontecem fora do sistema. Mostrar no rodapé e no detalhe do anúncio: *"O Colheita Compartilhada apenas divulga anúncios. Negociação, retirada e conservação são responsabilidade de anunciante e interessado."*

## RN13 — Bloqueio e sessão (decorre de UC12 e RNF01)
- Usuário com `ativo = false` não consegue entrar (mesma mensagem genérica de credencial inválida).
- Em **toda requisição autenticada**, um filtro relê `usuario.ativo`. Se for `false`, invalida a sessão e responde 401 `NAO_AUTENTICADO`.
- ADMIN não pode bloquear a si mesmo nem outro ADMIN.

## RN14 — Expiração (RF10)
A rotina roda a cada 1 hora (configurável; ver D10) e uma vez 1 minuto após o boot. Ela executa:
```sql
UPDATE anuncio SET status = 'EXPIRADO', atualizado_em = now()
WHERE status = 'ATIVO' AND expira_em <= now();
```
- É idempotente: rodar duas vezes é seguro (T09).
- `encerrado_em` e `resultado` continuam `null`.

## Tabela de validação de campos (fonte única para back e front)

| Campo | Regra | Mensagem ao usuário |
|-------|-------|---------------------|
| usuario.nome | Entre 2 e 120 caracteres, contados sem os espaços das pontas | "Informe seu nome (2 a 120 caracteres)." |
| usuario.email | Formato de e-mail, até 254 caracteres, guardado em minúsculas, único | "Informe um e-mail válido." / "E-mail já cadastrado." |
| usuario.senha | Entre 8 e 72 caracteres (limite do BCrypt) | "A senha deve ter de 8 a 72 caracteres." |
| usuario.tipo | MORADOR ou PEQUENO_PRODUTOR | "Selecione o tipo de participante." |
| usuario.whatsapp | Após remover tudo que não é dígito: 10 ou 11 dígitos (DDD + número) → grava `55` + dígitos; ou 12–13 dígitos já começando com `55`. Resultado final deve casar com `^55[1-9][0-9]{9,10}$` | "Informe o WhatsApp com DDD." |
| anuncio.titulo | Entre 3 e 100 caracteres, sem os espaços das pontas | "O título deve ter de 3 a 100 caracteres." |
| anuncio.descricao | Entre 10 e 1000 caracteres, sem os espaços das pontas | "A descrição deve ter de 10 a 1000 caracteres." |
| anuncio.categoriaId | ID existente | "Selecione a categoria." |
| anuncio.bairroId | ID existente | "Selecione o bairro." |
| anuncio.quantidade | RN06 | "Informe uma quantidade maior que zero." / "Para esta unidade, use número inteiro." |
| anuncio.unidade | Enum | "Selecione a unidade." |
| anuncio.modalidade | Enum | "Selecione doação ou venda." |
| anuncio.preco | RN03 | "Doação não tem preço." / "O preço deve ser maior que zero." |
| anuncio.conservacao | Entre 1 e 300 caracteres | "Informe como o alimento deve ser conservado." |
| anuncio.validadeAlimento | RN05, RN07 | "Informe a validade." / "A validade já passou." |
| anuncio.origemProduto | RN05; até 200 caracteres | "Informe a origem do produto." |
| anuncio.inspecao | RN05; até 200 caracteres | "Informe a identificação da inspeção." |
| anuncio.disponivelAte | RN07 | "Escolha uma data entre hoje e 7 dias." / "Não pode passar da validade." |
| anuncio.excedenteDeclarado | Deve ser `true` | "Confirme que é excedente próprio." |
| denuncia.motivo | Entre 10 e 500 caracteres | "Descreva o motivo (mínimo 10 caracteres)." |
| denuncia.decisao | Entre 1 e 500 caracteres | "Registre a decisão." |
