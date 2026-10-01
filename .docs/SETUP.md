# Executar a API PlannerFy

## Requisitos

- Java 21 e PostgreSQL com banco e usuário criados.
- Spring Boot 4.1.0. O Maven Wrapper (`mvnw.cmd`) já está no projeto; não é preciso instalar Maven.
- Na raiz de `api-workspace`, configure `.env` (ignorado pelo Git) ou variáveis de ambiente: `API_URL` (URL JDBC do PostgreSQL), `API_USER`, `API_PASSWORD` e `JWT_SECRET` (chave aleatória de pelo menos 32 bytes em Base64).

## Localhost

Com o PostgreSQL em execução, abra o terminal na pasta `api-workspace` e rode:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

A API estará em `http://localhost:8080`. Para usar o frontend, execute `npm start` na pasta `plannerFyFront` e abra `http://localhost:4200`. Se `CORS_ALLOWED_ORIGINS` estiver definido, use `http://localhost:4200`.

## Produção

Configure as variáveis de banco e JWT no ambiente de hospedagem, `SPRING_PROFILES_ACTIVE=production` e `CORS_ALLOWED_ORIGINS` com a origem HTTPS real do frontend. Gere o pacote com `.\mvnw.cmd clean package` e execute `java -jar target\api-0.0.1-SNAPSHOT.jar`.

Sirva a API atrás de um proxy com TLS. A configuração de produção atual espera proxy e backend na mesma máquina, com a API ouvindo em `127.0.0.1:8080`. Antes de implantar, confira o histórico das migrations Flyway V1/V2 no banco de destino.