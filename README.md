# Workspace API

## Contexto de desenvolvimento

Consulte [AGENTS.md](AGENTS.md) antes de alterar o projeto. O documento define a divisão de responsabilidades entre frontend e backend, os padrões de código e a direção das migrações pendentes. A mesma diretriz está no repositório `plannerFyFront`.

API para organização pessoal e produtividade, reunindo notas, tarefas, eventos, hábitos e metas em um único workspace.

O projeto oferece planejamento de agenda, acompanhamento de hábitos e definição de foco semanal para metas, além de consulta de clima. Cada usuário possui sua própria conta e acesso aos seus dados.

Desenvolvido com Java 21 e Spring Boot, utiliza PostgreSQL para persistência e autenticação JWT para proteger o acesso.

## Regras de negócio

Consulte [BUSINESS_RULES.md](BUSINESS_RULES.md) para os contratos migrados, os cálculos do servidor, os testes e a pendência de recuperação das migrations V1/V2 antes da implantação.
