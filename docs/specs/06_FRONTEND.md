# 06 — Frontend (HTML/CSS/JS puro)

> Decisão D01. Multi-página, sem build, sem framework. Tudo em `src/main/resources/static/`.

## 1. Estrutura
```
static/
├─ index.html              Busca (UC04)
├─ anuncio.html            Detalhe ?id= (UC05, UC09, UC10)
├─ entrar.html             Login (UC02)
├─ cadastro.html           Cadastro (UC01)
├─ perfil.html             Perfil e aceite (UC03)
├─ meus-anuncios.html      Lista do dono + encerrar (UC07, UC08)
├─ anuncio-form.html       Publicar / editar ?id= (UC06, UC07)
├─ admin.html              Fila de denúncias + resolver (UC11, UC12)
├─ css/estilo.css          Um único arquivo; variáveis CSS para cores e espaçamentos
├─ img/                    Logo e placeholder de foto
└─ js/
   ├─ api.js               Único ponto de chamada HTTP
   ├─ sessao.js            getUsuario() (cache de GET /me), exigirLogin(), exigirAdmin()
   ├─ layout.js            Cabeçalho, navegação e rodapé injetados em <header id="topo"> e <footer id="rodape">
   ├─ formatos.js          moeda, quantidade+unidade, datas pt-BR, máscara de WhatsApp, "a combinar"
   ├─ form-erros.js        mostrarErros(form, erro.campos), limparErros(form)
   └─ paginas/<pagina>.js  Um módulo por página (ex.: paginas/index.js)
```
Cada HTML carrega `<script type="module" src="js/paginas/<pagina>.js">`. Nada de JS inline.

## 2. `api.js` — contrato
```js
// Todas as chamadas passam por aqui.
export async function api(metodo, url, { json, formData, query } = {}) { ... }
// - Busca o token de GET /csrf uma vez por página (e de novo após login/logout) e envia o cabeçalho X-XSRF-TOKEN em métodos ≠ GET.
// - credentials: 'same-origin'.
// - json → Content-Type: application/json. formData → deixar o navegador definir o boundary.
// - 2xx: retorna o JSON (ou null em 204).
// - erro: lança ApiErro { status, codigo, mensagem, campos }.
// - 401 em página protegida: redireciona para entrar.html?voltar=<url atual>.
// - Falha de rede: lança ApiErro { status: 0, codigo: 'REDE', mensagem: 'Sem conexão. Tente novamente.' } (RNF07).
export function invalidarCsrf() { ... }
```
**Multipart (publicar/editar):**
```js
const fd = new FormData();
fd.append('dados', new Blob([JSON.stringify(dados)], { type: 'application/json' }));
if (arquivo) fd.append('foto', arquivo);
await api('POST', '/anuncios', { formData: fd });
```

## 3. Telas

### 3.1 Cabeçalho (todas as páginas)
- Visitante: logo (→ index), "Entrar", "Cadastrar".
- Usuário: logo, "Publicar" (→ anuncio-form), "Meus anúncios", "Perfil", "Sair".
- ADMIN: tudo acima, mais "Moderação".
- "Sair" → `DELETE /sessao` → `invalidarCsrf()` → index.
- Rodapé: aviso da RN12 e "Para correção ou exclusão de dados, contate: <e-mail da equipe>" (RNF02).

### 3.2 `index.html` — Busca
- Formulário: texto, bairro (select de `GET /bairros`), categoria (select de `GET /categorias`), modalidade (Todas/Doação/Venda), botão "Buscar" e "Limpar filtros".
- Os filtros vão para a URL (`?texto=&bairroId=&...&pagina=`), assim voltar e recarregar mantêm a busca.
- Cards: foto (ou placeholder), título, categoria, bairro, "Doação" ou preço (`R$ 12,00 / dúzia`, ou "a combinar"), quantidade + unidade, "Disponível até dd/mm".
- Paginação: "Anterior" / "Página X de Y" / "Próxima".
- Estados: carregando ("Carregando anúncios…"); **vazio** ("Nenhum anúncio encontrado." + botão "Limpar filtros"); erro ("Serviço indisponível. Tente novamente.", sem detalhes técnicos).

### 3.3 `anuncio.html?id=` — Detalhe
- Todos os campos de AnuncioDetalhe. Para categoria animal, um bloco "Informações declaradas" com origem, validade e inspeção, mais o aviso da RN05.
- 404 → "Este anúncio não está mais disponível." + link para a busca.
- Botão **"Entrar em contato"**:
  - visitante → `entrar.html?voltar=anuncio.html?id=...`;
  - logado → `GET /anuncios/{id}/contato` → mostrar o número formatado, o botão "Abrir WhatsApp" (`link`, `target="_blank" rel="noopener"`) e o botão "Copiar número" (`navigator.clipboard`, com fallback de selecionar o texto);
  - 404 na chamada → mensagem de indisponível.
- Botão **"Denunciar"** (somente logado e `souDono = false`): abre um `<dialog>` com textarea (contador 10–500) → `POST /anuncios/{id}/denuncias`. Sucesso: "Denúncia enviada. Obrigado." 409: "Você já tem uma denúncia aberta para este anúncio."
- `souDono = true`: esconder Contato e Denunciar; mostrar "Editar" (→ anuncio-form?id=).

### 3.4 `entrar.html`
E-mail e senha. Sucesso: `invalidarCsrf()` → redireciona para `?voltar` (somente se começar com `/` ou com o nome de uma página local; caso contrário, vai para index). ADMIN sem `voltar` → admin.html. Erro 401 → "E-mail ou senha inválidos."; 429 → "Muitas tentativas. Aguarde alguns minutos."

### 3.5 `cadastro.html`
Nome, e-mail, senha (com a regra visível), tipo (radio Morador / Pequeno produtor). 201 → entrar.html com a mensagem "Conta criada. Entre para continuar." 409 → erro no campo e-mail.

### 3.6 `perfil.html` (exige login)
- Nome, tipo, WhatsApp (máscara `(00) 00000-0000`), checkbox **"Autorizo mostrar meu WhatsApp a usuários logados interessados nos meus anúncios"**.
- Ao alterar o número com o checkbox marcado, mostrar "Ao trocar o número, a autorização será renovada."
- Ao desmarcar, mostrar o aviso "Seus anúncios ficarão ocultos enquanto a autorização estiver desligada."
- E-mail exibido somente para leitura.

### 3.7 `anuncio-form.html[?id=]` (exige login)
- Se `GET /me` retornar `podePublicar = false`: em vez do formulário, mostrar "Para publicar, cadastre seu WhatsApp e autorize o contato" + link para o perfil.
- Campos na ordem: título, categoria, descrição, quantidade + unidade, modalidade (radio), preço (**visível só em Venda**, placeholder "Deixe vazio para 'a combinar'"), bairro, conservação, **bloco de origem animal** (visível só quando a categoria tem `origemAnimal`: validade\*, origem\*, identificação da inspeção\*), validade (opcional para vegetais; fica fora do bloco animal quando a categoria é vegetal), disponível até (`<input type="date">` com `min` = hoje e `max` = hoje + 7 dias, limitado pela validade), foto (`accept="image/jpeg,image/png"`, prévia, aviso "até 2 MB"), checkbox "Declaro que é excedente próprio".
- Validação no cliente igual à tabela de 02_REGRAS (tamanho, inteiro por unidade, preço > 0, foto ≤ 2 MB). O servidor continua sendo a autoridade.
- Edição: carrega os dados de `GET /me/anuncios/{id}`; se `podeEditar = false`, mostra "Este anúncio não pode mais ser editado." Em vez de `disponivelAte`, mostra o prazo atual e permite **somente reduzir** (`max` = data do `expiraEm` atual).
- O botão "Publicar"/"Salvar" fica desabilitado durante o envio (evita duplicidade). Em erro, **preservar tudo o que foi preenchido** e marcar os campos.

### 3.8 `meus-anuncios.html` (exige login)
- Lista paginada com selo de status (Ativo, Encerrado, Expirado, Removido) e, se `ativo && !visivel`, o aviso "Oculto: verifique a autorização de contato no perfil."
- Ações quando `podeEditar`: "Editar", "Encerrar".
- "Encerrar" abre `<dialog>` com as opções compatíveis com a modalidade (Doado/Não informado ou Vendido/Não informado) → `POST .../encerramento`. 409 → recarregar a lista e mostrar "O anúncio mudou de situação: <status>."

### 3.9 `admin.html` (exige ADMIN; usuário comum → mensagem "Acesso restrito" e nenhum dado)
- Abas "Abertas" e "Resolvidas" (`status`). Lista com motivo, anúncio, anunciante e data.
- Ao abrir uma denúncia: `GET /admin/denuncias/{id}` → detalhe do anúncio (mesmo removido), dados do anunciante e formulário com: procedente (Sim/Não), decisão (textarea), "Bloquear anunciante" (habilitado só quando procedente, o anunciante é USUARIO e está ativo) → `PATCH`. Antes de enviar com bloqueio, confirmar com "Bloquear <nome>? Todos os anúncios dele ficarão ocultos."
- Denúncia resolvida: somente leitura.

## 4. Acessibilidade e responsividade (RNF03 — critérios de aceite)
- Todo input com `<label for>`; agrupamentos com `<fieldset><legend>`; erros em `<span id="erro-<campo>">` ligados por `aria-describedby`, com `aria-invalid="true"` no campo.
- Mensagens globais em uma região `role="alert"` (erro) ou `aria-live="polite"` (sucesso).
- Tudo operável por teclado; foco visível (`:focus-visible` com contorno de pelo menos 2 px); `<dialog>` com foco preso e Esc para fechar.
- Contraste de pelo menos 4,5:1 no texto. A cor nunca é o único indicador (o selo de status também tem texto).
- `<img alt="Foto: {titulo}">`; o placeholder usa `alt=""`.
- Layout mobile-first; **sem rolagem horizontal de 360 px a 1440 px**; cards em grid `repeat(auto-fill, minmax(260px, 1fr))`; alvos de toque com pelo menos 44 px.
- `<html lang="pt-BR">`, `<meta name="viewport" content="width=device-width, initial-scale=1">`, um `<h1>` por página.

## 5. Segurança no front
- Inserir texto vindo da API **somente** com `textContent` ou `createElement`. **Proibido `innerHTML` com dados.**
- Nunca guardar dados do usuário em `localStorage` (a sessão já está no cookie).
- O redirecionamento `?voltar` só aceita caminhos locais (§3.4).
