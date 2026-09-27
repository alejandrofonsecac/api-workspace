# Operação e persistência da retenção

## Agendamento e limites

[RetentionConfiguration.java](../../src/main/java/com/workspace/api/retention/RetentionConfiguration.java) habilita o agendamento do Spring. [RetentionJob.java](../../src/main/java/com/workspace/api/retention/service/RetentionJob.java) calcula um limite de sete dias completos com o `Clock` do backend e percorre tarefas, eventos e notas. Por padrão, roda diariamente às 03h no fuso `America/Sao_Paulo`.

- **`RETENTION_ENABLED`**
  - Padrão: `true`.
  - Defina como `false` para não registrar o job.
- **`RETENTION_CRON`**
  - Padrão: `0 0 3 * * *`.
  - Usa a expressão cron do Spring no fuso configurado; `-` desativa os disparos.
- **`APP_TIME_ZONE`**
  - Padrão: `America/Sao_Paulo`.
  - Define o fuso do cron e do relógio de negócio.
- **`RETENTION_BATCH_SIZE`**
  - Padrão: `500`.
  - Define o limite de registros por lote; aceita valores de 1 a 10.000.
- **`RETENTION_MAX_BATCHES`**
  - Padrão: `100`.
  - Define o número máximo de lotes por recurso em cada execução; deve ser positivo.

Esses valores são mapeados por [application.yml](../../src/main/resources/application.yml). **500 não é um limite total nem uma quantidade mínima para começar a excluir.** Se houver 1 nota elegível, o job pode excluí-la em um lote; se houver 1.200, processa 500, depois mais 500 e, por fim, 200. O job continua até não encontrar mais itens elegíveis ou alcançar o limite de 100 lotes por recurso. Com os padrões, uma execução processa no máximo 50.000 registros por recurso. Se houver mais itens elegíveis, os restantes ficam para uma execução posterior. Esse limite evita manter uma operação de limpeza sem fim em uma única madrugada.

## Transações e concorrência

Cada lote usa uma transação nova com timeout de 30 segundos, definida em [RetentionBatchService.java](../../src/main/java/com/workspace/api/retention/service/RetentionBatchService.java). Um lote confirmado não é desfeito se outro falhar. Uma falha é registrada e o job tenta os recursos seguintes; registros bloqueados ou não processados permanecem para outra execução.

[RetentionRepository.java](../../src/main/java/com/workspace/api/retention/repository/RetentionRepository.java) carrega a consulta de [delete-batch.sql](../../src/main/resources/retention/delete-batch.sql), recurso interno do classpath. O conjunto de tabelas vem de uma enumeração interna; os valores variáveis da consulta são passados como parâmetros. A seleção é limitada, ordenada por conclusão e usa bloqueio de linha com `SKIP LOCKED` do PostgreSQL. Atualizações e exclusões feitas pelos serviços de tarefa, evento e nota também bloqueiam a linha antes de alterar seu estado. O job pode rodar em mais de uma instância sem escolher simultaneamente a mesma linha bloqueada.

O arquivo SQL não é uma rota pública nem é servido pelo Spring como conteúdo estático. Movê-lo para um recurso do classpath organiza o código, mas não o torna secreto para quem pode ler o código-fonte ou o pacote da aplicação. O controle de acesso das operações do usuário permanece nos serviços e endpoints; o job não usa token de usuário. Os logs registram contagens por recurso e falhas de execução. Para requisitos de sigilo em logs, revise também a configuração de SQL do JPA, atualmente `spring.jpa.show-sql: true`.

## Schema e implantação

A migration [V5__completed_item_retention.sql](../../src/main/resources/db/migration/V5__completed_item_retention.sql) adiciona `completed_at`, `is_recurring` e `recurrence_rule` a `tb_tasks`, `events` e `tb_note`. Também adiciona `completed` a eventos e notas; tarefas já possuíam esse campo. Não há preenchimento retroativo do instante de conclusão. Os índices parciais em `(completed, completed_at, id)` abrangem apenas os registros sem proteção de recorrência.

Aplique a V5 pelo Flyway na sequência do histórico já registrado. Antes de implantar, confira os checksums e as migrations anteriores do banco de destino; não reescreva migrations já aplicadas nem use `repair` para encobrir divergências. Planeje a criação dos índices para uma janela apropriada ao volume das tabelas. A retenção apaga definitivamente as linhas elegíveis; não há tabela de histórico/arquivo nem restauração automática.

Após um `DELETE`, o PostgreSQL pode reutilizar espaço quando executar vacuum, mas os arquivos físicos do banco não diminuem necessariamente de imediato. Monitore as contagens de limpeza, duração da execução e eventuais falhas antes de ajustar os limites. O teste de migration e da consulta em PostgreSQL descartável está descrito em [Validação](VALIDACAO.md).
