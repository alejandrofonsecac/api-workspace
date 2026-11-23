# Deploy: Vercel (frontend) + Render (API)

## Arquitetura

```text
navegador ──HTTPS──> Vercel (plannerFyFront)
                       ├─ /api/*  ──rewrite HTTPS──> Render (api-workspace, Docker)
                       └─ demais rotas ─> index.html (SPA)
```

O navegador fala só com o domínio da Vercel. O cookie de sessão `plannerfy_refresh` é
`SameSite=Strict` e sem `Domain`; chamar `*.onrender.com` direto do navegador quebraria a
renovação da sessão. O `vercel.json` do frontend repassa `/api/*` para a API, removendo
o prefixo.

## Render (Web Service)

- Runtime **Docker**, com o `Dockerfile` da raiz. O build gera o jar sem rodar testes;
  rode `.\mvnw.cmd test` antes de publicar.
- Branch: a que contém o código atual de `main`.
- Health check: deixe vazio. Requisições sem HTTPS encaminhado recebem `426`, inclusive as internas.
- Variáveis de ambiente:
  - `SPRING_PROFILES_ACTIVE`: `production,render`.
  - `CORS_ALLOWED_ORIGINS`: origem exata do frontend, sem `/` no final; várias separadas por vírgula.
  - `API_URL`, `API_USER`, `API_PASSWORD`: conexão PostgreSQL (`jdbc:postgresql://...`).
  - `JWT_SECRET`: Base64 de pelo menos 32 bytes aleatórios; veja [JWT](../authentication/JWT_CONFIGURATION.md).
  - `APP_TIME_ZONE`: opcional; padrão `America/Sao_Paulo`.

O perfil `render` escuta em `0.0.0.0:$PORT` e só confia em `X-Forwarded-Proto` vindo de
endereços privados ou de loopback, de onde chega o proxy TLS do Render. Se o login
responder `426 HTTPS_REQUIRED`, o proxy está fora dessa faixa.

### Manter a instância acordada

No plano gratuito, o Render suspende o serviço após cerca de 15 minutos sem tráfego, e a
requisição seguinte espera a inicialização. Configure um monitor externo:

- URL `https://<servico>.onrender.com/ping`, sempre em HTTPS.
- Método `GET` ou `HEAD`, a cada 10 minutos, sem header `Authorization`.
- Resposta esperada: `200` com o texto `Ping`. Detalhes em [Ping](../../src/main/java/com/workspace/api/ping/README.md).

Manter a instância sempre ativa consome cerca de 744 das 750 horas mensais gratuitas,
compartilhadas entre os serviços gratuitos da conta.

### CORS mesmo com proxy

A Vercel repassa o header `Origin` do navegador, e a API recusa (`403`) origens fora de
`CORS_ALLOWED_ORIGINS`, o que também protege as rotas de sessão.

- Use o domínio estável do projeto (Vercel → Settings → Domains). URLs de deployment como
  `planner-fy-front-<hash>.vercel.app` mudam a cada deploy.
- Não use curingas em `*.vercel.app`: qualquer pessoa pode criar subdomínios ali.

## Banco de dados

O Flyway aplica as migrations pendentes (V1–V9) na inicialização. Faça backup antes de
publicar uma versão com migrations novas. Veja [Banco de dados](DATABASE.md).

## Verificação após o deploy

1. `GET https://<frontend>/api/users/me` sem token responde `401`, vindo da API.
2. O login pela interface cria o cookie no domínio da Vercel; recarregar a página mantém a sessão.
3. Login com `403`: origem ausente em `CORS_ALLOWED_ORIGINS`. Com `426`: proxy não reconhecido.
