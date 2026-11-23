# Regras de negócio

O backend é a fonte oficial dos dados, cálculos e decisões do produto; o frontend envia
intenções e exibe as respostas (veja [`AGENTS.md`](../../AGENTS.md)). Esta página indica
onde cada regra está documentada. O detalhe fica no README de cada feature.

## Onde estão as regras

- **Agenda e agendamento:** intervalos `00:00–24:00`, conflitos por usuário, bloqueio de
  escrita e agendamento de tarefas → [Eventos e tarefas](../../src/main/java/com/workspace/api/pages/event/README.md).
- **Recorrência:** expansão de `FREQ`/`UNTIL`, dias úteis, limites →
  [Recorrência](../../src/main/java/com/workspace/api/recurrence/README.md).
- **Resumo do dia:** blocos de disponibilidade, tempo livre, próxima atividade →
  [Planner](../../src/main/java/com/workspace/api/pages/planner/README.md).
- **Hábitos:** dias previstos, conclusões, sequências, consistência, marcos e histórico →
  [Hábitos](../../src/main/java/com/workspace/api/pages/habit/README.md).
- **Metas:** semana ISO, status, conclusão, adiamento e progresso →
  [Metas semanais](../../src/main/java/com/workspace/api/pages/goals/README.md).
- **Página inicial:** foco do dia, hábitos do dia e contadores →
  [Dashboard](../../src/main/java/com/workspace/api/pages/home/dashboard/README.md).
- **Notas:** datas de edição e fixação definidas pelo servidor →
  [Notas](../../src/main/java/com/workspace/api/pages/note/README.md).
- **Conclusão e limpeza:** `completedAt` oficial e exclusão após 7 dias →
  [Retenção](../../src/main/java/com/workspace/api/retention/README.md).
- **Contas e acesso:** dono dos dados, perfis e sessões →
  [Usuários e autenticação](../../src/main/java/com/workspace/api/user/README.md).
- **Data oficial e regras comuns:** fuso, formatos e erros → [Arquitetura](../ARCHITECTURE.md).

## O que fica no frontend

Organização visual dos eventos, posição em pixels, grade do calendário, formatação de
datas e durações, filtros de busca sobre listas já carregadas, estado de menus e
formulários e validações imediatas de preenchimento. O backend sempre valida a operação
gravada.

- Falhas de criação, atualização ou exclusão não viram objetos locais de sucesso.
- Formulários mantêm o rascunho quando a API falha.
- Listas vazias não geram exemplos; falhas em indicadores mostram indisponibilidade, sem
  números substitutos.

## Compatibilidade

- `/planner/events` é um alias de `/events`.
- As rotas `/habit` continuam para a tela do dia e clientes antigos; o gerenciamento usa `/habits`.
- A API `/event` foi removida e a tabela `tb_event` não é mais usada. Tarefas e eventos usam `events`
  desde a V6, e `tb_tasks` foi removida na V7.
