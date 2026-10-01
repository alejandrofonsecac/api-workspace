# Relatório de alterações e segurança — backend

Data: 18 de setembro de 2026  
Projeto: `api-workspace`

> Registro histórico anterior à implementação de refresh token. Para o estado atual de
> sessões, logout e rotação, consulte
> [`ACCESS_AND_REFRESH_TOKENS.md`](../authentication/ACCESS_AND_REFRESH_TOKENS.md).

## Objetivo

Transformar a API na fonte oficial de dados, regras de negócio, cálculos, autorização e isolamento entre usuários. O frontend passa a enviar intenções e apresentar as respostas da API.

Reformulando o relatório em formato de texto corrido.
Reformulando o relatório em formato de texto corrido.

------
## Relatório de Alterações

--Usuários e autenticação
Foi implementado cadastro, login e perfil de usuário na rota /users, com autenticação via JWT Bearer assinado em HS256. O objetivo é permitir contas distintas e autenticação sem necessidade de sessão mantida no servidor. Como consequência, rotas privadas passam a exigir um token válido, e é o usuário autenticado que define o acesso aos seus próprios dados.

--Senhas
As senhas agora são armazenadas com hash BCrypt, com validação do limite de 72 bytes UTF-8 e comparação contra um hash fictício quando o e-mail informado não existe. Isso evita persistir senha em texto puro e reduz a diferença de tempo de resposta entre tentativas com credenciais inválidas (mitigando ataques de timing). Senhas muito longas são rejeitadas, e qualquer alteração futura de senha deve continuar usando BCrypt.

--JWT
A chave de assinatura passou a exigir formato Base64 com no mínimo 32 bytes, além de validação de emissor e expiração. O token agora carrega sub, jti, iat, exp e a role do usuário. Isso evita o uso de chaves fracas, tokens sem validade ou emitidos por outra fonte. Como efeito colateral, a aplicação falha ao iniciar caso JWT_SECRET esteja ausente, inválido ou fraco. Importante: o conteúdo do JWT continua legível (não é criptografado), portanto não deve carregar dados sensíveis.

--Autorização
A SecurityConfiguration passou a exigir autenticação por padrão. O papel administrativo é obrigatório para acessar /users/*/role, e um usuário desabilitado perde acesso imediatamente, mesmo que seu token ainda não tenha expirado. Com isso, a autorização de dados deixa de depender do frontend.

--Isolamento de dados
Repositórios e serviços passaram a filtrar recursos pelo userId extraído do token. Isso evita que uma conta consulte ou altere dados de outra: IDs de eventos, tarefas, notas, metas e hábitos não concedem mais acesso fora da conta proprietária.

--Agenda
Eventos passaram a aceitar intervalos até 24:00, com validação de sobreposição e bloqueio por usuário durante alterações concorrentes. Isso trata corretamente o limite do dia e impede conflitos em requisições simultâneas. Eventos adjacentes continuam sendo aceitos normalmente; já sobreposições dentro da mesma conta são rejeitadas.

--Tarefas no planner
O endpoint POST /planner/tasks/{id}/schedule agora cria o evento e remove a tarefa correspondente na mesma transação, evitando o estado parcial em que o navegador cria um evento mas falha ao remover a tarefa. Repetições idênticas retornam o evento já existente; tentativas em horário diferente retornam conflito.

--Resumos e indicadores
Os endpoints de hábitos, calendário, planner e dashboard passaram a calcular métricas, disponibilidade, foco e próxima atividade diretamente no servidor. Isso elimina cálculos concorrentes e divergentes que antes ocorriam no navegador — a API passa a definir os números exibidos, e a interface apenas os formata.

--Tarefas e notas
Tarefas passaram a receber dueDate e dueTime, com dueLabel sendo calculado como resposta pelo servidor. Notas definem edited e fixedAt também no servidor. Isso impede que datas ou dados de auditoria sejam controlados pelo cliente. Payloads legados que enviam apenas o rótulo não definem prazo, e carimbos de notas enviados pelo cliente são ignorados.

--Flyway
Foram restauradas as migrações V1__existing_workspace_schema.sql e V2__users_and_data_ownership.sql a partir do histórico local, e criada a V3 para os novos campos. O banco já registrava V1 e V2 aplicadas, e o Flyway interrompia a inicialização por não encontrar esses arquivos no projeto. Com a restauração, a validação do Flyway volta a reconhecer o histórico e a V3 pode ser aplicada normalmente. Os conteúdos de V1 e V2 foram conferidos contra os blobs originais do Git para garantir integridade.

-----

## Contratos novos ou consolidados

- `GET /habits/board` e operações em `/habits` para gerenciamento e métricas.
- `GET /calendar/events` para eventos e lembretes de tarefas somente leitura.
- `GET /planner/tasks`, `POST /planner/tasks/{id}/schedule` e `GET /planner/summary`.
- `GET /home/dashboard` para os indicadores diários oficiais.
- `PUT /notes/{id}` para atualização parcial de nota.

Os detalhes dos payloads e regras estão em [`BUSINESS_RULES.md`](../business-rules/BUSINESS_RULES.md).

## Segurança atual

### Controles presentes

- Senhas armazenadas com BCrypt; senhas e hashes não são retornados pela API.
- JWT assinado, com emissor e expiração validados e chave mínima obrigatória.
- API stateless: não cria sessão HTTP no servidor.
- CORS limitado à lista `CORS_ALLOWED_ORIGINS`; não há origem curinga configurada.
- Acesso a recursos é filtrado pelo usuário autenticado e rotas administrativas exigem `ADMIN`.
- Tokens de usuários desabilitados são recusados, inclusive se foram emitidos antes da desativação.
- CSRF está desabilitado porque a autenticação usa Bearer no cabeçalho, sem cookie de sessão.
- Testes cobrem autenticação, JWT, isolamento entre usuários, regras de agenda e concorrência de agendamento.

## Relatório de Riscos e Limitações Atuais

- Ausência de limitação de tentativas de login/cadastro (Prioridade Alta)
- Não há rate limiting implementado, o que expõe a aplicação a ataques de força bruta e à criação abusiva de contas. Próxima ação recomendada: aplicar rate limit por IP e e-mail, seja no proxy ou na própria API, além de registrar e alertar sobre bloqueios ocorridos.

- Logout local sem revogação ou refresh token (Prioridade Alta)
- O logout atual é feito apenas no cliente, sem revogação no servidor nem uso de refresh token. Isso significa que um JWT copiado pode continuar sendo usado até sua expiração natural. Próxima ação recomendada: criar um refresh token rotativo e revogável, com logout efetivo no servidor, mantendo o access token com vida curta.

- Segurança HTTP dependente da infraestrutura (Prioridade Alta)
- Cabeçalhos de segurança HTTP e o uso de HTTPS dependem da infraestrutura externa à aplicação. Sem HTTPS, o token Bearer pode ser interceptado; sem uma Content Security Policy (CSP), um ataque XSS teria impacto maior. Próxima ação recomendada: forçar HTTPS e HSTS no proxy, além de adicionar CSP, X-Content-Type-Options, Referrer-Policy e frame-ancestors.

spring.jpa.show-sql ativo (Prioridade Média)
Essa configuração ativa gera logs excessivos e representa exposição operacional caso permaneça em produção. Próxima ação recomendada: desativar em produção e centralizar os logs com mascaramento de dados sensíveis.

- Ausência de verificação de e-mail, recuperação de senha, MFA e política de bloqueio de contas (Prioridade Média)
Sem esses mecanismos, fica mais fácil ocorrer tomada de conta (account takeover) e mais difícil realizar suporte manual aos usuários. Próxima ação recomendada: implementar confirmação de e-mail, reset de senha com token de uso único e MFA para usuários com papel ADMIN.

- HS256 com segredo compartilhado entre emissor e validador (Prioridade Média)
Como o HS256 usa uma chave simétrica, qualquer serviço que tenha acesso a essa chave é capaz de emitir tokens válidos. Próxima ação recomendada: caso existam múltiplos serviços na arquitetura, migrar para chaves assimétricas com rotação periódica.

- Configuração de CORS dependente de variável de ambiente (Prioridade Média)
Uma lista de origens permitidas configurada de forma permissiva em produção pode expor a API a origens indevidas. Próxima ação recomendada: manter uma lista explícita e restrita por ambiente, revisando-a a cada deploy.

- Custo de consulta para autorização baseada em role (Prioridade Baixa)
- Embora o JWT já contenha a role do usuário, a API consulta o banco a cada requisição para conceder autoridade. Isso gera um custo adicional de consulta, mas em troca permite revogação imediata em caso de desativação de conta. Próxima ação recomendada: manter esse comportamento até que exista um mecanismo de revogação de token mais completo.

## Próximos passos sugeridos

1. Publicar com HTTPS e configurar `JWT_SECRET`, `API_URL`, `API_USER`, `API_PASSWORD` e `CORS_ALLOWED_ORIGINS` no gerenciador de segredos do ambiente.
2. Executar a aplicação em ambiente controlado para o Flyway validar V1/V2 e aplicar V3; não usar `flyway repair` para mascarar erros.
3. Adicionar rate limit de autenticação e telemetria de tentativas falhas.
4. Projetar refresh token rotativo, revogação e logout no servidor.
5. Adicionar cabeçalhos de segurança e uma política de logs de produção.
6. Fazer revisão de segurança antes de expor contas ADMIN e endpoints de troca de papel.

## Verificações realizadas

- Compilação e suíte de testes do backend com H2 isolado concluídas.
- Teste de V3 em PostgreSQL temporário concluído.
- V1 e V2 comparadas com os blobs originais recuperados do Git.
- Nenhuma migration foi aplicada por esta atividade ao banco real.
