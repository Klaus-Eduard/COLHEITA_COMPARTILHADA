# 05 — Arquitetura, stack, hospedagem e padrões de código

> Requisito transversal: **o sistema será hospedado**. Toda escolha abaixo privilegia um deploy simples, gratuito e reproduzível:
> **um único container, sem estado em disco, configurado só por variáveis de ambiente, com o banco migrando sozinho no boot** (D13–D16).

## 1. Stack (fixar versões no `pom.xml` no primeiro commit)

| Item | Escolha | Observação |
|------|---------|------------|
| Linguagem | **Java 21 LTS** | `<java.version>21</java.version>` |
| Framework | **Spring Boot 3.5.x** (última correção da linha 3.5) | Web, Security, Data JPA, Validation, **Actuator** (só health). Não migrar para 4.x durante o projeto. |
| Build | Maven com wrapper (`mvnw`); `<finalName>app</finalName>` | Gera sempre `target/app.jar` (o Dockerfile depende disso) |
| Banco | **PostgreSQL 16** — Neon em produção | Driver `org.postgresql:postgresql`. Criar o projeto Neon **na versão 16**, igual à dos testes e do `pg_dump` |
| Migrações | Flyway: `flyway-core` + `flyway-database-postgresql` | Roda sozinho a cada deploy |
| Testes | JUnit 5, Spring Boot Test, MockMvc, `spring-security-test`, Testcontainers (`spring-boot-testcontainers`, `org.testcontainers:postgresql`, `junit-jupiter`) | Rodam no CI (GitHub Actions), nunca no build da hospedagem |
| Front | HTML5, CSS3 e JavaScript (ES2020, módulos nativos), sem framework e sem build | Servido pelo próprio Spring: **uma URL, um deploy, sem CORS** |
| Empacotamento | **Dockerfile** multi-stage na raiz do repositório | Igual em qualquer provedor |
| Hospedagem | **Render** (app, Docker, gratuito) + **Neon** (PostgreSQL, gratuito) | D14; passo a passo no §11 |

**Proibido sem aprovação registrada em `00_DECISOES.md`:** Lombok, MapStruct, Spring Session, Redis, JWT, bibliotecas JS de UI, frameworks CSS, ORMs alternativos, **qualquer serviço externo além de Render e Neon** (S3, e-mail, filas, CDN), **qualquer gravação em disco local**.

## 2. Estrutura do repositório
```
colheita-compartilhada/
├─ AGENTS.md                      ← regras para qualquer IA
├─ README.md                      ← como rodar, variáveis, URL de produção, equipe
├─ CLAUDE.md, .github/copilot-instructions.md  ← apontam para o AGENTS.md
├─ Dockerfile, .dockerignore, .gitattributes, .env.example
├─ docs/specs/                    ← estes arquivos 00–08
├─ docs/diagramas/, docs/evidencias/, docs/atas/
├─ pom.xml, mvnw, mvnw.cmd, .mvn/
├─ .github/workflows/ci.yml
├─ .gitignore                     ← inclui .env, target/, .idea/, *.iml
└─ src/
   ├─ main/java/br/com/colheitacompartilhada/
   │  ├─ ColheitaApplication.java
   │  ├─ config/        SecurityConfig, AtivoCheckFilter, ClockConfig, AdminBootstrap, DemoDataLoader
   │  ├─ comum/         ApiExceptionHandler, ErroResposta, Pagina, exceções (RegraNegocio, EstadoInvalido, NaoEncontrado, AcessoNegado, Conflito)
   │  ├─ usuario/       Usuario, Papel, TipoParticipante, UsuarioRepository, UsuarioService,
   │  │                 UsuarioAutenticado (UserDetails), UsuarioDetailsService, LoginRateLimiter,
   │  │                 AuthController (/usuarios, /sessao, /csrf), PerfilController (/me, /me/anuncios), dto/
   │  ├─ catalogo/      Bairro, Categoria, repositórios, CatalogoController
   │  ├─ anuncio/       Anuncio, enums, AnuncioRepository, AnuncioSpecs, AnuncioService, AnuncioController,
   │  │                 ExpiracaoJob, Foto, FotoRepository, FotoService, FotoController, dto/
   │  └─ denuncia/      Denuncia, StatusDenuncia, DenunciaRepository, DenunciaService,
   │                    DenunciaController, AdminDenunciaController, dto/
   ├─ main/resources/
   │  ├─ application.yml, application-dev.yml
   │  ├─ db/migration/V1__schema.sql, V2__seed_bairros.sql, V3__foto.sql
   │  └─ static/  (ver 06_FRONTEND)
   └─ test/java/br/com/colheitacompartilhada/  (espelha main)
```
**Pacote por funcionalidade.** Uma classe só importa de outro pacote de funcionalidade a **entidade** ou o **service** dele, nunca o controller.

## 3. Camadas e responsabilidades

| Camada | Faz | Não faz |
|--------|-----|---------|
| Controller | Mapear HTTP ↔ DTO, `@Valid`, obter o usuário logado, chamar **um** método do service | Regra de negócio, acesso a repositório, `try/catch` |
| Service (`@Transactional`) | Carregar entidades, **autorizar** (dono/papel), chamar métodos da entidade, salvar, chamar `FotoService` | Montar JSON; repetir regra que está na entidade |
| Entidade | Invariantes e transições (03_MODELO §4) | Acessar repositório ou Spring |
| Repository | Consultas (Spring Data + Specification) | Regras |
| DTO (`record`) | Formato da API (04_API) | Ser entidade; expor `senhaHash` |

Regras gerais: métodos curtos; nomes que dizem a intenção; sem código comentado; sem `Optional.get()` sem verificação; sem `catch (Exception)` fora do `ApiExceptionHandler`; `@Transactional(readOnly = true)` nas consultas.

## 4. Segurança (implementação de referência)

```java
@Bean
SecurityFilterChain api(HttpSecurity http, UsuarioRepository usuarios) throws Exception {
    var csrfHandler = new CsrfTokenRequestAttributeHandler();
    csrfHandler.setCsrfRequestAttributeName(null); // token resolvido em toda requisição -> cookie sempre presente
    http
      .csrf(c -> c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                  .csrfTokenRequestHandler(csrfHandler))
      .authorizeHttpRequests(a -> a
          .requestMatchers(HttpMethod.GET, "/", "/*.html", "/css/**", "/js/**", "/img/**",
                           "/fotos/*", "/csrf", "/categorias", "/bairros",
                           "/anuncios", "/anuncios/*", "/actuator/health/**").permitAll()  // "/anuncios/*" NÃO cobre "/anuncios/{id}/contato"
          .requestMatchers(HttpMethod.POST, "/usuarios", "/sessao").permitAll()
          .requestMatchers("/admin/**").hasRole("ADMIN")
          .anyRequest().authenticated())
      .exceptionHandling(e -> e
          .authenticationEntryPoint(/* escreve ErroResposta 401 NAO_AUTENTICADO */)
          .accessDeniedHandler(/* escreve ErroResposta 403 ACESSO_NEGADO */))
      .formLogin(AbstractHttpConfigurer::disable)
      .httpBasic(AbstractHttpConfigurer::disable)
      .logout(AbstractHttpConfigurer::disable)
      .addFilterAfter(new AtivoCheckFilter(usuarios), SecurityContextHolderFilter.class); // NÃO anotar o filtro com @Component
    return http.build();
}
```
- **Login (`POST /sessao`)**: `LoginRateLimiter.verificar(email)` → `authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, senha))` → criar `SecurityContext` → `request.getSession(true)`; `request.changeSessionId()` (contra fixação de sessão) → `HttpSessionSecurityContextRepository.saveContext(...)`. Qualquer `AuthenticationException` (inclusive `DisabledException`) → `rateLimiter.registrarFalha(email)` + 401 CREDENCIAIS_INVALIDAS.
- **UsuarioAutenticado** implementa `UserDetails`: `id`, `email`, `senhaHash`, `papel` → `ROLE_USUARIO`/`ROLE_ADMIN`, `isEnabled() = ativo`. Nos controllers: `@AuthenticationPrincipal UsuarioAutenticado eu`.
- **AtivoCheckFilter** (RN13): se houver usuário autenticado e `!usuarios.existsByIdAndAtivoTrue(id)` → invalida a sessão, limpa o contexto e responde 401 JSON.
- **LoginRateLimiter**: `ConcurrentHashMap<String, Deque<Instant>>` por e-mail em minúsculas; 5 falhas em 15 min → 429. Usa `Clock`. Funciona porque há uma única instância (D15).
- **Senha**: `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (BCrypt).
- **Ownership**: sempre no service (`anuncio.pertenceA(eu.id())`). Nunca confiar em ID de usuário vindo do corpo.
- **Atrás do proxy HTTPS da hospedagem**: `server.forward-headers-strategy: framework`, para que o Spring enxergue `https` e o IP real.
- **Logs**: nunca registrar senha, hash, WhatsApp, corpo de requisição nem token.

## 5. Tratamento de erros
`@RestControllerAdvice ApiExceptionHandler` é o **único** lugar que converte exceções em HTTP (tabela em 04_API §2):

| Exceção | Resposta |
|---------|----------|
| `MethodArgumentNotValidException`, `HandlerMethodValidationException` | 400 VALIDACAO + `campos` |
| `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException` | 400 VALIDACAO, "Requisição inválida." |
| `RegraNegocioException(codigo, campo, msg)` | 400; se `campo != null`, vai em `campos` |
| `NaoEncontradoException` | 404 |
| `AcessoNegadoException` | 403 |
| `EstadoInvalidoException(statusAtual)` | 409 ESTADO_INVALIDO |
| `ConflitoException(codigo)` e `DataIntegrityViolationException` de `uq_usuario_email` ou `uq_denuncia_aberta` | 409 com o código correspondente |
| `MaxUploadSizeExceededException` | 413 |
| `Exception` | 500 + `log.error` com stack trace |

## 6. Foto (FotoService) — no banco, sem disco
1. `validar(MultipartFile)`: tamanho ≤ 2 MB e bytes iniciais JPEG/PNG (RN06) → retorna `tipo` (`image/jpeg` ou `image/png`).
2. `salvarPara(anuncio, arquivo)`: apaga a foto anterior do anúncio, se houver (`deleteByAnuncioId` + `flush()`), cria uma `Foto` nova, salva e grava `anuncio.fotoUrl = "/fotos/" + foto.getId()`.
3. `removerDe(anuncio)`: `deleteByAnuncioId` e `anuncio.fotoUrl = null`.
4. É sempre chamado **dentro** da transação do `AnuncioService`: se a gravação do anúncio falhar, a foto também não é gravada (UC06).
5. `FotoController` (`GET /fotos/{id}`): `fotoRepository.findById` → `ResponseEntity<byte[]>` com `Content-Type`, `Content-Length` e `Cache-Control: public, max-age=31536000, immutable`.
6. `application.yml`: `spring.servlet.multipart.max-file-size: 2MB`, `max-request-size: 3MB`, `server.tomcat.max-swallow-size: 10MB` (assim o cliente recebe o 413 em vez de conexão resetada).

## 7. Configuração e ambientes
`application.yml` (sem segredos; tudo vem de variáveis de ambiente):
```yaml
spring:
  config.import: optional:file:.env[.properties]   # dev local lê o .env; em produção o arquivo não existe
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 5        # plano gratuito: poucas conexões
      minimum-idle: 0             # libera conexões ociosas -> o Neon pode suspender (economiza horas de compute)
      idle-timeout: 60000
      max-lifetime: 300000
  jpa:
    open-in-view: false
    hibernate.ddl-auto: validate
    properties.hibernate.jdbc.time_zone: UTC
  flyway.enabled: true
  servlet.multipart: { max-file-size: 2MB, max-request-size: 3MB }
server:
  port: ${PORT:8080}              # o Render (e outros PaaS) injeta PORT
  forward-headers-strategy: framework
  tomcat.max-swallow-size: 10MB
  servlet.session:
    timeout: 2h
    cookie: { http-only: true, same-site: lax, secure: ${COOKIE_SECURE:true} }
management:
  endpoints.web.exposure.include: health
  endpoint.health.probes.enabled: true   # /actuator/health/liveness NÃO consulta o banco
app:
  expiracao-intervalo: ${APP_EXPIRACAO_INTERVALO:PT1H}
  admin: { email: ${ADMIN_EMAIL:}, senha: ${ADMIN_PASSWORD:}, nome: ${ADMIN_NOME:Administrador} }
```

| Variável | Exemplo | Dev local | Render | CI |
|----------|---------|-----------|--------|----|
| DB_URL | `jdbc:postgresql://ep-xxx.sa-east-1.aws.neon.tech/neondb?sslmode=require` | `.env` | ✅ | — (Testcontainers) |
| DB_USER / DB_PASSWORD | — | `.env` | ✅ | — |
| COOKIE_SECURE | `false` em dev (http), `true` em produção | `false` | `true` | — |
| ADMIN_EMAIL / ADMIN_PASSWORD / ADMIN_NOME | — | opcional | ✅ | — |
| SPRING_PROFILES_ACTIVE | `demo` para popular dados de apresentação | opcional | opcional | — |
| DEMO_PASSWORD | — | se `demo` | se `demo` | — |

- `.env` (formato `CHAVE=valor`) fica na raiz, está no `.gitignore` e **nunca vai para o Git**. Commitar um `.env.example` só com as chaves.
- **Neon:** use a connection string **direta** (host sem `-pooler`). O endpoint com pooler (PgBouncer em modo transação) causa problemas com Flyway e prepared statements. Converta `postgresql://user:senha@host/db?sslmode=require` em `DB_URL=jdbc:postgresql://host/db?sslmode=require` + `DB_USER` + `DB_PASSWORD`.
- **Banco de dev:** cada integrante cria uma **branch do Neon** (`dev-klaus`, `dev-luan`, `dev-isabele`) a partir de `main`, sem instalar nada; ou usa um PostgreSQL 16 local. **Nunca desenvolver contra o banco de produção.**
- **AdminBootstrap** (`ApplicationRunner`): se `ADMIN_EMAIL` e `ADMIN_PASSWORD` estiverem definidos e o e-mail não existir, cria o usuário com `papel = ADMIN`, `tipo = MORADOR` e `ativo = true`. Registra no log apenas "Administrador inicial criado".
- **DemoDataLoader** (`@Profile("demo")`): se não houver anúncios, cria 2 usuários fictícios (senha = `DEMO_PASSWORD`) e cerca de 6 anúncios variados, com `disponivelAte` relativo a "hoje", para nunca nascerem vencidos.
- `ClockConfig`: `@Bean Clock clock() { return Clock.systemUTC(); }`. Fuso de negócio = constante `America/Sao_Paulo`. **Não depender do fuso do servidor** (o container roda em UTC).

## 8. Expiração
`@EnableScheduling` + `ExpiracaoJob.@Scheduled(fixedDelayString = "${app.expiracao-intervalo}", initialDelayString = "PT1M")` → `anuncioRepository.expirarVencidos(Instant.now(clock))` (UPDATE em lote). O log registra a quantidade de anúncios expirados.
- No plano gratuito o app "dorme" sem tráfego e a rotina não roda nesse período. **Não tem problema:** a visibilidade (RN08) já esconde os vencidos, e a rotina acerta os status quando o app acorda (`initialDelay`).
- Intervalo de 1 h (D10) para não acordar o banco do Neon a toda hora.

## 9. Testes

| Nível | O quê | Como |
|-------|-------|------|
| Unidade (maioria) | Métodos de `Anuncio`, `Usuario`, `Denuncia`, `LoginRateLimiter`, normalização de WhatsApp, cálculo de `expira_em` | JUnit puro com `Clock.fixed(...)`, sem Spring |
| Integração | Fluxos T01–T10 e matriz de autorização (04_API §4) | `@SpringBootTest` + `@AutoConfigureMockMvc` + Testcontainers `postgres:16-alpine` com `@ServiceConnection`; `with(csrf())`, `with(user(...))` |
| Manual | Roteiro de UI e acessibilidade (07 §3) | **Na URL de produção**, em 360 px e 1440 px, só teclado |

- Comando único: `./mvnw verify` (Windows: `mvnw.cmd verify`).
- Sem Docker na máquina: perfil `test-local` apontando para um PostgreSQL **de teste** (pode ser uma branch `test` do Neon). Os testes de integração rodam sempre no CI.
- Nomes de teste em português e descritivos: `naoPermiteEditarAnuncioDeOutroUsuario()`.

## 10. CI/CD
- `.github/workflows/ci.yml`: em push e PR para `main` → `actions/setup-java@v4` (temurin 21, cache maven) → `./mvnw -B verify`. Os runners Ubuntu já têm Docker, então o Testcontainers funciona.
- **CD:** o Render faz deploy automático a cada commit na `main`. Como a `main` só recebe merge com CI verde (proteção de branch), **CI verde = deploy**.
- No Render, ativar "Auto-Deploy: After CI Checks Pass" se a opção estiver disponível; caso contrário, "On Commit".
- O build do Docker **pula os testes** (`-DskipTests`); quem garante a qualidade é o CI.

## 11. Hospedagem — passo a passo (issue 1, na S5)
**Conta:** usar a conta do projeto no GitHub para criar Render e Neon (P01).

**Neon (banco)**
1. Criar o projeto `colheita-compartilhada`, **PostgreSQL 16**, região mais próxima do Render (ex.: AWS `sa-east-1` ou `us-east-1`, a mesma região nos dois).
2. Branch `main` = produção. Criar as branches `dev-<nome>` para cada integrante (§7).
3. Copiar a connection string **direta** e converter em `DB_URL`, `DB_USER` e `DB_PASSWORD`.

**Render (app)**
1. New → Web Service → conectar o repositório privado (dar acesso ao Render no GitHub) → branch `main` → Runtime **Docker** (detecta o `Dockerfile` da raiz).
2. Plano Free. Região igual à do Neon.
3. Environment: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `COOKIE_SECURE=true`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NOME` e, opcionalmente, `SPRING_PROFILES_ACTIVE=demo` + `DEMO_PASSWORD`.
4. Health Check Path: **`/actuator/health/liveness`** (não toca no banco; um health check que consulta o banco manteria o Neon sempre acordado e gastaria a cota gratuita).
5. Deploy. A URL `https://<nome>.onrender.com` vai para o README.

**Limitações conhecidas do plano gratuito (aceitas no MVP)** — conferir as condições vigentes ao criar as contas:
- O app hiberna depois de um período sem acesso e o primeiro acesso demora cerca de 1 minuto. **Antes da apresentação, abrir a URL 5 minutos antes.**
- O Neon suspende o banco sem uso; a primeira consulta leva alguns segundos a mais.
- RAM de cerca de 512 MB: por isso o Dockerfile usa `-XX:MaxRAMPercentage=75 -XX:+UseSerialGC`.
- Reiniciar ou fazer deploy desloga os usuários (sessão em memória, D15).

**Plano B (troca de provedor sem tocar no código):** Koyeb, Railway ou Fly.io também aceitam o mesmo Dockerfile e as mesmas variáveis. O banco continua no Neon.

**Backup (T10):** com Docker, sem instalar nada:
```bash
docker run --rm -v "${PWD}:/backup" postgres:16 pg_dump --format=custom --no-owner -d "postgresql://USER:SENHA@HOST/neondb?sslmode=require" -f /backup/backup_AAAAMMDD.dump
```
Ensaiar `pg_restore` numa branch `restore-teste` do Neon e guardar a evidência em `docs/evidencias/`.
