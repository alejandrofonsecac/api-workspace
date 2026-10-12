# Preferências por usuário

As configurações de interface escolhidas pelo usuário (tema, cor de destaque,
visualização do planner, horário de sono, largura da barra lateral...) ficam salvas na
conta. Ao entrar em outro navegador ou computador, a interface já abre configurada.

## Divisão de responsabilidades

- **Backend:** guarda **um documento JSON por conta** e o devolve. Não conhece o
  significado das chaves. Garante o dono (usuário do token), a junção das alterações e
  limites estruturais.
- **Frontend:** define quais preferências existem, seus valores aceitos e padrões, e
  trata valores que não reconhece. Veja `docs/USER_PREFERENCES.md` no `plannerFyFront`.

Por isso, **uma nova preferência visual não exige mudança neste repositório**: nenhuma
migration, DTO ou deploy coordenado.

Exceção: um valor que influencie regras ou cálculos do servidor (disponibilidade,
datas oficiais, limites) não é preferência visual. Ele precisa de campo próprio,
validado aqui, e não deve ser lido deste documento.

## Contrato

Ambos os endpoints exigem `Authorization: Bearer <token>`.

### `GET /users/me/preferences`

`200 OK`

```json
{
  "preferences": {
    "theme": "dark",
    "accent": "teal",
    "plannerDisplay": "radial",
    "sleep": { "enabled": true, "start": 22, "end": 6 },
    "sidebarWidth": 320
  },
  "updatedAt": "2026-10-12T12:00:00Z"
}
```

Se o usuário nunca salvou preferências: `{ "preferences": {}, "updatedAt": null }`.
Nenhuma linha é criada por uma consulta.

### `PATCH /users/me/preferences`

O corpo é um objeto com as chaves alteradas. Cada chave enviada **substitui** o valor
anterior por inteiro (objetos não são mesclados em profundidade); `null` **remove** a
chave; chaves não enviadas são **preservadas**. Assim, um cliente antigo nunca apaga uma
preferência que ele não conhece, e dois dispositivos alterando chaves diferentes não se
sobrescrevem. A resposta tem o mesmo formato do `GET`, com o documento completo.

```json
{ "theme": "system", "sleep": { "enabled": false, "start": 23, "end": 7 } }
```

Um `PATCH` vazio (`{}`) não grava nada. A primeira gravação cria a linha.

### Limites (400 quando violados)

| Regra | Limite |
| --- | --- |
| Corpo | objeto JSON (arrays, textos e JSON inválido são rejeitados) |
| Nomes de chave, inclusive em objetos internos | `camelCase`: `[a-z][A-Za-z0-9]{0,63}` |
| Valores | texto, número, booleano, `null`, objeto ou array |
| Profundidade | até 3 níveis de objetos/arrays dentro de uma chave |
| Texto | até 500 caracteres |
| Documento após a junção | até 100 chaves e 16 KB |

### Erros

| Status | Quando | Corpo |
| --- | --- | --- |
| `400` | Corpo fora dos limites acima | `{ "status": 400, "message": "..." }` |
| `401` | Token ausente, inválido ou expirado | — |

Gravações da mesma conta são serializadas com bloqueio da linha do usuário
(`UserRepository.lockById`), inclusive a primeira; não há resposta de conflito.

## Persistência

- Tabela `tb_user_preferences` (migration `V9__user_preferences.sql`): `user_id`
  (PK e FK para `tb_users` com `ON DELETE CASCADE`), `data jsonb` (com `CHECK` de que é
  um objeto) e `updated_at`, definido pelo servidor com o `Clock` injetado.
- No Java, `data` é um `Map<String, Object>` mapeado com `@JdbcTypeCode(SqlTypes.JSON)`.

## Código

`com.workspace.api.user.preferences`: `controller/UserPreferencesController`,
`service/UserPreferencesService` (leitura e junção), `service/PreferencesDocumentPolicy`
(limites), `dto/UserPreferencesResponse`, `domain/UserPreferences` e
`repository/UserPreferencesRepository`. Os erros usam o `UserExceptionHandler`.

Testes: `UserPreferencesTest` — documento vazio, chaves desconhecidas preservadas,
junção e remoção, `PATCH` vazio, limites, isolamento entre contas e 401. Também foi
executado em PostgreSQL 16 com Flyway e `ddl-auto=validate`.

## Quando mexer aqui

- Mudar os limites estruturais: `PreferencesDocumentPolicy` e esta página.
- Uma configuração que afete regras do servidor: crie campo próprio (migration, DTO,
  validação) fora deste documento.
- Para preferências visuais, nada muda neste repositório.
