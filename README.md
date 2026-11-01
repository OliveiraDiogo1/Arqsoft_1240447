# projeto_arqsoft_1240447

Projeto 1 da unidade curricular de Arquitetura de Software (ARQSOFT), 2026-2027.

O ponto de partida é o serviço REST de gestão de biblioteca desenvolvido em PSOFT
(`psoft-project-2024-g1`). Este repositório é independente e pessoal; o histórico
do projeto original não é reutilizado.

## Estado atual

- Base importada e a compilar.
- Fase 1 (políticas de empréstimo) implementada.
- Fase 2 (fontes bibliográficas e notificações) implementada.
- Fase 3 (mutação, componentes e sistema) implementada.
- Fase 4 (SAD/ADD, vistas, ADRs, rastreabilidade e demonstração) documentada.
- `mvn clean test` executa 186 testes, todos verdes.
- PIT: 133 mutantes, 104 mortos, 78% mutation score e 89% test strength.

## Como validar

```bash
mvn clean test
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

## Fase 2 — Fontes bibliográficas e notificações

Estado: implementada. A consulta bibliográfica e os canais de notificação estão
integrados no serviço, com fontes e canais selecionáveis por configuração.

### Objetivo

Cumprir os pontos 1.3.3.1 e 1.3.3.2 do enunciado:

- obter informação bibliográfica de livros a partir de sistemas externos;
- escolher, no arranque, as fontes usadas, podendo usar mais do que uma;
- suportar fontes novas com impacto limitado no código existente;
- notificar leitores de eventos de empréstimo por email, SMS e webhook,
  individualmente ou em combinação;
- escolher, no arranque, os canais usados;
- suportar canais novos com impacto limitado no código existente.

Esta fase não cria nem atualiza livros no LMS. A recolha bibliográfica é apenas
de consulta. Também não inclui notificações agendadas de atraso, persistência de
notificações, testes de mutação nem testes de sistema; esses pontos ficam para
fases posteriores.

### Bibliográfico

Novo módulo `bibliographicmanagement`:

- `model/BibliographicRecord` — registo normalizado com ISBN, título, autores,
  descrição, editora, data de publicação, número de páginas, categorias, capa e
  fontes que contribuíram.
- `model/BibliographicSource` — porta com `name()` e `findByIsbn(String)`.
- `infrastructure/GoogleBooksBibliographicSource` — adaptador para
  `https://www.googleapis.com/books/v1/volumes?q=isbn:{isbn}`.
- `infrastructure/OpenLibraryBibliographicSource` — adaptador para
  `https://openlibrary.org/api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data`.
- `services/BibliographicSourceRegistry` — valida os nomes configurados e
  mantém a ordem definida.
- `services/BibliographicServiceImpl` — consulta as fontes ativas, junta os
  resultados e isola falhas parciais.
- `api/BibliographicController` — expõe a consulta.
- `api/BibliographicRecordView` — resposta HTTP.

Endpoint:

```
GET /api/books/{isbn}/bibliographic
```

Acesso: leitor ou bibliotecário. O livro não precisa de existir no LMS; a
consulta é externa. O ISBN é normalizado e validado com `Isbn`.

Regras de composição:

- o título, a descrição, a editora, a data, as páginas e a capa vêm da primeira
  fonte, pela ordem configurada, que tenha o campo preenchido;
- os autores e as categorias são unidos sem duplicados;
- a resposta indica as fontes que contribuíram e as que falharam;
- se nenhuma fonte tiver dados, a resposta é `404`;
- se todas as fontes falharem por erro externo, a resposta é `502`;
- se uma fonte falhar e outra responder, a resposta é `200` com aviso.

Configuração:

```properties
bibliographic.sources=google-books,open-library
bibliographic.google-books.base-url=https://www.googleapis.com/books/v1
bibliographic.google-books.api-key=${GOOGLE_BOOKS_API_KEY:}
bibliographic.open-library.base-url=https://openlibrary.org
bibliographic.http.connect-timeout-ms=2000
bibliographic.http.read-timeout-ms=5000
```

As fontes são lidas no arranque. Um nome desconhecido impede o arranque. Uma
fonte nova é uma nova implementação de `BibliographicSource` registada como
bean; o serviço e o controller não mudam.

### Notificações

Novo módulo `notificationmanagement`:

- `model/LendingNotificationEvent` — tipo, número de empréstimo, leitor, livro,
  datas, contactos e consentimento de partilha com terceiros.
- `model/NotificationSender` — porta com `channel()` e `send(event)`.
- `infrastructure/EmailNotificationSender` — `JavaMailSender`.
- `infrastructure/SmsNotificationSender` — POST HTTP para um gateway SMS
  configurável.
- `infrastructure/WebhookNotificationSender` — POST JSON para um URL
  configurável.
- `infrastructure/LoggingNotificationSender` — canal `log`, usado na demonstração
  local sem credenciais externas.
- `services/NotificationServiceImpl` — envia para os canais ativos e isola
  falhas.
- `services/LendingNotificationFactory` — converte `Lending` no evento.

Eventos:

- `LENDING_CREATED`;
- `LENDING_RENEWED`;
- `LENDING_RETURNED`.

O `LendingServiceImpl` agenda a publicação do evento para depois do commit da
operação. A notificação é best-effort: uma falha externa é registada e não reverte
a criação, renovação ou devolução.

Regras de envio:

- email para o username do leitor, que é o email no modelo existente;
- SMS para o telefone do leitor;
- webhook apenas se o leitor tiver `thirdPartySharingConsent`; o payload não
  inclui email nem telefone, apenas número de leitor, empréstimo, ISBN, título e
  datas;
- canais sem contacto válido são ignorados;
- um canal que falhe não impede os restantes.

Configuração:

```properties
notifications.channels=log
notifications.email.from=no-reply@example.invalid
notifications.sms.url=
notifications.sms.api-key=
notifications.sms.sender=
notifications.webhook.url=
notifications.webhook.secret=
```

O canal `email` usa a configuração `spring.mail.*`. O SMS faz `POST` para
`notifications.sms.url` com `{"to": "...", "sender": "...", "message": "..."}` e,
se existir, `Authorization: Bearer <api-key>`. O webhook faz `POST` para
`notifications.webhook.url` com o evento em JSON e, se existir,
`X-Notification-Secret`. Se um canal obrigatório for selecionado sem a
configuração necessária, o arranque falha com uma mensagem clara. Um canal novo é
uma nova implementação de `NotificationSender`, registada como bean; o serviço de
empréstimos não muda.

### Integração e segurança

- `LendingServiceImpl` passa a receber `NotificationService` e notifica depois do
  commit de `create`, `renew` e `setReturned`.
- `SecurityConfig` autoriza `GET /api/books/*/bibliographic` para leitor e
  bibliotecário.
- Não há alterações ao esquema da base de dados.
- As chamadas externas têm timeouts e não bloqueiam a operação por tempo
  indeterminado.
- A chave Api Ninjas é lida da variável de ambiente `NINJAS_KEY`.
- A chave privada JWT em `src/main/resources/rsa.private.key` destina-se apenas à
  demonstração local e mantém-se porque a autenticação local depende dela. Não deve
  ser usada em produção; a decisão de rotação fica a cargo do responsável pelo projeto.

### Testes

- `GoogleBooksBibliographicSourceTest` e `OpenLibraryBibliographicSourceTest`:
  mapeamento, resposta vazia e erro HTTP.
- `BibliographicServiceImplTest`: ordem das fontes, composição, falha parcial,
  falha total e ISBN inválido.
- `BibliographicControllerTest`: `200`, `404`, `502` e resposta normalizada.
- `NotificationServiceImplTest`: canais ativos, contactos em falta, consentimento
  do webhook e isolamento de falhas.
- `LendingNotificationFactoryTest`: criação, renovação e devolução.
- `EmailNotificationSenderTest`, `SmsNotificationSenderTest` e
  `WebhookNotificationSenderTest`: conteúdo e configuração.
- `LendingNotificationAfterCommitTest`: confirma as três notificações depois do
  commit, sem transação de teste.
- `NotificationSenderRegistryTest` e `BibliographicSourceRegistryTest`: rejeição de
  canais e fontes duplicados na configuração.

Validação integral: `mvn clean test` — 179 testes, 0 falhas, 0 erros e 0 ignorados.
Os testes dos adaptadores HTTP usam `MockRestServiceServer`, sem chamadas às APIs
externas reais.

### Critérios de aceitação

- É possível escolher uma ou várias fontes por configuração.
- Uma fonte nova não obriga a alterar `BibliographicServiceImpl`.
- O endpoint devolve dados normalizados e distingue ausência de dados de falha
  externa.
- É possível escolher um ou vários canais por configuração.
- Um canal novo não obriga a alterar `LendingServiceImpl`.
- Criar, renovar e devolver um empréstimo gera os eventos esperados.
- Uma falha de notificação não desfaz a operação de empréstimo.
- Os testes novos e os anteriores passam: 179 testes no total.

### Limitações assumidas

- Não há credenciais reais de SMTP, SMS ou webhook; o canal predefinido é `log`.
- O email usa o username do leitor; o modelo atual não tem um campo de email
  separado.
- As fontes externas podem mudar o formato ou impor quotas.
- Não há persistência de notificações nem reenvio automático.
- A notificação de atraso fica para a fase seguinte.

### Diagramas

- [Classes da Fase 2](Docs/P1/classes-fase2.puml)
- [Sequência de consulta bibliográfica](Docs/P1/sequencia-bibliografico.puml)
- [Sequência de notificação](Docs/P1/sequencia-notificacao.puml)

## Fase 3 — Mutação, componentes e sistema

A Fase 3 acrescenta os níveis C3.3–C3.5 do enunciado.

### C3.3 — Mutação

O `pitest-maven` foi configurado para as classes desenvolvidas:

- políticas de empréstimo (`model.policy.*`);
- `Lending`;
- modelo e serviços bibliográficos;
- modelo, fábrica e registo de notificações.

Resultados:

| Indicador | Valor |
|---|---:|
| Mutantes gerados | 133 |
| Mutantes mortos | 104 |
| Mutation score | 78% |
| Test strength | 89% |
| Cobertura das classes mutadas | 88% |
| Gate de mutação | 75% |
| Gate de cobertura PIT | 80% |

### C3.4 — Componentes

- `BibliographicComponentTest`: controller + service + registry + adaptadores
  Google Books/Open Library com `MockRestServiceServer`.
- `LendingComponentTest`: controller + service + domínio + repositórios, com
  `NotificationService` substituído.

### C3.5 — Sistema

`ArqsoftSystemTest` usa `@SpringBootTest(webEnvironment = RANDOM_PORT)` e
`TestRestTemplate`:

- login com sucesso e falha;
- acesso sem token → `401`;
- consulta de livro autenticada;
- endpoint de política efetiva para o leitor de demonstração.

### Evidências

```
evidence/phase3/mvn-test.txt
evidence/phase3/pit.txt
```

A suite total passou de 179 para **186 testes**.

## Fase 4 — SAD/ADD, rastreabilidade e demonstração

### 1. System-as-is

O ponto de partida é o LMS REST desenvolvido em PSOFT. A aplicação é um
monólito modular Spring Boot 3.2.5, Java 17, com camadas de API, serviços,
domínio, repositórios e configuração:

- API REST para livros, géneros, autores, leitores e empréstimos;
- persistência JPA/H2;
- autenticação e autorização com JWT;
- integração externa com Api Ninjas para a citação de leitores;
- módulos `authormanagement`, `bookmanagement`, `genremanagement`,
  `lendingmanagement`, `readermanagement`, `usermanagement`, `shared`,
  `external`, `configuration` e `bootstrapping`.

Limitações do as-is:

- regras de empréstimo não configuráveis e sem políticas alternativas;
- inexistência de consulta bibliográfica externa;
- inexistência de notificações de empréstimo;
- forte acoplamento a detalhes da base e a APIs externas concretas;
- cobertura de testes limitada e sem testes de mutação, componente ou sistema;
- configuração e credenciais parcialmente embebidas no projeto.

### 2. Requisitos e fatores arquiteturais

O enunciado identifica três problemas: extensibilidade, configurabilidade e
testabilidade. A especificação de políticas acrescenta regras quantitativas e
seleção em runtime. A Fase 2 acrescenta fontes bibliográficas e notificações.

| ID | Requisito / ASR | Atributo |
|---|---|---|
| ASR-01 | Políticas Standard e Adaptive com parâmetros externos | Configurabilidade |
| ASR-02 | Selecionar a política aplicável em runtime | Extensibilidade |
| ASR-03 | Acrescentar políticas com impacto limitado | Extensibilidade |
| ASR-04 | Renovação coerente com a política aplicada | Correção |
| ASR-05 | Evitar excesso de empréstimos em concorrência | Fiabilidade |
| ASR-06 | Consultar Google Books e Open Library | Interoperabilidade |
| ASR-07 | Escolher uma ou várias fontes no arranque | Configurabilidade |
| ASR-08 | Acrescentar fontes com impacto limitado | Extensibilidade |
| ASR-09 | Notificar por email, SMS e webhook | Interoperabilidade |
| ASR-10 | Escolher um ou vários canais no arranque | Configurabilidade |
| ASR-11 | Isolar falhas de fontes e canais | Fiabilidade |
| ASR-12 | Testar classes, domínio, mutação, componentes e sistema | Testabilidade |
| ASR-13 | Não expor credenciais em logs | Segurança |
| ASR-14 | Rastrear requisitos, decisões, vistas, código e testes | Consistência |

### 3. Processo ADD

1. **Rever entradas**: enunciado, especificação de políticas, código base,
   testes existentes e restrições de prazo.
2. **Identificar e classificar requisitos**: ASR-01 a ASR-14.
3. **Escolher os elementos a projetar**: políticas de empréstimo, consulta
   bibliográfica, notificações e testes.
4. **Escolher táticas e padrões**: encapsulamento, configuração, injeção de
   dependências, portas/adaptadores, seleção por regras, transação e isolamento
   de falhas.
5. **Definir interfaces**: `LendingPolicy`, `BibliographicSource`,
   `NotificationSender`, `LendingPolicyService`, `NotificationService`.
6. **Verificar**: testes unitários, PIT, testes de componente e sistema.
7. **Documentar**: ADRs, vistas, matriz de rastreabilidade e guião de
   demonstração.

### 4. Táticas, padrões e alternativas

| Tática / padrão | Onde | Objetivo |
|---|---|---|
| Strategy | `LendingPolicy` + implementações | Encapsular políticas |
| Registry + regras ordenadas | `LendingPolicyRegistry`, `LendingPolicySelector` | Seleção extensível |
| Configuração validada | `LendingPoliciesProperties` | Falhar no arranque |
| Repository | `BookRepository`, `LendingRepository`, etc. | Isolar persistência |
| Ports & Adapters | `BibliographicSource`, `NotificationSender` | Isolar integrações |
| Composite | `BibliographicServiceImpl` | Combinar fontes |
| Transaction synchronization | `LendingServiceImpl` | Notificar após commit |
| Fail-fast | propriedades e registries | Detetar configuração inválida |
| Test doubles e PIT | testes | Verificar comportamento |

Alternativas consideradas:

- **Microserviços**: rejeitados; aumentariam deployment, latência e testes sem
  resolver os drivers num prazo curto.
- **`if/else` centralizado**: rejeitado; não permitia configuração nem extensão
  sem alterar o serviço.
- **Chamadas diretas às APIs no controller**: rejeitadas; misturavam HTTP,
  domínio e apresentação.
- **Notificação dentro da transação**: rejeitada; podia anunciar operações que
  depois sofriam rollback.
- **Fila/retry persistente**: adiada; não é necessária para o âmbito e aumentaria
  a infraestrutura.

### 5. Vistas e modelos

- [Contexto](Docs/P1/visao-contexto.puml)
- [Componentes](Docs/P1/visao-componentes.puml)
- [Implantação](Docs/P1/visao-implantacao.puml)
- [Dados](Docs/P1/visao-dados.puml)
- [Classes das políticas](Docs/P1/classes-politicas.puml)
- [Classes da Fase 2](Docs/P1/classes-fase2.puml)
- [Sequência de criação de empréstimo](Docs/P1/sequencia-emprestimo-politica.puml)
- [Sequência de renovação](Docs/P1/sequencia-renovacao.puml)
- [Sequência bibliográfica](Docs/P1/sequencia-bibliografico.puml)
- [Sequência de notificação](Docs/P1/sequencia-notificacao.puml)

### 6. ADRs

| ADR | Decisão | Estado |
|---|---|---|
| ADR-01 | Manter monólito modular com separação por camadas e módulos | Aceite |
| ADR-02 | Representar políticas com Strategy, registry e regras de seleção | Aceite |
| ADR-03 | Ler e validar parâmetros no arranque, sem constantes de negócio | Aceite |
| ADR-04 | Reavaliar a política em cada criação/renovação | Aceite |
| ADR-05 | Usar lock pessimista no leitor e `@Version`/`If-Match` no empréstimo | Aceite |
| ADR-06 | Isolar fontes bibliográficas com portas/adaptadores e composição | Aceite |
| ADR-07 | Recalcular o período Adaptive na renovação e usar `adaptive.max-renewals` | Aceite, a confirmar com o docente |
| ADR-08 | Notificar após commit, best-effort, com canais selecionáveis | Aceite |
| ADR-09 | Sanitizar erros externos e externalizar credenciais | Aceite |
| ADR-10 | Complementar testes unitários com PIT, componentes e sistema | Aceite |

### 7. Matriz de rastreabilidade

| Requisito | Decisão / mecanismo | Componentes | Testes / evidência |
|---|---|---|---|
| ASR-01 | ADR-03 | `LendingPoliciesProperties`, `application.properties`, policies | `LendingPoliciesPropertiesTest`, `StandardLendingPolicyTest`, `AdaptiveLendingPolicyTest` |
| ASR-02 | ADR-02, ADR-04 | `LendingPolicySelector`, `LendingServiceImpl` | `LendingPolicySelectorTest`, `LendingServiceImplTest` |
| ASR-03 | ADR-02 | `LendingPolicy`, `LendingPolicyRegistry` | testes das políticas e do registry |
| ASR-04 | ADR-07 | `Lending.renew`, `LendingPolicyService` | `LendingTest`, `LendingServiceImplTest` |
| ASR-05 | ADR-05 | `ReaderRepository`, `LendingServiceImpl` | `LendingRepositoryIntegrationTest`, testes de serviço |
| ASR-06 | ADR-06 | `GoogleBooksBibliographicSource`, `OpenLibraryBibliographicSource` | `GoogleBooksBibliographicSourceTest`, `OpenLibraryBibliographicSourceTest` |
| ASR-07 | ADR-06 | `BibliographicSourceRegistry`, `BibliographicServiceImpl` | `BibliographicSourceRegistryTest`, `BibliographicServiceImplTest` |
| ASR-08 | ADR-06 | `BibliographicSource`, adaptadores, registry | `BibliographicComponentTest` |
| ASR-09 | ADR-08 | `EmailNotificationSender`, `SmsNotificationSender`, `WebhookNotificationSender` | testes dos três senders |
| ASR-10 | ADR-08 | `NotificationSenderRegistry`, `NotificationServiceImpl` | `NotificationSenderRegistryTest`, `NotificationServiceImplTest` |
| ASR-11 | ADR-08 | `LendingServiceImpl`, `NotificationServiceImpl` | `LendingNotificationAfterCommitTest`, testes do serviço |
| ASR-12 | ADR-10 | suite de testes, PIT | 186 testes, 78% mutation score, 89% test strength |
| ASR-13 | ADR-09 | `GlobalExceptionHandler`, `NotificationServiceImpl` | testes de sanitização e logs |
| ASR-14 | ADR-01–ADR-10 | README, ADRs, vistas, matriz | esta secção + `evidence/phase3/` |

### 8. Avaliação e riscos

Pontos fortes:

- políticas e integrações isoladas por interfaces;
- configuração validada no arranque;
- comportamento de concorrência explícito;
- notificações fora da transação e best-effort;
- 186 testes, PIT com 78% e testes de componente/sistema;
- rastreabilidade entre requisito, decisão, componente e teste.

Riscos e limitações:

- Google Books/Open Library e canais reais não foram exercitados com credenciais
  de produção; os testes usam HTTP simulado/doubles;
- as notificações não são persistidas nem têm retry;
- não há notificação agendada de atrasos;
- a chave JWT de demonstração continua no repositório;
- a chave Api Ninjas antiga deve ser revogada se estiver ativa;
- PIT cobre apenas as classes selecionadas, não todo o sistema;
- a interpretação da renovação Adaptive deve ser confirmada com o docente.

### 9. Guião de demonstração

```bash
git switch fase-3
mvn clean test
mvn org.pitest:pitest-maven:mutationCoverage
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:h2:mem:projeto-arqsoft"
```

Demonstração:

1. Login como bibliotecário/leitor e mostrar o token JWT.
2. Mostrar `GET /api/lendings/policies/{year}/7` (Adaptive) e `{year}/8`
   (Standard por rácio).
3. Criar um empréstimo e mostrar a política/período aplicados.
4. Renovar e mostrar `renewalCount`, `appliedPolicy` e `appliedPeriodDays`.
5. Mostrar `GET /api/books/{isbn}/bibliographic` e explicar as fontes/merge.
6. Criar/renovar/devolver e mostrar o log de notificação; explicar que email,
   SMS e webhook estão configuráveis e que falhas não revertem o empréstimo.
7. Mostrar os testes, PIT, ADRs, vistas e matriz de rastreabilidade.

A resposta a `GET /api/books/{isbn}/bibliographic` depende de acesso externo; se
a rede não estiver disponível, demonstrar com os testes que usam
`MockRestServiceServer`.

## Estrutura

```
src/main/java/pt/psoft/g1/psoftg1/
├── authormanagement
├── bookmanagement
├── genremanagement
├── lendingmanagement
├── bibliographicmanagement
├── notificationmanagement
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
2. **Concluída** — Fontes bibliográficas Google Books/Open Library e notificações
   por email, SMS, webhook e log.
3. **Concluída** — Testes de mutação, componentes e sistema.
4. **Concluída** — SAD/ADD, vistas, ADRs, matriz de rastreabilidade e demonstração.
