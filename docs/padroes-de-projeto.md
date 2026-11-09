# Padrões de projeto no Synapse Forge

Documento de referência da equipe. Cada padrão listado aqui está implementado no
código, com o arquivo indicado. A ideia é que qualquer pessoa do time (e a banca)
consiga ir do nome do padrão direto para a classe que o aplica.

Repositórios:

- Backend: `victor-eidt/synapse-forge-back-end` (Spring Boot 3.3.5, Java 17, MongoDB)
- Frontend: `izabelly-code/synapse-forge-` (Vite, React, TypeScript)

---

## 1. Strategy

**Onde:** `src/main/java/synapseforge/crud/service/politica/`

- `PoliticaConsumo` (interface)
- `PoliticaConsumoImpressao`, `PoliticaConsumoPintura`, `PoliticaConsumoFinalizado`
- `PoliticaConsumoResolver` (contexto)

**Problema que resolve:** cada etapa de produção consome insumos diferentes. A
solução ingênua seria um `switch` ou uma cadeia de `if/else` sobre `StatusPedido`
dentro do serviço de estoque, que cresce a cada etapa nova e concentra regras de
negócio distintas no mesmo método.

**Como está aplicado:** cada etapa é uma classe que implementa `PoliticaConsumo` e
declara qual etapa atende em `etapa()`. O `PoliticaConsumoResolver` recebe todas as
políticas por injeção de dependência, indexa por etapa em um `Map` imutável e
devolve um `Optional` na consulta.

**Consequência prática:** etapa nova entra como classe nova, sem alterar o serviço
de estoque. E etapa sem política (MODELAGEM, por exemplo) simplesmente não consome
estoque, porque o `Optional` volta vazio. O comportamento padrão sai de graça da
estrutura, em vez de ser um `else` escrito à mão.

---

## 2. Repository

**Onde:** `src/main/java/synapseforge/crud/infrastructure/repository/`

**Como está aplicado:** interfaces que estendem `MongoRepository`, com métodos de
consulta derivados do nome (`findByEmail`, `findByEmailConfirmadoFalseAnd...`). A
implementação é gerada pelo Spring Data em tempo de execução.

**Consequência prática:** o serviço fala com uma abstração de coleção, não com o
driver do Mongo. Nos testes unitários o repositório é mockado com Mockito, o que
permite testar regra de negócio sem subir banco.

---

## 3. DTO e separação de contratos

**Onde:** `src/main/java/synapseforge/crud/DTO/<Dominio>/`, com `...RequestDTO` e
`...ResponseDTO` por domínio.

**Problema que resolve:** expor a entidade diretamente na API acopla o contrato
HTTP ao modelo persistido e abre brecha para o cliente escrever campos que não
deveria.

**Como está aplicado:** DTO de entrada com validação `jakarta.validation` e DTO de
saída separado. Um caso concreto do RF11: `saldo` foi removido do
`MaterialRequestDTO`, porque saldo é consequência das movimentações de estoque e
não pode ser definido por edição de cadastro. A entidade continua tendo o campo; o
contrato de entrada não.

---

## 4. Mapper

**Onde:** `src/main/java/synapseforge/crud/mapper/PedidoAdminMapper.java` (MapStruct)
e os métodos `toEntity` / `toResponseDTO` nos serviços.

**Como está aplicado:** no caso do admin, o MapStruct gera a implementação na
compilação, configurado com `nullValuePropertyMappingStrategy = IGNORE`, de modo
que campo nulo no DTO significa "não alterar", que é a semântica de um PATCH
parcial. Nos demais domínios a conversão é escrita à mão no serviço.

**Observação honesta para a banca:** a conversão não é uniforme no projeto. Parte
usa MapStruct, parte é manual. É uma dívida técnica conhecida, não um descuido.

---

## 5. Injeção de dependência por construtor

**Onde:** padrão nos serviços novos, por exemplo `MaterialService`,
`EstoqueService`, `EquipeContexto`.

**Como está aplicado:** campos `final` e `@RequiredArgsConstructor` do Lombok, que
gera o construtor com todas as dependências obrigatórias.

**Por que não `@Autowired` em campo:** com campos finais, a classe não pode ser
instanciada em estado incompleto, e nos testes as dependências entram pelo
construtor sem necessidade de reflexão. `PedidoService` ainda usa `@Autowired` em
campo, que é o estilo antigo do projeto; código novo não segue por ali.

---

## 6. Ledger (livro de movimentações) com saldo derivado

**Onde:** `MovimentoEstoque` (entidade), `EstoqueService`, `Material.saldo`.

**Problema que resolve:** um campo de saldo editável não guarda histórico, não
permite estorno rastreável e não responde a perguntas de período, como consumo por
mês.

**Como está aplicado:** cada entrada, baixa, estorno ou ajuste vira um documento em
`movimentos_estoque`, com quantidade, saldo após o movimento, pedido de origem,
etapa e usuário. O saldo do material é consequência dessas movimentações, nunca um
valor escrito diretamente pela API.

**Consequência prática:** o histórico do insumo é auditável e as métricas agregadas
do dashboard existem porque o dado bruto foi preservado.

---

## 7. Idempotência por chave única no banco

**Onde:** `MovimentoEstoque.chaveIdempotencia` e
`config/EstoqueIndexInitializer.java`.

**Problema que resolve:** uma requisição repetida (reenvio, duplo clique,
reprocessamento) não pode debitar o estoque duas vezes.

**Como está aplicado:** cada movimento carrega uma chave determinística derivada do
pedido, do insumo, da etapa e do ciclo de estorno. Um índice único sobre essa chave
é garantido na subida da aplicação pelo `EstoqueIndexInitializer`.

**Decisão de projeto:** a garantia vive no banco, não em um `if` da aplicação. Duas
requisições concorrentes podem passar pela mesma validação em memória, mas apenas
uma consegue gravar; a segunda é rejeitada pelo índice e tratada como violação de
integridade.

---

## 8. Tabela auxiliar com referência por id

**Onde:** `FormaEnvio` (coleção `formas_envio`) e `Pedido.formaEnvioId` (RF15).

**Problema que resolve:** uma lista de opções que muda com o tempo (transportadoras,
modalidades de envio) não deve exigir recompilação e novo deploy para ganhar um
item.

**Como está aplicado:** as opções vivem em coleção própria e o pedido guarda apenas
o id da opção escolhida. Um `CommandLineRunner` dedicado semeia as formas padrão na
primeira subida, se a coleção estiver vazia.

**Contraste deliberado:** `StatusEnvio` e `StatusPedido` são enums, porque são
conjuntos fechados e estáveis cujos valores a aplicação precisa conhecer em tempo de
compilação para decidir fluxo. O critério é esse: conjunto que o usuário administra
vira coleção; conjunto que o código interpreta vira enum.

---

## 9. Inicializadores de infraestrutura (CommandLineRunner)

**Onde:** `config/EstoqueIndexInitializer`, `PedidoIndexInitializer`,
`UserIndexInitializer`, `EquipeIndexInitializer`, `ConviteEquipeIndexInitializer`,
`NotificacaoIndexInitializer`, `OrcamentoIndexInitializer`, `MongoSchemaInitializer`,
`DataInitializer`.

**Problema que resolve:** o MongoDB não tem schema nem migração declarativa como um
banco relacional. Índices e dados de referência precisam de algum momento garantido
de criação.

**Como está aplicado:** cada preocupação tem seu próprio `CommandLineRunner`, que
roda na subida e é idempotente (cria apenas se não existir). Índices em um,
validação de schema em outro, dados de teste em outro.

**Por que separados:** um inicializador por assunto reduz conflito de merge, porque
cada requisito mexe no seu, e deixa claro o que é índice, o que é schema e o que é
semente de dados.

---

## 10. Tratamento centralizado de exceções

**Onde:** `exception/GlobalExceptionHandler.java`, com as exceções de domínio
`EstoqueInsuficienteException`, `RecursoNaoEncontradoException` e
`SemEquipeException`.

**Como está aplicado:** um `@RestControllerAdvice` mapeia cada exceção para o status
HTTP correto, em vez de cada controller montar sua resposta de erro.

| Exceção | Status | Significado |
|---|---|---|
| `EstoqueInsuficienteException` | 422 | a requisição é válida, mas a regra de negócio impede |
| `RecursoNaoEncontradoException` | 404 | id inexistente |
| `SemEquipeException` | 403 | usuário sem oficina associada |
| `MethodArgumentNotValidException` | 400 | falha de validação do DTO |
| `DataIntegrityViolationException` | 422 | violação de índice único (ver idempotência) |

**Consequência prática:** o front trata status, não texto de mensagem. O 422 do
estoque insuficiente é o caso em que a mensagem do backend é repassada ao usuário,
porque ela diz qual insumo falta e quanto.

---

## 11. Contexto de tenant (isolamento por equipe)

**Onde:** `service/EquipeContexto.java`, e o campo `equipeId` nas entidades de
negócio.

**Problema que resolve:** o sistema atende várias oficinas. Dados de uma não podem
aparecer para outra, e o cliente não pode escolher de qual equipe quer ler.

**Como está aplicado:** o `equipeId` é resolvido sempre a partir do usuário logado,
nunca lido do corpo da requisição. É o comentário que está escrito na própria
entidade: "sai sempre do usuário logado, nunca do request".

**Por que importa:** é uma decisão de segurança disfarçada de decisão de modelagem.
Aceitar `equipeId` no DTO transformaria o campo em vetor de acesso a dados de
terceiros.

---

## 12. Chain of Responsibility (filtros de segurança)

**Onde:** `infrastructure/security/JwtFilter.java`,
`infrastructure/security/RateLimitFilter.java`, `config/SecurityConfig.java`.

**Como está aplicado:** a cadeia de filtros do Spring Security processa a requisição
em ordem. O `JwtFilter` valida o token e popula o principal com o id do usuário; o
`RateLimitFilter` limita a frequência de chamadas. Cada filtro decide se trata e
repassa, ou interrompe.

---

## 13. Camadas (controller, service, repository)

**Onde:** estrutura de pastas do backend.

```
controller/      entrada HTTP, validação de contrato, status de resposta
service/         regra de negócio
infrastructure/repository/   acesso a dados
infrastructure/entity/       modelo persistido
DTO/             contratos de entrada e saída
```

**Regra seguida:** controller não contém regra de negócio e service não conhece
tipos de HTTP. O service lança exceção de domínio; quem traduz para status é o
handler global.

---

## Frontend

### 14. Camada de serviço

**Onde:** `src/services/` (um arquivo por domínio: `PedidoService.ts`,
`EstoqueService.ts`, `MaterialService.ts`, e assim por diante).

**Como está aplicado:** nenhum componente chama `fetch` diretamente. Cada serviço
concentra as rotas do seu domínio, monta o header de autenticação e normaliza o
erro antes de devolver para a tela.

### 15. Interceptador de requisições

**Onde:** `src/services/httpInterceptor.ts`.

**Como está aplicado:** trata de forma única o que é transversal a todas as
chamadas, como sessão expirada, em vez de repetir esse tratamento em cada serviço.

### 16. Custom hooks (reuso de comportamento)

**Onde:** `src/hooks/` (`useEscapeKey`, `useFocusTrap`, `useBodyScrollLock`,
`useNotificacoes`, `useMediaQuery`, entre outros).

**Problema que resolve:** comportamento repetido entre componentes (fechar modal no
Esc, prender o foco, travar o scroll do body) viraria código duplicado em cada
modal.

**Como está aplicado:** cada comportamento é um hook, e os modais os compõem. Os
três hooks de acessibilidade de modal aparecem juntos em `MaterialModal`,
`PedidoDetalheModal` e `NovoPedidoModal`.

### 17. Componentes de UI reutilizáveis

**Onde:** `src/components/ui/` (`Select`, `IconButton`, `LoadingButton`,
`FieldMessage`, `MenuSurface`, entre outros).

**Caso concreto:** `Select` substitui o `<select>` nativo porque o painel do sistema
fugia do tema e da tipografia do aplicativo, e no celular abria um menu nativo
pequeno demais. A motivação está comentada no próprio código do `NovoPedidoModal`.

### 18. Context API para estado global

**Onde:** `src/contexts/ThemeContext.tsx` e `theme-context.ts`, consumidos por
`useTheme`.

**Como está aplicado:** o tema é estado global de leitura frequente, então vive em
contexto em vez de ser passado por propriedade ao longo da árvore de componentes.

---

## Como usar este documento

Ao abrir um PR que introduz ou segue um destes padrões, cite o número da seção na
descrição. Ao criar um padrão novo, acrescente a seção aqui no mesmo formato:
onde está, que problema resolve, como foi aplicado, e qual a consequência prática.
