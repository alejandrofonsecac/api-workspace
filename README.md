# PlannerFy

PlannerFy é uma aplicação de produtividade pessoal. Ela reúne tarefas, agenda,
eventos, hábitos, metas, notas e indicadores diários em um único espaço de trabalho.
Cada conta acessa apenas seus próprios dados e o backend decide as regras, os cálculos
oficiais e as permissões.

Este repositório contém a API do PlannerFy, construída com Java 21, Spring Boot, JPA,
PostgreSQL e Flyway. O frontend Angular fica no repositório `plannerFyFront` e consome
os contratos HTTP desta API.

## O que a API oferece

- Cadastro, login e sessões com access token JWT de curta duração e refresh token
  rotativo, revogável e protegido por cookie.
- Planejamento de tarefas e eventos, incluindo disponibilidade e agendamento
  transacional de tarefas no calendário.
- Acompanhamento de hábitos, sequências, consistência e histórico calculados no servidor.
- Metas, foco semanal, dashboard diário, notas e consulta de clima.
- Isolamento de dados por usuário, autorização por perfil e migrations versionadas.

## Documentação

Toda a documentação complementar está em [`.docs`](.docs):

| Tema | Documento |
| --- | --- |
| Diretrizes de desenvolvimento | [`.docs/development/AGENTS.md`](.docs/development/AGENTS.md) |
| Autenticação e sessões | [`.docs/authentication/ACCESS_AND_REFRESH_TOKENS.md`](.docs/authentication/ACCESS_AND_REFRESH_TOKENS.md) |
| Configuração do JWT | [`.docs/authentication/JWT_CONFIGURATION.md`](.docs/authentication/JWT_CONFIGURATION.md) |
| Regras e contratos de negócio | [`.docs/business-rules/BUSINESS_RULES.md`](.docs/business-rules/BUSINESS_RULES.md) |
| Retenção automática | [`.docs/retention/README.md`](.docs/retention/README.md) |
| Operação da API | [`.docs/operations/API_OPERATION.md`](.docs/operations/API_OPERATION.md) |
| Relatórios técnicos | [`.docs/reports`](.docs/reports) |
| Histórico de metas | [`.docs/goals`](.docs/goals) |
| Referências do Spring Boot | [`.docs/reference/SPRING_BOOT_HELP.md`](.docs/reference/SPRING_BOOT_HELP.md) |

## Desenvolvimento local

Configure `API_URL`, `API_USER`, `API_PASSWORD` e `JWT_SECRET` no ambiente ou em um
arquivo `.env` local. Os detalhes de configuração, variáveis de sessão e os cuidados
para implantação estão na documentação de autenticação.

Execute a API com `./mvnw spring-boot:run` ou `./mvnw.cmd spring-boot:run` no Windows.
Para os testes, use `./mvnw test` ou `./mvnw.cmd test`.

Antes de implantar, confira o histórico Flyway do banco de destino. As migrations são
versionadas e não devem ser reescritas nem substituídas por arquivos inventados.
