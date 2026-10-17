# 01 — Visão, escopo e glossário

## 1. Produto em uma frase
Site responsivo onde moradores e pequenos produtores de **Blumenau** anunciam **excedentes de alimentos** (doação ou venda). Os interessados encontram as ofertas por bairro e combinam a retirada **por WhatsApp, fora do sistema**.

## 2. O que o sistema faz (MVP = RF01–RF10, todos obrigatórios)

| RF | Função | UC | Resumo verificável |
|----|--------|----|--------------------|
| RF01 | Cadastro e autenticação | UC01, UC02 | Cadastrar, entrar e sair. E-mail único (sem diferenciar maiúsculas de minúsculas), senha com hash e somente conta ativa. |
| RF02 | Perfil e contato | UC03 | Editar nome, tipo, WhatsApp e autorização de divulgação. A revogação oculta os anúncios. |
| RF03 | Busca pública | UC04 | Filtros por texto, bairro, categoria e modalidade. Páginas de 20 itens, mais recentes primeiro. |
| RF04 | Detalhes do anúncio | UC05 | Mostra alimento, foto, quantidade, unidade, bairro, conservação e prazo. **Nunca mostra o telefone.** |
| RF05 | Publicação | UC06 | Publicar excedente próprio com campos condicionais (preço; origem animal). |
| RF06 | Edição e encerramento | UC07, UC08 | O dono edita o anúncio ATIVO e encerra informando o resultado autodeclarado. |
| RF07 | Contato externo | UC09 | Libera o WhatsApp **somente ao usuário autenticado**, depois de revalidar a disponibilidade. |
| RF08 | Denúncias | UC10 | Denunciar anúncio de outra pessoa. No máximo uma denúncia aberta por autor e anúncio. |
| RF09 | Moderação | UC11, UC12 | O ADMIN resolve denúncias, remove anúncios e bloqueia usuários comuns. |
| RF10 | Expiração | rotina | Vencido some da consulta na hora; uma rotina periódica muda o status para EXPIRADO. |

## 3. Fora do escopo (recusar qualquer pedido, de pessoa ou de IA)
Carrinho, checkout, pedidos, pagamentos, comissão, nota fiscal, **chat ou mensagem interna**, reservas, entrega, rotas, geolocalização, publicidade, catálogo de lojas, app nativo, integração com a prefeitura, validação automática de inspeção, **recuperação de senha, troca de e-mail ou senha, desbloqueio de usuário, CRUD de categorias ou bairros, reativação de anúncio**, notificações por e-mail, mais de uma foto.

> Pedido fora desta lista = registrar como "trabalho futuro" e **não implementar** (risco R2 da doc).

## 4. Atores e permissões

| Ator | Como é identificado | Pode |
|------|---------------------|------|
| Visitante | Sem sessão | Buscar, ver detalhes, cadastrar-se, entrar |
| Usuário | Sessão, `papel = USUARIO`, `ativo = true` | Tudo o que o visitante pode, mais: perfil, publicar, editar e encerrar os próprios anúncios, ver contato, denunciar |
| Administrador | Sessão, `papel = ADMIN`, `ativo = true` | Tudo o que o usuário pode, mais: fila de denúncias, remover anúncio, bloquear USUARIO |
| Rotina de expiração | Agendamento interno | Mudar ATIVO vencido para EXPIRADO |

`tipo` (MORADOR / PEQUENO_PRODUTOR) **só classifica** a pessoa; **não altera permissões**.

## 5. Glossário (nomes usados no código)

| Termo | Significado |
|-------|-------------|
| Anúncio | Um excedente de **um** alimento, com quantidade e unidade. Não há estoque nem produto separado. |
| Excedente declarado | Checkbox obrigatório: "Declaro que é excedente próprio". `excedente_declarado` sempre `true`. |
| Aceite de contato | `aceite_contato_em` preenchido = autorização vigente para mostrar o WhatsApp a usuários autenticados. |
| Visível | Anúncio que pode aparecer publicamente. Regra exata em 02_REGRAS, RN08. |
| Disponível até | Data escolhida pelo anunciante; o servidor calcula a partir dela o `expira_em`. |
| Resultado | Autodeclaração no encerramento: DOADO, VENDIDO ou NAO_INFORMADO. Não comprova a transação. |
| Origem animal | Categorias Ovos e Mel (`categoria.origem_animal = true`). |

## 6. Convenção de idioma
- **Domínio, API, banco e mensagens:** português sem acento nos identificadores (`Anuncio`, `expiraEm`, `/anuncios`).
- **Sufixos técnicos:** inglês (`AnuncioController`, `AnuncioService`, `AnuncioRepository`).
- **Textos exibidos ao usuário:** português com acentuação correta.
