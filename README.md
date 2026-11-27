# Colheita Compartilhada

Site comunitário para divulgar **excedentes de alimentos** em Blumenau. Moradores e pequenos produtores anunciam doação ou venda; interessados encontram as ofertas por bairro e combinam a retirada pelo WhatsApp, fora do sistema.

- **Produção:** https://colheita-compartilhada.onrender.com (health check: [`/actuator/health/liveness`](https://colheita-compartilhada.onrender.com/actuator/health/liveness)). No plano gratuito o app hiberna; o primeiro acesso pode levar cerca de 1 minuto.
- **Specs:** [`docs/specs/`](docs/specs/README.md) — comece pelo README de lá.
- **Diagramas:** [`docs/diagramas/`](docs/diagramas/) (draw.io)

## Stack
Java 21 · Spring Boot 3.5 · PostgreSQL 16 (Neon) · Flyway · HTML/CSS/JS · Docker · Render

## Rodar localmente
Pré-requisitos: JDK 21. O Docker só é necessário para os testes de integração.

1. Copie `.env.example` para `.env` e preencha com os dados da **sua** branch de dev no Neon (`docs/specs/05_ARQUITETURA.md` §7). O `.env` não vai para o Git.
2. Suba a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```
   No Windows, use `mvnw.cmd spring-boot:run`.
3. Abra http://localhost:8080.

## Testes
```bash
./mvnw verify
```

## Deploy
Todo merge na `main` com CI verde é publicado automaticamente no Render. O passo a passo da configuração está em `docs/specs/05_ARQUITETURA.md` §11.

## Variáveis de ambiente
Veja [`.env.example`](.env.example) e `docs/specs/05_ARQUITETURA.md` §7.

## Dados de demonstração (apresentação)
Com `SPRING_PROFILES_ACTIVE=demo` e `DEMO_PASSWORD` (8 a 72 caracteres) definidos, o app cria na subida 2 usuários fictícios (`maria.demo@exemplo.com` e `joao.demo@exemplo.com`, ambos com essa senha) e 15 anúncios variados (9 com fotos geradas pelo próprio código). Só carrega se a conta `maria.demo@exemplo.com` ainda não tiver anúncio ativo no prazo (anúncios de teste de outras pessoas não atrapalham) e nunca impede o app de subir. Os dados de inspeção sanitária são fictícios e aparecem assim. A conta de moderação vem de `ADMIN_EMAIL` e `ADMIN_PASSWORD`. O roteiro completo de preparo da apresentação está em `docs/producao/PREPARO-APRESENTACAO.md`.

## Fluxo de trabalho
Branches `main`, `klaus`, `luan` e `isabele`. Um PR por issue, com revisão de um colega e CI verde. Detalhes em `docs/specs/08_PLANO_DE_ENTREGAS.md` §4.
Assistentes de IA seguem o [`AGENTS.md`](AGENTS.md).

## Equipe
- Klaus Eduard Padilha Petrikovski
- Luan Miguel Pereda
- Isabele Alves Martinelli
