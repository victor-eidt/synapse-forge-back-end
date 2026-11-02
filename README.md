# 🚀 Synapse Forge - Backend

## 🧠 Arquitetura

O projeto foi estruturado em camadas para manter a aplicação organizada e escalável:

- **Controller** → expõe os endpoints da API
- **Service** → concentra a lógica de negócio
- **Repository** → acessa os dados no MongoDB
- **DTO** → define entradas e saídas da aplicação
- **Entity** → representa os documentos persistidos
- **Config** → centraliza segurança, CORS, inicialização e documentação

Essa estrutura facilita manutenção, testes e evolução do sistema.

---

## ⚙️ Tecnologias Utilizadas

- Java 17
- Spring Boot 3
- Spring Security
- JWT
- MongoDB
- Spring Data MongoDB
- Bean Validation
- OpenAPI / Swagger
- Lombok
- Maven
- Spring Boot Actuator (health check)
- Docker · Render · MongoDB Atlas (deploy)

---

## 🗄️ Banco de Dados e Configuração

O sistema utiliza MongoDB como banco principal.

### Configuração padrão

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/synapse-forge
server.port=8081
spring.application.name=synapse-forge-db
```

Também há suporte para upload de arquivos, email e CORS configurados por variáveis de ambiente.

### Credenciais locais

Credenciais ficam em `src/main/resources/application-secrets.properties` (ignorado pelo git). Copie o modelo e preencha:

```bash
cp src/main/resources/application-secrets.properties.example src/main/resources/application-secrets.properties
```

O segredo do JWT é obrigatório: sem ele a aplicação não sobe. Gere um valor e coloque em `app.jwt.secret` nesse arquivo (ou na variável `APP_JWT_SECRET`):

```bash
openssl rand -base64 32
```

### Variáveis de ambiente

| Variável | Padrão | Uso |
|---|---|---|
| `APP_JWT_SECRET` | nenhum (obrigatória) | Segredo de assinatura dos tokens, em Base64 com pelo menos 32 bytes. Trocar o valor desloga todo mundo |
| `SPRING_DATA_MONGODB_URI` | `mongodb://localhost:27017/synapse-forge` | Conexão com o MongoDB. O nome do banco vai no caminho da URI |
| `PORT` | `8081` | Porta HTTP (o Render injeta a dele) |
| `SPRING_MAIL_HOST` / `SPRING_MAIL_PORT` | `smtp.gmail.com` / `587` | Servidor SMTP |
| `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | vazio | Login do SMTP (local: `application-secrets.properties`) |
| `APP_MAIL_FROM` | o `SPRING_MAIL_USERNAME` | Remetente dos e-mails |
| `APP_URL` | `http://localhost:5173` | URL do front usada nos links de email |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | Origens liberadas no CORS |
| `APP_ADMIN_EMAILS` | vazio | Emails autorizados a usar `/admin/**` |
| `APP_SWAGGER_ENABLED` | `true` | Liga/desliga o Swagger UI e o `/v3/api-docs` |
| `APP_SEED_ENABLED` | `true` | Liga/desliga a criação dos dados de teste |
| `APP_SEED_DASHBOARD_MOCKS` | `true` | Liga/desliga só os pedidos e materiais fictícios do dashboard (exige `APP_SEED_ENABLED`) |
| `APP_MONGO_SCHEMA_VALIDATION_ENABLED` | `true` | Aplica o `$jsonSchema` nas coleções na subida |
| `APP_MONGO_SCHEMA_VALIDATION_ACTION` | `error` | `error` recusa documento inválido; `warn` só registra no log do MongoDB |
| `JAVA_TOOL_OPTIONS` | definido no `Dockerfile` | Flags da JVM no container (memória, GC) |

> Em produção, use `APP_SWAGGER_ENABLED=false` e `APP_SEED_ENABLED=false`. O usuário do MongoDB precisa do privilégio `collMod` para aplicar o JSON Schema em coleções que já existem (o papel `readWrite` não tem); sem ele a aplicação sobe e só registra um aviso.

---

## ▶️ Como rodar

Pré-requisitos: Java 17 e um MongoDB rodando em `localhost:27017`.

```bash
./mvnw spring-boot:run   # sobe a API em http://localhost:8081
./mvnw verify            # build + testes
```

Com Docker (a mesma imagem que vai para a nuvem):

```bash
docker build -t synapse-forge-api .
docker run --rm -p 8081:8081 \
  -e SPRING_DATA_MONGODB_URI=mongodb://host.docker.internal:27017/synapse-forge \
  -e APP_JWT_SECRET="$(openssl rand -base64 32)" \
  synapse-forge-api
curl http://localhost:8081/actuator/health   # {"status":"UP"}
```

---

## ☁️ Deploy (CI/CD)

Deploy na nuvem com um pipeline por ambiente (SYN-89 no back, SYN-88 no front):

- **Back-end:** container Docker no **Render** (plano free), descrito em [`render.yaml`](render.yaml)
  (Blueprint = infraestrutura como código) e construído pelo [`Dockerfile`](Dockerfile).
- **Banco:** **MongoDB Atlas** (cluster M0 gratuito), um banco por ambiente no mesmo cluster.
- **Front-end:** **Vercel**, publicado pelo GitHub Actions do repositório do front (ver o README de lá).

```mermaid
flowchart LR
    PR[PR para dev/main] --> CI["CI: build · testes"]
    CI -->|merge na dev| DH["deploy · render<br/>Environment homologacao"]
    CI -->|merge na main| DP["deploy · render<br/>Environment producao"]
    DH -->|deploy hook + SHA| RH[Render<br/>synapse-forge-api-homolog]
    DP -->|deploy hook + SHA| RP[Render<br/>synapse-forge-api]
    RH --> AH[(Atlas<br/>synapse-forge-homolog)]
    RP --> AP[(Atlas<br/>synapse-forge)]
    VH[Vercel Preview<br/>synapse-forge-homolog.vercel.app] --> RH
    VP[Vercel Production<br/>synapse-forge-one.vercel.app] --> RP
```

### Ambientes

| Ambiente | Branch | GitHub Environment | Serviço no Render | Banco no Atlas | Front na Vercel |
|---|---|---|---|---|---|
| dev (local) | `feature/*`, `fix/*` | — | `localhost:8081` | `synapse-forge` (local) | `localhost:5173` |
| test (CI) | todo PR | — | — (Mongo embutido nos testes) | — | — |
| homologação | `dev` | `homologacao` | `synapse-forge-api-homolog` | `synapse-forge-homolog` | Preview: `synapse-forge-homolog.vercel.app` + previews dos PRs |
| produção | `main` | `producao` | `synapse-forge-api` | `synapse-forge` | Production: `synapse-forge-one.vercel.app` |

Os endereços públicos da API ficam `https://<serviço>.onrender.com` (o Render mostra o endereço exato
de cada serviço; se o nome já existir, ele acrescenta um sufixo).

Endereços atuais:

| Ambiente | API | Front |
|---|---|---|
| homologação | https://synapse-forge-api-homolog.onrender.com | https://synapse-forge-homolog.vercel.app |
| produção | https://synapse-forge-api-1dfp.onrender.com | https://synapse-forge-one.vercel.app |

### Como o pipeline funciona

1. **PR para a `dev` ou a `main`:** o job `build · testes` (`./mvnw verify`) é o gate do merge.
2. **Merge (push) na `dev` ou na `main`:** o mesmo job roda de novo no commit mergeado e, só se
   passar, o job `deploy · render` entra no GitHub Environment do ambiente e chama o deploy hook do
   Render com `&ref=<SHA>`, ou seja, publica exatamente o commit que foi testado.
3. **No Render:** a imagem é construída pelo `Dockerfile` (os testes não rodam de novo), o serviço sobe
   e o Render só troca o tráfego para a versão nova quando `GET /actuator/health` responde
   `{"status":"UP"}` (o health inclui o ping no MongoDB). Se o health check falhar, a versão anterior
   continua no ar.
4. O auto-deploy do Render está desligado (`autoDeployTrigger: "off"` no `render.yaml`): push que
   não passou na CI nunca vai para a nuvem.
5. Sem o segredo `RENDER_DEPLOY_HOOK_URL` no Environment, o job de deploy só mostra um aviso e termina
   verde (para os pushes não ficarem vermelhos antes de as contas existirem).

### Variáveis por ambiente

No Render, as marcadas como segredo são pedidas na criação do Blueprint (`sync: false`) e nunca
ficam no repositório; as outras estão escritas no `render.yaml`.

| Variável | Homologação | Produção | Segredo |
|---|---|---|---|
| `SPRING_DATA_MONGODB_URI` | `mongodb+srv://<usuario>:<senha>@<cluster>.mongodb.net/synapse-forge-homolog?retryWrites=true&w=majority` | o mesmo, com `/synapse-forge` | sim |
| `APP_JWT_SECRET` | `openssl rand -base64 32` (um só para homologação) | `openssl rand -base64 32` (outro) | sim |
| `SPRING_MAIL_HOST` | `smtp-relay.brevo.com` | `smtp-relay.brevo.com` | não |
| `SPRING_MAIL_PORT` | `2525` | `2525` | não |
| `SPRING_MAIL_USERNAME` | login SMTP do Brevo | login SMTP do Brevo | sim |
| `SPRING_MAIL_PASSWORD` | chave SMTP do Brevo | chave SMTP do Brevo | sim |
| `APP_MAIL_FROM` | remetente verificado no Brevo | remetente verificado no Brevo | sim (dado pessoal) |
| `APP_ADMIN_EMAILS` | e-mails dos admins | e-mails dos admins | sim (dado pessoal) |
| `APP_URL` | `https://synapse-forge-homolog.vercel.app` | `https://synapse-forge-one.vercel.app` | não |
| `APP_CORS_ALLOWED_ORIGINS` | `https://synapse-forge-homolog.vercel.app,https://synapse-forge-*.vercel.app` | `https://synapse-forge-one.vercel.app` | não |
| `APP_SEED_ENABLED` | `true` | `false` | não |
| `APP_SEED_DASHBOARD_MOCKS` | `true` | `false` | não |
| `APP_SWAGGER_ENABLED` | `true` | `false` (decisão do time) | não |
| `APP_MONGO_SCHEMA_VALIDATION_ENABLED` | `true` | `true` | não |
| `APP_MONGO_SCHEMA_VALIDATION_ACTION` | `error` | `error` | não |
| `PORT` | injetada pelo Render | injetada pelo Render | não |

No GitHub (repositório do back), cada Environment tem um segredo:

| Environment | Segredo | Valor |
|---|---|---|
| `homologacao` | `RENDER_DEPLOY_HOOK_URL` | deploy hook do `synapse-forge-api-homolog` |
| `producao` | `RENDER_DEPLOY_HOOK_URL` | deploy hook do `synapse-forge-api` |

Observações:

- **Seed em produção desligado.** Os usuários de teste têm senha `1234`; com o seed ligado, qualquer
  pessoa com o endereço entra como `gerente@teste.com`. Em homologação o seed fica ligado de propósito
  (dados fictícios para testar e demonstrar).
- **Swagger:** ligado em homologação. Em produção está `false`; se o time quiser a documentação
  pública em produção (como evidência, por exemplo), basta trocar para `"true"` no `render.yaml`. A
  API continua exigindo JWT de qualquer forma.
- **CORS:** o `CorsConfig` usa `setAllowedOriginPatterns`, então `https://synapse-forge-*.vercel.app`
  libera os previews dos PRs do front (`synapse-forge-<hash>-<conta>.vercel.app`) e a homologação. Em
  produção só o domínio de produção. Se o domínio da Vercel for outro, ajuste `APP_URL` e
  `APP_CORS_ALLOWED_ORIGINS` no `render.yaml` (PR normal; o Render sincroniza o Blueprint).
- **E-mail:** o plano free do Render **bloqueia as portas SMTP 25, 465 e 587** (desde set/2025), então
  o Gmail não funciona lá. O Brevo (gratuito, 300 e-mails/dia) aceita SMTP na porta **2525**, que
  passa. Sem e-mail funcionando, o cadastro não consegue confirmar a conta. Alternativa: plano pago do
  Render e manter o Gmail. O envio tem timeout de 10 s, para uma porta bloqueada não prender a
  requisição.
- **Admin em produção:** nenhum código cria ADMIN. Depois de cadastrar a conta, mude o `role` do
  documento para `ADMIN` no Atlas (*Data Explorer*, coleção `users`) e coloque o e-mail em
  `APP_ADMIN_EMAILS`.
- **Health check:** só `/actuator/health` é público e responde só `{"status":"UP"}` (sem componentes
  nem detalhes); nenhum outro endpoint do Actuator é exposto.

### Passos manuais (uma vez, nesta ordem)

1. **MongoDB Atlas**
   1. Criar a conta e um projeto; criar o cluster **M0** (Free) na **AWS, us-east-1 (N. Virginia)**,
      mesma região dos serviços no Render.
   2. *Database Access → Add New Database User* (senha sem caracteres especiais, ou codificada na URI):
      um usuário por ambiente, com *Specific Privileges* `readWrite` e `dbAdmin` só no banco dele
      (`synapse-forge-homolog` e `synapse-forge`). O `dbAdmin` dá o `collMod` que aplica o JSON Schema.
   3. *Network Access → Add IP Address → Allow access from anywhere* (`0.0.0.0/0`): o Render free não
      tem IP fixo.
   4. *Connect → Drivers*: copiar a string `mongodb+srv://...` e colocar o nome do banco no caminho
      (`...mongodb.net/synapse-forge-homolog?...`). Sem o nome, o Spring usa o banco `test`.
2. **Brevo (e-mail):** criar a conta, verificar o remetente (*Senders*) e gerar a chave em
   *SMTP & API → SMTP*. O login SMTP e a chave viram `SPRING_MAIL_USERNAME` e `SPRING_MAIL_PASSWORD`.
3. **GitHub:** mergear o PR da SYN-89 na `dev` e fazer a release `dev` → `main`. O serviço de produção
   constrói a `main`; sem o `Dockerfile` lá, o primeiro build dele falha.
4. **Render**
   1. Entrar com a conta do GitHub e dar acesso ao repositório `synapse-forge-back-end`.
   2. *New → Blueprint*, escolher o repositório e a branch onde está o `render.yaml`; preencher os
      valores pedidos (`sync: false`) de cada serviço e aplicar. Os dois serviços são criados e fazem
      o primeiro deploy.
   3. Conferir: `https://<serviço>.onrender.com/actuator/health` responde `{"status":"UP"}` e o login
      funciona (em homologação, `gerente@teste.com` / `1234`).
   4. Em cada serviço, *Settings → Deploy Hook*: copiar a URL (é um segredo).
5. **GitHub (repositório do back)**: *Settings → Environments*:
   - `homologacao`: *Deployment branches* = só `dev`; segredo `RENDER_DEPLOY_HOOK_URL` = hook do homolog;
   - `producao`: *Deployment branches* = só `main` (opcional: *Required reviewers* para aprovar cada
     deploy de produção); segredo `RENDER_DEPLOY_HOOK_URL` = hook de produção.
6. **Vercel:** criar o projeto do front e cadastrar `VITE_API_URL` (Production = API de produção,
   Preview = API de homologação), conforme o README do front.
7. **Teste ponta a ponta:** um PR qualquer mergeado na `dev` deve terminar com `deploy · render` verde
   e um deploy novo no painel do Render com o mesmo SHA.

### Limites do plano gratuito

- **Render free:** o serviço dorme depois de 15 min sem acesso e a primeira requisição leva cerca de
  1 min para acordar (abra o `/actuator/health` antes de uma apresentação). São 750 horas de instância
  por mês para a conta inteira, divididas entre os dois serviços; não deixe um "pinger" mantendo os dois
  acordados o mês todo. 512 MB de RAM (a JVM usa 75% para o heap; medido localmente: ~190 MB de
  memória depois de 300 requisições).
- **Atlas M0:** 512 MB de armazenamento, somando os dois bancos e os arquivos do GridFS.
- O limite de tentativas do login (`RateLimitFilter`) fica em memória: zera quando o serviço reinicia
  ou dorme.

---

## 🔐 Segurança e Autenticação

O backend possui autenticação baseada em JWT e controle de acesso por perfil de usuário.

### Roles disponíveis

- **ADMIN** → administração geral (as rotas `/admin/**` exigem, além do papel, que o email esteja em `APP_ADMIN_EMAILS`)
- **GERENTE** → gestão de equipe e operações da loja
- **TECNICO** → operação técnica e produção
- **CLIENTE** → acesso ao cliente

### Recursos de segurança

- autenticação via login, com token JWT de 1 hora assinado com `APP_JWT_SECRET`
- papel e situação do usuário lidos do cadastro a cada requisição: conta apagada, desativada ou rebaixada perde o acesso na hora
- confirmação de email
- recuperação e redefinição de senha
- bloqueio de 15 minutos na conta depois de 5 senhas erradas
- limite de tentativas por IP em login, cadastros e recuperação de senha (429 com `Retry-After`)
- controle de acesso por rota e permissão
- arquivos enviados só são servidos com tipos de uma lista fixa (imagens, modelos 3D e PDF)

---

## 📚 Swagger / OpenAPI

O projeto agora inclui documentação interativa da API com Swagger UI.

### Acesso

- Swagger UI: http://localhost:8081/swagger-ui/index.html
- JSON da API: http://localhost:8081/v3/api-docs

O Swagger também foi configurado para aceitar autenticação JWT via bearer token, permitindo testar endpoints protegidos diretamente pela interface: faça `POST /auth/login`, copie o `access_token` e cole em **Authorize**.

As rotas do Swagger são públicas. Para não expor o mapa da API em produção, desligue com `APP_SWAGGER_ENABLED=false`.

---

## 👤 Funcionalidades do Sistema

### 1) Usuários

O sistema conta com CRUD completo de usuários, incluindo:

- criar usuário
- listar usuários
- buscar por ID
- atualizar dados
- excluir usuário
- cadastro em lote
- alteração de perfil próprio
- confirmação de email
- reset de senha
- gerenciamento de email pendente para troca
- preferências salvas do próprio usuário: tema, idioma e dashboard (período e widgets na ordem escolhida)

### 2) Autenticação e conta

- cadastro de usuários
- login com JWT
- confirmação de acesso por email
- recuperação de senha
- redefinição de senha
- validação de email e senha

### 3) Equipes e gestão operacional

- criação e administração de equipes
- vínculo de usuários a equipes
- convites de equipe
- controle de gerente e membros da equipe

### 4) Pedidos e produção

- criação e gerenciamento de pedidos
- acompanhamento de consumo por pedido
- orçamentos
- misturas
- ordens de pintura
- controle de estoque
- métricas de produção
- eventos e movimentações relacionadas ao processo

### 5) Materiais / Cores / Estoque

- cadastro de materiais
- cadastro de cores
- controle de acabamentos
- gestão de estoque
- ajustes e consumo por pedido

---

## 🧪 Dados de Teste Inseridos automaticamente

Ao iniciar a aplicação, o sistema cria automaticamente usuários e dados fictícios para facilitar testes de desenvolvimento e demonstração. Isso pode ser desligado com `APP_SEED_ENABLED=false` (obrigatório em produção, já que todos usam a mesma senha).

Esses dados foram inseridos para simular cenários reais do sistema, sem depender de um ambiente externo.

### Usuários de teste criados

#### Clientes falsos

- **Alice Silva** — `alice.silva@teste.com`  
  Papel: `CLIENTE`  
  Uso: simula um cliente comum para testar login, visualização de pedidos e fluxo do cliente.

- **Antonio Santos** — `antonio.santos@teste.com`  
  Papel: `CLIENTE`  
  Uso: representa outro cliente de exemplo para validar listagens, filtros e operações que envolvem clientes distintos.

#### Funcionários de teste

- **Funcionario Teste** — `funcionario@teste.com`  
  Papel: `TECNICO`  
  Uso: representa um colaborador técnico para testar manutenção de estoque, produção, pedidos e associação à equipe.

- **Funcionario Teste 2** — `funcionario2@teste.com`  
  Papel: `TECNICO`  
  Uso: simula outro técnico na mesma equipe para testar fluxo de trabalho em equipe e permissões de acesso.

- **Funcionario Teste 3** — `funcionario3@teste.com`  
  Papel: `TECNICO`  
  Uso: serve como terceiro colaborador para validar divisão de tarefas, equipe e relatórios operacionais.

#### Gerente de teste

- **Gerente Teste** — `gerente@teste.com`  
  Papel: `GERENTE`  
  Uso: representa o gestor da equipe, com acesso às operações administrativas e controle da equipe de teste.

### Equipe de teste

O sistema cria uma equipe chamada **Equipe Teste** e associa os usuários técnicos e gerente a ela. Isso ajuda a validar:

- autenticação por perfil
- associação de usuários à equipe
- permissões de gerente e técnicos
- operações que dependem de equipe

### Cores de teste

Também são criadas cores de exemplo como:

- Vermelho Queimado
- Azul Cobalto
- Verde Oliva
- Bege Areia
- Preto Fosco
- Branco Gelo
- Terracota
- Amarelo Mostarda

Essas paletas simulam dados reais usados em orçamentos, misturas e pedidos de pintura.

### Pedidos e materiais do dashboard

Para o dashboard de pedidos (`/pedidos/metricas/por-status` e `/pedidos/metricas/materiais-mais-usados`) ter o que mostrar, a equipe de teste recebe 6 materiais (PLA, PETG, resina, ABS e TPU) e 24 pedidos espalhados pelos últimos 90 dias, em todos os status, com preços calculados como no orçamento e alguns prazos vencidos. A carga usa ids fixos, então reiniciar a aplicação não duplica nada. Desligue só essa parte com `APP_SEED_DASHBOARD_MOCKS=false`.

---

## 🔑 Credenciais de Acesso de Teste

Todos os usuários de teste usam a mesma senha padrão:

```text
1234
```

Exemplo:

- cliente: `alice.silva@teste.com` / `1234`
- gerente: `gerente@teste.com` / `1234`
- técnico: `funcionario@teste.com` / `1234`

> Esses usuários existem para facilitar testes de desenvolvimento e validação do front-end/backend em ambiente local. Eles são criados sempre que `APP_SEED_ENABLED` não for `false`.

---

## ✅ Validação de Dados

O sistema utiliza Bean Validation para garantir integridade das entradas:

- `@NotBlank` → campos obrigatórios
- `@Email` → validação de formato do email
- regras de negócio adicionais em services

Caso os dados sejam inválidos, a API retorna erro automaticamente.

---

## 🌐 Endpoints principais

A lista completa, com parâmetros e modelos de requisição/resposta, está no Swagger UI (`/swagger-ui/index.html`), que é gerado a partir do código e fica sempre atualizado. Visão geral por recurso:

| Base | O que faz |
|---|---|
| `/auth` | cadastro (cliente e gerente), login, confirmação de email, esqueci/redefinir senha |
| `/users` | CRUD de usuários, lote, perfil próprio (`/me`), preferências (`/me/preferencias`), busca de clientes, troca de email |
| `/equipes` | criar/editar/excluir equipe, `/minha`, integrantes, convites, foto e banner |
| `/pedidos` | CRUD de pedidos, avanço/regressão de status, cancelamento, objeto 3D e ordem de serviço |
| `/orcamentos` | cálculo, criação (com arquivos), aprovação/rejeição, objeto 3D e imagens |
| `/consumos-pedido` | registro e consulta do consumo de insumos por pedido |
| `/estoque` | entrada, ajuste, saldo, movimentos e alertas de estoque |
| `/estoque/metricas` | consumo por insumo/etapa, custo por pedido, média semanal, insumos críticos |
| `/materiais` | CRUD de materiais |
| `/cores` | CRUD de cores |
| `/misturas` | CRUD de misturas |
| `/ordens-pintura` | CRUD de ordens de pintura e avanço de etapa |
| `/evento` | CRUD de eventos da agenda e busca por mês |
| `/admin` | gestão de usuários e pedidos (restrito a administradores autorizados) |

---

## 🧩 Modelagem do Sistema

Esta seção apresenta os diagramas que representam as funcionalidades do Synapse Forge e os principais fluxos do sistema. A modelagem ajuda a entender as interações entre os usuários e a aplicação e a comunicação entre as camadas durante as operações.

### 1. Diagrama de Casos de Uso

O diagrama apresenta as funcionalidades do Synapse Forge e os atores que interagem com o sistema, de acordo com as permissões de cada perfil no back-end.

- **Visitante**: quem ainda não entrou. Cria a conta (como cliente, ou como gerente junto com a loja) e recupera a senha.
- **Usuário** (ator abstrato): o que todo usuário autenticado faz, isto é, entrar, gerenciar o perfil e consultar as notificações do sino.
- **Cliente**: acompanha os próprios pedidos (de qualquer loja) e responde convites para entrar numa equipe.
- **Técnico**: trabalha nos dados da própria equipe, como pedidos e etapas de produção, comentários, ordens de pintura, cores e misturas, materiais e estoque e agenda.
- **Gerente**: é um técnico com mais permissões (generalização Gerente → Técnico). Aprova ou rejeita orçamentos, gerencia a equipe e os convites e emite a ordem de serviço em PDF.
- **Admin**: painel de administração, com usuários (papel e ativação) e pedidos de todas as lojas.

Cliente, Técnico e Admin são especializações de Usuário. O Gerente também é, por herdar do Técnico.

![Diagrama de Casos de Uso](docs/synapse-forge-casos-de-uso.png)

A fonte editável está em [`docs/synapse-forge-casos-de-uso.drawio`](docs/synapse-forge-casos-de-uso.drawio) (abre no [draw.io](https://app.diagrams.net)).

### 2. Diagrama de Sequência: Criação de Pedido

Só técnico, gerente e admin criam pedidos. O pedido pode chegar em JSON, quando não há arquivos, ou em `multipart/form-data`, com o modelo 3D e as imagens de referência.

No caminho multipart, o controller valida o pedido **antes** de gravar os arquivos no GridFS (`prepararParaCriacao`), para que uma requisição recusada não deixe arquivo órfão. Em seguida, `criar` repete a validação: resolve a equipe do usuário logado (403 `SEM_EQUIPE` se ele não tiver equipe) e confere se o cliente informado é da equipe. Depois define o status inicial `MODELAGEM` e as datas e salva o pedido no MongoDB.

```mermaid
sequenceDiagram
    actor Usuario as Técnico / Gerente
    participant Front as Frontend
    participant Controller as PedidoController
    participant Service as PedidoService
    participant GridFS
    participant DB as MongoDB

    Usuario->>Front: Preenche e envia o pedido
    alt Sem arquivos (JSON)
        Front->>Controller: POST /pedidos
        Controller->>Service: toEntity(dto, usuarioId)
    else Com arquivos (multipart/form-data)
        Front->>Controller: POST /pedidos
        Controller->>Service: toEntity(...)
        Controller->>Service: prepararParaCriacao(pedido)
        Note over Service: equipe do usuário e cliente da equipe
        Controller->>GridFS: Grava modelo 3D e imagens
        GridFS-->>Controller: Ids dos arquivos
    end
    Controller->>Service: criar(pedido)
    Service->>Service: prepararParaCriacao(pedido)
    Service->>Service: Status MODELAGEM, criadoEm e atualizadoEm
    Service->>DB: save(pedido)
    DB-->>Service: Pedido salvo
    Service-->>Controller: Pedido
    Controller-->>Front: PedidoResponseDTO
    Front-->>Usuario: Pedido aparece na lista
```

### 3. Diagrama de Sequência: Edição de Pedido

A edição existe em JSON (`PUT /pedidos/{id}`) e em `multipart/form-data`, quando há arquivo novo ou removido. O diagrama mostra o caminho multipart.

Antes de gravar qualquer arquivo, `validarAtualizacao` busca o pedido para edição. Cliente não edita, o usuário precisa ter equipe e o pedido precisa ser da equipe dele. A mesma validação confere o cliente informado. Depois disso, os arquivos novos vão para o GridFS e `atualizarComArquivos` aplica os dados recebidos ao pedido (os campos são substituídos pelo que chegou, inclusive vazios) e salva. Os arquivos antigos que foram substituídos ou removidos só são apagados do GridFS depois que o pedido é salvo.

A etapa de produção não muda por aqui: ela tem endpoints próprios (`/status`, `/status/regredir` e `/cancelar`).

```mermaid
sequenceDiagram
    actor Usuario as Técnico / Gerente
    participant Front as Frontend
    participant Controller as PedidoController
    participant Service as PedidoService
    participant GridFS
    participant DB as MongoDB

    Usuario->>Front: Edita o pedido
    Front->>Controller: PUT /pedidos/{id} (multipart/form-data)
    Controller->>Service: toEntity(...)
    Controller->>Service: validarAtualizacao(id, usuarioId, role, dados)
    Service->>DB: Busca o pedido por id e equipe
    DB-->>Service: Pedido
    Note over Service: recusa CLIENTE, sem equipe ou pedido de outra equipe

    opt Arquivos novos
        Controller->>GridFS: Grava os arquivos novos
        GridFS-->>Controller: Ids dos arquivos
    end

    Controller->>Service: atualizarComArquivos(id, usuarioId, role, dados, novos ids, remoções)
    Service->>DB: Busca o pedido por id e equipe
    Service->>Service: Aplica os dados e as referências de arquivo
    Service->>DB: save(pedido)
    DB-->>Service: Pedido atualizado

    opt Arquivos substituídos ou removidos
        Service->>GridFS: Apaga os arquivos antigos
    end

    Service-->>Controller: Pedido atualizado
    Controller-->>Front: PedidoResponseDTO
    Front-->>Usuario: Mostra os dados atualizados
```

### 4. Considerações sobre a Modelagem

O diagrama de casos de uso dá a visão geral das funcionalidades e dos perfis, e os diagramas de sequência detalham a comunicação entre os componentes na criação e na edição de pedidos. Os diagramas precisam acompanhar o código: quando uma permissão ou um fluxo mudar, atualize o `.drawio` (e exporte o PNG em tema claro) e os diagramas desta seção.

---

## 📝 Observações Finais

Este backend foi pensado para funcionar em ambiente de desenvolvimento local, com dados de teste já pré-carregados para demonstrar o comportamento real da aplicação. Isso acelera a validação de fluxo de autenticação, equipe, pedidos e produção sem a necessidade de cadastrar tudo manualmente.


