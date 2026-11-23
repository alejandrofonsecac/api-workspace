# Banco de dados

PostgreSQL, com o schema versionado pelo Flyway em `src/main/resources/db/migration`.
Na inicialização, o Flyway aplica as migrations pendentes e o Hibernate apenas valida o
schema (`ddl-auto: validate`).

## Regras

- Toda mudança de schema é uma nova migration `V<n>__descricao.sql`.
- Nunca altere uma migration já aplicada: o Flyway compara checksums e interrompe a
  inicialização. Não use `flyway repair` para esconder divergências.
- Faça backup antes de implantar uma versão com migrations novas.
- Confira o histórico (`flyway_schema_history`) do banco de destino antes de implantar.

## Migrations

- `V1`: schema original. Usa `IF NOT EXISTS` para preservar tabelas criadas pela versão
  anterior com `ddl-auto: update`.
- `V2`: cria `tb_users`, adiciona `user_id` obrigatório a todos os dados e muda a chave do
  foco semanal para `(user_id, week_id)`.
- `V3`: campos das regras de negócio no backend (hábitos com frequência, status, horário,
  `created_at` e `tb_habit_weekdays`; `kind` em eventos).
- `V4`: `tb_refresh_tokens` para as sessões.
- `V5`: campos de conclusão e recorrência para a retenção, com índices parciais.
- `V6`: torna data e horário de eventos opcionais e copia as tarefas de `tb_tasks` para
  `events`, verificando cada linha.
- `V7`: confere a cópia novamente e remove `tb_tasks`.
- `V8`: índice para a limpeza de refresh tokens expirados.
- `V9`: `tb_user_preferences`.

V3 e as seguintes pressupõem o schema de V1/V2; não aplique uma delas isoladamente em um
banco vazio.

## Dados anteriores à autenticação

A V2 associou os registros que existiam antes do login à conta desativada
`00000000-0000-0000-0000-000000000000` (`legacy@workspace.invalid`). Essa conta não faz
login e nenhum usuário novo vê esses dados. Não a habilite.

Para entregar um registro ao dono real, atualize-o em uma transação:

```sql
BEGIN;
UPDATE tb_note SET user_id = '<uuid-do-usuario>'
WHERE id = '<id-da-nota>'
  AND user_id = '00000000-0000-0000-0000-000000000000';
COMMIT;
```

O mesmo vale para `events`, `tb_habit`, `tb_goals` e `tb_goal_focus`. No foco semanal,
resolva antes um possível conflito com um foco do usuário na mesma semana. As conclusões
de hábitos acompanham o hábito.

## Perfis de acesso

Para criar o primeiro administrador, cadastre a conta pela API e promova-a no banco:

```sql
UPDATE tb_users SET role = 'ADMIN'
WHERE email = 'administrador@example.com' AND enabled = true;
```

A mudança vale na próxima requisição, inclusive para tokens já emitidos.

## Testes de migration

Os testes em PostgreSQL descartável estão listados em [Setup](../SETUP.md#testes).
