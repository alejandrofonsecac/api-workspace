# Como funciona a autenticação

## O erro de inicialização corrigido

A aplicação precisava de `JWT_SECRET`, mas o `../.env` local tinha apenas as variáveis do banco.
O erro observado foi:

```text
Could not resolve placeholder 'JWT_SECRET'
```

O Spring precisa criar os objetos usados pela aplicação antes de aceitar requisições.
A ausência da chave causou esta sequência:

1. `jwtKey` não pôde ser criado.
2. `jwtEncoder` precisa dessa chave, então também falhou.
3. `UserService` recebe o encoder no construtor, então sua criação falhou.
4. `UserController` depende do service, e a aplicação não iniciou.

Por isso o erro citava o service mesmo sem haver uma tentativa de login.
Foi gerada uma chave aleatória no `../.env` local, que é ignorado pelo Git.
Nenhuma chave real está neste documento ou no código versionado.

Agora, se a configuração estiver ausente, a mensagem informa:
`JWT_SECRET não configurado` e aponta para este documento.
O valor vazio em `${JWT_SECRET:}` permite chegar a essa validação;
uma chave vazia continua sendo rejeitada. Não existe chave padrão compartilhada.

## Três objetos com funções diferentes

| Objeto | Função | Onde é criado |
| --- | --- | --- |
| `PasswordEncoder` | Calcula e verifica o hash da senha com BCrypt | `SecurityConfiguration.passwordEncoder()` |
| `JwtEncoder` | Monta e assina o JWT emitido após o login ou cadastro | `JwtConfiguration.jwtEncoder()` |
| `JwtDecoder` | Verifica o JWT recebido nas próximas requisições | `JwtConfiguration.jwtDecoder()` |

O nome “encoder” é genérico. Neste projeto, **hash de senha e assinatura de token são operações distintas**.

### PasswordEncoder: guardar a senha

No cadastro, `UserService.register()` executa:

```java
user.setPasswordHash(passwordEncoder.encode(request.password()));
```

O banco recebe um hash BCrypt, não a senha digitada.
BCrypt inclui um valor aleatório, chamado salt, por isso a mesma senha pode produzir hashes diferentes.
Não se “desfaz” o hash para obter a senha.

No login, usamos:

```java
passwordEncoder.matches(request.password(), user.getPasswordHash());
```

Esse método verifica a senha contra o hash salvo. Gerar outro hash e comparar as strings seria incorreto.
O projeto limita senhas a 72 bytes em UTF-8 por causa do limite do BCrypt; caracteres acentuados
podem ocupar mais de um byte.

O `dummyHash` no service é um hash de comparação usado quando o e-mail não existe.
Ele faz o caminho de usuário inexistente também executar BCrypt, reduzindo a diferença de tempo
em relação a uma senha incorreta. Não representa uma conta ou uma senha alternativa.

### JwtEncoder: emitir um comprovante de autenticação

Depois de validar o login, o servidor precisa informar ao cliente qual usuário foi autenticado.
O JWT é um token assinado usado nas chamadas seguintes, sem reenviar a senha.

O método `UserService.token()` prepara os dados do token, chamados de claims:

| Campo | Significado no projeto |
| --- | --- |
| `iss` | Quem emitiu: valor de `JWT_ISSUER` |
| `sub` | ID do usuário; identifica o proprietário dos dados |
| `iat` | Quando foi emitido |
| `exp` | Quando expira |
| `jti` | Identificador aleatório deste token |
| `role` | Perfil no momento da emissão |

`JwsHeader.with(MacAlgorithm.HS256)` informa o algoritmo de assinatura.
`JwtEncoderParameters.from(header, claims)` reúne o cabeçalho e os dados.
`jwtEncoder.encode(...).getTokenValue()` entrega a string assinada que vai em `accessToken`.

**Assinatura não criptografa o conteúdo.** O cliente consegue ler os dados do JWT.
A assinatura permite detectar alterações. Por isso nunca colocamos senha ou hash no token.

### JwtDecoder: verificar esse comprovante

O cliente envia:

```http
Authorization: Bearer <accessToken>
```

O filtro configurado em `SecurityConfiguration.oauth2ResourceServer(...)` recebe o token e chama o decoder.
O decoder aceita HS256 e verifica a assinatura, o emissor e os limites temporais,
incluindo expiração. A biblioteca aplica uma pequena tolerância para diferenças entre relógios.

Depois, o conversor em `SecurityConfiguration` exige `sub` e `exp`, procura a conta no banco
e rejeita contas inexistentes ou desativadas. Ele usa o perfil atual do banco para as permissões;
o campo `role` de um token antigo pode estar desatualizado.

Se tudo estiver válido, o Spring guarda a identidade na autenticação da requisição.
`CurrentUser.id()` lê essa identidade. Os services usam esse ID para criar e consultar os registros,
em vez de confiar em um `userId` enviado pelo cliente.

## Entendendo JwtConfiguration.java

- `@Configuration`: esta classe contém definições de objetos gerenciados pelo Spring.
- `@Bean`: o retorno do método é um desses objetos. O Spring o cria e o fornece a quem precisa dele.
- `@Value("${app.jwt.secret:}")`: lê a configuração, mapeada para `JWT_SECRET` no YAML.
- `Base64.getDecoder().decode(secret)`: transforma o texto Base64 nos bytes originais da chave.
  Esse decoder do Java apenas converte um formato; ele não valida JWTs.
- `new SecretKeySpec(bytes, "HmacSHA256")`: representa esses bytes como uma chave HMAC.
- `new ImmutableSecret<>(jwtKey)`: adapta a chave para a biblioteca Nimbus; não gera nem troca a chave.
- `new NimbusJwtEncoder(...)`: cria a implementação que assina JWTs.
- `NimbusJwtDecoder.withSecretKey(jwtKey)`: configura a verificação com a mesma chave.
- `JwtValidators.createDefaultWithIssuer(issuer)`: acrescenta as validações de tempo e emissor.

HS256 usa um segredo compartilhado: a mesma chave assina e verifica.
A chave precisa ter pelo menos 32 bytes aleatórios; uma senha humana com 32 caracteres não equivale
a uma chave aleatória. Base64 é apenas uma forma de armazenar esses bytes em texto.

No construtor de `UserService`, os parâmetros `PasswordEncoder` e `JwtEncoder` são preenchidos pelo Spring
com os objetos definidos acima. Isso é a injeção de dependências.
O service não precisa construir outra chave ou outro encoder.

## Configuração local e em outros ambientes

`ApiApplication.main()` carrega o `../.env` do diretório de execução e publica suas variáveis antes de iniciar o Spring.
No IntelliJ, execute `ApiApplication` com a raiz do projeto como diretório de trabalho.

A configuração local já recebeu `JWT_SECRET`. Em outro computador ou servidor, gere uma chave própria:

```powershell
$jwtBytes = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtBytes)
[Convert]::ToBase64String($jwtBytes)
$jwtRandom.Dispose()
```

Copie o resultado para `JWT_SECRET=<resultado>` no `../.env`, ou configure a variável de ambiente.
Não use a chave pública do perfil de testes na aplicação.

| Variável | Configuração |
| --- | --- |
| `JWT_SECRET` | Obrigatória; segredo Base64 |
| `JWT_ISSUER` | Padrão `workspace-api`; deve ser igual na emissão e verificação |
| `JWT_TTL` | Padrão `PT15M`; duração de 15 minutos, no formato ISO-8601 |

Mantenha a chave entre reinicializações para continuar aceitando tokens válidos já emitidos.
Se houver várias instâncias da mesma API, elas precisam compartilhar a chave e o emissor.
Trocar a chave invalida os tokens assinados com a anterior.

Os testes Spring usam uma chave fictícia em `application-test.yml`. Eles não executam
`ApiApplication.main()` para carregar o `../.env`. Portanto, os testes passarem com essa chave
não comprovava que o ambiente local estava configurado — essa foi a lacuna da primeira entrega.

## O que está disponível

- `POST /users/register`: cria uma conta USER e emite JWT.
- `POST /users/login`: verifica e-mail e senha e emite JWT.
- `GET /users/me`: retorna a conta autenticada.

A remoção do endpoint de alteração de perfis foi preservada. Atualmente perfis são alterados no banco.
O fluxo de refresh, cookies, rotação e revogação está documentado em [`ACCESS_AND_REFRESH_TOKENS.md`](ACCESS_AND_REFRESH_TOKENS.md).
Desativar uma conta bloqueia seus tokens nas próximas requisições.

## Como validar

Execute `.\mvnw.cmd test` no Windows ou `./mvnw test` nos demais ambientes.
`JwtConfigurationTest` cobre ausência da chave, chave vazia, Base64 inválido, chave curta
e emissão/verificação com a chave configurada.
`MultiUserSecurityTest` cobre login, hash da senha, tokens inválidos e isolamento dos dados.

Referência: [Spring Security — JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
