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

#### Opção A — Interface Gráfica Estilo WhatsApp Desktop (Recomendada):
```powershell
.\scripts\cliente-gui.ps1
```

#### Opção B — Terminal (Console):
```powershell
.\scripts\cliente.ps1
```

#### Opção C — Bot de Inteligência Artificial (Opcional):
```powershell
.\scripts\bot-ia.ps1
```



### Passo 4 

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

### Execução direta sem scripts (Java 21+ no PATH / Maven / IDE)

Caso o computador já possua **Java 21+** e **Maven** configurados no `PATH` (como no PC da professora ou laboratório), não é necessário utilizar os scripts `.ps1`. Você pode executar diretamente pelos comandos abaixo ou pela IDE:

#### Opção 1 — Pelo Terminal (PowerShell ou CMD) com `java` e `mvn`

**1. Compilar o projeto (apenas uma vez):**
```powershell
mvn clean compile
```

**2. Iniciar o Servidor (Terminal 1):**
- No **PowerShell**:
  ```powershell
  java -cp "target/classes;$env:USERPROFILE\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar" br.com.luka.chat.server.Servidor
  ```
- No **Prompt de Comando (CMD)**:
  ```cmd
  java -cp "target/classes;%USERPROFILE%\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar" br.com.luka.chat.server.Servidor
  ```

**3. Abrir os Clientes com Interface Gráfica Estilo WhatsApp (abra um terminal para cada usuário):**
```powershell
java -cp target/classes br.com.luka.chat.client.ClienteChatGui
```
*(Dica: o cliente gráfico usa apenas bibliotecas nativas do Java 21 e o código compilado em `target/classes`, portanto o comando acima funciona tanto no PowerShell quanto no CMD ou Linux/macOS).*

**4. Abrir Cliente via Console/Terminal (opcional):**
```powershell
java -cp target/classes br.com.luka.chat.client.ClienteChat
```

**5. Executar a suíte de testes automatizados (JUnit 5):**
```powershell
mvn test
```

#### Opção 2 — Direto pela IDE (IntelliJ IDEA, VS Code ou Eclipse)
1. Abra a pasta do projeto na IDE como um projeto **Maven**.
2. Execute (`Run`) o método `main` da classe [`br.com.luka.chat.server.Servidor`](src/main/java/br/com/luka/chat/server/Servidor.java).
3. Execute (`Run`) o método `main` da classe [`br.com.luka.chat.client.ClienteChatGui`](src/main/java/br/com/luka/chat/client/ClienteChatGui.java) (repita para abrir 2 ou 3 janelas de usuários simultâneos).

> [!NOTE]
> Caso o comando `java -version` no computador mostre o Java 8 mas haja um JDK 21+ instalado em outra pasta, basta apontar o `JAVA_HOME` na sessão antes dos comandos:
> ```powershell
> $env:JAVA_HOME = "C:\caminho\para\jdk-21-ou-superior"
> $env:Path = "$env:JAVA_HOME\bin;$env:Path"
> ```

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

---

## 🤖 Assistente de IA Integrado (Groq / Gemini) e Configuração de API Key

O projeto inclui um **cliente autônomo com IA** (`br.com.luka.chat.bot.BotIaCliente`) que participa das conversas como `@Gemini` (**Assistente IA ✨**), já fixado no topo da barra lateral de conversas na interface gráfica e iniciado automaticamente em segundo plano.

### Como falar com a IA no Chat:
- **Conversa padrão na Barra Lateral:** Clique em **Assistente IA ✨** no topo da lista de conversas e envie qualquer pergunta diretamente.
- **Em Grupos ou no Console:** Mencione `@gemini`, `@ia` ou envie `/ia <sua pergunta>`.

### Como ativar a IA Real (Groq ou Gemini) no PC da Professora:
Por padrão, se nenhuma chave for fornecida, o assistente executa em **Modo Demonstração (Fallback)** com respostas simuladas, garantindo que o projeto funcione imediatamente mesmo sem chave ou internet.

Para ativar a **IA Real com sua chave da Groq (`gsk_...`)**:

**Forma 1 — Direto pela Interface Gráfica (Mais Rápida para Apresentação):**
1. Na conversa **Assistente IA ✨**, clique no botão roxo **`🔑 Configurar Key IA`** no canto superior direito.
2. Cole sua chave da Groq (`gsk_...`) e clique em **Salvar API Key**.
3. A chave é salva localmente em `chat.local.properties` (ignorado pelo `.gitignore`) e ativada imediatamente sem precisar reiniciar o servidor.

**Forma 2 — Arquivo de Configuração Local (`chat.local.properties`):**
Copie o modelo `chat.properties.example` para `chat.local.properties` na raiz do projeto:
```properties
groq.api.key=gsk_sua_chave_aqui
```

**Forma 3 — Variável de Ambiente:**
```powershell
$env:GROQ_API_KEY = "gsk_sua_chave_aqui"
```

### Arquitetura e Concorrência do Bot:
- **Cliente Autônomo:** Não altera o servidor nem o domínio. O bot é tratado como qualquer outro cliente TCP.
- **Chamadas Assíncronas:** A requisição HTTP externa para o Google Gemini roda em um `ExecutorService` desacoplado da thread que lê do socket. Assim, mesmo com latência de rede na API externa, o chat continua em tempo real sem travamentos.

---

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

31 testes passando:

- **Unitários (29)**, sem rede nem banco, usando `InMemoryMensagemRepository`,
  `FakeConexao` e `Clock.fixed`:
  - `ServicoIaTest`: respostas do bot no modo simulado e parsing correto de respostas JSON da API Gemini.
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
- **Gemini (via Antigravity)**: reformulou visualmente a interface gráfica
  (`ClienteChatGui`) inspirada no WhatsApp Desktop, adicionando cabeçalho
  de perfil, busca e filtros na barra lateral, prévia e horário da última
  mensagem, tela inicial sem conversa selecionada e balões de mensagem
  arredondados alinhados à esquerda e à direita, preservando integralmente
  o protocolo TCP, a concorrência na EDT e a arquitetura existente.

Validação:
- teste manual com servidor real e 4 clientes TCP: broadcast entregue a
  todos, nickname duplicado recusado, queda abrupta de um cliente sem
  afetar os demais e histórico recebido por um cliente que entrou depois;
- revisão do código gerado antes de cada commit.

## Próximos passos

- Comandos `/help`, `/users` e `/history`.
- Desconexão de clientes inativos por timeout.
- Relatório e vídeo com pelo menos 3 clientes simultâneos.
