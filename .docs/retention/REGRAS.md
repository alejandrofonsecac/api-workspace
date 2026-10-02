# Regras de retenção

## Recursos abrangidos

A retenção alcança as tarefas da tabela `tb_tasks`, os eventos do planner/calendário da tabela `events` e as notas da tabela `tb_note`. A API legada `/event` usa `tb_event` e não participa desta limpeza: seu modelo não possui conclusão nem data civil correspondente. Hábitos, metas e históricos de outros módulos não são removidos por este job.

## Quando um registro é removido

As quatro condições precisam ser verdadeiras no momento da exclusão:

1. `completed` é `true`.
2. `completedAt` existe e é anterior ou igual ao instante do job menos **168 horas**.
3. `isRecurring` é `false`.
4. `recurrenceRule` é `null`.

Assim, qualquer indicação de recorrência protege o item. Uma regra não vazia protege mesmo quando `isRecurring=false`. Um item com `isRecurring=true` permanece protegido mesmo sem regra. `recurrenceRule` é apenas um marcador de proteção nesta implementação; não gera ocorrências.

O prazo é contado a partir do instante oficial de conclusão, não da data de vencimento de uma tarefa, da data de um evento nem da última edição de uma nota. O registro fica elegível exatamente após 168 horas; a remoção efetiva acontece na próxima execução que conseguir processá-lo. Não há arquivamento: a exclusão é definitiva.

## Transições de conclusão

- Passar de pendente para concluído grava `completedAt` usando o relógio do servidor.
- Enviar `completed=true` novamente ou editar outro campo mantém o mesmo instante e o prazo em curso.
- Reabrir com `completed=false` limpa `completedAt`. Uma conclusão posterior define um novo instante.
- Campos de conclusão ausentes ou nulos em uma atualização não mudam o estado.
- Enviar `recurrenceRule` em branco remove a regra; para deixar de proteger o item por completo, `isRecurring` também precisa ser `false`.

Tarefas concluídas antes da introdução de `completedAt` podem ter `completed=true` e data nula. Elas são preservadas porque o instante da conclusão é desconhecido. Reenviar `completed=true` sem reabrir não inventa uma data antiga. A migration não deduz conclusão de `createdAt`, `edited` ou de datas agendadas.

Ao agendar uma tarefa, o backend transforma a tarefa em um evento do planner na mesma transação e copia `isRecurring` e `recurrenceRule` para o evento. A proteção acompanha o registro resultante.

Veja [Contrato da API](CONTRATO_API.md) para alterar esses campos e [Operação e persistência](OPERACAO.md) para a execução da limpeza.
