# Regras de negócio no backend

## Responsabilidades após a migração

O backend `api-workspace` calcula os indicadores, decide a validade das operações e persiste os dados do usuário autenticado. O frontend `plannerFyFront` envia intenções, exibe as respostas e mantém o estado de interação. As diretrizes gerais estão em [`AGENTS.md`](../development/AGENTS.md).

| Funcionalidade | Contrato usado pela interface |
| --- | --- |
| Hábitos e indicadores | `GET /habits/board`: lista com métricas e resumo calculados pelo servidor |
| Cadastro e edição de hábito | `POST /habits`, `PUT /habits/{id}`, `DELETE /habits/{id}` |
| Pausa e retomada | `PUT /habits/{id}/status`, com `{ "status": "active" }` ou `paused` |
| Execução de hábitos | `GET /habit/planner?date=YYYY-MM-DD` e `POST /habit/{id}/completions/{date}/toggle` |
| Eventos persistidos | `GET/POST /events`, `PUT/DELETE /events/{id}`; `/planner/events` mantém o mesmo contrato |
| Calendário | `GET /calendar/events`: eventos e lembretes de tarefas com prazo; lembretes têm `readOnly: true` |
| Bandeja de tarefas | `GET /planner/tasks`: tarefas pendentes elegíveis para agendamento |
| Agendamento de tarefa | `POST /planner/tasks/{id}/schedule`, com `{ "date": "2030-01-01", "startTime": "23:00" }` |
| Resumo do planner | `GET /planner/summary?date=YYYY-MM-DD`: eventos, disponibilidade, contagens, durações, percentual e próxima atividade |
| Disponibilidade | `GET /planner/availability?date=YYYY-MM-DD` |
| Início | `GET /home/dashboard`: data do servidor, foco, agenda, hábitos e indicadores do dia |
| Notas | `PUT /notes/{id}`: atualização parcial de conteúdo, favorito ou fixação; ID é uma string UUID |

As rotas antigas de hábitos em `/habit` continuam disponíveis para compatibilidade. Novas telas de gerenciamento usam `/habits`. A API legada `/event` continua separada e não alimenta os novos resumos; os eventos de calendário e planner usam a tabela `events`.

## Agenda e agendamento

- Eventos aceitam intervalos crescentes no mesmo dia, com precisão de minutos, entre `00:00` e `24:00`. `24:00` só é válido no fim do intervalo. Para atravessar a madrugada, use eventos em datas distintas.
- No banco, o fim `24:00` é representado por meia-noite; `EventTimes` centraliza essa conversão e a comparação numérica dos intervalos.
- Eventos adjacentes são permitidos. Sobreposições do mesmo usuário são rejeitadas. Usuários diferentes podem ocupar o mesmo horário.
- O servidor serializa as alterações da agenda pelo registro do usuário para impedir que duas criações simultâneas reservem o mesmo intervalo.
- A disponibilidade possui 24 blocos de uma hora. Um intervalo parcial ocupa todos os blocos que intercepta; os indicadores de tempo do planner refletem esses blocos, não a soma exata da duração dos eventos.
- Agendar uma tarefa cria um evento de uma hora, copia título e notas, marca `kind: "task"` e retira a tarefa da bandeja na mesma transação. O navegador não faz uma exclusão posterior.
- Repetir o agendamento da mesma tarefa no mesmo destino retorna o evento já criado. Uma repetição em outro destino resulta em conflito. A referência interna `scheduledTaskId` evita duplicação por nova tentativa enquanto o evento existir.
- `kind` e `scheduledTaskId` são definidos pelo servidor, não pelo payload de criação de eventos. Eventos comuns têm `kind: "event"`.
- A próxima atividade é escolhida no servidor. Enquanto o planner está aberto, a interface atualiza o resumo periodicamente e ao recuperar foco; a contagem regressiva é apenas apresentação.

## Hábitos

O contrato de gerenciamento usa `name`, `icon`, `categoryId`, `frequencyType`, `time`, `goal`, `note` e `daysOfWeek`. `categoryId` mantém os identificadores da interface (`trabalho`, `estudo`, `saude`, `mente`, `bem-estar`, `pessoal`, `projetos`), mapeados no servidor para o enum de domínio.

- O backend aceita frequência diária, dias úteis e personalizada. Nesta última, `daysOfWeek` contém números de 1 (segunda-feira) a 7 (domingo). A tela atual continua oferecendo frequência diária.
- O histórico só aceita execução em dia previsto, a partir da criação, sem data futura e com o hábito ativo.
- A sequência considera os dias previstos pela frequência. O dia atual ainda não concluído não quebra a sequência até terminar; dias não previstos não a interrompem.
- A consistência é o percentual de dias previstos concluídos nos últimos 30 dias, limitado à data de criação. Melhor sequência, total de dias e marcos também vêm do servidor.
- As métricas usam a frequência atual. Esta versão não guarda versões históricas de frequência ou períodos de pausa. Hábitos pausados saem da lista de execução, mas mantêm o histórico no painel.
- `asOfDate` informa a data de referência da resposta. O frontend usa essa data para desenhar os quadrados do histórico; não recalcula sequências ou percentuais.

## Tarefas, notas e dashboard

- Tarefas recebem `dueDate` e `dueTime` como campos estruturados. `dueLabel` é uma saída calculada; enviar um rótulo como “Hoje” não define um prazo. Tarefas novas sem data permanecem sem prazo.
- As atualizações existentes de tarefas continuam parciais: campos ausentes ou nulos preservam o valor atual. Uma operação explícita de remoção de prazo ainda não é oferecida na interface.
- O calendário projeta tarefas pendentes com data como lembretes de leitura. Sem horário informado, o lembrete aparece às 08h, com duração visual de até uma hora. Lembretes não reservam horários e não são editados como eventos.
- O foco do dashboard é a primeira tarefa pendente com prazo no dia, ordenada por ID. A contagem de tarefas concluídas também se refere aos prazos do dia.
- `edited` e `fixedAt` de notas são definidos no backend; valores enviados pelo navegador são ignorados. Favoritar e fixar usam o mesmo endpoint de atualização.
- O `Clock` compartilhado usa `app.time-zone`, com padrão `America/Sao_Paulo`. Os testes podem substituir o relógio sem depender da hora do computador.
- Metas continuam usando o resumo e as operações de domínio já existentes no backend.

## Comportamento do frontend

Falhas de criação, atualização ou exclusão não são convertidas em objetos locais de sucesso. Formulários de criação/edição mantêm o rascunho quando a API falha. Consultas vazias não geram exemplos automaticamente; falhas em indicadores exibem indisponibilidade, sem recalcular números substitutos.

Permanecem locais: organização visual de eventos, posicionamento em pixels, grade do calendário, formato de datas/durações, filtros de busca sobre listas carregadas, estado de menus/formulários e validações imediatas de preenchimento. O backend sempre valida a operação persistida.

## Banco e implantação

Foi adicionada `src/main/resources/db/migration/V3__backend_business_rules.sql`, que acrescenta os campos necessários preservando registros existentes e o histórico de hábitos. Esta migration pressupõe o schema multiusuário anterior.

As migrations originais `V1__existing_workspace_schema.sql` e `V2__users_and_data_ownership.sql` foram restauradas do histórico local do repositório e verificadas contra seus blobs originais. Elas devem permanecer sem alterações para preservar os checksums registrados em `flyway_schema_history`. Não execute `repair` para ocultar divergências nem aplique V3 diretamente em um banco vazio.

O código foi validado com banco H2 isolado. O teste opcional `BusinessRulesMigrationTest` valida V3 sobre um schema anterior mínimo em PostgreSQL descartável, com `RULES_MIGRATION_TEST_URL` explicitamente apontando para `127.0.0.1`, usuário `rules_test`. Esse teste verifica a alteração incremental e a preservação dos registros.

Nenhuma migration desta alteração foi aplicada ao banco real da aplicação.
