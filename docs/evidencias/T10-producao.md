# Evidências T10: persistência e hospedagem (RNF05)

Preencher depois de cada teste do roteiro `docs/producao/PREPARO-APRESENTACAO.md`. Os prints e logs ficam nesta pasta (`docs/evidencias/`), **sem senhas, sem hosts de banco e sem dados pessoais**.

| Campo | Valor |
|---|---|
| URL testada | |
| Data do preenchimento | |
| Quem fez | |

## 1. Redeploy mantém dados e fotos

| Item | Resultado |
|---|---|
| Anúncio criado (título) | |
| Tinha foto? | sim / não |
| Deploy manual feito no Render às | |
| Depois do deploy, o anúncio continuou lá? | sim / não |
| Depois do deploy, a foto continuou lá? | sim / não |
| Evidência (arquivo) | |

## 2. HTTPS e cookie de sessão

| Item | Resultado |
|---|---|
| Endereço com `https://` e cadeado | sim / não |
| Cookie `JSESSIONID` com **HttpOnly** | sim / não |
| Cookie `JSESSIONID` com **Secure** | sim / não |
| Evidência (arquivo) | |

## 3. Cold start

| Item | Resultado |
|---|---|
| Tempo sem acessar o site | ______ minutos |
| Tempo do primeiro acesso (`/bairros`) | ______ segundos |
| Tempo do segundo acesso | ______ segundos |
| Houve erro de conexão com o banco? | sim / não |
| A página inicial carregou os anúncios? | sim / não |
| Evidência (arquivo) | |

## 4. Backup e restauração

| Tabela | Produção | Branch `restore-teste` | Iguais? |
|---|---|---|---|
| usuario | | | |
| bairro | | | |
| categoria | | | |
| anuncio | | | |
| foto | | | |
| denuncia | | | |

| Conferência das chaves estrangeiras | Resultado |
|---|---|
| anuncio_sem_usuario, anuncio_sem_bairro, anuncio_sem_categoria | 0 / outro: ______ |
| foto_sem_anuncio | 0 / outro: ______ |
| denuncia_sem_anuncio, denuncia_sem_autor, denuncia_sem_moderador | 0 / outro: ______ |
| chaves_estrangeiras (esperado 7) | ______ |
| Evidência (arquivo) | |

- [ ] O arquivo `.dump` foi apagado depois do ensaio.

## 5. Entrega ao professor

| Item | Resultado |
|---|---|
| URL e credenciais (administrador e usuário comum) enviadas por canal restrito | sim / não |
| Canal usado (e-mail, mensagem privada) | |
| Data e hora do envio | |
| Professor é colaborador do repositório privado | sim / não |

## Problemas encontrados

| # | O que aconteceu | O que foi feito | Resolvido? |
|---|---|---|---|
| 1 | | | |
