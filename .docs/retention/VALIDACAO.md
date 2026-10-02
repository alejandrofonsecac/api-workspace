# Validação da retenção

## Cobertura existente

- **`RetentionLifecycleTest`**
  - Verifica conclusão, conclusão repetida, reabertura e uma nova conclusão.
  - Confere a preservação das sinalizações de recorrência.
- **`RetentionApiTest`**
  - Verifica contratos HTTP de tarefas, eventos e notas.
  - Confere o instante definido pelo servidor, o isolamento entre contas e a cópia da recorrência ao agendar.
- **`RetentionJobTest`**
  - Verifica o tamanho e o processamento dos lotes, o limite máximo por execução, falha isolada por recurso e configuração. O tamanho do lote limita cada operação; não é uma contagem mínima de notas para exclusão.
- **`RetentionSchedulingTest`**
  - Verifica o registro do agendamento e sua desativação por configuração.
- **`RetentionPostgresTest`**
  - Verifica o limite de sete dias, recorrência, tamanho do lote, bloqueio e reabertura concorrentes, transações independentes, migration V5 e índices.

Os quatro primeiros testes rodam com o perfil `test`, H2 em memória e Flyway desativado. O teste PostgreSQL só é habilitado quando a propriedade `retention.test.url` aponta explicitamente para um banco local chamado `retention_test`. Ele executa migrations, insere registros e limpa as tabelas do banco informado. Use sempre um banco descartável e isolado, sem dados da aplicação.

Na raiz do repositório, para executar somente os testes de retenção em H2:

```powershell
.\mvnw.cmd '-Dtest=Retention*Test' test
```

Para incluir `RetentionPostgresTest`, crie antes um banco PostgreSQL local descartável chamado `retention_test`, com usuário `retention_test` e senha vazia, e confirme que a porta usada corresponde à sua instância. Exemplo para porta 55439:

```powershell
.\mvnw.cmd '-Dtest=Retention*Test' '-Dretention.test.url=jdbc:postgresql://127.0.0.1:55439/retention_test' test
```

A suite PostgreSQL valida o schema com Hibernate, aplica Flyway V1–V5 em banco isolado e também testa V4→V5 em schema temporário. O teste não deve receber URL de um banco com dados reais. A suíte normal de backend continua disponível com `.\mvnw.cmd test`; alguns testes de migration têm condições de ambiente próprias e podem ser ignorados quando o banco descartável correspondente não estiver configurado.

Para uma revisão apenas da documentação, confira os links e o diff. Iniciar a API ou executar testes de negócio não é necessário para uma mudança documental.
