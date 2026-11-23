# Retenção automática

Apaga definitivamente eventos/tarefas e notas concluídos há mais de 7 dias, desde que não
estejam marcados como recorrentes. É um job interno: não existe rota HTTP para executá-lo.
Hábitos, metas e demais dados não são afetados.

## Quando um registro é apagado

As quatro condições precisam ser verdadeiras:

1. `completed` é `true`.
2. `completedAt` é anterior ou igual a “agora menos 7 dias” (168 horas).
3. `isRecurring` é `false`.
4. `recurrenceRule` é `null`.

- Qualquer sinal de recorrência protege o registro: `isRecurring: true` sem regra, ou uma
  regra preenchida com `isRecurring: false`.
- O prazo conta a partir da conclusão, não da data do evento nem da última edição.
- A remoção acontece na primeira execução após o prazo. Não há arquivo nem restauração.

## Ciclo de conclusão

Aplicado por `RetainedEntity.updateRetention`, herdado por `Event` e `Note`:

- Pendente → concluído: `completedAt` recebe o horário do servidor.
- Reenviar `completed: true` ou editar outro campo mantém o mesmo `completedAt`.
- `completed: false` reabre e limpa `completedAt`; uma nova conclusão gera outra data.
- Campos ausentes ou `null` não mudam o estado atual.
- `recurrenceRule: ""` remove a regra. Para tirar toda a proteção, envie também
  `isRecurring: false`.
- O cliente não define `completedAt`. Registros concluídos antes da V5 ficam com
  `completedAt` nulo e nunca são apagados automaticamente.

Exemplos de corpo, nos endpoints de [eventos](../pages/event/README.md) e
[notas](../pages/note/README.md):

- Concluir: `{ "completed": true }`; reabrir: `{ "completed": false }`.
- Proteger da limpeza: `{ "isRecurring": true, "recurrenceRule": "FREQ=WEEKLY" }`.
- Remover a proteção: `{ "isRecurring": false, "recurrenceRule": "" }`.

O `PUT` de eventos substitui o evento inteiro; inclua também os campos obrigatórios.

## Execução

- Roda diariamente às 03h no fuso `app.time-zone`.
- Processa cada recurso (`events`, `tb_note`) em lotes. Cada lote tem transação própria
  com tempo limite de 30 s; um lote confirmado não é desfeito se outro falhar.
- Continua até não haver itens elegíveis ou atingir o limite de lotes por recurso. Com os
  padrões, são no máximo 50.000 registros por recurso por execução; o restante fica
  para a próxima.
- Uma falha em um recurso é registrada no log e o job segue para o próximo.
- A seleção usa `FOR UPDATE SKIP LOCKED`, e os serviços bloqueiam a linha antes de
  alterá-la. Assim, várias instâncias podem rodar o job ao mesmo tempo e uma edição
  concorrente não é apagada no meio.
- O log registra a quantidade apagada por recurso.

## Configuração

- `RETENTION_ENABLED` (padrão `true`): `false` não registra o job.
- `RETENTION_CRON` (padrão `0 0 3 * * *`): cron do Spring; `-` desativa os disparos.
- `RETENTION_BATCH_SIZE` (padrão `500`): registros por lote, de 1 a 10.000. Não é um
  mínimo: com 1 item elegível, ele é apagado no primeiro lote.
- `RETENTION_MAX_BATCHES` (padrão `100`): lotes por recurso em cada execução.
- `APP_TIME_ZONE` (padrão `America/Sao_Paulo`): fuso do cron.

## Persistência

- A V5 adicionou `completed`, `completed_at`, `is_recurring` e `recurrence_rule` e
  índices parciais em `(completed, completed_at, id)` para os registros sem recorrência.
- A consulta de exclusão está em `src/main/resources/retention/delete-batch.sql`. A lista
  de tabelas é fixa no código (`RetentionRepository.Resource`); os valores são parâmetros.
- Após o `DELETE`, o PostgreSQL reaproveita o espaço no vacuum; o tamanho dos arquivos
  não diminui necessariamente na hora.

## Código

- `RetentionConfiguration` (habilita o agendamento), `service/RetentionJob`,
  `service/RetentionBatchService`, `repository/RetentionRepository`, `domain/RetainedEntity`.

## Testes

- Em H2 (perfil `test`): `RetentionLifecycleTest`, `RetentionApiTest`, `RetentionJobTest`,
  `RetentionSchedulingTest`.

  ```powershell
  .\mvnw.cmd '-Dtest=Retention*Test' test
  ```

- `RetentionPostgresTest` (opcional): prazo, recorrência, lotes, bloqueio concorrente,
  transações independentes, migrations e índices. Exige um banco local descartável
  `retention_test`, com usuário `retention_test` e senha vazia. O teste apaga dados das
  tabelas; nunca aponte para um banco real.

  ```powershell
  .\mvnw.cmd '-Dtest=Retention*Test' '-Dretention.test.url=jdbc:postgresql://127.0.0.1:55439/retention_test' test
  ```
