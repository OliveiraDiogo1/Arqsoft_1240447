# P1 ARQSOFT (2026/2027)

## Contexto

Este repositório possui o Projeto 1 de Arquitetura de Software. O ponto de
partida é o serviço REST de gestão de biblioteca usado em PSOFT
(psoft-project-2024-g1). O objetivo é resolver os problemas de extensibilidade,
configurabilidade e testabilidade sem substituir o LMS.

O repositório é independente do projeto original e do ODSOFT. O histórico do
PSOFT não foi reutilizado. O estado final está na branch **main**, no commit
*#e8bc46b*. As branches *fase-1, fase-2 e fase-3* também estão publicadas no
GitHub.

Repositório remoto:

<https://github.com/OliveiraDiogo1/Arqsoft_1240447>

A solução mantém o monólito modular existente e introduz pontos de extensão nas
políticas de empréstimo, na consulta de fontes bibliográficas e nas notificações.

## Ponto de partida

O LMS é uma aplicação Spring Boot 3.2.5 com Java 17, API REST, persistência
JPA/H2, autenticação JWT e uma integração com Api Ninjas para a citação de
leitores. A organização inicial separava controllers, serviços, domínio,
repositórios e configuração, mas não tinha:

- Políticas de empréstimo configuráveis;
- Consulta de fontes bibliográficas externas;
- Notificações associadas aos empréstimos;
- Testes de mutação, de componente e de sistema;
- Uma matriz que ligasse requisitos, decisões, código e testes.

A suite tinha 138 testes no fim da Fase 1, 179 no fim da Fase 2 e 186 no fim da
Fase 3. Os valores vêm de execuções locais de *mvn clean test*, o projeto não
teve builds de CI.

## Políticas de empréstimo

A especificação complementar define duas políticas. A Standard usa um limite de
empréstimos ativos, um prazo inicial, um número máximo de renovações e um prazo
de renovação. A Adaptive escolhe o prazo a partir do número de empréstimos ativos
do leitor: usa **LongPeriod** abaixo do primeiro limiar,**`StandardPeriod** entre os
dois limiares e **ShortPeriod** a partir do segundo.

A seleção é feita em cada criação ou renovação. A Standard é o valor por omissão.
A Adaptive aplica-se quando, nos últimos **HistoryPeriod** meses, o leitor tem pelo
menos **MinCompletedLendings** empréstimos devolvidos, uma taxa de pontualidade
igual ou superior a **MinOnTimeRatio** e nenhum empréstimo em atraso. Um leitor com
qualquer empréstimo em atraso não pode criar nem renovar.

Os parâmetros são lidos no arranque e validados. Uma configuração inválida
impede a aplicação de arrancar.

| Propriedade | Valor por omissão |
| --- | ---: |
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

O bootstrap cria dois históricos de demonstração. O leitor `{ano}/7`
qualifica-se para a Adaptive. O leitor `{ano}/8` tem três devoluções, uma delas
fora do prazo e mantém-se na Standard com uma taxa de 66,7%.

A renovação usa **If-Match** e a versão da entidade. Na criação e na renovação, o
leitor é bloqueado com lock pessimista. Sem este bloqueio, dois pedidos podiam
observar o mesmo número de empréstimos ativos e ultrapassar o limite configurado.

### Renovação na política Adaptive

A especificação define **MaxRenewals** e **RenewalPeriod** para a Standard. Na
Adaptive, a renovação reavalia a política; se o leitor continuar elegível,
recalcula o período com os empréstimos ativos e usa **adaptive.max-renewals**.
Usar sempre **RenewalPeriod** ignoraria o estado que a própria política Adaptive usa para calcular o prazo.

### Endpoints

| Método | Rota | Acesso | Função |
| --- | --- | --- | --- |
| PATCH | `/api/lendings/{year}/{seq}/renew` | leitor ou bibliotecário | renovar com **If-Match** e reavaliar a política |
| GET | `/api/lendings/policies/{year}/{seq}` | leitor com o próprio número ou bibliotecário | consultar política efetiva, atrasos, histórico e período aplicável |

## Consulta bibliográfica

O módulo **bibliographicmanagement** consulta informação de livros em sistemas
externos. A consulta é apenas de leitura: não cria nem altera livros no LMS.

As fontes são escolhidas por configuração e podem ser usadas em conjunto:

```
bibliographic.sources=google-books,open-library
bibliographic.google-books.base-url=https://www.googleapis.com/books/v1
bibliographic.google-books.api-key=${GOOGLE_BOOKS_API_KEY:}
bibliographic.open-library.base-url=https://openlibrary.org
bibliographic.http.connect-timeout-ms=2000
bibliographic.http.read-timeout-ms=5000
```

O **BibliographicSourceRegistry** valida os nomes e a ordem das fontes. Cada
adaptador devolve um **BibliographicRecord** normalizado. O
**BibliographicServiceImpl** reúne os resultados, escolhe o primeiro valor
disponível para os campos escalares e junta autores e categorias sem duplicados.
A resposta indica as fontes que responderam e as que falharam.

O endpoint é:

```
GET /api/books/{isbn}/bibliographic
```

O ISBN é normalizado e validado com a classe *Isbn*. Se nenhuma fonte tiver
dados, a resposta é **404**.

Se todas falharem por erro externo, é **502**.

Se uma falhar e outra responder, é **200** com a indicação da falha. O livro não precisa de existir no LMS.

As fontes são beans que implementam **BibliographicSource**. Uma fonte nova é uma
nova implementação registada no contexto; o serviço e o controller não precisam
de ser alterados.

## Notificações

O módulo  *notificationmanagement* envia notificações de criação, renovação e
devolução de empréstimos. Os canais disponíveis são email, SMS, webhook e um
canal *log*, usado na demonstração local.

A escolha é feita no arranque:

```
notifications.channels=log
notifications.email.from=no-reply@example.invalid
notifications.sms.url=
notifications.sms.api-key=
notifications.sms.sender=
notifications.webhook.url=
notifications.webhook.secret=
```

O email usa o username do leitor, que é o email no modelo existente. O SMS faz
um `POST` para o URL configurado com *to*, *sender* e *message*. O webhook envia
o evento em JSON e pode usar o header **X-Notification-Secret**. O payload do
webhook não inclui email nem telefone.

As notificações são enviadas depois do commit da operação. O **LendingServiceImpl**
regista uma sincronização de transação e só chama o serviço de notificações em
afterCommit. Assim, uma operação que sofra rollback não chega a anunciar-se. O
envio é best-effort: uma falha num canal é registada e não impede os outros nem
desfaz o empréstimo.

O webhook só é enviado quando o leitor tem *thirdPartySharingConsent*. Canais sem
contacto válido são ignorados. Um canal novo é uma implementação de
*NotificationSender* registada como bean; o serviço de empréstimos não muda.

## Arquitetura e decisões

### Estilo e processo

A solução mantém um monólito modular com separação entre API, aplicação, domínio,
persistência e integrações. O ADD seguiu sete passos: rever entradas, identificar
requisitos e fatores arquiteturais, escolher elementos, escolher táticas, definir
interfaces, verificar com testes e documentar.

Os fatores arquiteturais considerados foram:

| ID | Fator | Atributo |
| --- | --- | --- |
| ASR-01 | Políticas com parâmetros externos | Configurabilidade |
| ASR-02 | Seleção da política em runtime | Extensibilidade |
| ASR-03 | Novas políticas sem alterar o serviço | Extensibilidade |
| ASR-04 | Renovação coerente com a política | Correção |
| ASR-05 | Limite de empréstimos em concorrência | Fiabilidade |
| ASR-06 | Google Books e Open Library | Interoperabilidade |
| ASR-07 | Fontes configuráveis e combináveis | Configurabilidade |
| ASR-08 | Acrescentar fontes com impacto limitado | Extensibilidade |
| ASR-09 | Notificar por email, SMS e webhook | Interoperabilidade |
| ASR-10 | Escolher um ou vários canais no arranque | Configurabilidade |
| ASR-11 | Isolar falhas de fontes e canais e não notificar transações revertidas | Fiabilidade |
| ASR-12 | Testar classes, domínio, mutação, componentes e sistema | Testabilidade |
| ASR-13 | Não expor credenciais em logs | Segurança |
| ASR-14 | Rastrear requisitos, decisões, vistas, código e testes | Consistência |

As táticas principais foram Strategy, Registry, Ports & Adapters, Repository,
Composite, configuração validada, isolamento de falhas e sincronização
transacional. Um monólito modular evita o custo de operação e de testes de uma
solução distribuída, que o problema não exigia.

Microserviços, um *if/else* centralizado e chamadas externas no controller não
resolviam os drivers. Notificações dentro da transação podiam anunciar operações
que sofressem rollback, e uma fila com retry persistente acrescentaria
infraestrutura sem necessidade para o âmbito.

### Vistas

As vistas estão em `Docs/P1/`:

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

### Decisões registadas

| ADR | Decisão | Estado |
| --- | --- | --- |
| ADR-01 | Manter monólito modular com separação por camadas e módulos | Aceite |
| ADR-02 | Representar políticas com Strategy, registry e regras de seleção | Aceite |
| ADR-03 | Ler e validar parâmetros no arranque | Aceite |
| ADR-04 | Reavaliar a política em cada criação ou renovação | Aceite |
| ADR-05 | Usar lock pessimista no leitor e **@Version/If-Match** no empréstimo | Aceite |
| ADR-06 | Isolar fontes bibliográficas com portas/adaptadores e composição | Aceite |
| ADR-07 | Recalcular o período Adaptive na renovação e usar *adaptive.max-renewals* | Aceite; confirmar com o docente |
| ADR-08 | Notificar após commit, best-effort e com canais selecionáveis | Aceite |
| ADR-09 | Sanitizar erros externos e externalizar credenciais | Aceite |
| ADR-10 | Complementar testes unitários com PIT, componentes e sistema | Aceite |

## Testes

A suite cobre os cinco níveis pedidos pelo enunciado:

| Nível | Implementação |
| --- | --- |
| C3.1: caixa opaca em classes | testes dos contratos das políticas, modelos e adaptadores |
| C3.2: caixa transparente no domínio | limites, ramos, atrasos, renovação e erros |
| C3.3: mutação | PIT sobre políticas, *Lending*, bibliográfico e notificações |
| C3.4: componentes | *BibliographicComponentTest* e *LendingComponentTest* |
| C3.5: sistema | *ArqsoftSystemTest* com servidor HTTP real |

O *BibliographicComponentTest* liga o controller ao serviço, ao registry e aos
adaptadores Google Books/Open Library, com respostas HTTP simuladas. O
*LendingComponentTest* liga o controller ao serviço, ao domínio e aos
repositórios, substituindo o serviço de notificações. O *ArqsoftSystemTest*
cobre login com sucesso e falha, acesso sem token, consulta de livro e consulta
da política efetiva.

## Resultados e análise

Resultados da última execução local:

| Indicador | Valor |
| --- | ---: |
| Testes | 186 |
| Falhas | 0 |
| Mutantes PIT | 133 |
| Mutantes mortos | 104 |
| Mutation score | 78% |
| Test strength | 89% |
| Cobertura PIT das classes mutadas | 88% |
| Gates PIT | 75% mutação / 80% cobertura |

A cobertura de 88% refere-se apenas às classes incluídas no PIT. Os testes dos
adaptadores usam *MockRestServiceServer*. Não foram feitas chamadas reais a
Google Books ou Open Library.

As evidências estão em:

```
evidence/phase3/mvn-test.txt
evidence/phase3/pit.txt
evidence/phase3/pit/mutations.xml
evidence/phase3/pit/index.html
```

## Rastreabilidade

| Requisito | Decisão ou mecanismo | Componentes | Testes ou evidência |
| --- | --- | --- | --- |
| ASR-01 | ADR-03 | `LendingPoliciesProperties`, `application.properties`, políticas | `LendingPoliciesPropertiesTest`, `StandardLendingPolicyTest`, `AdaptiveLendingPolicyTest` |
| ASR-02 | ADR-02, ADR-04 | `LendingPolicySelector`, `LendingServiceImpl` | `LendingPolicySelectorTest`, `LendingServiceImplTest` |
| ASR-03 | ADR-02 | `LendingPolicy`, `LendingPolicyRegistry` | testes das políticas e do registry |
| ASR-04 | ADR-07 | `Lending.renew`, `LendingPolicyService` | `LendingTest`, `LendingServiceImplTest` |
| ASR-05 | ADR-05 | `ReaderRepository`, `LendingServiceImpl` | `LendingRepositoryIntegrationTest`, testes de serviço |
| ASR-06 | ADR-06 | `GoogleBooksBibliographicSource`, `OpenLibraryBibliographicSource` | `GoogleBooksBibliographicSourceTest`, `OpenLibraryBibliographicSourceTest` |
| ASR-07 | ADR-06 | `BibliographicSourceRegistry`, `BibliographicServiceImpl` | `BibliographicSourceRegistryTest`, `BibliographicServiceImplTest` |
| ASR-08 | ADR-06 | `BibliographicSource`, adaptadores, registry | `BibliographicComponentTest` |
| ASR-09 | ADR-08 | `EmailNotificationSender`, `SmsNotificationSender`, `WebhookNotificationSender` | testes dos três senders |
| ASR-10 | ADR-08 | `NotificationSenderRegistry`, `NotificationServiceImpl` | `NotificationSenderRegistryTest`, `NotificationServiceImplTest` |
| ASR-11 | ADR-08 | `BibliographicServiceImpl`, `NotificationServiceImpl`, `LendingServiceImpl` | `BibliographicServiceImplTest`, `NotificationServiceImplTest`, `LendingNotificationAfterCommitTest` |
| ASR-12 | ADR-10 | suite de testes e PIT | 186 testes, 78% de mutação e 89% de test strength |
| ASR-13 | ADR-09 | `GlobalExceptionHandler`, `NotificationServiceImpl` | testes de sanitização e logs |
| ASR-14 | ADR-01 a ADR-10 | README, ADRs, vistas e matriz | esta secção e `evidence/phase3/` |

## Limitações

- As fontes Google Books e Open Library e os canais email, SMS e webhook não
  foram exercitados com credenciais de produção. Os testes usam respostas
  simuladas e doubles.
- As notificações não são persistidas e não têm retry. Também não existe
  notificação agendada de atrasos.
- A chave JWT em src/main/resources/rsa.private.key é uma chave de demonstração
  local. Não deve ser usada em produção e deve ser rodada pelo responsável.
- A chave Api Ninjas anteriormente commitada deve ser revogada se estiver ativa.
- O PIT cobre apenas as classes selecionadas. Os mutantes sem cobertura e os
  sobreviventes não foram classificados como equivalentes sem análise.
- A interpretação da renovação Adaptive deve ser confirmada com o docente.
- O projeto não tem um pipeline Jenkins; os resultados de testes e mutação são
  locais.

## Reprodução e demonstração

Validação:

```
cd "C:\Users\Diogo Oliveira\ISEP\ARQSOFT\projeto_arqsoft_1240447"
git switch main
mvn clean test
mvn org.pitest:pitest-maven:mutationCoverage
```

Arranque local sem o servidor H2 TCP do base:

```
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:h2:mem:projeto-arqsoft"
```

A aplicação fica em <http://localhost:8080>. O perfil *bootstrap* cria os dados
de demonstração.

Para a demonstração, autenticar como bibliotecário (<maria@gmail.com> /
Mariaroberta!123) e obter o token do header **Authorization**. Depois:

1. consultar */api/lendings/policies/{ano}/7* e *{ano}/8*;
2. criar um empréstimo para *{ano}/7* e observar a política e o período;
3. renovar e observar *renewalCount*, *appliedPolicy* e *appliedPeriodDays*;
4. consultar */api/books/{isbn}/bibliographic* e explicar as fontes e o merge;
5. mostrar o log de notificação após criação, renovação ou devolução;
6. percorrer as vistas, os ADRs e a matriz de rastreabilidade.

O endpoint bibliográfico depende de acesso externo. Se a rede não estiver
disponível, a demonstração pode usar os testes com *MockRestServiceServer*.

## Estrutura do Projeto

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
