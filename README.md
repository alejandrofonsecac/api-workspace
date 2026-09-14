# PlannerFy API

A branch `production` prepara o backend Java 21 para hospedagem com Docker.
Frontend: https://plannerfy-production.jfonsecacorrea08.chatgpt.site

## Render

O arquivo `render.yaml` define um Web Service Docker na branch `production`,
com o perfil Spring `production`, CORS para o frontend e health check configurados.
Ele usa o plano `free`, sem criar outro banco de dados. Deploys posteriores são
manuais (`autoDeployTrigger: off`). A criação do arquivo não publica a aplicação.

Depois de enviar a branch ao GitHub:

1. No [painel do Render](https://dashboard.render.com), escolha **New > Blueprint**.
2. Conecte `alejandrofonsecac/api-workspace` e selecione a branch `production`.
3. Use `render.yaml` e preencha `API_URL`, `API_USER` e `API_PASSWORD` com as
   credenciais do PostgreSQL que o serviço deve acessar. `API_URL` precisa ter o
   formato `jdbc:postgresql://HOST:5432/DATABASE?sslmode=require`.
4. Crie o serviço e aguarde o deploy ficar **Live**. Copie a URL HTTPS exibida pelo
   Render e confirme `/actuator/health` antes de configurar o frontend.

O plano gratuito pode suspender o serviço por inatividade; consulte os
[limites do Render Free](https://render.com/docs/free). Para disponibilidade
contínua, escolha um plano adequado no painel.

Referência: [Render Blueprints](https://render.com/docs/blueprint-spec).

## Publicação

1. Conecte este repositório à hospedagem do backend, selecionando a branch `production`
   e o `Dockerfile` na raiz.
2. Configure os segredos `API_URL` (URL **JDBC** do PostgreSQL), `API_USER` e
   `API_PASSWORD` no serviço. Consulte `.env.example`, sem versionar credenciais.
3. O container ativa `SPRING_PROFILES_ACTIVE=production` e escuta em `PORT`
   (padrão `8080`). Configure o health check em `/actuator/health`.
4. Habilite uma URL pública **HTTPS** no provedor. O health check deve retornar
   HTTP 200 e `{"status":"UP"}` com acesso ao banco.
5. Configure essa URL no frontend, incluindo `API_BASE_URL` em
   `src/app/core/data-access/api-client.service.ts` e a URL atualmente fixa em
   `src/app/pages/calendar/service/calendar-events.service.ts`. Compile e publique
   uma nova versão do frontend no Sites.

`CORS_ALLOWED_ORIGINS` aceita origens exatas separadas por vírgula, sem barra final.
No perfil `production`, o padrão é apenas a origem HTTPS do PlannerFy acima.
O perfil local mantém `http://localhost:4200`.

O push da branch não cria um servidor: é necessário conectar um serviço de hospedagem.
O frontend publicado continua sem acesso à API até concluir a hospedagem e atualizar
a URL no frontend.

## Execução local do container

Preencha `.env` com as variáveis do exemplo e execute:

```sh
docker compose up --build -d
```

O PostgreSQL é externo; o Compose não cria nem apaga um banco. O build não copia
`.env` para a imagem. O processo Java executa sem privilégios de root.

## Validação

Com JDK 21:

```sh
./mvnw verify
```

No Windows, use `mvnw.cmd verify`. Os testes usam H2 isolado e verificam também
o CORS de produção e o health check.

## Limitações atuais

- A API não implementa autenticação: clientes HTTP podem ler e modificar dados.
  CORS controla chamadas de navegadores e não substitui autenticação.
- O projeto mantém `ddl-auto: update`, usado pelo schema atual. Não há migrações SQL
  versionadas; revise alterações de entidades e mantenha backup do banco antes de
  atualizar uma instalação com dados.
- O health check segue o [Spring Boot Actuator](https://docs.spring.io/spring-boot/api/rest/actuator/health.html).
