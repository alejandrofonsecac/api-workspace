# Deploy: Vercel (frontend) + Render (API)

## Arquitetura

```text
navegador ──HTTPS──> Vercel (plannerFyFront)
                       ├─ /api/*  ──rewrite HTTPS──> Render (api-workspace, Docker)
                       └─ demais rotas ─> index.html (SPA)
```

O navegador conversa apenas com o domínio da Vercel. O cookie `plannerfy_refresh` é
`SameSite=Strict` e host-only (veja [ACCESS_AND_REFRESH_TOKENS.md](../authentication/ACCESS_AND_REFRESH_TOKENS.md)),
então chamar `*.onrender.com` diretamente do navegador quebraria a renovação de sessão.
O `vercel.json` do frontend repassa `/api/*` à API removendo o prefixo, como o proxy de desenvolvimento.

## Render (Web Service)

- Runtime: **Docker**, usando o `Dockerfile` da raiz. Branch: a que contém o código atual de `main`.
- Health check: deixe vazio. Toda requisição sem HTTPS encaminhado recebe `426`, inclusive as internas.
- Variáveis de ambiente:
  - `SPRING_PROFILES_ACTIVE`: `production,render`.
  - `CORS_ALLOWED_ORIGINS`: origem exata do frontend, sem `/` final; várias separadas por vírgula.
  - `API_URL`, `API_USER`, `API_PASSWORD`: conexão PostgreSQL (`jdbc:postgresql://...`).
  - `JWT_SECRET`: Base64 de no mínimo 32 bytes aleatórios; veja [API_OPERATION.md](API_OPERATION.md).
  - `APP_TIME_ZONE`: opcional; padrão `America/Sao_Paulo`.

O perfil `render` sobrepõe o `production`: escuta em `0.0.0.0:$PORT` e confia em
`X-Forwarded-Proto` apenas de endereços privados/loopback, de onde chega o proxy TLS do Render.
O Render não publica a faixa do proxy; se o login responder `426 HTTPS_REQUIRED`, o proxy está fora dessa faixa.

### Manter a instância acordada

No plano gratuito, o Render suspende o serviço após cerca de 15 minutos sem tráfego, e a
requisição seguinte espera a inicialização (cold start). Para evitar isso, um monitor externo
chama a API a cada 10 minutos:

- URL: `https://<servico>.onrender.com/ping`, sempre em HTTPS; em HTTP o filtro de sessão responde `426`.
- Método: `GET` ou `HEAD`. A resposta é `204` sem corpo e com `Cache-Control: no-store`.
- Sem cabeçalho `Authorization`: a rota é pública e um token inválido resultaria em `401`.

`/ping` não consulta o banco nem serviços externos e não revela versão, estado ou horário, então
custa o mesmo que uma requisição recusada. Só `GET`/`HEAD` nesse caminho exato são públicos; outros
métodos e subcaminhos (`/ping/`, `/ping/x`) continuam exigindo autenticação.
Manter a instância sempre ativa consome cerca de 744 das 750 horas mensais gratuitas do Render,
compartilhadas entre os serviços gratuitos da conta.

### CORS mesmo com proxy

A Vercel repassa o cabeçalho `Origin` do navegador. A API compara essa origem com a lista de
`CORS_ALLOWED_ORIGINS` e recusa (`403`) as demais, o que também protege os endpoints de sessão.
Use o domínio estável do projeto (Vercel → Settings → Domains). URLs de deployment como
`planner-fy-front-<hash>.vercel.app` mudam a cada deploy e exigiriam atualizar a variável.
Não use curingas em `*.vercel.app`: qualquer pessoa pode criar subdomínios ali.

## Banco de dados

O Flyway aplica V1–V8 na inicialização. V1 usa `IF NOT EXISTS` para preservar tabelas criadas
pela versão anterior com `ddl-auto: update`. Faça backup do banco antes do primeiro deploy desta versão.

## Verificação após o deploy

1. `GET https://<frontend>/api/users/me` sem token responde `401` vindo da API.
2. Login pela interface cria o cookie no domínio da Vercel; recarregar a página mantém a sessão.
3. Login respondendo `403` indica origem ausente em `CORS_ALLOWED_ORIGINS`; `426`, proxy não reconhecido.
