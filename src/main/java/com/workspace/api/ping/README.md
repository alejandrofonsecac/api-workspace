# Ping

Endpoint público usado por um monitor externo para manter a instância do Render acordada
(no plano gratuito, ela hiberna após cerca de 15 minutos sem tráfego).

## Endpoint

- `GET /ping` ou `HEAD /ping`, sem autenticação.
- Responde `200` com o texto `Ping` (`text/plain`; sem corpo no `HEAD`) e
  `Cache-Control: no-store`.

## Regras

- Não consulta banco nem serviços externos e não revela versão, estado ou horário.
- Só `GET` e `HEAD` neste caminho exato são públicos; outros métodos e subcaminhos
  (`/ping/`, `/ping/x`) exigem autenticação.
- Não envie `Authorization`: um token inválido resulta em `401`.
- Fora dos perfis `local` e `test`, chamadas sem HTTPS recebem `426`.

A configuração do monitor está em
[Deploy Render + Vercel](../../../../../../../.docs/operations/DEPLOY_RENDER_VERCEL.md#manter-a-instância-acordada).

## Código e testes

- `PingController`; liberação da rota em `SecurityConfiguration`.
- `PingTest`.
