# Arquitetura da API

Visão geral do código e das regras que valem para todas as features. O comportamento de
cada feature está no `README.md` dentro do seu pacote.

## Features

Pacote base: `src/main/java/com/workspace/api`.

- [`user`](../src/main/java/com/workspace/api/user/README.md): contas, login, sessões, perfis e identificação do usuário.
- [`user/preferences`](../src/main/java/com/workspace/api/user/preferences/README.md): preferências de interface salvas na conta.
- [`pages/event`](../src/main/java/com/workspace/api/pages/event/README.md): eventos, tarefas e calendário.
- [`pages/planner`](../src/main/java/com/workspace/api/pages/planner/README.md): resumo do dia e disponibilidade.
- [`pages/habit`](../src/main/java/com/workspace/api/pages/habit/README.md): hábitos, conclusões e métricas.
- [`pages/goals`](../src/main/java/com/workspace/api/pages/goals/README.md): metas semanais e foco da semana.
- [`pages/note`](../src/main/java/com/workspace/api/pages/note/README.md): notas.
- [`pages/home/dashboard`](../src/main/java/com/workspace/api/pages/home/dashboard/README.md): indicadores da página inicial.
- [`pages/home/weather`](../src/main/java/com/workspace/api/pages/home/weather/README.md): clima pela localização.
- [`recurrence`](../src/main/java/com/workspace/api/recurrence/README.md): expansão de regras de repetição.
- [`retention`](../src/main/java/com/workspace/api/retention/README.md): limpeza automática de itens concluídos.
- [`ping`](../src/main/java/com/workspace/api/ping/README.md): endpoint público para manter a instância ativa.

Pacotes de apoio:

- `config`: CORS (`CorsConfig`) e cliente HTTP para APIs externas (`RestClientConfig`).
- `util/domain`: enums compartilhados `Category`, `Priority` e `Role`.

## Camadas

Cada feature segue `controller` → `service` → `repository`, com `dto` para os contratos
HTTP e `domain` (ou `entity`, em módulos antigos) para as entidades JPA.

- `controller`: recebe a requisição, valida o formato do DTO e delega.
- `service`: regras de negócio, cálculos, permissões e transações.
- `repository`: consultas sempre filtradas pelo usuário.
- Entidades nunca são retornadas diretamente; as respostas usam DTOs.

## Regras transversais

- **Autenticação:** toda rota exige `Authorization: Bearer <token>`, exceto `POST`
  `/users/register`, `/users/login`, `/users/refresh`, `/users/logout` e `GET`/`HEAD /ping`.
- **Dono dos dados:** o usuário vem do token (`CurrentUser.id()`), nunca do corpo ou da
  URL. Registros de outra conta respondem `404`.
- **Data e hora:** o `Clock` da aplicação usa o fuso `app.time-zone` (`APP_TIME_ZONE`,
  padrão `America/Sao_Paulo`) para “hoje”, semanas e horários oficiais. Ele é definido em
  `pages/goals/service/GoalsConfiguration` e pode ser substituído nos testes.
- **Formatos:** datas `YYYY-MM-DD`, horários `HH:mm`, instantes em ISO 8601 UTC, semanas
  `YYYY-Www`, JSON em `camelCase`.
- **Concorrência:** escritas que precisam ser exclusivas por conta (agenda, preferências e
  sessões) bloqueiam a linha do usuário com `UserRepository.lockById`. Alterações de um
  registro usam bloqueio pessimista da própria linha.
- **Jobs agendados:** retenção às 03h e limpeza de refresh tokens às 03h30, no fuso
  `app.time-zone`. Ambos são desativados no perfil `test`.
- **Banco:** o schema muda só por migrations Flyway; o Hibernate apenas valida
  (`ddl-auto: validate`). Veja [Banco de dados](operations/DATABASE.md).

## Erros

Os formatos ainda variam por feature:

- Planner e eventos: `{ "status", "message" }`, com `422` para entrada inválida e `409` para conflito.
- Metas, usuários e preferências: `{ "status", "message" }`.
- Sessão (`REFRESH_UNAVAILABLE`, `SESSION_HEADER_REQUIRED`, `HTTPS_REQUIRED`):
  `{ "status", "code", "message", "details" }`.
- Demais casos: formato padrão de erro do Spring Boot.

Para erros novos, use código estável, mensagem e detalhes, como pede o [`AGENTS.md`](../AGENTS.md).

## CORS

`CorsConfig` aceita apenas as origens exatas de `CORS_ALLOWED_ORIGINS`, com credenciais,
os métodos `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` e os headers `Content-Type`,
`Authorization` e `X-PlannerFy-Session`. Origens não listadas recebem `403`.

## Perfis do Spring

- `local`: desenvolvimento em HTTP no localhost (cookie sem `Secure`, sem exigência de HTTPS).
- `test`: H2 em memória, Flyway e jobs desativados, chave JWT fictícia.
- `production`: API em `127.0.0.1:8080` atrás de um proxy TLS na mesma máquina.
- `render`: usado junto com `production` no Render; escuta em `0.0.0.0:$PORT`.

Fora de `local` e `test`, HTTPS é obrigatório. `prod`/`production` mantêm essa exigência
mesmo combinados com `local` ou `test`.
