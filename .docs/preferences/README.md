# Preferências por usuário

As configurações de interface escolhidas pelo usuário ficam salvas na conta. Assim, ao
entrar em outro navegador ou computador, a interface já abre configurada. O frontend
mantém uma cópia em `localStorage` apenas como cache; a API é a fonte de verdade.

## Contrato

Ambos os endpoints exigem `Authorization: Bearer <token>`. O usuário é sempre o do
token: o corpo não aceita `userId` (campos desconhecidos são ignorados).

### `GET /users/me/preferences`

`200 OK`

```json
{
  "theme": "dark",
  "accent": "teal",
  "plannerDisplay": "radial",
  "sleep": { "enabled": true, "start": 22, "end": 6 },
  "sidebarWidth": 320,
  "updatedAt": "2026-10-09T12:00:00Z"
}
```

Se o usuário nunca salvou preferências, a resposta traz os padrões e `updatedAt: null`.
Nenhuma linha é criada por uma consulta.

### `PUT /users/me/preferences`

Substitui **todas** as preferências (sem gravação parcial) e devolve o estado salvo,
no mesmo formato do `GET`. A primeira gravação cria a linha.

| Campo | Valores | Padrão |
| --- | --- | --- |
| `theme` | `light`, `dark`, `high-contrast`, `system` | `light` |
| `accent` | `indigo`, `teal`, `orange`, `pink`, `blue` | `blue` |
| `plannerDisplay` | `compact`, `periods`, `radial` | `compact` |
| `sleep.enabled` | booleano | `false` |
| `sleep.start`, `sleep.end` | hora inteira de 0 a 23; o intervalo pode atravessar a meia-noite | `23`, `7` |
| `sidebarWidth` | inteiro de 240 a 340 (pixels) | `280` |

Todos os campos são obrigatórios.

### Erros

| Status | Quando | Corpo |
| --- | --- | --- |
| `400` | Campo ausente, valor fora da lista ou do intervalo, JSON inválido | `{ "status": 400, "message": "campo: motivo" }` |
| `401` | Token ausente, inválido ou expirado | — |
| `409` | Duas primeiras gravações simultâneas da mesma conta (a chave primária garante uma única linha); repetir a requisição resolve | `{ "status": 409, "message": "..." }` |

## Persistência

- Tabela `tb_user_preferences` (migration `V9__user_preferences.sql`): uma linha por
  usuário, `user_id` é chave primária e chave estrangeira para `tb_users` com
  `ON DELETE CASCADE`.
- `CHECK` constraints repetem as listas e intervalos da validação do DTO, protegendo o
  banco contra gravações por outros caminhos.
- `updated_at` é definido pelo servidor (`Clock` injetado).

## Código

`com.workspace.api.user.preferences`: `controller/UserPreferencesController`,
`service/UserPreferencesService`, `dto/UserPreferencesRequest|Response`,
`domain/UserPreferences` e `repository/UserPreferencesRepository`. Os erros usam o
`UserExceptionHandler`. Testes: `UserPreferencesTest` (padrões, gravação, validação,
isolamento entre contas e 401).

## Como adicionar uma nova preferência

Regra do projeto (veja `AGENTS.md`): toda atualização que cria ou altera uma
configuração do usuário deve incluí-la aqui.

1. Nova migration Flyway (`V<n>__...sql`) adicionando a coluna com `DEFAULT` e
   `NOT NULL` (as linhas existentes recebem o padrão) e, se for o caso, um `CHECK`.
   Nunca altere a `V9`.
2. Atualize a entidade, `UserPreferencesRequest` (com validação),
   `UserPreferencesResponse` (incluindo `DEFAULTS`) e o service.
3. Como o `PUT` substitui tudo, um frontend antigo não enviará o campo novo. Publique
   backend e frontend juntos ou aceite o campo como opcional no `PUT` (nulo mantém o
   valor atual) durante a transição.
4. Cubra o campo em `UserPreferencesTest` e atualize esta página e
   `docs/USER_PREFERENCES.md` no frontend.
