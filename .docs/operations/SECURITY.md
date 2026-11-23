# Segurança

Controles implementados na API e riscos ainda abertos.

## Controles atuais

- Senhas com hash BCrypt (8 caracteres a 72 bytes); senhas e hashes nunca saem da API.
- Login com e-mail inexistente também executa BCrypt, reduzindo diferença de tempo.
- Access token JWT HS256 de 15 minutos, com assinatura, emissor e validade verificados.
  Chave obrigatória de pelo menos 32 bytes; a API não inicia sem ela.
- Refresh token opaco em cookie `HttpOnly`, `Secure`, `SameSite=Strict`, trocado a cada
  uso, guardado só como SHA-256 e revogado por inteiro ao detectar reuso.
- Conta e perfil consultados no banco a cada requisição: desativar a conta bloqueia
  também tokens já emitidos.
- API sem sessão HTTP no servidor. Rotas de sessão exigem `X-PlannerFy-Session: 1` e CORS
  com origens exatas, o que impede CSRF, inclusive no login.
- HTTPS obrigatório fora dos perfis `local` e `test` (`426` caso contrário). Headers
  encaminhados só são aceitos de proxies confiáveis.
- Dados filtrados pelo usuário do token em todas as consultas; registros de outra conta
  respondem `404`.

Detalhes em [Usuários e autenticação](../../src/main/java/com/workspace/api/user/README.md)
e [Access e refresh token](../authentication/ACCESS_AND_REFRESH_TOKENS.md).

## Riscos abertos

- **Sem limite de tentativas (alta):** login e cadastro não têm rate limiting, o que
  permite força bruta e criação abusiva de contas. Aplicar limite por IP e e-mail no
  proxy ou na API.
- **Headers de segurança dependem da infraestrutura (alta):** HSTS, CSP,
  `X-Content-Type-Options`, `Referrer-Policy` e `frame-ancestors` não são definidos pela API.
- **`spring.jpa.show-sql: true` (média):** gera logs com todo o SQL também em produção.
  Desativar fora do desenvolvimento.
- **Sem verificação de e-mail, recuperação de senha nem MFA (média).**
- **HS256 com segredo compartilhado (média):** quem tem a chave pode emitir tokens.
  Considerar chaves assimétricas se outros serviços precisarem validar tokens.
- **JWT continua válido após logout (baixa):** revogar a sessão não invalida o access
  token já emitido; ele vale até expirar (15 minutos).
