Diagnóstico de goals — 06/09/2026

Atualização de 07/09/2026: a integração de GET/POST foi corrigida em `plannerFyFront`, incluindo a conversão dos campos, categorias e prioridades. A home usa `name` como título e `expectedTarget` como descrição. O backend agora armazena `weekId` e `habitTargetCount`; metas antigas sem semana são apresentadas na semana atual. O formulário permanece aberto quando a API rejeita a gravação.

Validação desta atualização: 65 testes do backend e 23 testes Angular passaram; build de produção e lint dos arquivos alterados passaram. Na API local, POST retornou 201 e os GETs confirmaram título, semana e hábito nulo. A meta temporária foi removida. O título na home foi verificado pelo teste do componente; não havia navegador conectado para uma inspeção visual. Os parágrafos abaixo registram o diagnóstico original de 06/09.

O backend foi corrigido e a suíte completa passou: 64 testes, zero falhas e zero erros. A integração da tela Angular ainda depende de alinhar o contrato enviado e recebido; o frontend foi inspecionado, mas não alterado.

Causas e evidências

- O log mostra SQLState 23502: tentativa de inserir `category = null` em `tb_goals.category`, uma coluna obrigatória. A consulta somente leitura ao PostgreSQL confirmou que `goal_name`, `expected_target`, `priority` e `category` exigem valor; `habit_id` já permite nulo.
- Em `../plannerFyFront/src/app/pages/goals/service/goal.service.ts`, o frontend envia `title`, `expectedResult` e `categoryId`. O backend recebe `name`, `expectedTarget` e `category`. Essa divergência explica os campos nulos do log; o JSON exato daquela requisição não foi fornecido.
- O DTO de resposta havia voltado a chamar `getHabit().getId()` sem verificar nulo. Essa regressão foi reproduzida nos testes antes da correção.

Correções aplicadas

- Validação dos quatro campos obrigatórios em POST e PUT, mantendo `habitId` opcional.
- Resposta HTTP 400 com mensagem de validação para dados incompletos ou tipos inválidos, antes de salvar no banco.
- Conversão segura da resposta quando não há hábito, retornando `habitId: null`.
- Mapeamento JPA alinhado às restrições reais do banco e CORS configurado para `/goals/**`.
- Testes que ainda dependiam do módulo removido `weekly_goals` adaptados para `/goals`.

Contrato válido para POST /goals e PUT /goals/{id}

```json
{
  "name": "Estudar Java",
  "expectedTarget": "Concluir cinco aulas",
  "priority": "NORMAL",
  "category": "STUDY",
  "habitId": null
}
```

`habitId` também pode ser omitido. No PUT, a ausência ou o valor nulo remove o vínculo. Um ID de hábito inexistente continua retornando 404.

Pendências da integração

- Converter `title → name`, `expectedResult → expectedTarget`, `categoryId → category` e `habitTarget.habitId → habitId`. Exemplo de categoria: `estudo → STUDY`. Prioridades do modelo Angular `ALTA/MEDIA/BAIXA` correspondem a `HIGH/NORMAL/LOW`.
- Adaptar a resposta da API ao modelo visual. A tela também usa `weekId`, `status` e `taskLinks`, que não fazem parte do contrato atual do backend; apenas renomear os campos de entrada não conclui essa integração.
- Remover o comportamento `catchError(() => of(goal))` no frontend: ele apresenta uma meta local mesmo quando o servidor rejeita a gravação. Isso pode explicar metas que parecem salvas e desaparecem ao recarregar.

Validação e limites

Os testes verificaram CRUD com e sem hábito, persistência, remoção/restauração de vínculo, rejeição de dados inválidos sem alterar registros, migrações, CORS e os demais testes do projeto. Execução: Maven offline, Java 21 e H2. Log: `target/goals-repair-validation.log`.

O PostgreSQL foi consultado apenas para metadados, sem escrita. A aplicação em execução não foi reiniciada e o fluxo Angular completo não foi validado. Ao executar o backend atualizado, o payload incompatível passa a receber 400; para salvar pela tela, é necessário concluir a adaptação do frontend descrita acima.
