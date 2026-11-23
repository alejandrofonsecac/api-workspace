# Access token e refresh token

Como a API mantém o usuário logado. Este é o contrato entre a API e o frontend; o
resumo da feature está em [Usuários e autenticação](../../src/main/java/com/workspace/api/user/README.md).

## Visão geral

- **Access token:** JWT HS256 que vai no corpo da resposta, em `accessToken`. Vale 15
  minutos (`JWT_TTL`) e é enviado em `Authorization: Bearer`. O frontend o guarda só em memória.
- **Refresh token:** valor aleatório que vai apenas no cookie `plannerfy_refresh`. Serve
  só para pedir um novo access token em `POST /users/refresh`.
- Cada login ou cadastro abre uma **família** (uma sessão). Cada refresh troca o token
  por outro da mesma família.

## Cookie

- Nome `plannerfy_refresh`, `HttpOnly`, `SameSite=Strict`, `Path=/`, sem `Domain`.
- `Secure` fora dos perfis `local` e `test`.
- `Max-Age` igual ao menor prazo restante entre inatividade e limite absoluto.
- Frontend e API precisam estar no mesmo site. Em produção, o frontend chama a API por
  `/api` através do proxy da Vercel; veja [Deploy](../operations/DEPLOY_RENDER_VERCEL.md).

## Prazos

- `REFRESH_IDLE_TIMEOUT` (padrão `P30D`): a sessão expira se ficar esse tempo sem refresh.
  A inatividade conta desde a emissão ou o último refresh, não desde cliques na tela.
- `REFRESH_MAX_AGE` (padrão `P90D`, máximo 90 dias): limite absoluto da família. O refresh
  nunca o prolonga.
- Os dois precisam ser positivos e a inatividade não pode passar do máximo; caso
  contrário, a API não inicia.

## Rotação e reuso

- O token é formado por 32 bytes de `SecureRandom`. Só o SHA-256 é gravado em
  `tb_refresh_tokens`; o valor original aparece apenas no `Set-Cookie`.
- No refresh, o token atual é marcado como usado e outro é emitido na mesma família.
- Usar de novo um token já usado ou revogado revoga a **família inteira**. As outras
  sessões do usuário continuam válidas. A revogação é gravada mesmo com resposta `401`.
- Tokens vencidos e contas desativadas não renovam.
- Emissão, refresh e revogação bloqueiam a linha do usuário, o que serializa as
  operações mesmo entre várias instâncias da API.
- Um novo login no mesmo navegador revoga a família do cookie anterior. Um login que
  falha não troca o cookie.

## Contrato HTTP

Todas as rotas `POST` abaixo exigem o header `X-PlannerFy-Session: 1` e o frontend envia
`withCredentials: true`. Esse header força preflight CORS, e a API só aceita as origens
exatas de `CORS_ALLOWED_ORIGINS`; isso protege contra CSRF, inclusive no login e no logout.

- `POST /users/register`: corpo `name`, `email`, `password` → `201` + cookie.
- `POST /users/login`: corpo `email`, `password` → `200` + cookie.
- `POST /users/refresh`: cookie, corpo vazio ou `{}` → `200` + cookie novo.
- `POST /users/logout`: cookie, corpo vazio ou `{}` → `204`; revoga a família e apaga o cookie.
- `POST /users/logout-all`: `Authorization: Bearer` → `204`; revoga todas as sessões da conta.

Login, cadastro e refresh respondem `accessToken`, `tokenType`, `expiresIn` (segundos) e
`user` (`id`, `name`, `email`, `role`). O logout é idempotente e funciona sem access token.
Respostas de sessão usam `Cache-Control: no-store`.

## Erros

- `401 REFRESH_UNAVAILABLE`: cookie ausente, desconhecido, expirado, revogado,
  reutilizado ou de conta desativada. Corpo:
  `{ "status": 401, "code": "REFRESH_UNAVAILABLE", "message": "Session expired or revoked", "details": [] }`.
  A resposta não diferencia reuso de token inválido e não apaga o cookie, para não
  sobrescrever o cookie de uma resposta concorrente.
- `403 SESSION_HEADER_REQUIRED`: falta o header de sessão.
- `426 HTTPS_REQUIRED`: requisição sem HTTPS fora de `local`/`test`.
- Login e cadastro mantêm os erros descritos em [Usuários](../../src/main/java/com/workspace/api/user/README.md#erros).

Revogar uma sessão **não invalida na hora** o access token já emitido: ele vale até
expirar (15 minutos, mais a pequena tolerância de relógio do decoder). Desativar a conta
bloqueia também o access token.

## Limpeza

`RefreshTokenCleanupJob` apaga, uma vez por dia, as famílias cujo limite absoluto passou
há mais de 1 dia (margem para diferença de relógio). Tokens usados ou revogados de
famílias ainda dentro do limite são mantidos, pois servem para detectar reuso. A
exclusão roda em lotes com transação própria e `SKIP LOCKED`; o que sobrar fica para a
próxima execução.

- `REFRESH_CLEANUP_ENABLED` (padrão `true`): `false` remove o job.
- `REFRESH_CLEANUP_CRON` (padrão `0 30 3 * * *`): horário no fuso `app.time-zone`.
- `REFRESH_CLEANUP_BATCH_SIZE` (padrão `500`): linhas por lote, de 1 a 10.000.
- `REFRESH_CLEANUP_MAX_BATCHES` (padrão `100`): lotes por execução.

## Frontend (plannerFyFront)

A integração está em `src/app/core/auth` e em `docs/AUTHENTICATION.md` do frontend.

- O access token é considerado vencido 30 segundos antes de `expiresIn`; a próxima
  requisição renova a sessão antes de ser enviada.
- Um `401` provoca um único refresh e uma única repetição da requisição. Requisições
  simultâneas compartilham o mesmo refresh.
- Refresh com `401`, ou novo `401` após a repetição, encerra a sessão e leva a
  `/login?reason=expired`, limpando caches. `403`, `5xx`, timeout e falha de rede não
  são tratados como expiração.
- Ao abrir ou recarregar a aplicação, o frontend tenta restaurar a sessão pelo cookie;
  sem sessão, o visitante continua anônimo, sem loop de redirecionamento.
- Web Locks (`plannerfy-auth-session`) serializa refresh e logout entre abas quando
  disponível. Sem ele, uma colisão entre abas pode acionar a revogação por reuso e exigir
  novo login.
- O logout espera um refresh pendente e mostra erro se falhar. O logout em uma aba não
  encerra as outras na hora: elas saem na renovação seguinte.
- Não há tokens em `localStorage`/`sessionStorage` nem leitura de cookies por JavaScript.

## Testes

- `RefreshSessionIntegrationTest`: login, refresh, expiração, reuso com revogação,
  concorrência, logout local e global, isolamento, conta desativada, CSRF e CORS.
- `SessionRequestFilterTest`: HTTPS e atributos do cookie.
- `RefreshTokenCleanupTest`: limpeza diária.
- `RefreshMigrationTest` (opcional): V4 em PostgreSQL descartável; veja [Setup](../SETUP.md#testes).

Referência: [RFC 9700, seção 4.14](https://www.rfc-editor.org/rfc/rfc9700.html#section-4.14).
