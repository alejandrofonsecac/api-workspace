# Preferências por usuário

Salva na conta as configurações de interface escolhidas pelo usuário (tema, cor de
destaque, visualização do planner, horário de sono, largura da barra lateral...). Em
outro navegador ou computador, a interface já abre configurada.

## Como funciona

- A API guarda **um documento JSON por conta** e não interpreta as chaves. Ela garante o
  dono (usuário do token), a junção das alterações e os limites de tamanho.
- O frontend define quais preferências existem, seus valores aceitos e padrões, e ignora
  valores que não reconhece (`docs/USER_PREFERENCES.md` no `plannerFyFront`).
- Por isso, **uma nova preferência visual não exige mudança neste repositório**: nem
  migration, nem DTO, nem deploy coordenado.
- Exceção: um valor que influencie regras ou cálculos do servidor (disponibilidade, datas
  oficiais, limites) não é preferência. Ele precisa de um campo próprio, validado no
  backend, e não deve ser lido deste documento.

## Endpoints

Ambos exigem autenticação.

- `GET /users/me/preferences`

  ```json
  {
    "preferences": { "theme": "dark", "sleep": { "enabled": true, "start": 22, "end": 6 } },
    "updatedAt": "2030-01-01T12:00:00Z"
  }
  ```

  Sem preferências salvas: `{ "preferences": {}, "updatedAt": null }`. A consulta não cria linha.

- `PATCH /users/me/preferences`: o corpo tem só as chaves alteradas.

  ```json
  { "theme": "system", "sleep": { "enabled": false, "start": 23, "end": 7 } }
  ```

  - Cada chave enviada **substitui** o valor anterior inteiro (sem mescla profunda).
  - `null` **remove** a chave.
  - Chaves não enviadas são **mantidas**. Assim, um cliente antigo não apaga o que não
    conhece, e dois dispositivos alterando chaves diferentes não se sobrescrevem.
  - `{}` não grava nada. A primeira gravação cria a linha.
  - A resposta tem o formato do `GET`, com o documento completo.

Gravações da mesma conta são serializadas pelo bloqueio da linha do usuário; não há
resposta de conflito.

## Limites

Violações respondem `400` com `{ "status": 400, "message": "..." }`.

- Corpo: objeto JSON (array, texto ou JSON inválido são recusados).
- Chaves, inclusive em objetos internos: `camelCase`, padrão `[a-z][A-Za-z0-9]{0,63}`.
- Valores: texto, número, booleano, `null`, objeto ou array.
- Profundidade: até 3 níveis de objetos/arrays dentro de uma chave.
- Texto: até 500 caracteres.
- Documento final: até 100 chaves e 16 KB.

## Persistência

- Tabela `tb_user_preferences` (V9): `user_id` (chave e FK para `tb_users` com
  `ON DELETE CASCADE`), `data jsonb` (com `CHECK` de que é um objeto) e `updated_at`,
  definido pelo servidor.

## Código

- `controller/UserPreferencesController`.
- `service/UserPreferencesService`: leitura e junção.
- `service/PreferencesDocumentPolicy`: limites. Mude os limites aqui e nesta página.
- `domain/UserPreferences`, `repository/UserPreferencesRepository`.
- Erros tratados pelo `UserExceptionHandler`.

## Testes

- `UserPreferencesTest`: documento vazio, chaves desconhecidas preservadas, junção,
  remoção, `PATCH` vazio, limites, isolamento entre contas e `401`.
