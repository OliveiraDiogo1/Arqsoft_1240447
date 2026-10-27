# projeto_arqsoft_1240447

Projeto 1 da unidade curricular de Arquitetura de Software (ARQSOFT), 2026-2027.

O ponto de partida é o serviço REST de gestão de biblioteca desenvolvido em PSOFT
(`psoft-project-2024-g1`). Este repositório é independente e pessoal; o histórico
do projeto original não é reutilizado.

## Estado atual

- Base importada e a compilar.
- `mvn test` executa 102 testes, todos verdes.
- As extensões do P1 ainda não foram implementadas.

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

1. Políticas de empréstimo Standard e Adaptive, com configuração externa e renovação.
2. Integração com Google Books e Open Library através de portas e adaptadores.
3. Notificações por email, SMS e webhook, configuráveis e extensíveis.
4. Testes funcionais de caixa opaca, caixa transparente, mutação, componentes e sistema.
5. SAD/ADD, vistas, ADRs, matriz de rastreabilidade e demonstração.
