# Projeto: Chat Multiusuário com Histórico

## Contexto

- Trabalho de Programação Orientada a Objetos.
- Java 21 como versão de compilação, Maven e JUnit 5.
- Comunicação por sockets TCP.
- Uma thread de atendimento por cliente.
- Histórico de mensagens via JDBC, a implementar.

## Pacotes

Base: br.com.luka.chat

- model: classes e regras do domínio.
- server: servidor, conexões e broadcast.
- client: interação do usuário com o chat.
- persistence: implementação de Repository/DAO via JDBC.

## Regras de arquitetura

- O domínio não importa java.sql, java.io ou java.net.
- Mensagem deve ser imutável, com atributos privados e finais.
- Mensagem deve conter remetente, conteúdo e horário.
- Remetente e conteúdo não podem ser nulos ou vazios.
- Todo SQL deve ficar na camada persistence.
- A interface Repository deve ficar no domínio.
- A implementação JDBC deve ficar em persistence.
- Objetos compartilhados entre threads devem ter proteção
  adequada para acesso concorrente.
- Não manter um bloqueio global de clientes durante escrita
  em sockets ou acesso ao banco.

## Testes

- Escrever os testes das regras antes da implementação.
- Usar JUnit 5.
- Testes unitários não dependem de rede ou banco real.
- Usar Repository em memória quando necessário nos testes.
- Executar mvn test depois de alterações nas regras.
- Aula 1: pelo menos 3 testes passando.
- Projeto completo: pelo menos 5 testes passando.

## Forma de trabalho

- Explicar a mudança antes de escrever código.
- Trabalhar uma classe ou etapa por vez.
- Usar nomes claros em português.
- Explicar decisões de concorrência.
- Compilar e testar antes de considerar uma etapa concluída.
- Fazer commits pequenos com mensagens descritivas.
- Registrar no README o uso de IA e como o código foi validado.

## Arquivos que não devem entrar no Git

- target/
- Arquivos de banco de dados locais.
- Senhas e chaves de API.