# Retenção automática

A API remove tarefas, eventos e notas concluídos depois de sete dias completos, desde que não estejam marcados como recorrentes. A conclusão e seu instante são definidos pelo backend. A limpeza é um job interno; não existe rota HTTP para executá-la.

- **Regras de retenção:** [elegibilidade, conclusão, recorrência e dados antigos](REGRAS.md).
- **Contrato da API:** [campos aceitos, respostas e exemplos](CONTRATO_API.md).
- **Operação e persistência:** [agendamento, lotes, banco, configuração e implantação](OPERACAO.md).
- **Validação:** [cenários cobertos e execução segura dos testes](VALIDACAO.md).

A implementação principal está em [RetentionJob.java](../../src/main/java/com/workspace/api/retention/service/RetentionJob.java). O ciclo de conclusão é compartilhado em [RetainedEntity.java](../../src/main/java/com/workspace/api/retention/domain/RetainedEntity.java). As diretrizes gerais do backend permanecem em [AGENTS.md](../../AGENTS.md).
