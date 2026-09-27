# Contrato da API para retenção

Não foi criada uma rota pública de limpeza. A API recebe a intenção de concluir, reabrir ou alterar recorrência pelos endpoints existentes. A autenticação identifica o proprietário; tentativas de alterar um ID inexistente ou de outra conta retornam 404.

## Endpoints por recurso

- **Tarefas**
  - Criar: `POST /schedule/tasks`.
  - Atualizar: `PUT /schedule/tasks/{id}`. A atualização é parcial.
- **Eventos do planner**
  - Criar: `POST /events` ou `POST /planner/events`.
  - Atualizar: `PUT /events/{id}` ou `PUT /planner/events/{id}`. O PUT exige os campos obrigatórios do evento.
- **Notas**
  - Criar: `POST /notes`.
  - Atualizar: `PUT /notes/{id}`. A atualização é parcial.

## Campos de retenção

- `completed`
  - Entrada: booleano opcional.
  - Saída: booleano.
  - Na criação, o padrão é pendente.
- `isRecurring`
  - Entrada: booleano opcional.
  - Saída: booleano.
  - Na criação, o padrão é não recorrente.
- `recurrenceRule`
  - Entrada: string opcional.
  - Saída: string ou nulo.
  - Uma regra não nula protege o item; uma string vazia remove a regra.
- `completedAt`
  - Gerado pelo servidor; o cliente não pode defini-lo.
  - Saída: timestamp ISO 8601 ou nulo.
  - Informa o instante oficial da última transição para concluído.

O cliente não define `completedAt`. Nas atualizações, campos de retenção ausentes ou nulos preservam seus valores atuais. As respostas incluem os quatro campos. A API usa `camelCase`; no banco, as colunas correspondentes usam `snake_case`. A escolha de recorrência não cria eventos repetidos automaticamente.

Concluir ou reabrir uma tarefa/nota:

```json
{ "completed": true }
```

```json
{ "completed": false }
```

Registrar recorrência em uma atualização:

```json
{ "isRecurring": true, "recurrenceRule": "FREQ=WEEKLY" }
```

Remover as duas proteções:

```json
{ "isRecurring": false, "recurrenceRule": "" }
```

O PUT de evento continua exigindo o restante do contrato, por exemplo:

```json
{
  "title": "Reunião",
  "date": "2030-01-01",
  "startTime": "09:00",
  "endTime": "10:00",
  "source": "internal",
  "category": "work",
  "completed": true
}
```

`date` é uma data civil; `startTime` e `endTime` representam horários locais. Os erros já existentes de entrada inválida e conflito de agenda continuam aplicáveis. A interface atual oferece a ação de concluir tarefas; os campos de eventos, notas e recorrência estão disponíveis na API, sem controles visuais novos nesta mudança.

Veja [Regras de retenção](REGRAS.md) para o prazo e as exceções.
