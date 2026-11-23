# Planner

Monta a visão de um dia: eventos, horas livres, tarefas sem data e próxima atividade.
Os números exibidos pela tela vêm prontos daqui; o frontend só os desenha.
A criação e a edição de eventos ficam em [Eventos e tarefas](../event/README.md).

## Como funciona

- **Disponibilidade:** o dia tem 24 blocos de uma hora. Um evento com início e fim ocupa
  todos os blocos que intercepta, mesmo parcialmente (`09:30–10:15` ocupa `09h` e `10h`).
- **Tempo planejado e livre:** `plannedMinutes` e `freeMinutes` são calculados pelos
  blocos (`blocos × 60`), não pela soma exata das durações. `freeTimePercentage` é a
  porcentagem de blocos livres, arredondada.
- **Tarefas sem data:** `undatedCount` conta todos os registros do usuário sem `date`.
- **Próxima atividade:** o primeiro evento do dia, por horário de início, que tenha
  início, não esteja concluído e ainda não tenha começado. Para datas passadas é `null`;
  para hoje, considera o horário atual do servidor (`app.time-zone`).
- A interface atualiza o resumo periodicamente; a contagem regressiva é só apresentação.

## Endpoints

Ambos exigem autenticação e recebem `date` no formato `YYYY-MM-DD`.

- `GET /planner/summary?date=2030-01-01`

  ```json
  {
    "date": "2030-01-01",
    "events": [ { "id": 7, "title": "Reunião", "startTime": "09:00", "endTime": "10:00" } ],
    "availability": { "...": "mesmo formato de /planner/availability" },
    "undatedCount": 3,
    "eventCount": 1,
    "plannedMinutes": 60,
    "freeMinutes": 1380,
    "freeTimePercentage": 96,
    "nextActivity": { "id": 7, "title": "Reunião" }
  }
  ```

  `events` e `nextActivity` usam o formato completo de evento.

- `GET /planner/availability?date=2030-01-01`

  ```json
  {
    "date": "2030-01-01",
    "totalBlocks": 24,
    "occupiedBlocks": 1,
    "availableBlocks": 23,
    "availableHours": 23,
    "slots": [ { "startTime": "09:00", "endTime": "10:00", "available": false } ]
  }
  ```

  `slots` sempre tem 24 itens; o último termina em `00:00`.

## Erros

- `422` em `/planner/availability` com data fora do formato:
  `{ "status": 422, "message": "date must use the YYYY-MM-DD format." }`.
- `400` em `/planner/summary` com data inválida (formato padrão do Spring).

## Código

- `controller/PlannerController`, `service/PlannerService` (resumo do dia).
- A disponibilidade é calculada em `EventService.getAvailability`.
- `controller/PlannerExceptionHandler`: converte `InvalidPlannerSlotException` em `422`,
  `OccupiedPlannerSlotException` em `409` e erros de validação em `422`, para este
  controller e para o `EventController`.

## Testes

- `BusinessRulesIntegrationTest`: resumo, disponibilidade, próxima atividade e isolamento.
- `PlannerAvailabilityServiceTest`: blocos ocupados e limites do dia.
- `BusinessRulesMigrationTest` (opcional): V3 sobre o schema anterior, com
  `RULES_MIGRATION_TEST_URL=jdbc:postgresql://127.0.0.1:<porta>/<banco>` e usuário `rules_test`.
