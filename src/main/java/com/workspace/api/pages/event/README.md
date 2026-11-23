# Eventos e tarefas

Guarda os compromissos do usuário. Um registro da tabela `events` é um **evento** quando
tem data e horário, e uma **tarefa** quando não tem data ou não tem horário. A mesma API
cria, agenda, conclui e exclui os dois. Também expõe a lista do calendário.

## Como funciona

- Agendar uma tarefa é atualizá-la com `date`, `startTime` e `endTime` pelo `PUT /events/{id}`.
  A validação de conflito e a gravação acontecem na mesma transação.
- Horários têm precisão de minutos, no mesmo dia, entre `00:00` e `24:00`, com início
  menor que o fim. `24:00` só vale como fim e é gravado como meia-noite (`EventTimes`).
  Para atravessar a madrugada, crie eventos em datas diferentes.
- `startTime` exige `date`; `endTime` exige `startTime`. Um evento só com início não
  participa da verificação de conflito.
- Eventos do mesmo usuário não podem se sobrepor; eventos adjacentes (`09:00–10:00` e
  `10:00–11:00`) são permitidos. Usuários diferentes podem ocupar o mesmo horário.
- Toda escrita (criar, atualizar, excluir) bloqueia a linha do usuário em `tb_users`.
  Assim, duas requisições simultâneas não reservam o mesmo intervalo.
- Uma regra de recorrência com data final cria todas as ocorrências de uma vez; veja
  [Recorrência](../../recurrence/README.md). Atualizar ou excluir afeta só o ID informado.
- `completed`, `isRecurring`, `recurrenceRule` e `completedAt` seguem a
  [retenção automática](../../retention/README.md): eventos concluídos há mais de 7 dias
  e sem recorrência são apagados.
- `kind` é sempre `event` e `source` indica a origem (`internal` ou `google`).

## Endpoints

Todos exigem `Authorization: Bearer <token>`. `/planner/events` é um alias de `/events`
com o mesmo contrato.

- `GET /events`: lista os eventos e tarefas do usuário, ordenados por data e início.
- `GET /events/getEventCount`: número de eventos do dia atual (fuso `app.time-zone`).
- `POST /events`: cria o evento ou a série recorrente; responde `201` com a primeira ocorrência.
- `PUT /events/{id}`: substitui os campos do evento (não é parcial); responde `200`.
- `DELETE /events/{id}`: exclui o evento; responde `204`.
- `GET /calendar/events`: lista apenas os registros com data, para o calendário.

Corpo de `POST` e `PUT`:

```json
{
  "title": "Reunião",
  "date": "2030-01-01",
  "startTime": "09:00",
  "endTime": "10:00",
  "description": "Pauta semanal",
  "location": "Sala 2",
  "source": "internal",
  "category": "work",
  "priority": "Alta",
  "completed": false,
  "isRecurring": false,
  "recurrenceRule": null
}
```

- Obrigatórios: `title` (até 255 caracteres), `source` e `category`.
- `category`: `work`, `study`, `personal`, `health`, `event` ou `habit`.
- `priority`: `Alta`, `Média`, `Normal` ou nulo.
- `date`: `YYYY-MM-DD`; `startTime`: `HH:mm`; `endTime`: `HH:mm` ou `24:00`.
- A resposta repete esses campos e acrescenta `id`, `userId`, `kind` e `completedAt`.

## Erros

- `422`: corpo ilegível, campo obrigatório ausente ou intervalo inválido. Formato
  `{ "status": 422, "message": "..." }`.
- `409`: o intervalo se sobrepõe a outro evento do usuário. Mesmo formato.
- `400`: regra de recorrência inválida.
- `404`: o ID não existe ou pertence a outro usuário.

## Persistência

- Tabela `events`. As datas e horários podem ser nulos desde a V6, que também migrou as
  antigas tarefas de `tb_tasks`; a V7 removeu `tb_tasks`.
- Campos de retenção adicionados na V5.

## Código

- `controller/EventController`, `controller/CalendarController`.
- `service/EventService`: validação, conflitos, bloqueio, disponibilidade por hora.
- `service/CalendarQueryService`: consulta do calendário.
- `domain/Event`, `domain/EventTimes`; `validation/ValidEventTimeRange`.
- Os erros `422`/`409` são tratados pelo `PlannerExceptionHandler` do [planner](../planner/README.md).

## Testes

- `EventControllerTest`, `EventUnificationTest`, `PlannerAvailabilityServiceTest`.
- `ConcurrentSchedulingTest`: criações simultâneas no mesmo intervalo.
- `EventMigrationTest` (opcional): V6/V7 em PostgreSQL descartável, com
  `-Devent.migration.url=jdbc:postgresql://127.0.0.1:<porta>/event_migration_test`.
