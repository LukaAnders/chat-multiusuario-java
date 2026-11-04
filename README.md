# Chat Multiusuário com Histórico

Projeto de Programação Orientada a Objetos (Java) — 2º bimestre, Opção A.


## ▶️ Como executar

### Pré-requisitos

| Item | Versão | Como conferir |
|---|---|---|
| JDK | **21 ou superior** | `java -version` (ou JDK em `JAVA_HOME`) |
| Maven | 3.9+ | `mvn -v` |
| Windows PowerShell | 5.1+ | já vem no Windows |

> [!IMPORTANT]
> Não é preciso instalar banco de dados: o H2 é baixado pelo Maven e o
> arquivo do banco (`data/chat.mv.db`) é criado na primeira execução.

### Passo 1 — Liberar scripts no PowerShell (apenas uma vez)

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

### Passo 2 — Iniciar o servidor

Na pasta do projeto, abra um terminal e execute:

```powershell
.\scripts\servidor.ps1 -Compilar
```

Saída esperada:

```text
Compilando o projeto...
Java: C:\...\jdk-26.0.2.1\bin\java.exe
Servidor iniciado na porta 5000
```


> [!TIP]
> Use `-Compilar` na primeira vez e sempre que alterar o código.
> Para outra porta: `.\scripts\servidor.ps1 6000`.

### Passo 3 — Abrir os clientes

Abra novos terminais ou janelas gráficas para cada usuário (pelo menos 3 para a
demonstração). Você pode escolher entre **Interface Gráfica (Janela)** ou **Terminal**:

#### Opção A — Interface Gráfica (Recomendada):
```powershell
.\scripts\cliente-gui.ps1
```
> Abre uma janela desktop nativa. Basta informar o nickname e conversar com rolagem automática e avisos visuais.

#### Opção B — Terminal (Console):
```powershell
.\scripts\cliente.ps1
```

### Passo 4 — Conversar

1. Digite seu **nickname** (sem espaços, até 20 caracteres, único).
2. Você recebe as últimas mensagens do histórico.
3. Digite mensagens e pressione Enter; todos os conectados recebem.
4. Digite `/sair` para encerrar o cliente.

Exemplo:

```text
Digite seu nome: ana
[03/11 13:59] carlos: mensagem antiga do histórico
* ana entrou no chat
olá pessoal!
[03/11 14:02] ana: olá pessoal!
* bruno entrou no chat
[03/11 14:02] bruno: oi ana
```

### Passo 5 — Encerrar

- Cliente: `/sair` ou fechar o terminal (o servidor trata a queda).
- Servidor: `Ctrl + C`. O histórico permanece salvo em `data/`.

### Roteiro de demonstração (vídeo)

1. Iniciar o servidor.
2. Conectar 3 clientes (`ana`, `bruno`, `carlos`) e trocar mensagens.
3. Tentar entrar com um nome repetido e mostrar que ele é recusado.
4. Fechar um terminal à força e mostrar que os outros continuam.
5. Conectar um 4º cliente e mostrar que ele recebe o histórico.
6. Reiniciar o servidor e mostrar que o histórico foi mantido.

### Problemas comuns

| Erro | Causa | Solução |
|---|---|---|
| `UnsupportedClassVersionError ... 65.0 ... 52.0` | `java` do PATH é o Java 8 | use os scripts ou ajuste `JAVA_HOME` para um JDK 21+ |
| `ClassNotFoundException: org.h2.Driver` | driver H2 fora do classpath | use os scripts (eles montam o classpath) |
| `não pode ser carregado porque a execução de scripts foi desabilitada` | política do PowerShell | Passo 1 |
| `Address already in use` / `BindException` | porta 5000 ocupada | feche o outro servidor ou use outra porta |
| `Connection refused` no cliente | servidor não iniciado | execute o Passo 2 antes |
| Acentos estranhos | console fora de UTF-8 | use `scripts\cliente.ps1`, que configura UTF-8 |

### Execução manual (sem scripts)

```powershell
$env:JAVA_HOME = "C:\caminho\para\jdk-21-ou-superior"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn compile
$cp = "target/classes;$env:USERPROFILE\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar"
java -cp $cp br.com.luka.chat.server.Servidor          # servidor
java -cp target/classes br.com.luka.chat.client.ClienteChat   # cliente
```

---

## Tecnologias

- Java: código compilado para Java 21 (JDK 26 no ambiente de desenvolvimento).
- Maven.
- Sockets TCP e uma thread por cliente.
- JDBC com banco H2 (arquivo local `data/chat`).
- JUnit 5.

## Requisitos atendidos

| Requisito | Onde |
|---|---|
| Servidor com `ServerSocket` e laço de `accept()` | `server.Servidor` |
| Uma thread por conexão | `Servidor.iniciar()` |
| Atendimento que implementa `Runnable` | `server.ClienteHandler` |
| Coleção de clientes protegida para acesso concorrente | `service.ClientRegistry` |
| Broadcast para todos os conectados | `ChatService.broadcast()` |
| Mensagem com remetente, conteúdo e horário | `model.Mensagem` |
| Persistência via JDBC | `persistence.JdbcMensagemRepository` |
| Repository isolando o SQL | `model.MensagemRepository` + `persistence` |
| Histórico enviado ao conectar | `ChatService.conectar()` |
| Desconexão segura | `finally` do `ClienteHandler` |
| Mínimo de 5 testes sem rede/banco | 26 testes unitários |
| Pacotes separados | ver Arquitetura |

## Rubrica de avaliação

| Critério | Pontuação |
|---|---:|
| Múltiplos clientes simultâneos | 1,3 |
| Broadcast sem perder mensagens | 1,2 |
| Persistência JDBC | 1,0 |
| Pacotes e responsabilidades | 0,5 |
| Testes unitários | 0,5 |
| Código e relatório | 0,5 |
| Total | 5,0 |

## Arquitetura

Pacote base: `br.com.luka.chat`.

| Pacote | Conteúdo | Pode importar |
|---|---|---|
| `model` | `Usuario`, `Mensagem`, `MensagemRepository` (interface), exceções | nada de infraestrutura |
| `service` | `ChatService`, `ClientRegistry`, `ConexaoCliente` (interface) | `model` |
| `persistence` | `JdbcMensagemRepository`, `ConnectionFactory` | `model`, `java.sql` |
| `server` | `Servidor`, `ClienteHandler`, `Protocolo` | `service`, `model`, `java.net` |
| `client` | `ClienteChat` | apenas rede |

Regra prática: socket não conhece SQL, repository não conhece socket,
o domínio não conhece nenhum dos dois e o serviço conecta as partes.
As implementações concretas são criadas apenas no `main` do `Servidor`
e injetadas pelo construtor.

```mermaid
classDiagram
    direction LR

    class Servidor {
        -int porta
        -ChatService chatService
        +iniciar() void
        +parar() void
        +main(String[] args)$ void
    }
    class ClienteHandler {
        -Socket socket
        -PrintWriter saida
        -Usuario usuario
        +run() void
        +enviar(String linha) boolean
    }
    class ConexaoCliente {
        <<interface>>
        +getUsuario() Usuario
        +enviar(String linha) boolean
    }
    class ChatService {
        +conectar(ConexaoCliente) boolean
        +desconectar(ConexaoCliente) void
        +enviarMensagem(Usuario, String) Mensagem
        +broadcast(String) void
        +buscarHistorico() List~Mensagem~
    }
    class ClientRegistry {
        -CopyOnWriteArrayList~ConexaoCliente~ conexoes
        +adicionar(ConexaoCliente) boolean
        +remover(ConexaoCliente) boolean
        +snapshot() List~ConexaoCliente~
    }
    class MensagemRepository {
        <<interface>>
        +salvar(Mensagem) Mensagem
        +buscarUltimas(int) List~Mensagem~
    }
    class JdbcMensagemRepository
    class ConnectionFactory
    class Mensagem {
        -Long id
        -Usuario remetente
        -String conteudo
        -LocalDateTime horario
        +formatar() String
        +comId(Long) Mensagem
    }
    class Usuario {
        -String nome
    }

    Servidor --> ClienteHandler : cria uma thread por conexão
    ClienteHandler ..|> ConexaoCliente
    ClienteHandler ..|> Runnable
    ClienteHandler --> ChatService
    ChatService --> ClientRegistry
    ChatService --> MensagemRepository
    ClientRegistry o-- ConexaoCliente
    JdbcMensagemRepository ..|> MensagemRepository
    JdbcMensagemRepository --> ConnectionFactory
    Mensagem --> Usuario
```

### Fluxo de uma mensagem

1. `ClienteChat` envia uma linha UTF-8 pelo socket.
2. `ClienteHandler.run()` lê a linha e chama `ChatService.enviarMensagem()`.
3. O serviço cria a `Mensagem`, que valida o conteúdo no construtor.
4. A mensagem é salva com `MensagemRepository.salvar()` (JDBC).
5. O serviço faz o broadcast para um snapshot dos clientes conectados.

Ao conectar, a primeira linha do cliente é o nickname. Se for válido e
estiver livre, o cliente recebe as últimas 20 mensagens e os demais
recebem o aviso `* nome entrou no chat`.

## Concorrência

| Objeto compartilhado | Proteção |
|---|---|
| Interface Gráfica (Swing) | A thread de rede nunca toca componentes gráficos; despachos via `SwingUtilities.invokeLater()` na Event Dispatch Thread (EDT) |
| Lista de clientes (`ClientRegistry`) | `CopyOnWriteArrayList`; o broadcast itera sobre um snapshot, sem lock durante a escrita nos sockets |
| Nickname único | verificar e adicionar sob o mesmo lock (check-then-act atômico) |
| `PrintWriter` de cada cliente | `enviar()` é `synchronized`, então as linhas não se intercalam |
| Ordem das mensagens | salvar e fazer o broadcast em uma única seção crítica, para que o banco e os clientes vejam a mesma ordem (custo: menos paralelismo nos envios) |
| Banco de dados | uma `Connection` por operação, fechada com try-with-resources |
| `Mensagem` e `Usuario` | imutáveis; circulam entre threads sem lock |

Race conditions tratadas:
- **Nickname duplicado**: dois clientes com o mesmo nome ao mesmo tempo.
  Testado com 20 threads disputando o nome.
- **ConcurrentModificationException no broadcast**: evitada pelo snapshot.
- **Ordem salvar/broadcast divergente**: evitada pela seção crítica única.

Se o envio para um cliente falhar, só esse cliente é removido; os demais
continuam recebendo. Uma queda abrupta do cliente cai no `finally` do
`ClienteHandler` e não afeta o servidor.

## Persistência

Script em [`src/main/resources/schema.sql`](src/main/resources/schema.sql),
executado automaticamente ao iniciar o servidor:

```sql
CREATE TABLE IF NOT EXISTS mensagem (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    remetente VARCHAR(30)  NOT NULL,
    conteudo  VARCHAR(500) NOT NULL,
    horario   TIMESTAMP    NOT NULL
);
```

- Sempre `PreparedStatement` e try-with-resources.
- Convenção do id: a mensagem nasce com id nulo; o repository devolve uma
  nova instância com o id gerado.
- `SQLException` é convertida em `PersistenciaException`, para que
  `java.sql` não chegue às camadas de serviço e rede.
- Avisos de entrada e saída não são persistidos.

## Testes

```bash
mvn test
```

29 testes passando:

- **Unitários (27)**, sem rede nem banco, usando `InMemoryMensagemRepository`,
  `FakeConexao` e `Clock.fixed`:
  - `ConexaoRedeClienteTest`: conexão, envio de handshake e troca assíncrona de mensagens.
  - `UsuarioTest`: nome vazio, nulo, com espaço, acima do limite, trim e
    igualdade sem diferenciar maiúsculas.
  - `MensagemTest`: remetente nulo, conteúdo vazio e acima do limite,
    trim, `formatar()` e convenção do id.
  - `ClientRegistryTest`: adicionar/remover, nickname duplicado, snapshot
    imutável e corrida concorrente de nickname.
  - `ChatServiceTest`: salvar no repository, broadcast para todos,
    cliente removido não recebe, mensagem inválida não é salva,
    histórico em ordem cronológica, histórico ao conectar, avisos de
    entrada/saída, falha de uma conexão não afeta as outras e broadcast
    concorrente (10 threads × 50 mensagens) sem perda e na mesma ordem
    do banco.
- **Integração (2)**: `JdbcMensagemRepositoryIntegracaoTest` com H2 em memória.

Os testes das regras foram escritos antes das respectivas implementações.

## Uso de IA e validação

- **ChatGPT**: orientou a configuração do ambiente, explicou sockets e
  threads e auxiliou na construção do código inicial por etapas, nos
  testes e nas classes do domínio (aula 1).
- **Claude (via Antigravity)**: analisou o código existente e refatorou
  para a arquitetura em camadas (service, persistence, Repository),
  implementou a persistência JDBC, o histórico, o nickname único, os
  testes de concorrência e os scripts de execução.

Validação:
- teste manual com servidor real e 4 clientes TCP: broadcast entregue a
  todos, nickname duplicado recusado, queda abrupta de um cliente sem
  afetar os demais e histórico recebido por um cliente que entrou depois;
- revisão do código gerado antes de cada commit.

## Próximos passos

- Comandos `/help`, `/users` e `/history`.
- Desconexão de clientes inativos por timeout.
- Relatório e vídeo com pelo menos 3 clientes simultâneos.
