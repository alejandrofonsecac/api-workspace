# Regras de negócio no backend

## Responsabilidades após a migração

O backend `api-workspace` calcula os indicadores, decide a validade das operações e persiste os dados do usuário autenticado. O frontend `plannerFyFront` envia intenções, exibe as respostas e mantém o estado de interação. As diretrizes gerais estão em [`AGENTS.md`](../development/AGENTS.md).

Contratos usados pela interface em cada funcionalidade:

- **Hábitos e indicadores:** `GET /habits/board` retorna a lista com métricas e resumo calculados pelo servidor.
- **Cadastro e edição de hábito:** `POST /habits`, `PUT /habits/{id}` e `DELETE /habits/{id}`.
- **Pausa e retomada:** `PUT /habits/{id}/status`, com `{ "status": "active" }` ou `paused`.
- **Execução de hábitos:** `GET /habit/planner?date=YYYY-MM-DD` e `POST /habit/{id}/completions/{date}/toggle`.
- **Eventos e tarefas:** `GET/POST /events` e `PUT/DELETE /events/{id}`; `/planner/events` mantém o mesmo contrato. Tarefas são eventos sem data ou sem horário.
- **Calendário:** `GET /calendar/events` retorna os eventos que possuem data.
- **Resumo do planner:** `GET /planner/summary?date=YYYY-MM-DD` retorna eventos, disponibilidade, contagem de eventos sem data, durações, percentual e próxima atividade.
- **Disponibilidade:** `GET /planner/availability?date=YYYY-MM-DD`.
- **Início:** `GET /home/dashboard` retorna data do servidor, foco, agenda, hábitos e indicadores do dia.
- **Notas:** `PUT /notes/{id}` faz atualização parcial de conteúdo, favorito ou fixação; o ID é uma string UUID.

As rotas antigas de hábitos em `/habit` continuam disponíveis para compatibilidade. Novas telas de gerenciamento usam `/habits`. A API legada `/event` (tabela `tb_event`) foi removida; tarefas e eventos usam a tabela `events` desde a migration V6 (`V6__unify_tasks_into_events.sql`).

## Agenda e agendamento

- Eventos aceitam intervalos crescentes no mesmo dia, com precisão de minutos, entre `00:00` e `24:00`. `24:00` só é válido no fim do intervalo. Para atravessar a madrugada, use eventos em datas distintas.
- No banco, o fim `24:00` é representado por meia-noite; `EventTimes` centraliza essa conversão e a comparação numérica dos intervalos.
- Eventos adjacentes são permitidos. Sobreposições do mesmo usuário são rejeitadas. Usuários diferentes podem ocupar o mesmo horário.
- O servidor serializa as alterações da agenda pelo registro do usuário para impedir que duas criações simultâneas reservem o mesmo intervalo.
- A disponibilidade possui 24 blocos de uma hora. Um intervalo parcial ocupa todos os blocos que intercepta; os indicadores de tempo do planner refletem esses blocos, não a soma exata da duração dos eventos.
- Agendar uma tarefa é atualizar o evento sem data/horário pelo `PUT /events/{id}`; a validação de conflito e a gravação ocorrem na mesma transação do servidor.
- A próxima atividade é escolhida no servidor. Enquanto o planner está aberto, a interface atualiza o resumo periodicamente e ao recuperar foco; a contagem regressiva é apenas apresentação.

## Hábitos

O contrato de gerenciamento usa `name`, `icon`, `categoryId`, `frequencyType`, `time`, `goal`, `note` e `daysOfWeek`. `categoryId` mantém os identificadores da interface (`trabalho`, `estudo`, `saude`, `mente`, `bem-estar`, `pessoal`, `projetos`), mapeados no servidor para o enum de domínio.

- O backend aceita frequência diária, dias úteis e personalizada. Nesta última, `daysOfWeek` contém números de 1 (segunda-feira) a 7 (domingo). A tela oferece “Todo dia” e “Definir”, com seleção de dias; hábitos legados de dias úteis são editados como segunda a sexta.
- O histórico só aceita execução em dia previsto, a partir da criação, sem data futura e com o hábito ativo.
- A sequência considera os dias previstos pela frequência. O dia atual ainda não concluído não quebra a sequência até terminar; dias não previstos não a interrompem.
- A consistência é o percentual de dias previstos concluídos nos últimos 30 dias, limitado à data de criação. Melhor sequência, total de dias e marcos também vêm do servidor.
- As métricas usam a frequência atual. Esta versão não guarda versões históricas de frequência ou períodos de pausa. Hábitos pausados saem da lista de execução, mas mantêm o histórico no painel.
- `asOfDate` informa a data de referência da resposta. O frontend usa essa data para desenhar os quadrados do histórico; não recalcula sequências ou percentuais.

## Notas, dashboard e relógio

- O foco do dashboard é o primeiro evento do dia ainda não concluído, ordenado por ID. A contagem de concluídos refere-se aos eventos do dia. A maior sequência de hábitos considera todos os hábitos do usuário.
- `edited` e `fixedAt` de notas são definidos no backend; valores enviados pelo navegador são ignorados. Favoritar e fixar usam o mesmo endpoint de atualização (`PUT /notes/{id}`).
- O `Clock` compartilhado usa `app.time-zone`, com padrão `America/Sao_Paulo`. Os testes podem substituir o relógio sem depender da hora do computador.
- Metas têm regras e contratos próprios; veja o histórico em [`../goals`](../goals).

## Comportamento do frontend

Falhas de criação, atualização ou exclusão não são convertidas em objetos locais de sucesso. Formulários de criação/edição mantêm o rascunho quando a API falha. Consultas vazias não geram exemplos automaticamente; falhas em indicadores exibem indisponibilidade, sem recalcular números substitutos.

Permanecem locais: organização visual de eventos, posicionamento em pixels, grade do calendário, formato de datas/durações, filtros de busca sobre listas carregadas, estado de menus/formulários e validações imediatas de preenchimento. O backend sempre valida a operação persistida.

## Banco e implantação

Foi adicionada `src/main/resources/db/migration/V3__backend_business_rules.sql`, que acrescenta os campos necessários preservando registros existentes e o histórico de hábitos. Esta migration pressupõe o schema multiusuário anterior.

As migrations originais `V1__existing_workspace_schema.sql` e `V2__users_and_data_ownership.sql` foram restauradas do histórico local do repositório e verificadas contra seus blobs originais. Elas devem permanecer sem alterações para preservar os checksums registrados em `flyway_schema_history`. Não execute `repair` para ocultar divergências nem aplique V3 diretamente em um banco vazio.

O código foi validado com banco H2 isolado. O teste opcional `BusinessRulesMigrationTest` valida V3 sobre um schema anterior mínimo em PostgreSQL descartável, com `RULES_MIGRATION_TEST_URL` explicitamente apontando para `127.0.0.1`, usuário `rules_test`. Esse teste verifica a alteração incremental e a preservação dos registros.

Nenhuma migration desta alteração foi aplicada ao banco real da aplicação.

## Retenção automática de itens concluídos

As regras, os contratos, a operação e a validação da limpeza estão em [Retenção automática](../retention/README.md).

## Recorrência com data final

`POST /events` (também `/planner/events`) expande no servidor regras
`FREQ=DAILY|WEEKLY|MONTHLY|YEARLY;UNTIL=YYYYMMDD`, usando `date` como âncora.
`UNTIL` é inclusivo; o máximo é de 1.000 ocorrências, dentro de dez anos.
Datas inexistentes (dia 31 ou 29/02) são puladas sem deslocar a âncora.
Regras vazias ou ausentes criam um único evento (`RecurrencePolicy`).

A variante “Apenas dias úteis” usa `FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR;UNTIL=YYYYMMDD`,
nessa ordem. Ela cria ocorrências só de segunda a sexta; sábados e domingos são
pulados, inclusive quando `date` cai no fim de semana (a série começa no próximo
dia útil e a resposta traz essa primeira ocorrência). Feriados não são considerados.
O limite de 1.000 conta apenas as ocorrências criadas. `BYDAY` com outra frequência,
outra lista de dias ou sem `UNTIL`, e um período sem nenhum dia útil, resultam em 400.

A criação persiste todas as ocorrências na mesma transação e mantém a resposta
anterior (primeira ocorrência). O cliente recarrega a listagem. A conclusão inicial
só afeta a primeira; cada ID mantém sua própria conclusão e a proteção da retenção V5.
Conflito em qualquer data rejeita tudo com 409; regra ou data inválida resulta em 400.
O bloqueio do usuário continua serializando todas as escritas da agenda, e os eventos
existentes na janela da recorrência são consultados uma única vez.

Atualizar, reabrir ou excluir afeta apenas o ID selecionado; não há operação de
série inteira. Regras legadas sem `UNTIL` continuam como metadados de proteção,
sem geração retroativa.

### Histórico visual dos hábitos

As respostas de /habits e /habits/board incluem history: uma lista cronológica
de até 30 objetos { date: YYYY-MM-DD, completed: boolean }. O servidor seleciona
os 30 dias previstos mais recentes, até asOfDate, respeitando createdAt e a regra
HabitMetrics.scheduled. Não há datas futuras, dias não previstos nem preenchimento
anterior à criação. Um hábito novo pode ter histórico vazio até seu primeiro dia.

A consistência conserva a janela dos últimos 30 dias civis e considera somente
dias previstos nessa janela. O histórico visual pode cobrir mais de 30 dias civis
quando a frequência é personalizada. Alterar a frequência usa a nova configuração
para histórico e métricas; não há versionamento histórico de frequência.
O frontend apenas formata as datas e os tooltips, usando asOfDate para “Hoje”.
Nenhuma alteração de schema ou migration é necessária.
