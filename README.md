# PlannerFy API

API do PlannerFy, uma aplicação de produtividade pessoal com agenda, tarefas, hábitos,
metas, notas e indicadores diários. Cada conta acessa apenas os próprios dados, e o
backend decide as regras, os cálculos oficiais e as permissões.

Feita com Java 21, Spring Boot, JPA, PostgreSQL e Flyway. O frontend Angular fica no
repositório `plannerFyFront` e consome esta API.

## Início rápido

1. Crie um `.env` na raiz com `API_URL`, `API_USER`, `API_PASSWORD` e `JWT_SECRET`.
2. Rode `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"` (ou `./mvnw` fora do Windows).
3. Rode os testes com `.\mvnw.cmd test`.

Detalhes em [Executar e testar](.docs/SETUP.md).

## Features

Cada pacote de feature tem um `README.md` com o que ela faz, como funciona, endpoints,
erros, tabelas, código e testes.

- [Usuários e autenticação](src/main/java/com/workspace/api/user/README.md): cadastro, login, sessões e perfis.
- [Preferências](src/main/java/com/workspace/api/user/preferences/README.md): configurações de interface salvas na conta.
- [Eventos e tarefas](src/main/java/com/workspace/api/pages/event/README.md): agenda, tarefas sem horário e calendário.
- [Planner](src/main/java/com/workspace/api/pages/planner/README.md): resumo do dia e disponibilidade.
- [Hábitos](src/main/java/com/workspace/api/pages/habit/README.md): frequência, conclusões e métricas.
- [Metas semanais](src/main/java/com/workspace/api/pages/goals/README.md): metas por semana e foco semanal.
- [Notas](src/main/java/com/workspace/api/pages/note/README.md): notas com favorito e fixação.
- [Dashboard](src/main/java/com/workspace/api/pages/home/dashboard/README.md): indicadores da página inicial.
- [Clima](src/main/java/com/workspace/api/pages/home/weather/README.md): temperatura e condição pela localização.
- [Recorrência](src/main/java/com/workspace/api/recurrence/README.md): eventos repetidos com data final.
- [Retenção](src/main/java/com/workspace/api/retention/README.md): limpeza automática de itens concluídos.
- [Ping](src/main/java/com/workspace/api/ping/README.md): endpoint público para manter a instância ativa.

## Documentação geral

- [Diretrizes de desenvolvimento](AGENTS.md)
- [Arquitetura e regras comuns](.docs/ARCHITECTURE.md)
- [Executar e testar](.docs/SETUP.md)
- [Regras de negócio: índice](.docs/business-rules/BUSINESS_RULES.md)
- [Access e refresh token](.docs/authentication/ACCESS_AND_REFRESH_TOKENS.md)
- [Configuração do JWT](.docs/authentication/JWT_CONFIGURATION.md)
- [Banco de dados e migrations](.docs/operations/DATABASE.md)
- [Deploy Render + Vercel](.docs/operations/DEPLOY_RENDER_VERCEL.md)
- [Segurança](.docs/operations/SECURITY.md)
