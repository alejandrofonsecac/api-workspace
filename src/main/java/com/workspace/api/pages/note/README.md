# Notas

Guarda notas de texto do usuário, com favorito, fixação e conclusão. As datas de edição e
de fixação são sempre definidas pelo servidor.

## Como funciona

- O ID da nota é um UUID em texto.
- `edited` recebe o horário do servidor na criação e em toda atualização.
- `fixedAt` é preenchido quando a nota passa a ser fixada e limpo quando deixa de ser.
  Reenviar `fixed` com o mesmo valor não muda a data.
- `edited` e `fixedAt` enviados pelo cliente são ignorados.
- A atualização é **parcial**: campos ausentes ou `null` mantêm o valor atual.
- Favoritar, fixar e concluir usam o mesmo `PUT /notes/{id}`.
- `completed`, `isRecurring`, `recurrenceRule` e `completedAt` seguem a
  [retenção automática](../../retention/README.md): notas concluídas há mais de 7 dias e
  sem recorrência são apagadas.

## Endpoints

Todos exigem autenticação.

- `GET /notes`: lista as notas do usuário.
- `POST /notes`: cria; responde `201`.
- `PUT /notes/{id}`: atualização parcial.
- `DELETE /notes/{id}`: exclui; responde `204`.

Corpo de `POST` e `PUT`:

```json
{
  "title": "Ideias",
  "description": "Texto longo da nota",
  "favourite": true,
  "fixed": true,
  "completed": false
}
```

- `title` é obrigatório na criação e, se enviado no `PUT`, não pode ser vazio (até 255).
- A resposta traz `id`, `userId`, `title`, `description`, `edited`, `favourite`, `fixed`,
  `fixedAt`, `completed`, `completedAt`, `isRecurring` e `recurrenceRule`.

## Erros

- `400`: título ausente, vazio ou com mais de 255 caracteres.
- `404`: a nota não existe ou pertence a outro usuário.

## Persistência

- Tabela `tb_note`; campos de retenção adicionados na V5.

## Código

- `controller/NoteController`, `service/NoteService`, `domain/Note`.

## Testes

- `NoteDeletionTest`; contrato de conclusão em `RetentionApiTest`.
