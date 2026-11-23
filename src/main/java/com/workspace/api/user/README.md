# Usuários e autenticação

Cria contas, faz login, mantém a sessão e identifica o usuário de cada requisição.
Todas as outras features usam o usuário autenticado definido aqui para filtrar dados.

## Como funciona

- **Cadastro:** cria a conta com perfil `USER`, mesmo que o cliente envie outro perfil.
  O e-mail é normalizado (sem espaços, minúsculo) e precisa ser único.
- **Senha:** guardada como hash BCrypt; nunca aparece em respostas, tokens ou logs.
  Aceita de 8 caracteres a 72 bytes em UTF-8 (limite do BCrypt; letras acentuadas
  ocupam mais de um byte). E-mail inexistente também executa BCrypt, para não revelar
  pelo tempo de resposta se a conta existe.
- **Sessão:** login e cadastro devolvem um **access token** JWT de 15 minutos no corpo e um
  **refresh token** em cookie `HttpOnly`. O refresh token renova o access token e é
  trocado a cada uso. Detalhes em
  [Access e refresh token](../../../../../../../.docs/authentication/ACCESS_AND_REFRESH_TOKENS.md).
- **Requisições autenticadas:** o cliente envia `Authorization: Bearer <accessToken>`.
  A API valida assinatura, emissor e validade do JWT e consulta a conta no banco a cada
  requisição: contas desativadas são recusadas e o perfil usado é sempre o atual do banco.
  Configuração da chave em [JWT](../../../../../../../.docs/authentication/JWT_CONFIGURATION.md).
- **Usuário atual:** os serviços obtêm o ID por `CurrentUser.id()`. Um `userId` enviado
  no corpo ou na URL nunca escolhe o dono de um registro.
- **Propriedade dos dados:** eventos, notas, hábitos, metas e foco semanal herdam de
  `UserOwnedEntity` (coluna `user_id` obrigatória). Acessar um registro de outra conta
  responde `404`, sem revelar que ele existe.
- **Perfis:** `USER`, `USER_PLUS` e `ADMIN` (`util/domain/Role`). Hoje todos usam as mesmas
  funcionalidades e só veem os próprios dados. Não há endpoint para trocar o perfil: a
  mudança é feita no banco e vale já na próxima requisição.

## Endpoints

Todas as rotas `POST` abaixo exigem o header `X-PlannerFy-Session: 1`. `register`,
`login`, `refresh` e `logout` são públicas; as demais rotas da API, exceto
[`/ping`](../ping/README.md), exigem `Authorization: Bearer <accessToken>`.

- `POST /users/register`: `{ "name", "email", "password" }`; responde `201` + cookie.
- `POST /users/login`: `{ "email", "password" }`; responde `200` + cookie.
- `POST /users/refresh`: usa o cookie; responde `200` com novo access token + novo cookie.
- `POST /users/logout`: revoga a sessão do cookie e apaga o cookie; responde `204`.
- `POST /users/logout-all`: autenticado; revoga todas as sessões da conta; responde `204`.
- `GET /users/me`: autenticado; retorna `{ "id", "name", "email", "role" }`.
- Preferências de interface: [`/users/me/preferences`](preferences/README.md).

Resposta de login, cadastro e refresh:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": { "id": "<uuid>", "name": "Maria", "email": "maria@example.com", "role": "USER" }
}
```

## Erros

- `400`: campos inválidos ou senha acima de 72 bytes. Formato `{ "status": 400, "message": "..." }`.
- `401`: credenciais inválidas, token ausente, inválido ou expirado, ou conta desativada.
- `401 REFRESH_UNAVAILABLE`: refresh ausente, expirado, revogado ou reutilizado.
- `403 SESSION_HEADER_REQUIRED`: rota de sessão sem `X-PlannerFy-Session: 1`.
- `409`: e-mail já cadastrado.
- `426 HTTPS_REQUIRED`: requisição sem HTTPS fora dos perfis `local` e `test`.

## Persistência

- `tb_users` (V2) e `tb_refresh_tokens` (V4, índice de limpeza na V8).
- A V2 associou os dados anteriores à autenticação a uma conta desativada; veja
  [Banco de dados](../../../../../../../.docs/operations/DATABASE.md).

## Código

- `controller/UserController`; `service/UserService` (cadastro, login, emissão do JWT).
- `service/RefreshSessionService` (sessões) e `service/RefreshTokenCleanupJob` (limpeza diária).
- `security/config/SecurityConfiguration`: rotas públicas, JWT e conversão para o usuário.
- `security/config/JwtConfiguration`: chave, encoder e decoder HS256.
- `security/filter/SessionRequestFilter`: HTTPS obrigatório e header de sessão.
- `security/RefreshCookie`, `security/CurrentUser`, `handler/UserExceptionHandler`.

## Testes

- `MultiUserSecurityTest`: login, hash, tokens inválidos, perfis e isolamento entre contas.
- `RefreshSessionIntegrationTest`, `RefreshTokenCleanupTest`, `SessionRequestFilterTest`,
  `ProductionProxyTest`, `JwtConfigurationTest`, `CurrentUserTest`.
- Opcionais em PostgreSQL descartável: `PostgresMigrationTest` (`MIGRATION_TEST_URL`) e
  `RefreshMigrationTest` (`REFRESH_TEST_URL`); veja [Setup](../../../../../../../.docs/SETUP.md#testes).
