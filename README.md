# 🚀 Synapse Forge - Backend
## 🧠 Arquitetura

O projeto foi estruturado utilizando uma arquitetura em camadas, seguindo boas práticas de desenvolvimento:

- **Controller** → Responsável por expor os endpoints da API  
- **Service** → Contém as regras de negócio  
- **Repository** → Responsável pelo acesso ao banco de dados  
- **DTO (Data Transfer Object)** → Controla os dados de entrada e saída  
- **Entity** → Representa os dados persistidos  

Essa separação garante melhor organização, manutenção e escalabilidade do sistema.

---

## ⚙️ Tecnologias Utilizadas

- Java 17  
- Spring Boot  
- MongoDB  
- Spring Data MongoDB  
- Lombok  
- Bean Validation  
- Maven  

---

## 🗄️ Banco de Dados

O sistema utiliza o MongoDB, um banco de dados NoSQL orientado a documentos.

### Configuração:

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/synapse-forge
server.port=8081
```
---

## 👤 Funcionalidade Implementada
### CRUD de Usuários

O sistema atualmente possui um CRUD completo de usuários, permitindo:

- Criar usuários
- Listar usuários
- Buscar usuário por ID
- Atualizar usuário
- Deletar usuário
- Inserir múltiplos usuários (batch)

---

## 📄 Estrutura do Usuário
A entidade de usuário contém os seguintes campos:

- id → Identificador único
- nome → Nome do usuário
- email → Email do usuário
- senha → Senha do usuário
- role → Tipo de usuário

---

## 🔐 Tipos de Usuário (Role)
- ADMIN → Administrador
- GERENTE → Gerente
- TECNICO → Funcionário técnico
- CLIENTE → Cliente

---

## 🔄 DTO (Data Transfer Object)

Foram implementados DTOs para separar a entrada e saída de dados:

- UserRequestDTO → Dados recebidos pela API
- UserResponseDTO → Dados retornados pela API

⚠️ A senha do usuário não é retornada nas respostas, garantindo maior segurança.

---

## ✅ Validação de Dados

O sistema utiliza Bean Validation para garantir integridade dos dados:

- @NotBlank → Campos obrigatórios
- @Email → Validação de formato de email

Caso os dados sejam inválidos, a API retorna erro automaticamente.

---

## 🌐 Endpoints da API

- BASE URL: http://localhost:8081/users

- Criação de Usuário (POST)
- Criação em Lote de Usuário (POST)
- Buscar (GET)
- Buscar por Id (GET)
- Atualizar Usuário (PUT)
- Remover usuário (DELETE)


