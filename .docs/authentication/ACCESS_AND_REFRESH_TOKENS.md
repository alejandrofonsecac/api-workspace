# Autenticação: access token e refresh token

O JWT HS256 e o contrato AuthResponse continuam existentes. O padrão de JWT_TTL foi
ajustado de PT1H para PT15M; configure JWT_TTL=PT15M também nos ambientes que tenham
override. O access token fica apenas na memória do Angular.

## Sessões

Cada login/cadastro cria uma família por sessão, com refresh token opaco de 32 bytes
gerados por SecureRandom. Somente SHA-256 é persistido em tb_refresh_tokens; a
entropia aleatória permite usar hash criptográfico sem o custo de BCrypt das senhas.
O valor original aparece somente no Set-Cookie, nunca no JSON ou em logs.

O cookie plannerfy_refresh é HttpOnly, Secure em produção, SameSite=Strict, Path=/,
sem Domain (host-only). Seu Max-Age corresponde ao menor prazo restante: inatividade
ou limite absoluto. Frontend e API precisam ser **same-site**; preferir /api via proxy.
Hosts de sites diferentes não são suportados por essa política de cookie.

Padrões configuráveis: REFRESH_MAX_AGE=P90D e REFRESH_IDLE_TIMEOUT=P30D.
A inatividade é medida desde a emissão/última renovação, não desde cliques na tela.
O máximo é validado em até 90 dias; ambos os valores precisam ser positivos e a
inatividade não pode ultrapassar o máximo. Renovar nunca prolonga o limite absoluto.

A rotação marca o hash anterior como usado e grava outro token na mesma família.
Um bloqueio pessimista da linha do usuário serializa emissão, refresh e revogação,
inclusive entre instâncias da API. Reutilizar um token usado/revogado revoga a
família inteira, incluindo seus descendentes. Outras sessões não são revogadas.
Essa revogação é confirmada no banco mesmo quando a resposta é 401.
Tokens vencidos e usuários desativados também não renovam.

Os hashes consumidos são retidos para detectar reuso. Uma futura rotina de limpeza
só deve remover famílias depois de absolute_expires_at; não remover hashes usados
enquanto a família puder estar ativa. Esta entrega não agenda limpeza automática.

## Contrato HTTP

Todos os POST abaixo exigem X-PlannerFy-Session: 1. O Angular envia esse header e
withCredentials: true para login, cadastro, refresh e logout. O header força
preflight em chamadas cross-origin; CORS aceita somente as origens exatas configuradas
em CORS_ALLOWED_ORIGINS, com credenciais. Não configure curingas ou origens não
confiáveis. Essa proteção contra CSRF também cobre login e logout.

| Endpoint | Entrada | Resposta |
| --- | --- | --- |
| POST /users/register | JSON name, email, password | 201 AuthResponse + cookie |
| POST /users/login | JSON email, password | 200 AuthResponse + cookie |
| POST /users/refresh | Cookie; corpo vazio ou {} | 200 AuthResponse + cookie rotacionado |
| POST /users/logout | Cookie; corpo vazio ou {} | 204; revoga família e apaga cookie |
| POST /users/logout-all | Authorization: Bearer JWT + header de sessão | 204; revoga todas as sessões da conta e apaga cookie |
| GET /users/me | Authorization: Bearer JWT | UserResponse existente |

AuthResponse mantém accessToken, tokenType, expiresIn e user (id, name, email, role).
Logout é idempotente e funciona sem access token válido. Novo login no mesmo
navegador revoga a família do cookie anterior. Uma falha de login não troca o cookie.

Refresh ausente, desconhecido, expirado, revogado, reutilizado ou de conta desativada
retorna 401 com {status:401, code:"REFRESH_UNAVAILABLE",
message:"Session expired or revoked", details:[]}. Não há indicação externa que
permita distinguir reuso de credencial inválida. Header de sessão ausente retorna
403 SESSION_HEADER_REQUIRED; HTTP em produção retorna 426 HTTPS_REQUIRED.
As respostas de sessão usam Cache-Control: no-store. Erros de refresh não apagam
cookies para evitar sobrescrever o cookie de outra resposta concorrente.
Erros de login/cadastro preservam o formato e os status anteriores.

Revogar refresh tokens **não invalida imediatamente JWTs já emitidos**: continuam
válidos até expirar (15 minutos, mais a tolerância de relógio do decoder).
Isso preserva o access token atual. Desativar a conta bloqueia também o JWT.
Logout global está disponível na API, sem novo botão nesta entrega.

## Angular

AuthSession mantém a identidade e o token em memória. Ao expirar o access token,
a próxima requisição renova a sessão; um 401 também provoca uma única renovação e
uma única repetição da requisição original. Requisições simultâneas compartilham o
refresh; um 401 atrasado usa o token mais recente. Falha na renovação ou outro 401
encerra a sessão com navegação completa para /login?reason=expired, limpando caches.
403 e 5xx de recursos não são tratados como expiração.

O inicializador restaura a sessão via cookie ao abrir a aplicação. Ausência de
sessão nessa tentativa inicial mantém o visitante anônimo, sem loop de redireção.
Guards esperam a renovação quando a identidade existe e o access token expirou.

Web Locks serializa refresh/logout entre abas da mesma origem quando disponível.
Sem Web Locks, a concorrência é agrupada dentro de cada aba; uma colisão entre abas
pode acionar a revogação conservadora por reuso e exigir login. Não há janela que
reaceite tokens usados. Troca de conta detectada na renovação encerra a sessão
antes de repetir requisições da conta anterior. Credenciais não são enviadas para
origens ou caminhos fora da API configurada.

Logout aguarda uma rotação pendente antes de revogar o cookie mais recente.
Falha de logout é exibida na interface e permite tentar novamente; não simula
revogação bem-sucedida. Não há tokens em localStorage/sessionStorage nem leitura
de cookies por JavaScript.

## Execução e implantação

Use SPRING_PROFILES_ACTIVE=local somente no desenvolvimento HTTP localhost.
Fora dos perfis local/test, HTTPS é obrigatório e os cookies são Secure.
Os perfis prod/production mantêm essa exigência mesmo combinados com local/test.
Em produção, configure TLS no servidor ou em proxy confiável que informe o esquema
correto à aplicação. Se usar headers encaminhados, aceite-os somente desse proxy,
removendo headers fornecidos pelo cliente. Nunca ative local/test em produção.

A migration aditiva V4__refresh_token_sessions.sql depende de tb_users (V2).
Não altere V1/V2/V3 nem desative validação Flyway. Embora V1/V2 estejam neste checkout,
confira checksums e histórico do banco de destino antes de implantar.
Nenhum banco da aplicação deve ser usado para os testes.

Backend: .\\mvnw.cmd test. RefreshSessionIntegrationTest cobre login, refresh,
expiração, reuso com revogação confirmada, concorrência, logout local/global,
isolamento, conta desativada e CSRF/CORS. SessionRequestFilterTest cobre HTTPS e
atributos do cookie. O perfil test usa explicitamente H2 em memória.
Frontend: npm test -- --watch=false, npm run lint e npm run build; testes de auth
cobrem credenciais, renovação agrupada, retry, restauração, expiração e logout.

A política de rotação, reuso e inatividade segue
[RFC 9700, seção 4.14](https://www.rfc-editor.org/rfc/rfc9700.html#section-4.14).

## Validação desta alteração

- Frontend: 102 testes passaram; lint e build de produção passaram.
- Backend: 9 testes de fluxo passaram em H2 e novamente em PostgreSQL 18 temporário;
  2 testes de HTTPS/cookie e 6 testes de configuração JWT passaram.
- RefreshMigrationTest validou V4 e suas restrições em PostgreSQL temporário.
  Ele só executa com REFRESH_TEST_URL=jdbc:postgresql://127.0.0.1:<porta>/postgres,
  usando a conta refresh_test de um cluster descartável. A fixture mínima de
  tb_users é criada em schema aleatório e revertida ao final.
- A execução conjunta de MultiUserSecurityTest encontrou 3 erros preexistentes:
  ValidEventTimeRange não declara groups, exigido pelo Bean Validation.
  A anotação de eventos não foi alterada neste trabalho.
- Não houve implantação nem acesso ao banco real. O teste isolado de V4 não
  substitui a conferência do histórico Flyway completo do ambiente de destino.
