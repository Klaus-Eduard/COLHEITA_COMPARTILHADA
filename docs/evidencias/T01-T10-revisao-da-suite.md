# Revisão da suíte T01–T10 (issue #17)

Conferência de cada cenário `[A]` de `docs/specs/07_CRITERIOS_DE_ACEITE.md` contra os testes do repositório (nomes seguem o padrão `ucNN_k_...`). **Esta tabela foi feita lendo os nomes e o código dos testes, sem rodar a suíte.** A confirmação de que tudo passa é o CI verde do pull request: anexe aqui o link da execução.

| Teste do plano | Casos de uso | Cenários `[A]` | Onde estão os testes | Situação |
|---|---|---|---|---|
| T01 Cadastro e sessão | UC01, UC02 | 1.1 a 1.4 e 2.1 a 2.7 | `CadastroTest`, `SessaoTest`, `LoginRateLimiterTest` | Coberto |
| T02 Autorização por dono | UC07.1, UC08.4 | 403 para outro usuário | `AnuncianteTest`, `MatrizAutorizacaoTest` (novo) | Coberto |
| T03 Preço e modalidade | UC06.2 | doação com preço, venda sem preço e preço inválido | `AnuncioTest`, `AnuncianteTest` | Coberto |
| T04 Origem animal e categoria | UC06.3, UC06.4 | ovos/mel sem os três campos, categoria inexistente | `AnuncioTest`, `AnuncianteTest` | Coberto |
| T05 Busca e detalhe | UC04, UC05 | 4.1 a 4.4 e 5.1 e 5.2 | `BuscaEDetalheTest` | Coberto |
| T06 Encerramento | UC08 | 8.1 a 8.5 | `AnuncianteTest`, `ContatoTest` (8.5, novo) | Coberto |
| T07 Perfil e contato | UC03, UC09 | 3.1 a 3.5 e 9.1 a 9.3 | `PerfilTest`, `ContatoTest` (novo) | Coberto |
| T08 Denúncia e moderação | UC10, UC11, UC12 | 10.1 a 10.4, 11.1 a 11.6 e 12.1 a 12.4 | `DenunciaTest`, `DenunciarTest`, `ModeracaoTest` (novos) | Coberto |
| T09 Expiração | RF10 | 1 a 3 | `ExpiracaoTest` (novo, issue #14) | Coberto |
| T10 Persistência e hospedagem | RNF05 | todos `[M]` | `docs/evidencias/T10-producao.md` | Manual: depende de Render e Neon |

## Matriz de autorização (04_API §4)

`MatrizAutorizacaoTest` executa uma linha da tabela por vez para os quatro acessos (visitante, usuário, dono do anúncio e ADMIN), com 20 linhas e 4 atores. Se uma rota nova for criada, acrescente uma linha em `linhasDaMatriz()`.

Duas decisões de leitura da tabela, para quem revisar:
- A coluna "USUARIO dono" é testada com a conta que publicou o anúncio; a coluna "USUARIO" com uma conta comum.
- `POST /anuncios` e `POST /anuncios/{id}/denuncias` aceitam o ADMIN (a tabela marca ✅). O front esconde o botão Denunciar do moderador por decisão da equipe, mas a API continua aceitando.

## Cenários manuais `[M]` (precisam de evidência no PR ou na pasta `docs/evidencias`)

- UC04.5: estado vazio com "Limpar filtros".
- UC05.3: anúncio de Ovos com origem, validade e aviso da RN05.
- UC09.4: no celular, "Abrir WhatsApp" e "Copiar número".
- RNF05 (T10): redeploy, HTTPS, cold start e backup/restauração, em `docs/evidencias/T10-producao.md`.
- Roteiro de usabilidade com 5 pessoas e acessibilidade, em `docs/usabilidade/ROTEIRO.md` e `docs/acessibilidade/RELATORIO.md` (issue #18).
