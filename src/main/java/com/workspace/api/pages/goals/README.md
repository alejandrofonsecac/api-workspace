# Metas semanais

Organiza metas por semana ISO, com prioridade, categoria, conclusão, adiamento e um foco
semanal. O servidor define a semana, o status, as datas e o progresso da semana.

## Como funciona

- **Semana:** identificada por `weekId` no formato ISO `YYYY-Www` (ex.: `2030-W01`),
  começando na segunda-feira. Sem `weekId`, a meta é criada na semana atual
  (fuso `app.time-zone`).
- **Status** (calculado, não enviado pelo cliente):
  - `COMPLETED`: a meta tem `completedAt`.
  - `PLANNED`: ainda não concluída e a semana começa depois de hoje.
  - `IN_PROGRESS`: nos demais casos.
- **Concluir:** grava `completedAt` com o relógio do servidor. Repetir não altera a data.
  Não há operação para reabrir.
- **Adiar:** move a meta para a semana seguinte e soma 1 em `deferredCount`. Uma meta
  concluída não pode ser adiada.
- **Editar:** o `PUT` não troca a semana. Enviar outro `weekId` retorna `400`; use o adiamento.
- **Progresso da semana:** `completed / total` das metas da semana, em porcentagem
  limitada a 100. Os filtros do painel não alteram esse resumo.
- **Foco semanal:** um texto por usuário e semana, salvo por substituição.
- `canComplete` e `canDefer` indicam se a ação está disponível (ambos `false` após concluir).
- Metas antigas sem semana recebem a semana atual uma única vez, na inicialização da API
  (`GoalsInitialization`).

## Endpoints

Todos exigem autenticação.

- `GET /goals/board?weekId=&category=&status=`: painel da semana (padrão: atual).
  Retorna `week` (`id`, `previousId`, `nextId`, `currentId`, `start`, `end`, `current`),
  `goals` filtradas, `summary` (`completed`, `total`, `percentage`), `focus`, `defaults`
  para o formulário e `options` com os rótulos de prioridade e categoria.
- `GET /goals` e `GET /goals/{id}`: lista todas as metas ou uma meta.
- `POST /goals`: cria; responde `201`.
- `PUT /goals/{id}`: atualiza nome, resultado esperado, prioridade e categoria.
- `POST /goals/{id}/complete`: conclui.
- `POST /goals/{id}/defer`: adia para a semana seguinte.
- `PUT /goals/focus/{weekId}`: salva o foco da semana.
- `DELETE /goals/{id}`: exclui; responde `204`.

Corpo de `POST` e `PUT /goals/{id}`:

```json
{
  "name": "Estudar Java",
  "expectedTarget": "Concluir cinco aulas",
  "priority": "NORMAL",
  "category": "STUDY",
  "weekId": "2030-W01"
}
```

- Obrigatórios: `name` e `expectedTarget` (até 255), `priority` e `category`.
- `priority`: `LOW`, `NORMAL` ou `HIGH`.
- `category`: `WORK`, `STUDY`, `HEALTH`, `MIND`, `WELLBEING`, `PERSONAL` ou `PROJECT`.

Corpo do foco: `{ "focus": "Entregar o relatório", "message": "Sem reuniões à tarde" }`
(`focus` obrigatório, até 255; `message` opcional, até 2000).

A resposta de uma meta traz `id`, `userId`, `name`, `expectedTarget`, `priority`,
`category`, `weekId`, `status`, `createdAt`, `completedAt`, `deferredCount`,
`canComplete` e `canDefer`.

## Erros

Formato `{ "status": 400, "message": "..." }`.

- `400`: campos ausentes, enum ou semana inválidos, ou troca de semana no `PUT`.
- `404`: a meta não existe ou pertence a outro usuário.
- `409`: adiar uma meta concluída.

## Persistência

- `tb_goals` (metas) e `tb_goal_focus` (chave `user_id` + `week_id`).

## Código

- `controller/GoalsController`, `service/GoalsService`, `service/GoalWeeks` (semanas ISO).
- `exception/GoalsExceptionHandler`: formato de erro desta feature.
- `service/GoalsConfiguration`: define o `Clock` compartilhado por toda a API.

## Testes

- `GoalsControllerTest`, `GoalsLifecycleTest`, `GoalsRequestValidationTest`, `GoalsCorsTest`.
