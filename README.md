# projeto_arqsoft_1240447

Projeto 1 da unidade curricular de Arquitetura de Software (ARQSOFT), 2026-2027.

O ponto de partida é o serviço REST de gestão de biblioteca desenvolvido em PSOFT
(`psoft-project-2024-g1`). Este repositório é independente e pessoal; o histórico
do projeto original não é reutilizado.

## Estado atual

- Base importada e a compilar.
- Fase 1 (políticas de empréstimo) implementada.
- `mvn test` executa 138 testes, todos verdes.
- A Fase 2 (fontes bibliográficas e notificações) ainda não foi implementada.

## Como validar

```bash
mvn test
```

Para arrancar a aplicação sem depender do servidor H2 TCP configurado no base:

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:h2:mem:projeto-arqsoft"
```

A documentação de arquitetura, decisões, vistas e rastreabilidade será mantida
neste único `README.md`. Não serão criados outros ficheiros Markdown.

## Fase 1 — Políticas de empréstimo

### Regras implementadas

- **Standard**: máximo de empréstimos ativos, prazo inicial, máximo de renovações
  e prazo de renovação configuráveis.
- **Adaptive**: o prazo depende do número de empréstimos ativos; os limiares e os
  três prazos são configuráveis.
- A política é selecionada em runtime, em cada criação ou renovação:
  - Standard por defeito;
  - Adaptive se, nos últimos `HistoryPeriod` meses, o leitor tiver pelo menos
    `MinCompletedLendings` empréstimos devolvidos, uma taxa de pontualidade igual
    ou superior a `MinOnTimeRatio` e nenhum empréstimo em atraso.
- Um leitor com qualquer empréstimo em atraso não pode criar nem renovar.
- A renovação usa `If-Match` e `@Version`. O leitor é bloqueado com lock
  pessimista durante a criação/renovação para evitar ultrapassar o máximo de
  empréstimos ativos em concorrência.

### Configuração

| Propriedade | Valor por defeito |
|---|---:|
| `lending.policies.selection.history-period-months` | 12 |
| `lending.policies.selection.min-completed-lendings` | 3 |
| `lending.policies.selection.min-on-time-ratio-percent` | 80 |
| `lending.policies.standard.max-active-lendings` | 3 |
| `lending.policies.standard.lending-period-days` | 15 |
| `lending.policies.standard.max-renewals` | 2 |
| `lending.policies.standard.renewal-period-days` | 7 |
| `lending.policies.adaptive.max-active-lendings` | 3 |
| `lending.policies.adaptive.first-threshold` | 1 |
| `lending.policies.adaptive.second-threshold` | 2 |
| `lending.policies.adaptive.long-period-days` | 21 |
| `lending.policies.adaptive.standard-period-days` | 14 |
| `lending.policies.adaptive.short-period-days` | 7 |
| `lending.policies.adaptive.max-renewals` | 2 |

As restrições da especificação são verificadas no arranque. Uma configuração
inválida impede a aplicação de arrancar.

O `bootstrap` cria históricos de demonstração: o leitor `{ano}/7` qualifica-se
para a política Adaptive; o leitor `{ano}/8` tem três empréstimos devolvidos, mas
um deles fora do prazo, pelo que continua na Standard.

### Endpoints

| Método | Rota | Papel | Objetivo |
|---|---|---|---|
| `PATCH` | `/api/lendings/{year}/{seq}/renew` | Reader/Librarian | Renovar com `If-Match` e reavaliação da política |
| `GET` | `/api/lendings/policies/{year}/{seq}` | Reader (próprio)/Librarian | Política efetiva, métricas e período aplicável |

### ADR-07 — Renovação na política Adaptive

A especificação define `MaxRenewals` e `RenewalPeriod` apenas na política Standard.
Para a Adaptive, a renovação:

- reavalia a política aplicável;
- se continuar Adaptive, recalcula o período (`Long`/`Standard`/`Short`) a partir
  dos empréstimos ativos;
- usa `adaptive.max-renewals` como limite de renovações;
- exige ausência de atrasos e não aumenta o número de empréstimos ativos.

Alternativa considerada: usar sempre `RenewalPeriod` também na Adaptive.
Rejeitada porque tornaria a renovação independente do estado que a política
Adaptive usa para calcular o prazo. A decisão deve ser revista se o docente
interpretar o ponto 3.6 da especificação de outra forma.

### Testes da fase 1

- `StandardLendingPolicyTest` e `AdaptiveLendingPolicyTest`: contrato público das
  políticas, limites e erros.
- `LendingPolicySelectorTest`: tabela de verdade da seleção e ordem das regras.
- `LendingPoliciesPropertiesTest`: validação da configuração no arranque.
- `LendingTest`: renovação, versão obsoleta, atraso e pontualidade.
- `LendingServiceImplTest`: Standard por defeito, Adaptive após histórico,
  reavaliação na renovação e snapshot da política.

### Ficheiros principais

- `lendingmanagement/model/policy/` — domínio puro das políticas e da seleção.
- `lendingmanagement/configuration/` — propriedades e beans.
- `lendingmanagement/services/LendingPolicyService` — histórico e decisão.
- `lendingmanagement/model/Lending` — renovação e snapshot da política aplicada.
- `lendingmanagement/api/LendingController` — endpoints de renovação e política.

### Diagramas

- [Classes das políticas](Docs/P1/classes-politicas.puml)
- [Sequência de criação de empréstimo](Docs/P1/sequencia-emprestimo-politica.puml)
- [Sequência de renovação](Docs/P1/sequencia-renovacao.puml)

## Estrutura

```
src/main/java/pt/psoft/g1/psoftg1/
├── authormanagement
├── bookmanagement
├── genremanagement
├── lendingmanagement
├── readermanagement
├── usermanagement
├── shared
├── external
├── configuration
└── bootstrapping
```

## Fases previstas

1. **Concluída** — Políticas de empréstimo Standard e Adaptive, com configuração
   externa e renovação.
2. Integração com Google Books e Open Library através de portas e adaptadores.
3. Notificações por email, SMS e webhook, configuráveis e extensíveis.
4. Testes funcionais de caixa opaca, caixa transparente, mutação, componentes e sistema.
5. SAD/ADD, vistas, ADRs, matriz de rastreabilidade e demonstração.
