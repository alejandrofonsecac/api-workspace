# Funcionamento da Workspace API

## Configuração

Configure as variáveis de ambiente ou um arquivo local `.env` (veja `.env.example`):

- `API_URL`, `API_USER`, `API_PASSWORD`: conexão PostgreSQL.
- `JWT_SECRET`: chave aleatória de pelo menos 32 bytes, codificada em Base64. Obrigatória, sem valor padrão.
- `JWT_ISSUER`: emissor dos tokens; padrão `workspace-api`.
- `JWT_TTL`: duração ISO-8601; padrão `PT15M` (15 minutos).
- `REFRESH_MAX_AGE`: duração máxima de uma sessão; padrão `P90D`.
- `REFRESH_IDLE_TIMEOUT`: prazo de inatividade da sessão; padrão `P30D`.
- `CORS_ALLOWED_ORIGINS`: origens do frontend separadas por vírgula; padrão `http://localhost:4200`.
- `JPA_SHOW_SQL`: `true` imprime as consultas SQL no log; padrão `false`. Use apenas localmente.

Para publicar com Vercel e Render, veja [DEPLOY_RENDER_VERCEL.md](DEPLOY_RENDER_VERCEL.md).

Para gerar uma chave no PowerShell:

```powershell
$jwtBytes = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtBytes)
[Convert]::ToBase64String($jwtBytes)
$jwtRandom.Dispose()
```

Guarde o resultado em `JWT_SECRET`. Todas as instâncias da API devem usar a mesma chave e emissor.
Execute com `./mvnw spring-boot:run` ou `.\mvnw.cmd spring-boot:run` no Windows.

## Cadastro e login

- `POST /users/register`: público; cria conta com perfil USER e retorna JWT.
- `POST /users/login`: público; valida e-mail e senha e retorna JWT.
- `POST /users/refresh`: usa o cookie HttpOnly para renovar o access token e rotacionar a sessão.
- `POST /users/logout`: revoga a sessão atual e remove o cookie.
- `POST /users/logout-all`: usuário autenticado; revoga todas as sessões da conta.
- `GET /users/me`: usuário autenticado.

Cadastro:

```json
{
  "name": "Maria",
  "email": "maria@example.com",
  "password": "uma-senha-de-teste"
}
```

Login:

```json
{
  "email": "maria@example.com",
  "password": "uma-senha-de-teste"
}
```

Ambos retornam:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "<uuid>",
    "name": "Maria",
    "email": "maria@example.com",
    "role": "USER"
  }
}
```

O e-mail é normalizado para minúsculas e deve ser único. Senhas usam BCrypt,
com mínimo de 8 caracteres e máximo de 72 bytes em UTF-8. Senhas e hashes não aparecem nas respostas.

Nas demais chamadas, o frontend deve enviar:

```http
Authorization: Bearer <accessToken>
```

Todas as funcionalidades, inclusive clima e os aliases antigos, exigem autenticação.
O access token expira em 15 minutos; o refresh token fica somente em cookie `HttpOnly`,
é rotacionado a cada uso e pode ser revogado pelo logout. Tokens inválidos, expirados ou
de contas desativadas retornam `401`. Consulte a documentação completa em
[`ACCESS_AND_REFRESH_TOKENS.md`](../authentication/ACCESS_AND_REFRESH_TOKENS.md).

## Perfis

Cada conta tem um perfil do enum `com.workspace.api.util.domain.Role`: `USER`, `USER_PLUS` ou `ADMIN`.
Todos usam as funcionalidades existentes e acessam apenas seus próprios dados.
`USER_PLUS` está disponível para futuras regras de produto; ainda não há funcionalidades exclusivas.
`ADMIN` está disponível para regras administrativas e também acessa apenas seus próprios dados.

O cadastro público sempre cria `USER`, mesmo se o cliente enviar `role`.
Para preparar o primeiro administrador de testes, cadastre a conta normalmente e promova
explicitamente a conta escolhida no banco:

```sql
UPDATE tb_users SET role = 'ADMIN'
WHERE email = 'administrador@example.com' AND enabled = true;
```

Depois, use `PATCH /users/{id}/role`, autenticado como administrador:

```json
{"role": "USER_PLUS"}
```

O perfil e a situação da conta são consultados no banco a cada requisição:
mudanças de perfil e desativação valem também para tokens já emitidos.

## Propriedade dos dados

Notas, tarefas, eventos (`/event` e `/events`), hábitos, metas e foco semanal possuem
`user_id` obrigatório com chave estrangeira para `tb_users`.
As respostas dos registros incluem `userId`; o servidor sempre define o proprietário pelo JWT.
Enviar `userId` no corpo ou na query não permite escolher outro proprietário.
Na criação, IDs de registros enviados pelo cliente também são ignorados.

Listagens, contagens, quadro de metas, conclusão de hábitos, disponibilidade e conflitos
do planner são calculados por usuário. Duas pessoas podem reservar o mesmo horário.
Consultar ou alterar um registro de outro usuário retorna `404`.
O foco semanal usa a chave composta `(user_id, week_id)`.
As datas de conclusão de hábitos pertencem ao hábito e herdam seu proprietário.

## Migração do banco

O Flyway executa as migrações versionadas antes do Hibernate, que agora apenas valida o esquema
(`ddl-auto: validate`). Faça backup antes de atualizar um banco existente.

- `V1`: cria o esquema original quando o banco é novo, preservando tabelas existentes.
- `V2`: cria usuários, vincula os dados, adiciona índices e altera a chave do foco semanal.
- `V3`: adiciona os contratos e campos das regras de negócio migradas.
- `V4`: cria as sessões de refresh token, suas famílias e as restrições de expiração.

Registros anteriores à autenticação são preservados e associados ao usuário desativado
`00000000-0000-0000-0000-000000000000`, com e-mail `legacy@workspace.invalid`.
Essa conta não pode fazer login. Novas contas começam sem acesso aos registros antigos.
Após identificar o dono real, reatribua os registros desejados por SQL em uma transação.
Exemplo para uma nota:

```sql
BEGIN;
UPDATE tb_note SET user_id = '<uuid-do-usuario-cadastrado>'
WHERE id = '<id-da-nota>'
  AND user_id = '00000000-0000-0000-0000-000000000000';
COMMIT;
```

Repita conforme necessário para `tb_tasks`, `tb_event`, `events`, `tb_habit`, `tb_goals`
e `tb_goal_focus`. Para foco semanal, resolva antes qualquer conflito com um foco já existente
do usuário na mesma semana. As conclusões de hábitos permanecem vinculadas ao hábito.
Não habilite a conta reservada para dados antigos.

## Testes

`./mvnw test` (Windows: `.\mvnw.cmd test`) executa os testes com H2, incluindo JWTs reais,
isolamento entre contas nos três perfis, tentativas de trocar proprietário, permissões,
validação, CORS e os cenários anteriores de metas, planner, notas e clima.

Os testes de migração PostgreSQL são opcionais e exigem um banco descartável vazio:

```powershell
$env:MIGRATION_TEST_URL = 'jdbc:postgresql://localhost:55439/workspace_migration_test'
$env:MIGRATION_TEST_USER = 'workspace_test'
$env:MIGRATION_TEST_PASSWORD = ''
.\mvnw.cmd test
```

`PostgresMigrationTest` verifica o banco novo com validação Hibernate e uma instalação anterior
com registros em todas as funcionalidades. Use apenas uma conexão de testes.
Sem `MIGRATION_TEST_URL`, esses dois testes são ignorados.

Referência técnica: [Spring Security — JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
