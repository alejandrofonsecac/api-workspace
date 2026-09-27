# Contexto de desenvolvimento — Workspace

## 1. Objetivo e aplicação

Este documento orienta pessoas e agentes de código que trabalham no produto.
Leia-o antes de implementar ou revisar mudanças.

- Backend: repositório `api-workspace`, Java 21, Spring Boot, JPA, PostgreSQL e Flyway.
- Frontend: repositório `plannerFyFront`, Angular standalone, TypeScript, RxJS e signals.
- Os dois repositórios pertencem à mesma aplicação de produtividade com usuários, tarefas, eventos, hábitos, metas e notas.
- Mantenha este documento com o mesmo conteúdo na raiz dos dois repositórios. Ao mudar uma diretriz compartilhada, atualize ambas as cópias.
- Estas são as diretrizes de construção, não uma declaração de que o código atual já as atende. Código legado divergente não deve servir de padrão para código novo.
- Instruções explícitas do usuário definem o escopo da tarefa. Não migre funcionalidades ou faça refatorações amplas quando a solicitação for apenas análise ou documentação.

## 2. Filosofia central

**O backend é a fonte de verdade dos dados e das regras de negócio. O frontend é principalmente uma interface para consultar esses dados e enviar intenções do usuário.**

Cálculos que determinam resultados do produto devem ficar no backend: progresso, estatísticas, sequências de hábitos, disponibilidade, conflitos, prazos, recorrência, limites de plano e decisões de estado. A mesma operação deve produzir um resultado consistente independentemente da tela ou do cliente que a solicita.

O navegador não é uma autoridade confiável sobre usuário, permissões, datas oficiais, totais ou validade de uma operação. Validar na interface melhora a experiência, mas nunca substitui a validação no servidor.

Uma pequena regra pode permanecer no frontend quando for necessária para interação imediata. Se afetar um resultado de negócio, ela deve ser apenas uma antecipação não autoritativa: o servidor valida e devolve o resultado oficial.

## 3. Divisão de responsabilidades

| Assunto | Backend | Frontend |
| --- | --- | --- |
| Dados | Persistir, consultar, filtrar por usuário e garantir integridade | Exibir respostas e manter rascunhos e cache de sessão |
| Segurança | Autenticar, validar JWT, autorizar e verificar propriedade dos recursos | Conduzir login, enviar credencial e ajustar navegação e ações visíveis |
| Regras | Decidir operações permitidas, transições, limites e recorrência | Antecipar validação simples para dar feedback, sem conceder autorização |
| Cálculos do produto | Calcular percentuais, totais, sequências, indicadores e disponibilidade | Renderizar os valores recebidos |
| Datas | Definir datas oficiais, fuso de negócio, semanas, vencimentos e recorrências | Capturar a escolha de data e formatar sua exibição |
| Fluxos de negócio | Executar operações relacionadas com consistência transacional | Enviar a intenção, mostrar andamento e resultado |
| Apresentação | Retornar dados e códigos sem depender de CSS ou HTML | Cuidar de layout, cores, ícones, animações, acessibilidade e responsividade |
| Preferências visuais | Persistir se precisarem acompanhar a conta entre dispositivos | Gerenciar tema, menus, abas, seleção e modo de visualização |

### Exemplos concretos

- O backend calcula a consistência e a sequência de um hábito; o frontend desenha o gráfico.
- O backend calcula horários disponíveis e valida conflitos; o frontend posiciona os cartões na agenda.
- O backend conclui ou adia uma meta e recalcula seu progresso; o frontend exibe a resposta.
- Agendar uma tarefa deve ser um caso de uso do backend. Criar um evento e alterar a tarefa correspondente não deve depender de duas gravações independentes coordenadas pelo navegador.
- O backend define `createdAt`, `updatedAt` e outros instantes oficiais. O frontend pode formatar “há 5 minutos”. Datas escolhidas pelo usuário são entradas do caso de uso, não timestamps de auditoria.
- Converter minutos em pixels, dimensionar barras e montar a grade visual do calendário são cálculos de apresentação e permanecem no frontend.
- Filtrar uma lista já carregada para busca visual pode ficar no frontend. Filtros que afetam totais oficiais, paginação, elegibilidade ou acesso pertencem ao backend.

### Critério para exceções no frontend

Antes de adicionar lógica no navegador, verifique:

1. É apenas apresentação, estado visual ou feedback imediato de um formulário?
2. O dado original e qualquer decisão persistida continuam sob responsabilidade do backend?
3. Uma tela diferente ou uma chamada direta à API obteria o mesmo resultado de negócio?

Se a lógica precisar antecipar uma regra de negócio, registre a razão junto ao código ou na documentação da funcionalidade, consuma parâmetros fornecidos pela API quando possível e trate a rejeição do servidor. Não mantenha duas implementações independentes de cálculos oficiais.

## 4. Contratos entre frontend e backend

- Confira controller, DTO e consumidor antes de integrar uma operação. Não invente endpoints ou trate um tipo TypeScript como prova de que a API retorna aquele formato.
- Defina entradas e saídas explícitas. Use DTOs no backend e tipos de transporte correspondentes no frontend; não exponha entidades JPA diretamente.
- Separe o contrato HTTP do modelo visual quando precisarem de formatos diferentes. Faça conversões em um ponto identificado, sem regras de negócio escondidas no mapper.
- Mantenha nomes de campos, identificadores, enums, nulabilidade e formatos alinhados. Não altere contratos existentes apenas para adequá-los a uma convenção de nomes.
- Para contratos novos, prefira JSON em `camelCase`, recursos HTTP com substantivos no plural e ações de domínio explícitas quando CRUD não expressar a intenção. Preserve rotas existentes até migrar seus consumidores.
- Use datas civis no formato `YYYY-MM-DD`, horários locais no formato documentado pelo contrato e timestamps com offset ou UTC. Defina o fuso usado nas regras de “hoje” e “semana”; não dependa implicitamente do relógio de cada cliente.
- Valores estáveis de enum devem ser separados dos rótulos em português da interface. Preserve os valores já publicados e faça mudanças coordenadas.
- Resumos de tela podem ter endpoints próprios quando agregarem regras e indicadores. O backend retorna dados estruturados; textos puramente visuais, cores e classes CSS continuam na interface.
- Documente payloads e erros das operações alteradas. Para erros novos, adote um formato consistente com código estável, mensagem e detalhes de validação, sem expor stack traces ou dados internos.
- Diferencie autenticação ausente/inválida, falta de permissão, recurso indisponível, entrada inválida e conflito. Para recursos de outro usuário, preserve a política de não revelar sua existência.

## 5. Padrões do backend

### Organização e responsabilidades

- Organize por funcionalidade dentro de `com.workspace.api`, seguindo os pacotes existentes.
- `controller`: adaptação HTTP, validação da entrada e delegação. Não contém cálculos de negócio ou consultas diretas ao banco.
- `service`: casos de uso, regras, autorização contextual, coordenação e limites transacionais.
- `repository`: consultas e persistência, com escopo de usuário quando aplicável.
- `domain` ou `entity`: modelo persistente e comportamento de domínio apropriado. Preserve a organização da funcionalidade existente; para novos módulos, prefira `domain`.
- `dto`: contratos de entrada e saída; prefira records quando adequados.
- Extraia políticas ou calculadores quando houver uma responsabilidade clara. Evite classes genéricas `Utils` que misturem regras de funcionalidades diferentes.

### Código e integridade

- Use nomes de classes em `PascalCase`, métodos e campos em `camelCase`, constantes em `UPPER_SNAKE_CASE` e pacotes em minúsculas.
- Use identificadores em inglês, nomes que expressem o domínio e indentação Java de quatro espaços. Siga o estilo do arquivo ao fazer alterações pequenas.
- Prefira injeção por construtor e dependências `final`; use Lombok de forma consistente com o módulo.
- Valide formato e presença nos DTOs; valide invariantes, permissões e transições nos serviços/domínio.
- Delimite transações no caso de uso. Operações dependentes devem confirmar ou falhar juntas; chamadas externas exigem tratamento explícito de falha, pois a transação do banco não as reverte.
- Trate concorrência quando duas requisições puderem violar a mesma regra. Uma consulta antes de salvar, isoladamente, não garante exclusividade; use restrições ou bloqueios apropriados e teste o comportamento relevante.
- Para regras temporais, prefira um `Clock` injetável e um fuso explícito, permitindo testes determinísticos.
- Faça alterações de schema por novas migrations Flyway. Não reescreva migrations já aplicadas nem use atualização automática de schema como estratégia de produção.
- Não aceite `userId`, papel ou totais enviados pelo cliente como autoridade. Obtenha o usuário da autenticação e restrinja leitura, atualização e exclusão ao escopo permitido.
- `USER`, `USER_PLUS` e `ADMIN` são papéis do servidor. Visibilidade de botões e guards no frontend não concedem permissões.
- Senhas são armazenadas como hash pelo mecanismo do backend. Não registre senhas, tokens, segredos ou dados sensíveis em logs.

## 6. Padrões do frontend

### Organização e responsabilidades

- Mantenha componentes Angular standalone e organização por funcionalidade em `pages/`.
- Use `core/` para autenticação, configuração e infraestrutura HTTP; `shared/` para elementos realmente compartilhados.
- Componentes recebem dados, coordenam interação e emitem intenções. Services de acesso à API cuidam do transporte; não devem se tornar uma segunda implementação do domínio.
- Use signals e `computed` para estado e derivações visuais; RxJS para HTTP e fluxos assíncronos, respeitando o ciclo de vida e cancelando consultas obsoletas quando necessário.
- Modelos contêm tipos e contratos. Utilitários contêm funções de apresentação, não indicadores oficiais do produto.
- Mantenha arquivos do componente próximos (`.ts`, `.html`, `.css` e testes). Não reorganize uma funcionalidade inteira apenas para padronizar uma pequena alteração.

### Código, apresentação e dados

- Para novos arquivos, use `kebab-case`, services com `.service.ts` e modelos com `.model.ts`; preserve nomes legados até uma migração direcionada.
- Use classes e interfaces em `PascalCase`, membros em `camelCase` e constantes de módulo em `UPPER_SNAKE_CASE`. Identificadores em inglês; textos voltados ao usuário em português.
- Siga `.editorconfig`, `.prettierrc` e ESLint: dois espaços, aspas simples em TypeScript e largura configurada no Prettier. Evite `any` e use `import type` para imports exclusivamente de tipos.
- Use aliases existentes para caminhos distantes e imports relativos curtos para vizinhos. Não crie dependências circulares entre funcionalidades.
- Reutilize design tokens e componentes existentes. Preserve acessibilidade, navegação por teclado e estados de carregamento, vazio, erro e sucesso.
- Centralize a URL base na configuração e use a infraestrutura HTTP existente. Não espalhe URLs de ambiente nem anexação manual de JWT pelos componentes.
- Não transforme erros de gravação em sucesso com `catchError(() => of(objetoLocal))`. Confirme a gravação pela resposta da API; atualizações otimistas só são aceitáveis com reversão ou reconciliação explícita e feedback de erro.
- Uma lista vazia da API é um estado válido. Não insira dados de demonstração ou IDs inventados nos fluxos reais. Mocks pertencem a testes ou a um modo de demonstração claramente isolado.
- Falha de consulta não deve produzir totais ou cálculos substitutos apresentados como oficiais. Mostre indisponibilidade e permita nova tentativa.
- Cache de dados privados deve respeitar a sessão e ser invalidado ao sair ou trocar de conta. Preferências visuais podem usar armazenamento local; ele não é a fonte de verdade dos dados de negócio.
- Preserve a estratégia documentada em `AUTHENTICATION.md`: token em memória, envio limitado à API configurada e tratamento de expiração. Mudanças na persistência da sessão exigem revisão do fluxo completo com o backend.

## 7. Fluxo para novas funcionalidades e correções

1. Identifique o caso de uso, suas regras, seu usuário e as respostas esperadas.
2. Verifique o contrato e os recursos existentes nos dois repositórios.
3. Implemente ou ajuste regras, persistência e contratos no backend.
4. Integre o frontend como consumidor e trate todos os resultados relevantes.
5. Remova duplicações de regras no frontend dentro do escopo da mudança, preservando apenas apresentação e antecipações necessárias.
6. Verifique sucesso, entrada inválida, falha da API e isolamento entre usuários quando aplicáveis.
7. Atualize a documentação afetada e informe o que foi validado e o que continua pendente.

Não introduza dependências, camadas ou abstrações sem uma necessidade concreta. Prefira mudanças pequenas e coerentes com o caso de uso; preserve trabalho local existente.

## 8. Validação e testes

- Backend: teste regras, limites, transições, isolamento entre usuários e contratos HTTP afetados. Use o Maven Wrapper (`.\mvnw.cmd test` no Windows) conforme o escopo.
- Frontend: teste interação, contrato de requisições, exibição das respostas, erros e sessão afetados. Use `npm test -- --watch=false`, `npm run lint` e `npm run build` conforme a alteração.
- Testes de integração devem usar banco isolado e configuração explicitamente verificada. Não suponha que apenas ativar um perfil impede acesso a um banco real.
- Não crie contas ou registros de teste em ambientes reais como parte de uma verificação local.
- Alterações apenas de documentação pedem revisão de conteúdo, links e diff; não exigem iniciar a aplicação ou executar testes de negócio.
- Comentários explicam decisões e restrições relevantes; documentação deve explicar fluxos que não são óbvios, especialmente autenticação e operações transacionais.

## 9. Estado da migração de regras

As regras identificadas na análise foram migradas para o backend: métricas de hábitos, resumos de planner e dashboard, projeção de tarefas no calendário, disponibilidade e agendamento transacional. Os consumidores usam os contratos correspondentes; falhas de escrita não simulam sucesso e notas possuem atualização com datas definidas no servidor.

Consulte [BUSINESS_RULES.md](.docs/BUSINESS_RULES.md) para contratos, semântica dos cálculos, compatibilidade e limitações. A implementação não equivale a uma implantação no banco real.

Pendência de implantação: o repositório estava sem os arquivos originais de migrations V1/V2. A nova V3 depende do schema anterior. Recupere o histórico original e valide o Flyway antes de implantar; não invente arquivos substitutos ou desative a validação para ocultar a ausência.

Na interface permanecem apresentação, rascunhos, filtros visuais e validação imediata. Reavalie novas funcionalidades por esses critérios, sem tomar código legado como autorização para duplicar regras oficiais.
