# Hábitos

Cadastra hábitos, registra em quais dias foram cumpridos e calcula sequência,
consistência, marcos e histórico. Todas as métricas são calculadas no servidor; o
frontend só desenha os gráficos.

## Como funciona

- **Frequência:** `daily` (todo dia), `weekdays` (segunda a sexta) ou `custom` (dias
  escolhidos em `daysOfWeek`, de `1` = segunda a `7` = domingo). Um dia é **previsto**
  quando a frequência o inclui e ele não é anterior a `createdAt`.
- **Conclusão:** só pode ser marcada em dia previsto, sem data futura e com o hábito
  `active`. Marcar de novo o mesmo dia desfaz a conclusão (toggle).
- **Sequência atual:** dias previstos concluídos seguidos até hoje. Hoje ainda não
  concluído não quebra a sequência; dias não previstos são ignorados.
- **Melhor sequência:** a maior sequência já registrada.
- **Consistência:** porcentagem de dias previstos concluídos nos últimos 30 dias civis.
- **Marco:** o maior entre 7, 30, 90 e 365 dias já alcançado pela melhor sequência, ou `null`.
- **Histórico:** os últimos 30 dias previstos até `asOfDate`, em ordem cronológica, como
  `{ "date": "YYYY-MM-DD", "completed": true }`. Não inclui datas futuras, dias não
  previstos nem dias anteriores à criação; um hábito novo pode ter histórico vazio.
- **Pausa:** um hábito `paused` sai da lista do dia e não aceita conclusões, mas continua
  no painel com seu histórico.
- As métricas usam a frequência atual: mudar a frequência recalcula todo o histórico.
  Não há versões antigas de frequência nem registro dos períodos de pausa.
- `asOfDate` é a data de referência do servidor; o frontend a usa para marcar “Hoje”.

## Endpoints de gerenciamento (`/habits`)

Todos exigem autenticação.

- `GET /habits/board`: hábitos com métricas e um resumo geral.
- `GET /habits`: apenas a lista de hábitos com métricas.
- `POST /habits`: cria um hábito; responde `201`.
- `PUT /habits/{id}`: substitui os dados do hábito.
- `PUT /habits/{id}/status`: corpo `{ "status": "active" }` ou `{ "status": "paused" }`.
- `DELETE /habits/{id}`: exclui o hábito e suas conclusões; responde `204`.

Corpo de `POST` e `PUT`:

```json
{
  "name": "Ler",
  "icon": "menu_book",
  "categoryId": "estudo",
  "frequencyType": "custom",
  "daysOfWeek": [1, 3, 5],
  "time": "07:30",
  "goal": "20 páginas",
  "note": "Antes do trabalho"
}
```

- Obrigatórios: `name` (até 255), `icon` (até 80), `categoryId` e `frequencyType`.
- `categoryId`: `trabalho`, `estudo`, `saude`, `mente`, `bem-estar`, `pessoal` ou `projetos`.
- `custom` exige ao menos um dia em `daysOfWeek`. `time` é opcional, em `HH:mm`.

Cada hábito na resposta traz `id`, `name`, `icon`, `categoryId`, `frequencyType`, `time`,
`goal`, `note`, `status`, `completedDates`, `createdAt`, `daysOfWeek`, `asOfDate`,
`metrics` (`currentStreak`, `bestStreak`, `consistency`, `completedDays`, `milestone`)
e `history`. O resumo de `/habits/board` traz `activeCount`, `highestCurrentStreak`,
`accumulatedDays` e `averageConsistency`.

## Endpoints de execução (`/habit`)

Rotas antigas, mantidas para a tela do dia e para compatibilidade.

- `GET /habit/planner?date=YYYY-MM-DD`: hábitos ativos previstos na data, com
  `id`, `userId`, `title` e `completed`.
- `POST /habit/{id}/completions/{date}/toggle`: marca ou desmarca a conclusão na data.
- `GET /habit` e `POST /habit`: contrato legado (`name`, `category`, `frequencyEveryDay`,
  `localDate`, `objective`, `observation`). Novas telas usam `/habits`.

## Erros

- `400`: categoria, dias da semana, horário ou status inválidos.
- `404`: o hábito não existe ou pertence a outro usuário.
- `409`: conclusão em data futura, não prevista ou com o hábito pausado.

## Persistência

- `tb_habit`; dias da semana em `tb_habit_weekdays`; conclusões em `tb_habit_completions`
  (uma linha por hábito e data). Campos de frequência, status e criação vieram na V3.

## Código

- `controller/HabitBoardController` (`/habits`) e `controller/HabitController` (`/habit`).
- `service/HabitBoardService`: cadastro e montagem do painel.
- `service/HabitService`: conclusões e lista do dia.
- `service/HabitMetrics`: dias previstos, sequências, consistência, marcos e histórico.

## Testes

- `HabitMetricsTest`: cálculos de sequência, consistência e histórico.
- `HabitFrequencyIntegrationTest`: frequências, conclusões e contratos HTTP.
