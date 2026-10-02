Relatório de arquitetura e uso dos campos de goals — 07/09/2026

Minha recomendação, considerando sua preferência, é concentrar no backend as regras, os valores padrão de negócio, a persistência e os cálculos de progresso. O frontend deve apresentar os dados, coletar o formulário e controlar carregamento, erros e navegação. O mapper criado resolveu a incompatibilidade entre dois modelos existentes, mas sua implementação atual também preenche informações de negócio que o backend ainda não fornece. Essa segunda responsabilidade precisa ser retirada dele.

Este relatório descreve o código atual e propõe uma direção. Nesta revisão não foi feita uma nova refatoração do aplicativo.

**O que eu alterei em pages/goals.** O formulário já usava `title`, `expectedResult`, prioridades em português e categorias como `estudo`. A API esperava `name`, `expectedTarget`, `NORMAL/HIGH/LOW` e `STUDY/WORK/...`. O serviço enviava o objeto visual diretamente e tratava a resposta como se tivesse o mesmo formato. Além disso, transformava erros de gravação em uma meta local aparentemente salva.

As alterações foram:

| Arquivo | Alteração e finalidade |
| --- | --- |
| [goal.service.ts](../../../plannerFyFront/src/app/pages/goals/service/goal.service.ts) | Passou a enviar o formato aceito pela API, converter a resposta, usar o ID retornado pelo servidor e apresentar erros reais. Removi a meta de demonstração quando o GET retorna vazio. O mesmo contrato passou a ser usado no PUT. |
| [goals.ts](../../../plannerFyFront/src/app/pages/goals/goals.ts) | `saveGoal()` espera a confirmação da API antes de fechar o formulário. Expõe o estado de gravação e a ação de recarregar. |
| [goals.html](../../../plannerFyFront/src/app/pages/goals/goals.html) | Conecta esses estados à interface, oferece nova tentativa e repassa erro/gravação ao formulário. |
| [goal-drawer.ts](../../../plannerFyFront/src/app/pages/goals/components/goal-drawer/goal-drawer.ts) e HTML correspondente | Impedem novo envio enquanto há gravação, mostram o erro e usam prioridades consistentes com o modelo visual. |
| `goal-api.mapper.ts` | Arquivo histórico com as conversões entre formulário/modelo visual e contrato HTTP. |
| `goals-api.ts` | Arquivo histórico de tipos TypeScript para o JSON de entrada e saída da API, compartilhado com a home. |
| [goal.service.spec.ts](../../../plannerFyFront/src/app/pages/goals/service/goal.service.spec.ts) | Novos testes de GET, POST, erro, semana, hábito e atualização. |

A pasta `utils`, o arquivo `week.utils.ts`, o cálculo em `goal-progress.ts` e o modelo em `models/goal.ts` já existiam. Eu adicionei `goal-api.mapper.ts` à pasta existente. `utils` é apenas uma convenção para organizar funções auxiliares; não é uma funcionalidade especial do Angular.

Na home, alterei [weekly-goals.service.ts](../../../plannerFyFront/src/app/pages/home/service/weekly-goals.service.ts) para converter `name` em `title` e `expectedTarget` em `description`. O HTML do card já exibia `goal.title`; faltava alimentar essa propriedade com o campo correto. Também ajustei o tipo do hábito para aceitar nulo e o teste do card para usar o contrato real.

**O que é o goal-api.mapper.** Um mapper é uma função que transforma um objeto de um formato em outro. Neste caso, ele tem duas funções:

| Função | Quando é chamada | O que faz |
| --- | --- | --- |
| `toGoalRequest()` | Antes de POST e PUT, pelo serviço Angular | Converte o formulário no JSON esperado pelo backend. |
| `fromGoalResponse()` | Depois de GET, POST e PUT, pelo serviço Angular | Converte o JSON recebido no objeto utilizado pelos componentes. |

Exemplo de envio:

```text
Formulário                         JSON enviado à API
title: "Estudar Java"          →   name: "Estudar Java"
expectedResult: "Cinco aulas"  →   expectedTarget: "Cinco aulas"
priority: "MEDIA"              →   priority: "NORMAL"
categoryId: "estudo"           →   category: "STUDY"
habitId: null                  →   habitId: null
```

No recebimento, os nomes e enums são convertidos no sentido inverso. `habitId` e `habitTargetCount` são agrupados no objeto visual `habitTarget`.

O arquivo não acessa o banco nem substitui o DTO Java. Eu o criei para preservar os componentes existentes enquanto corrigia a comunicação. Isso evita espalhar renomeações em cada chamada HTTP, mas mantém dois vocabulários para o mesmo domínio. Não é uma exigência do Angular e não é a estrutura final que recomendo para um frontend mais simples.

**DTO no backend e tipos no frontend.** Há quatro coisas diferentes no fluxo:

| Estrutura | Papel |
| --- | --- |
| `Goals` no Java | Entidade persistida pelo JPA, com colunas e relacionamento com `Habit`. |
| `GoalsRequestDTO` | Contrato de entrada. Recebe os valores enviados pelo cliente e contém as validações do pedido. |
| `GoalsResponseDTO` | Contrato de saída. Seleciona os dados expostos e transforma o relacionamento em `habitId`, sem enviar a entidade inteira. |
| `GoalApiRequest` / `GoalApiResponse` no TypeScript | Descrevem ao compilador o JSON que o frontend deve enviar/receber. Não executam regras de negócio nem validação de dados em tempo de execução. |

Declarar `getAll<GoalApiResponse>()` não faz o Angular converter `name` para `title`. O tipo genérico é uma declaração sobre a resposta esperada, não um conversor nem um validador. Essa distinção é documentada no [guia oficial do HttpClient](https://angular.dev/guide/http/making-requests).

Manter tipos TypeScript alinhados aos DTOs Java é adequado mesmo com um backend responsável pelas regras. A duplicação a eliminar é a divergência desnecessária de nomes e valores. Opcionalmente, os tipos podem ser gerados a partir de um contrato OpenAPI; isso seria uma melhoria futura, não algo instalado nesta solução.

**Onde cada campo da entidade é usado.** A entrada passa por [GoalsController](../../src/main/java/com/workspace/api/pages/goals/controller/GoalsController.java), recebe validação com `@Valid`, chega a [GoalsService](../../src/main/java/com/workspace/api/pages/goals/service/GoalsService.java), é aplicada à entidade e salva pelo repositório. A resposta é construída em [GoalsResponseDTO](../../src/main/java/com/workspace/api/pages/goals/dto/GoalsResponseDTO.java).

| Campo da entidade | Caminho e uso atual | É necessário? Onde deve ficar? |
| --- | --- | --- |
| `id` | Gerado pelo banco e devolvido na resposta. Identifica a meta no GET individual, PUT e DELETE; a interface usa o ID para atualizar a lista. | Sim. Sua geração e identidade pertencem ao backend/banco. |
| `goalName` | Recebe `request.name()`. Sai como `name`, vira `title` no frontend e aparece no card de goals e na home. | Sim. São três nomes para um mesmo dado; recomendo um nome consistente no contrato e na interface. Não exige renomear a coluna existente. |
| `expectedTarget` | Recebe o resultado esperado do formulário, persiste como texto e aparece como `expectedResult` no card e como `description` na home. | Sim para o formulário atual. É uma descrição do resultado, não a quantidade numérica de execuções do hábito. Deve ser persistido e validado no backend. |
| `priority` | O frontend converte a prioridade para `LOW`, `NORMAL` ou `HIGH`. O backend persiste o enum. A tela usa a prioridade em rótulos e classes visuais. | Sim para a funcionalidade atual. O código do enum deve ser compartilhado pelo contrato; a tradução para “Alta/Média/Baixa” pode permanecer na apresentação. |
| `category` | Recebe o enum, persiste e retorna à interface. É usada nos rótulos e no filtro por categoria da página. | Sim para o formulário atual. O backend define os valores aceitos; o frontend apresenta os rótulos. Atualmente `categoryId` no frontend é um código textual, não o ID de uma entidade Category. |
| `weekId` | O frontend calcula a semana escolhida e a envia. O backend persiste e devolve. `goals.ts` compara esse valor com `selectedWeekId()` para decidir quais metas mostrar. Também muda quando a meta é adiada. | A associação da meta a uma semana é necessária para a navegação existente. Persistir no backend foi adequado. A representação como String não é obrigatória. |
| `habitTargetCount` | Vem do campo “Meta de execuções na semana”. O backend salva a quantidade; o mapper a coloca em `habitTarget.targetCount`. `goal-progress.ts` a usa como total no cálculo de progresso. | Necessário se a meta exige uma quantidade de execuções do hábito. Deve pertencer à meta no backend, porque metas distintas podem ter quantidades diferentes para o mesmo hábito. |
| `habit` | `request.habitId()` é usado para buscar o hábito. Se existir, o relacionamento é salvo; se não existir, há 404. Se o ID for nulo, a meta fica sem vínculo. A resposta expõe apenas `habitId`. | Necessário para a vinculação opcional solicitada. O relacionamento e sua integridade pertencem ao backend/banco. |

Eu acrescentei `weekId` e `habitTargetCount`. Os demais campos da entidade já existiam. Sem persistir a semana, o frontend precisaria reconstruí-la ao recarregar, podendo mostrar a meta na semana errada. Sem persistir a quantidade do hábito, o valor escolhido no formulário também se perderia. Esses campos não foram necessários apenas para fazer um POST retornar sucesso: foram acrescentados para preservar informações que a interface já coletava/usava.

**Detalhes dos dois campos novos.** `weekId` é um código como `2027-W01`: ano da semana ISO e número da semana. Não é uma chave estrangeira nem o ID de outra tabela. `length = 8` limita o tamanho da coluna mapeada; não valida sozinho uma semana. O DTO verifica o formato e a faixa 01–53, mas não verifica se aquele ano realmente possui semana 53. No PUT, se `weekId` vier nulo ou ausente, o serviço mantém a semana anterior; portanto, o contrato atual não oferece uma forma de apagá-la com nulo.

Para um domínio semanal mais explícito, minha preferência seria avaliar `LocalDate weekStart`, representando a segunda-feira, com validação no backend. Isso facilita consultas por intervalo e elimina a necessidade de interpretar uma String em regras do servidor. Não é obrigatório migrar agora; manter `weekId` também funciona, desde que o backend valide a semana de verdade e defina o tratamento dos registros antigos. Não recomendo persistir as duas representações sem necessidade.

`habitTargetCount` é uma quantidade desejada, não a quantidade já realizada. Para uma meta de caminhar quatro vezes, o valor é `4`; se houve duas execuções, o progresso é `2/4`. Hoje o DTO permite ausência/nulo e rejeita valores informados menores que 1. O serviço limpa a quantidade quando não há hábito. Com hábito e quantidade nula, a interface assume `1`: essa definição deveria ficar no backend, ou o backend deveria exigir uma quantidade ao vincular um hábito. O POST/PUT atual não estabelece uma regra completa para essa combinação.

A migração histórica de metas acrescenta as duas colunas sem atribuir uma semana a registros antigos. A API pode retornar semana nula, e o mapper coloca essas metas na semana atual. Essa escolha pode fazer uma meta antiga aparecer em semanas diferentes conforme o tempo passa; é uma compatibilidade provisória, não uma regra histórica adequada.

**O significado das anotações.** `@Id` identifica a chave da entidade. `@GeneratedValue(IDENTITY)` configura a geração da identidade pelo banco. `@Column(nullable = false)` descreve a restrição de nulidade da coluna; não substitui a validação de JSON nem impede texto vazio. Por isso os DTOs usam `@NotBlank` e `@NotNull` antes de persistir.

`@Enumerated(EnumType.STRING)` persiste nomes como `NORMAL` e `STUDY` em vez de posições numéricas dos enums. `@ManyToOne` permite que várias metas apontem para um mesmo hábito. `@JoinColumn(name = "habit_id")` define a coluna de ligação. Sem a proibição de nulo, esse relacionamento é opcional. `LAZY` solicita que o carregamento do hábito seja adiado; não exige que o cliente faça uma segunda requisição HTTP. Na especificação, LAZY é uma indicação ao provedor de persistência, não uma garantia absoluta de consulta. Referências: [ManyToOne](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/manytoone) e [JoinColumn](https://jakarta.ee/specifications/platform/10/apidocs/jakarta/persistence/joincolumn).

**O problema real dentro do mapper atual.** Além das renomeações, `fromGoalResponse()` fabrica valores ou reaproveita dados em memória:

| Informação | Comportamento atual | Avaliação |
| --- | --- | --- |
| `status` | Mantém o estado anterior ou deduz “em andamento/planejada” comparando a semana com a semana atual. | Regra de negócio no lugar errado. A API deveria fornecer o estado oficial. |
| `createdAt` | Usa o valor anterior ou a data do navegador no momento da conversão. | Não representa necessariamente a criação real. Deve ser gerado e persistido pelo backend. |
| `completedAt` | Só reaproveita o valor anterior em memória. | Se a conclusão existir no produto, sua data deve ser registrada no backend. |
| `deferredCount` | Mantém o valor anterior ou usa zero. | Não é um histórico persistido de adiamentos. Deve ficar no backend se essa informação for necessária ao produto. |
| `taskLinks` | Mantém os vínculos anteriores ou usa uma lista vazia. | Não carrega vínculos reais da API atual. Exige modelagem no backend se a função for mantida. |

Esses campos já existiam no modelo visual. Eu preservei esse modelo e completei a resposta para continuar usando seus componentes. A consequência que deveria ter ficado mais explícita na entrega anterior é: **GET/POST funcionarem não significa que conclusão, datas, histórico e progresso estejam integrados de ponta a ponta.**

Exemplo concreto: `GoalService.complete()` coloca `CONCLUIDA` e `completedAt` no objeto local, mas `toGoalRequest()` não envia esses campos e o backend não os persiste. A tela pode mostrar a conclusão após um PUT bem-sucedido e perdê-la em uma recarga completa. No adiamento, a semana agora é persistida; o contador de adiamentos continua apenas local. O foco semanal também é salvo somente em um signal do frontend.

O cálculo de progresso já existia em `goal-progress.ts` e continua no frontend. Ele conta tarefas concluídas ou datas de execução do hábito. Entretanto, [HabitResponseDTO](../../src/main/java/com/workspace/api/pages/habit/dto/HabitResponseDTO.java) não retorna `completedDates`, informação esperada por esse cálculo. Persistir `habitTargetCount` não resolve a origem das execuções. O backend precisa definir de onde elas vêm e devolver o progresso calculado, se essa funcionalidade for mantida.

**Divisão de responsabilidades que recomendo para este projeto.**

| Responsabilidade | Lugar recomendado |
| --- | --- |
| Validar os campos, a existência do hábito e a semana | DTOs para formato; serviço de domínio para regras e consultas. |
| Definir semana padrão, quantidade padrão, estado e datas oficiais | Backend. Deve haver uma regra explícita, independente do navegador. |
| Concluir/adiar metas, registrar datas e histórico | Backend, em operações que apliquem essas regras. |
| Calcular progresso com base em execuções/tarefas persistidas | Backend; frontend recebe os números para renderizar. |
| Buscar metas por semana, categoria e estado | Backend pode oferecer filtros no GET. Hoje o backend devolve todas e o frontend filtra. |
| Abrir/fechar formulário, exibir carregamento e mensagens | Frontend. Isso não é regra de negócio. |
| Formatar datas para exibição, escolher cores e traduzir rótulos | Frontend. Pode usar os mesmos enums da API e traduzir apenas os textos exibidos. |
| Validar campos no formulário para resposta imediata | Frontend, como auxílio; a validação obrigatória continua no backend. |

**Pastas e complexidade.** `goals-api.ts` contém tipos específicos de metas. Colocá-lo em `core/data-access` permitiu compartilhamento com a home, mas eu preferiria uma área de domínio compartilhada, como `shared/goals/models`, mantendo em `core` o cliente HTTP genérico. O formulário e os componentes podem continuar em `pages/goals`.

Se ainda houver uma transformação visual necessária, uma função pequena em `pages/goals/mappers` ou junto da camada de acesso a dados é mais descritiva que uma pasta genérica `utils`. Não é necessário criar uma classe, biblioteca ou infraestrutura de mapeamento para isso. No backend, a conversão simples de entidade para DTO no construtor atual é suficiente; só separaria um `GoalsMapper` se o tamanho ou o reaproveitamento justificassem. As regras continuam no serviço, não nesse mapper.

**Próxima direção recomendada.** Primeiro, padronizar o frontend para usar `name`, `expectedTarget`, `priority` e `category` com os mesmos valores da API. O título do card pode ser `goal.name`, sem um segundo modelo inteiro. Depois, decidir quais funcionalidades de status, histórico e progresso fazem parte do produto e implementá-las no backend antes de apresentá-las como persistidas. Por fim, remover do mapper as informações inventadas; com os contratos alinhados, ele pode ficar mínimo ou deixar de existir.

Essa direção reduz o trabalho de domínio no frontend sem transferir ao servidor detalhes como “o formulário está aberto”. A decisão de persistir semana e quantidade do hábito foi coerente com a interface existente. A adaptação dos nomes resolveu o defeito imediato. Já os padrões de negócio preenchidos no mapper e os dados apenas locais são pontos que recomendo corrigir na arquitetura.

Esta avaliação foi feita por leitura do código atual. Os 65 testes do backend e 23 testes Angular da entrega anterior cobriam os fluxos descritos naquela entrega; não comprovavam a persistência das funcionalidades ausentes identificadas aqui. Nenhum novo teste foi executado para este relatório.
