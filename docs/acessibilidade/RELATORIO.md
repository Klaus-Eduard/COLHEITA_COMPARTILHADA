# Revisão de acessibilidade e responsividade (issue #18)

Responsável: Isabele. Base: `06_FRONTEND.md` §4 e `07_CRITERIOS_DE_ACEITE.md` §3 (checklist técnico em 360 px e 1440 px).

## Resumo

- 12 estados de tela revisados, em 3 larguras (1280, 360 e 320 px).
- Resultado final da auditoria automática: **0 problemas**.
- Encontrados e corrigidos **4 problemas** (um deles de segurança, ver item 1).
- Ainda falta a **confirmação na URL pública** (seção 6) e o **teste de usabilidade com 5 pessoas** (`docs/usabilidade/ROTEIRO.md`).

## 1. Como a revisão foi feita

| Verificação | Como |
|---|---|
| Auditoria automática | Script com Playwright (Chromium) que abre cada tela e confere nomes dos campos, texto alternativo, idioma, títulos, contraste, tamanho dos alvos, foco visível e rolagem horizontal |
| Teclado | 7 fluxos feitos **só com Tab, Shift+Tab, Enter, Espaço, setas e Esc**, sem mouse |
| Larguras | 1280, 360 e 320 px (320 px equivale a ampliar a página em 400%) |

As telas foram abertas com os **mesmos arquivos do front** servidos por um servidor de teste que simula a API (a API real ainda não tinha todos os endpoints). Por isso a seção 6 pede uma conferência rápida na URL pública.

Telas auditadas: entrar, cadastro, busca (início), anúncio (visitante), anúncio (usuário logado, com contato aberto), perfil (usuário), perfil (moderador), meus anúncios, publicar anúncio (com o bloco de ovos e mel aberto e com erros), editar anúncio, moderação (fila) e moderação (detalhe, com erros).

## 2. Critérios verificados

| Critério (06 §4 e 07 §3) | Resultado |
|---|---|
| Todo campo tem nome (`<label for>`); grupos com `fieldset` e `legend` | OK: nenhum campo sem nome nas 12 telas |
| Erros junto ao campo, ligados por `aria-describedby`, com `aria-invalid` | OK: nenhuma ligação quebrada; erros aparecem ao lado do campo |
| Mensagens globais em região `role="alert"` (erro) ou `aria-live` (sucesso) | OK (conferido no código das 8 páginas) |
| Tudo operável por teclado (Tab, Enter e Esc) | OK nos 7 fluxos testados (seção 4) |
| Foco visível, contorno de pelo menos 2 px | OK: todos os controles percorridos com Tab têm contorno de 3 px (verde; dourado sobre fundo verde escuro) |
| Diálogos: foco preso, Esc fecha, foco volta ao botão | OK nos 3 diálogos (denunciar, encerrar, bloquear) |
| Contraste do texto: 4,5:1 (3:1 para texto grande) | OK em todas as telas, depois da correção do item 2 |
| Contraste das bordas dos campos: 3:1 | OK depois da correção do item 3 |
| A cor nunca é o único indicador | OK: todos os selos de status têm texto; erros têm texto e borda |
| Imagens: `alt="Foto: título"`; placeholder com `alt=""` | OK: nenhuma imagem sem `alt`; ilustrações em SVG marcadas como decorativas |
| `lang="pt-BR"`, `viewport`, um `<h1>` por página, títulos sem pular nível | OK nas 12 telas |
| Link "Ir para o conteúdo" como primeiro item do Tab | OK |
| Alvos de toque de pelo menos 44 px | OK depois da correção do item 4 |
| **Sem rolagem horizontal de 360 a 1440 px** | OK nas 12 telas (também em 320 px) |

## 3. Resultado por tela

| Tela | Rolagem 1280 / 360 / 320 | Controles percorridos com foco visível | Problemas restantes |
|---|---|---|---|
| Entrar | ok / ok / ok | 7 | 0 |
| Cadastro | ok / ok / ok | 9 | 0 |
| Busca (início) | ok / ok / ok | 28 | 0 |
| Anúncio (visitante) | ok / ok / ok | 7 | 0 |
| Anúncio (usuário) | ok / ok / ok | 11 | 0 |
| Perfil (usuário) | ok / ok / ok | 13 | 0 |
| Perfil (moderador) | ok / ok / ok | 11 | 0 |
| Meus anúncios | ok / ok / ok | 10 | 0 |
| Publicar anúncio | ok / ok / ok | 9 | 0 |
| Editar anúncio | ok / ok / ok | 17 | 0 |
| Moderação (fila) | ok / ok / ok | 10 | 0 |
| Moderação (detalhe) | ok / ok / ok | 13 | 0 |

## 4. Fluxos feitos só com o teclado

| Fluxo | Resultado |
|---|---|
| Cadastro: "Ir para o conteúdo", Enter com tudo vazio (foco vai ao primeiro erro), preencher, escolher o tipo com as setas, enviar | OK |
| Login com Enter | OK |
| Denunciar: abrir com Enter, foco no campo, foco preso no diálogo, Esc fecha e o foco volta ao botão, enviar com Enter | OK |
| Pedir contato (WhatsApp) com Enter | OK |
| Encerrar anúncio: abrir, foco preso, Esc fecha e o foco volta ao botão certo | OK |
| Publicar anúncio: Enter com tudo vazio (foco no primeiro erro), trocar a categoria com as setas e abrir o bloco de ovos e mel | OK |
| Moderação: abrir uma denúncia, marcar "Sim", escrever a decisão, marcar o bloqueio, confirmar no diálogo, Esc fecha e o foco volta | OK |

## 5. Problemas encontrados e corrigidos

| # | Problema | Antes | Correção |
|---|---|---|---|
| 1 | **Senha na barra de endereço.** Se a pessoa apertasse Enter antes de o script da página terminar de carregar (conexão lenta), o navegador enviava o formulário do jeito antigo e o e-mail e a **senha iam para a URL** | `entrar.html?email=...&senha=...` | O envio passou a ser registrado no início dos scripts de entrar e cadastro, e todos os formulários receberam `method="post"` (a busca usa `method="get"`, porque não tem dados sensíveis). Testado com o script atrasado em 3 segundos: nada vai para a URL |
| 2 | Contraste da etiqueta "Blumenau · SC" | 3,90:1 (mínimo 4,5:1) | Fundo da etiqueta escurecido |
| 3 | Contraste da borda dos campos | 2,27:1 (mínimo 3:1) | Cor `--cor-borda-campo` de `#a9ae9f` para `#6b6f63` (cerca de 5,1:1) |
| 4 | Alvos de toque abaixo de 44 px | "Ir para o conteúdo" com 40 px, campo de foto com 41 px, títulos em Meus anúncios com 22 px, "Ver como o público vê" com 20 px | `min-height` de 44 px nesses quatro |

Um alarme falso: o título de cada cartão da busca parece ter 21 px, mas o clique cobre o cartão inteiro (a área do link se estende por ele todo), então o alvo real é bem maior que 44 px.

## 6. Conferência final na URL pública (cerca de 10 minutos)

Marque cada item depois de testar no site no ar.

- [ ] **Sem rolagem lateral:** em cada tela, apertar F12, ligar o modo celular (ícone de telefone) e testar a largura de 360 px e depois a de 1440 px. Não pode aparecer barra de rolagem horizontal.
- [ ] **Só teclado:** em Entrar e Cadastrar, usar apenas Tab, Enter e Esc. O contorno verde precisa aparecer em tudo.
- [ ] **Erros ao lado dos campos:** enviar Entrar, Cadastrar e Publicar anúncio vazios. Cada erro aparece colado no seu campo, e o foco vai ao primeiro.
- [ ] **Diálogos:** abrir "Denunciar" e "Encerrar". O Esc fecha e o foco volta ao botão.
- [ ] **Ampliar a página:** apertar Ctrl e + até 200%. Nada pode ficar cortado nem exigir rolagem lateral.
- [ ] **No celular de verdade:** abrir o site, criar uma conta e publicar um anúncio.
- [ ] **Uma tela em outro navegador** (Firefox ou o do celular).
- [ ] **Leitor de tela (recomendado):** ouvir Entrar e Cadastrar com o NVDA (Windows) ou o VoiceOver (celular). Os campos devem ser lidos com o nome e a mensagem de erro.

## 7. O que esta revisão não cobre

- **Leitor de tela:** não foi testado (só a estrutura que ele usa: nomes, títulos, `aria-describedby`, regiões de aviso).
- **Navegadores:** a auditoria foi feita no Chromium. Firefox e Safari não foram testados.
- **Lista que se abre nos campos de escolha** (bairro, categoria): é desenhada pelo sistema e segue o estilo dele.
- **Movimento reduzido:** o CSS respeita `prefers-reduced-motion`, mas isso não foi testado visualmente.
- **URL pública:** ver a seção 6.
