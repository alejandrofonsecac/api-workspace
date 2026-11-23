# Executar e testar a API

## Requisitos

- Java 21. O Maven Wrapper já está no projeto (`mvnw` / `mvnw.cmd`); não é preciso instalar Maven.
- PostgreSQL com banco e usuário criados.

## Variáveis de ambiente

Defina no ambiente ou em um arquivo `.env` na raiz do projeto (ignorado pelo Git).
`ApiApplication` carrega o `.env` do diretório de execução antes de iniciar o Spring;
no IntelliJ, use a raiz do projeto como diretório de trabalho.

Obrigatórias:

- `API_URL`: URL JDBC do PostgreSQL, ex.: `jdbc:postgresql://localhost:5432/workspace`.
- `API_USER` e `API_PASSWORD`: credenciais do banco.
- `JWT_SECRET`: pelo menos 32 bytes aleatórios em Base64; veja [JWT](authentication/JWT_CONFIGURATION.md).

Opcionais (padrão entre parênteses):

- `CORS_ALLOWED_ORIGINS` (`http://localhost:4200`): origens do frontend, separadas por vírgula.
- `APP_TIME_ZONE` (`America/Sao_Paulo`): fuso das regras de data e dos jobs.
- `JWT_ISSUER` (`workspace-api`) e `JWT_TTL` (`PT15M`): emissor e validade do access token.
- `REFRESH_MAX_AGE` (`P90D`) e `REFRESH_IDLE_TIMEOUT` (`P30D`): duração máxima e inatividade da sessão.
- `REFRESH_CLEANUP_*`: limpeza de refresh tokens; veja [sessões](authentication/ACCESS_AND_REFRESH_TOKENS.md#limpeza).
- `RETENTION_*`: limpeza de itens concluídos; veja [retenção](../src/main/java/com/workspace/api/retention/README.md#configuração).

## Rodar localmente

Com o PostgreSQL ativo, na raiz do projeto:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

A API fica em `http://localhost:8080`. O perfil `local` permite HTTP; sem ele, as rotas
respondem `426 HTTPS_REQUIRED`. Para usar o frontend, rode `npm start` no `plannerFyFront`
e abra `http://localhost:4200`.

Na primeira execução, o Flyway cria ou atualiza o schema; veja [Banco de dados](operations/DATABASE.md).

## Testes

```powershell
.\mvnw.cmd test
```

A suíte usa o perfil `test`: H2 em memória, Flyway e jobs desativados e chave JWT
fictícia. Ela não acessa nenhum banco real.

Testes opcionais em PostgreSQL ficam desativados até que sua variável seja definida.
Use sempre um banco descartável e local; vários deles apagam dados.

- `PostgresMigrationTest`: `MIGRATION_TEST_URL`, `MIGRATION_TEST_USER`, `MIGRATION_TEST_PASSWORD` (banco vazio).
- `RefreshMigrationTest`: `REFRESH_TEST_URL=jdbc:postgresql://127.0.0.1:<porta>/postgres`, usuário `refresh_test`.
- `BusinessRulesMigrationTest`: `RULES_MIGRATION_TEST_URL=jdbc:postgresql://127.0.0.1:<porta>/<banco>`, usuário `rules_test`.
- `EventMigrationTest`: `-Devent.migration.url=jdbc:postgresql://127.0.0.1:<porta>/event_migration_test`.
- `RetentionPostgresTest`: `-Dretention.test.url=jdbc:postgresql://127.0.0.1:<porta>/retention_test`.

Exemplo:

```powershell
$env:MIGRATION_TEST_URL = 'jdbc:postgresql://localhost:55439/workspace_migration_test'
$env:MIGRATION_TEST_USER = 'workspace_test'
$env:MIGRATION_TEST_PASSWORD = ''
.\mvnw.cmd test
```

## Produção

- Render + Vercel (ambiente atual): [Deploy](operations/DEPLOY_RENDER_VERCEL.md).
- Servidor próprio: defina as variáveis obrigatórias, `SPRING_PROFILES_ACTIVE=production`
  e `CORS_ALLOWED_ORIGINS` com a origem HTTPS do frontend. Gere o pacote com
  `.\mvnw.cmd clean package` e rode `java -jar target\api-0.0.1-SNAPSHOT.jar`. A API
  escuta em `127.0.0.1:8080` e espera um proxy TLS na mesma máquina.
- Antes de qualquer implantação, confira o histórico Flyway do banco de destino.
