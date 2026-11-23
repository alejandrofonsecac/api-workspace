# Configuração do JWT

Como a API assina e verifica o access token, e como configurar a chave.

## Configurar a chave

`JWT_SECRET` é obrigatória e não tem valor padrão. Ela precisa conter pelo menos 32 bytes
aleatórios, codificados em Base64. Uma senha digitada com 32 caracteres não substitui
bytes aleatórios.

Para gerar no PowerShell:

```powershell
$jwtBytes = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtBytes)
[Convert]::ToBase64String($jwtBytes)
$jwtRandom.Dispose()
```

Guarde o resultado em `JWT_SECRET=<resultado>` no `.env` ou no ambiente de hospedagem.

- A API não inicia se a chave estiver ausente, vazia, fora de Base64 ou com menos de 32
  bytes. A mensagem de erro aponta para esta página.
- Todas as instâncias da API precisam da mesma chave e do mesmo `JWT_ISSUER`.
- Trocar a chave invalida todos os tokens emitidos com a anterior.
- Os testes usam uma chave fictícia em `application-test.yml`; nunca a use fora dos testes.

Outras variáveis:

- `JWT_ISSUER` (padrão `workspace-api`): emissor gravado e exigido no token.
- `JWT_TTL` (padrão `PT15M`): validade do access token, em ISO-8601. Aumentá-lo não
  resolve sessões que terminam cedo; o frontend deve chamar `POST /users/refresh`. Um
  TTL maior só amplia o tempo em que um token revogado continua aceito.

## Hash de senha e assinatura de token

São operações diferentes, feitas por componentes diferentes:

- `PasswordEncoder` (BCrypt, em `SecurityConfiguration`): gera o hash da senha no cadastro
  e confere a senha no login com `matches`. O hash não pode ser desfeito.
- `JwtEncoder` (em `JwtConfiguration`): assina o JWT emitido após login, cadastro ou refresh.
- `JwtDecoder` (em `JwtConfiguration`): verifica o JWT recebido nas requisições seguintes.

## Conteúdo do token

`UserService.token()` emite o JWT com:

- `iss`: emissor (`JWT_ISSUER`).
- `sub`: ID do usuário.
- `iat` e `exp`: emissão e expiração.
- `jti`: identificador aleatório do token.
- `role`: perfil no momento da emissão (informativo).

O JWT é **assinado, não criptografado**: o cliente consegue ler seu conteúdo. Por isso
ele nunca leva senha, hash ou outro dado sensível.

## Verificação

1. O resource server do Spring Security lê `Authorization: Bearer <token>`.
2. O decoder aceita apenas HS256 e confere assinatura, emissor e validade, com uma
   pequena tolerância de relógio.
3. `SecurityConfiguration` exige `sub` e `exp`, busca a conta no banco e recusa contas
   inexistentes ou desativadas. As permissões usam o perfil atual do banco, não o `role`
   do token.
4. O ID do usuário fica disponível para os serviços por `CurrentUser.id()`.

## Testes

- `JwtConfigurationTest`: chave ausente, vazia, fora de Base64, curta e emissão/verificação.
- `MultiUserSecurityTest`: login, hash, tokens inválidos e isolamento dos dados.

Referência: [Spring Security — JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
