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

---

## 🔐 Segurança e Autenticação

O backend possui autenticação baseada em JWT e controle de acesso por perfil de usuário.

### Roles disponíveis

- **ADMIN** → administração geral
- **GERENTE** → gestão de equipe e operações da loja
- **TECNICO** → operação técnica e produção
- **CLIENTE** → acesso ao cliente

### Recursos de segurança

- autenticação via login
- confirmação de email
- recuperação e redefinição de senha
- controle de acesso por rota e permissão
- proteção de endpoints sensíveis com JWT

---

## 📚 Swagger / OpenAPI

O projeto agora inclui documentação interativa da API com Swagger UI.

### Acesso

- Swagger UI: http://localhost:8081/swagger-ui/index.html
- JSON da API: http://localhost:8081/v3/api-docs

O Swagger também foi configurado para aceitar autenticação JWT via bearer token, permitindo testar endpoints protegidos diretamente pela interface.

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

Ao iniciar a aplicação, o sistema cria automaticamente usuários e dados fictícios para facilitar testes de desenvolvimento e demonstração.

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

> Esses usuários existem para facilitar testes de desenvolvimento e validação do front-end/backend em ambiente local.

---

## ✅ Validação de Dados

O sistema utiliza Bean Validation para garantir integridade das entradas:

- `@NotBlank` → campos obrigatórios
- `@Email` → validação de formato do email
- regras de negócio adicionais em services

Caso os dados sejam inválidos, a API retorna erro automaticamente.

---

## 🌐 Endpoints principais

### Autenticação

- `POST /auth/cadastro`
- `POST /auth/login`
- `GET /auth/confirmar-email/{token}`
- `POST /auth/esqueci-senha`
- `POST /auth/redefinir-senha`

### Usuários

- `GET /users`
- `GET /users/{id}`
- `POST /users`
- `PUT /users/{id}`
- `DELETE /users/{id}`

### Equipes

- `GET /equipes`
- `GET /equipes/minha`
- `POST /equipes`
- `GET /equipes/convites/**`

### Pedidos e produção

- `GET /pedidos`
- `POST /pedidos`
- `PUT /pedidos/{id}`
- `DELETE /pedidos/{id}`

### Estoque, materiais e cores

- `GET /estoque`
- `GET /materiais`
- `GET /cores`
- `GET /misturas`
- `GET /orcamentos`

---

## 📝 Observações Finais

Este backend foi pensado para funcionar em ambiente de desenvolvimento local, com dados de teste já pré-carregados para demonstrar o comportamento real da aplicação. Isso acelera a validação de fluxo de autenticação, equipe, pedidos e produção sem a necessidade de cadastrar tudo manualmente.


