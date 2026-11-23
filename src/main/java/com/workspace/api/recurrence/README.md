# Recorrência

Transforma uma regra de repetição em uma lista de datas. É usada na criação de
[eventos](../pages/event/README.md): o servidor expande a regra e grava todas as
ocorrências de uma vez.

## Regras aceitas

O campo `recurrenceRule` usa um subconjunto do formato iCalendar (RRULE):

- `FREQ=DAILY|WEEKLY|MONTHLY|YEARLY;UNTIL=YYYYMMDD`
- `FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR;UNTIL=YYYYMMDD` (apenas dias úteis, nessa ordem exata)

## Como funciona

- `date` do evento é a âncora (primeira data). `UNTIL` é inclusivo.
- A série deve terminar em até 10 anos após a âncora e ter no máximo 1.000 ocorrências.
- `MONTHLY` e `YEARLY` pulam datas que não existem (dia 31, 29/02) sem deslocar a âncora:
  uma série no dia 31 só ocorre nos meses com 31 dias.
- Dias úteis: sábados e domingos são pulados, inclusive quando a âncora cai no fim de
  semana (a série começa no próximo dia útil). Feriados não são considerados. O limite
  de 1.000 conta só as datas criadas.
- Regra vazia ou ausente cria um único evento.
- Regra só com `FREQ` (sem `UNTIL`) também cria um único evento. Esse formato antigo é
  mantido apenas como marcador de proteção da [retenção](../retention/README.md).

## Efeitos na criação do evento

- Todas as ocorrências são validadas antes de gravar e salvas na mesma transação. Um
  conflito de horário em qualquer data rejeita a série inteira com `409`.
- A resposta é a primeira ocorrência; o cliente recarrega a lista para ver as demais.
- `completed: true` na criação conclui só a primeira ocorrência.
- Cada ocorrência é um evento independente, com ID e conclusão próprios. Não há edição
  ou exclusão da série inteira.

## Erros (`400`)

- Formato não suportado; `BYDAY` com outra frequência, outra lista de dias ou sem `UNTIL`.
- `UNTIL` inválido, anterior à âncora ou mais de 10 anos depois.
- Regra com `UNTIL` sem `date` ou com `isRecurring: false`.
- Mais de 1.000 ocorrências, ou nenhuma ocorrência no período.

## Código e testes

- `RecurrencePolicy.dates(start, isRecurring, rule)`.
- `RecurrencePolicyTest`.
