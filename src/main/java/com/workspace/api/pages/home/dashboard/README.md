# Dashboard da página inicial

Reúne em uma única resposta os indicadores do dia atual: resumo do planner, foco,
hábitos do dia e contadores. A data é sempre a de hoje no fuso `app.time-zone`.

## Como funciona

- **`planner`:** o mesmo resumo de `GET /planner/summary` para hoje
  (veja [Planner](../../planner/README.md)).
- **`focus`:** o primeiro evento de hoje ainda não concluído, pela ordem de ID (ordem de
  criação). `null` quando não há. `progress` é sempre `0` nesta versão.
- **`habits`:** hábitos ativos previstos para hoje. Cada um tem `completed` `0` ou `1`,
  `target` `1` e `progress` `0` ou `100`.
- **`summary`:**
  - `completedEvents` e `totalEvents`: eventos de hoje concluídos e totais.
  - `highestCurrentStreak`: a maior sequência atual entre **todos** os hábitos do usuário.

As regras de hábito usadas aqui (dia previsto e sequência) são as mesmas de
[Hábitos](../../habit/README.md).

## Endpoint

- `GET /home/dashboard` (autenticado)

```json
{
  "date": "2030-01-01",
  "planner": { "...": "resumo do planner de hoje" },
  "focus": { "id": 7, "title": "Reunião", "description": null, "progress": 0 },
  "habits": [ { "id": 3, "icon": "menu_book", "name": "Ler", "completed": 1, "target": 1, "progress": 100 } ],
  "summary": { "completedEvents": 2, "totalEvents": 5, "highestCurrentStreak": 12 }
}
```

## Código

- `DashboardController`, `DashboardService`.

## Testes

- Coberto por `BusinessRulesIntegrationTest`.
