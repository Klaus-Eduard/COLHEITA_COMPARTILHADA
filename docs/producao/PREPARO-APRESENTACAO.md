# Produção pronta para a apresentação (issue #19)

Base: `05_ARQUITETURA.md` §7 e §11, `07_CRITERIOS_DE_ACEITE.md` RNF05 (T10) e `08_PLANO_DE_ENTREGAS.md` §6.1.
Evidências: preencher `docs/evidencias/T10-producao.md` e guardar prints e logs na pasta `docs/evidencias/`.

**Datas do plano:** congelamento de funcionalidades em 20/11, **ensaio geral em 26/11 na URL pública**, **entrega e apresentação em 27/11**.

## Quem faz o quê

| Tarefa | Precisa de | Quem |
|---|---|---|
| Variáveis no Render (perfil demo, administrador) | Acesso ao painel do Render | Quem for o dono da conta do projeto |
| Backup e restauração | Docker instalado e a connection string do Neon | Quem tiver Docker |
| Testes de redeploy e de cold start | A URL pública e um usuário de demonstração | Qualquer integrante |
| Enviar URL e credenciais ao professor | Os dados de acesso | Combinar no grupo |

Depois de cada passo, **anote a evidência** em `docs/evidencias/T10-producao.md`.

## 1. Dados de demonstração (perfil `demo`)

Com o perfil `demo`, a aplicação cria, na subida, usuários e anúncios **fictícios** (`05` §7, classe `DemoDataLoader`). Tudo passa pelos mesmos serviços das telas (cadastro, perfil e publicação), então os dados obedecem às regras de negócio.

**O que ele cria**

| Usuário | E-mail | Tipo |
|---|---|---|
| Maria Souza | `maria.demo@exemplo.com` | Morador |
| João Pereira | `joao.demo@exemplo.com` | Pequeno produtor |

A **senha dos dois é o valor de `DEMO_PASSWORD`**. Os dois já têm WhatsApp fictício e autorização de contato, então podem publicar.

Seis anúncios, com prazo de 5 a 7 dias a partir de hoje: laranjas do quintal (doação, Garcia), limão tahiti, alface crespa (venda "a combinar"), mandioca fresca (doação), ovos caipiras e mel de laranjeira (com origem, inspeção e validade). **Quatro têm foto.** A doação de frutas no Garcia serve para a tarefa 1 do teste de usabilidade.

**Quando ele roda:** só se **não existir nenhum anúncio ativo e dentro do prazo**. Por isso:

- um deploy novo **depois de os anúncios vencerem** (prazo máximo de 7 dias) cria anúncios novos, para a busca da apresentação nunca ficar vazia;
- se já houver anúncios ativos (de teste ou reais), **nada é criado nem duplicado**;
- os usuários nunca são duplicados (o e-mail é único).

Se `DEMO_PASSWORD` estiver vazia ou fora de 8 a 72 caracteres, ele **não carrega nada** e escreve um aviso no log. Se algo falhar, a aplicação **sobe do mesmo jeito** e o erro vai para o log: a demonstração nunca derruba a produção.

**No painel do Render, em Environment, conferir:**

| Variável | Valor | Observação |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `demo` | Liga o carregador de dados de demonstração |
| `DEMO_PASSWORD` | uma senha que o grupo escolher | Senha dos dois usuários fictícios |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NOME` | os da conta de moderação | Cria a conta ADMIN na primeira subida, se o e-mail ainda não existir |
| `COOKIE_SECURE` | `true` | Obrigatório em produção (HTTPS) |

Depois de salvar, o Render faz um novo deploy sozinho. **Conferir nos logs do Render** uma destas linhas:

- `Dados de demonstração carregados: 6 anúncios` (deu certo);
- `Já existem anúncios ativos: dados de demonstração não carregados` (já havia anúncios ativos);
- `... DEMO_PASSWORD não tem de 8 a 72 caracteres ...` (corrigir a variável).

**Decisão do grupo se o banco de produção já tem anúncios de teste ativos:** o carregador não roda. Escolham uma saída: (1) esperar os anúncios de teste vencerem, ou apagá-los pela moderação ou pelo banco, e fazer um novo deploy; ou (2) deixar como está e publicar à mão alguns anúncios de demonstração.

Depois do deploy, abrir a URL, conferir os anúncios na busca e entrar com o usuário de demonstração e com o administrador.

## 2. Redeploy mantém dados e fotos (T10, item 1)

1. Na URL pública, entrar com um usuário de demonstração.
2. Publicar um anúncio **com foto**. Anotar o título.
3. No painel do Render: **Manual Deploy**, **Deploy latest commit**. Esperar terminar.
4. Abrir o site de novo. **Esperado:** o anúncio e a foto continuam lá. A sessão cai (é normal: sai quem estava logado), então é preciso entrar de novo.
5. Anotar o resultado e guardar um print do anúncio com a foto depois do deploy.

## 3. HTTPS e cookie de sessão (T10, item 2)

1. Confirmar que o endereço começa com `https://` e que o navegador mostra o cadeado.
2. Entrar no site, apertar **F12**, abrir a aba **Aplicativo** (ou **Application**), **Cookies**, e escolher o endereço do site.
3. No cookie de sessão (`JSESSIONID`), conferir que as colunas **HttpOnly** e **Secure** estão marcadas.
4. Guardar um print.

## 4. Cold start (T10, item 3)

O plano gratuito do Render hiberna depois de cerca de 15 minutos sem acesso, e o Neon suspende o banco sem uso.

1. **Não acessar o site por 20 minutos.**
2. Medir o primeiro acesso, no PowerShell. Esse endereço consulta o banco:

   ```powershell
   Measure-Command { Invoke-WebRequest -UseBasicParsing https://colheita-compartilhada.onrender.com/bairros | Out-Null }
   ```

3. **Esperado:** a resposta vem (pode levar cerca de 1 minuto), **sem erro de conexão com o banco**. Depois, repita o comando: o segundo acesso deve ser rápido.
4. Abrir também a página inicial no navegador e conferir que a busca carrega anúncios.
5. Anotar os dois tempos.

## 5. Backup e restauração (T10, item 4)

O dump contém dados reais do banco (inclusive os hashes das senhas). **Nunca vá para o Git.** Guarde o arquivo em uma pasta fora do repositório e apague depois do ensaio.

### 5.1 Fazer o backup

Use a connection string **direta** do Neon (host **sem** `-pooler`). Se aparecer um erro por causa de `channel_binding=require`, apague esse trecho da string.

No PowerShell, em uma pasta fora do repositório (cmd: troque `${PWD}` por `%cd%`):

```powershell
docker run --rm -v "${PWD}:/backup" postgres:16 pg_dump --format=custom --no-owner -d "postgresql://USUARIO:SENHA@HOST/neondb?sslmode=require" -f /backup/backup_AAAAMMDD.dump
```

Troque `AAAAMMDD` pela data (por exemplo, `20261125`).

### 5.2 Contar as linhas na produção

```powershell
docker run --rm postgres:16 psql "postgresql://USUARIO:SENHA@HOST/neondb?sslmode=require" -c "select 'usuario' tabela, count(*) from usuario union all select 'bairro', count(*) from bairro union all select 'categoria', count(*) from categoria union all select 'anuncio', count(*) from anuncio union all select 'foto', count(*) from foto union all select 'denuncia', count(*) from denuncia order by 1;"
```

Anote os números.

### 5.3 Restaurar em uma branch de teste

1. No painel do Neon, crie a branch **`restore-teste`** a partir de `main` e copie a connection string direta dela.
2. Restaure o dump nela (o `--clean --if-exists` apaga o que já existir antes de recriar):

   ```powershell
   docker run --rm -v "${PWD}:/backup" postgres:16 pg_restore --no-owner --clean --if-exists -d "postgresql://USUARIO:SENHA@HOST_DA_BRANCH_DE_TESTE/neondb?sslmode=require" /backup/backup_AAAAMMDD.dump
   ```

3. Repita a contagem do passo 5.2 **na branch de teste**. **Esperado:** os mesmos números da produção.

### 5.4 Conferir as chaves estrangeiras

Rode na branch de teste. Todos os números devem ser **0**, e o último, **7** (as sete chaves estrangeiras do banco):

```powershell
docker run --rm postgres:16 psql "postgresql://USUARIO:SENHA@HOST_DA_BRANCH_DE_TESTE/neondb?sslmode=require" -c "select (select count(*) from anuncio a left join usuario u on u.id=a.usuario_id where u.id is null) anuncio_sem_usuario, (select count(*) from anuncio a left join bairro b on b.id=a.bairro_id where b.id is null) anuncio_sem_bairro, (select count(*) from anuncio a left join categoria c on c.id=a.categoria_id where c.id is null) anuncio_sem_categoria, (select count(*) from foto f left join anuncio a on a.id=f.anuncio_id where a.id is null) foto_sem_anuncio, (select count(*) from denuncia d left join anuncio a on a.id=d.anuncio_id where a.id is null) denuncia_sem_anuncio, (select count(*) from denuncia d left join usuario u on u.id=d.autor_id where u.id is null) denuncia_sem_autor, (select count(*) from denuncia d left join usuario u on u.id=d.moderador_id where d.moderador_id is not null and u.id is null) denuncia_sem_moderador, (select count(*) from pg_constraint where contype='f') chaves_estrangeiras;"
```

### 5.5 Guardar a evidência e limpar

- Salve em `docs/evidencias/` um print (ou o texto copiado) das duas contagens e da conferência das chaves. **Cuidado:** apague qualquer senha ou host que apareça nele antes de guardar.
- Apague o arquivo `.dump` e, se quiser, a branch `restore-teste` do Neon.

## 6. Enviar a URL e as credenciais ao professor

**Canal restrito:** e-mail direto ou mensagem privada. **Nunca** no Git, no README, em uma issue, em um pull request ou em um print público.

Modelo de mensagem:

> **Assunto:** Colheita Compartilhada: acesso de demonstração
>
> Professor Everton, segue o acesso ao sistema:
>
> - Endereço: https://colheita-compartilhada.onrender.com
> - **Administrador (moderação):** e-mail `_______`, senha `_______`
> - **Usuário comum:** e-mail `_______`, senha `_______`
>
> O primeiro acesso depois de um tempo parado pode levar cerca de 1 minuto, porque o plano gratuito hiberna. Todos os dados do sistema são fictícios.
>
> Atenciosamente, equipe do Colheita Compartilhada.

Também conferir (`08` §6.2): o repositório continua privado e o professor está como **colaborador**.

Depois da avaliação, **troque as senhas** de demonstração.

## 7. No dia da apresentação

- [ ] **Abrir a URL 5 minutos antes**, para o plano gratuito não estar hibernando.
- [ ] Entrar com o administrador e com um usuário comum, para conferir que os dois acessos funcionam.
- [ ] Ter a página de moderação e a de publicar aberta em abas, para não depender do primeiro carregamento.

## 8. Lista final (`08` §6.1)

- [ ] URL pública em HTTPS, com deploy automático da `main`.
- [ ] Dados de demonstração carregados.
- [ ] Redeploy mantém dados e fotos (seção 2).
- [ ] Cold start funciona (seção 4).
- [ ] Backup feito e restauração ensaiada (seção 5).
- [ ] URL e credenciais enviadas ao professor por canal restrito (seção 6).
- [ ] Nenhum segredo no histórico do Git: procurar por `password` e `DB_` antes da entrega.
