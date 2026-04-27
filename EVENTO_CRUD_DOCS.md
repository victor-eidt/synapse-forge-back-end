# CRUD de Eventos - Documentação

## 📍 Informações Gerais

- **Porta**: `8081`
- **Base URL**: `http://localhost:8081/evento`
- **Banco de Dados**: MongoDB (local)

---

## 📋 Endpoints

### 1. **Registrar Novo Evento** (POST)
```
POST http://localhost:8081/evento/registrar
```

**Body (JSON):**
```json
{
  "userId": "user123",
  "nome": "Reunião de Sprint",
  "data": "2026-04-10T14:30:00",
  "descricao": "Reunião de planejamento da sprint 5"
}
```

**Response:**
```json
{
  "id": "507f1f77bcf86cd799439011",
  "userId": "user123",
  "nome": "Reunião de Sprint",
  "data": "2026-04-10T14:30:00",
  "descricao": "Reunião de planejamento da sprint 5",
  "criadoEm": "2026-04-07T16:03:00",
  "atualizadoEm": "2026-04-07T16:03:00"
}
```

---

### 2. **Listar Todos os Eventos** (GET)
```
GET http://localhost:8081/evento
```

**Response:**
```json
[
  {
    "id": "507f1f77bcf86cd799439011",
    "userId": "user123",
    "nome": "Reunião de Sprint",
    "data": "2026-04-10T14:30:00",
    "descricao": "Reunião de planejamento da sprint 5",
    "criadoEm": "2026-04-07T16:03:00",
    "atualizadoEm": "2026-04-07T16:03:00"
  }
]
```

---

### 3. **Buscar Evento por ID** (GET)
```
GET http://localhost:8081/evento/{id}
```

**Exemplo:**
```
GET http://localhost:8081/evento/507f1f77bcf86cd799439011
```

---

### 4. **Buscar Eventos por Usuário** (GET)
```
GET http://localhost:8081/evento/usuario/{userId}
```

**Exemplo:**
```
GET http://localhost:8081/evento/usuario/user123
```

---

### 5. **Atualizar Evento** (PUT)
```
PUT http://localhost:8081/evento/{id}
```

**Exemplo:**
```
PUT http://localhost:8081/evento/507f1f77bcf86cd799439011
```

**Body (JSON):**
```json
{
  "userId": "user123",
  "nome": "Reunião de Sprint - ATUALIZADA",
  "data": "2026-04-10T15:00:00",
  "descricao": "Reunião com agenda revisada"
}
```

---

### 6. **Deletar Evento** (DELETE)
```
DELETE http://localhost:8081/evento/{id}
```

**Exemplo:**
```
DELETE http://localhost:8081/evento/507f1f77bcf86cd799439011
```

---

### 7. **Criar Múltiplos Eventos** (POST - Batch)
```
POST http://localhost:8081/evento/batch
```

**Body (JSON):**
```json
[
  {
    "userId": "user123",
    "nome": "Evento 1",
    "data": "2026-04-10T14:30:00",
    "descricao": "Primeiro evento"
  },
  {
    "userId": "user123",
    "nome": "Evento 2",
    "data": "2026-04-11T10:00:00",
    "descricao": "Segundo evento"
  }
]
```

---

## 📊 Atributos do Evento

| Atributo | Tipo | Obrigatório | Descrição |
|----------|------|-------------|-----------|
| `id` | String | Não | ID único (gerado automaticamente) |
| `userId` | String | **Sim** | ID do usuário proprietário do evento |
| `nome` | String | **Sim** | Nome/título do evento |
| `data` | LocalDateTime | **Sim** | Data e hora do evento (formato: yyyy-MM-ddTHH:mm:ss) |
| `descricao` | String | Não | Descrição opcional do evento |
| `criadoEm` | LocalDateTime | Não | Data de criação (gerada automaticamente) |
| `atualizadoEm` | LocalDateTime | Não | Data da última atualização (gerada automaticamente) |

---

## 🏗️ Estrutura do Projeto

```
src/main/java/synapseforge/crud/
├── infrastructure/
│   ├── entity/
│   │   └── Evento.java              # Entidade do MongoDB
│   └── repository/
│       └── EventoRepository.java    # Interface de acesso ao BD
├── DTO/
│   └── Evento/
│       ├── EventoRequestDTO.java    # DTO para requisições
│       └── EventoResponseDTO.java   # DTO para respostas
├── service/
│   └── EventoService.java           # Lógica de negócio
└── controller/
    └── EventoController.java        # Endpoints REST
```

---

## ✅ Validações

- `userId`: Não pode estar vazio
- `nome`: Não pode estar vazio
- `data`: Campo obrigatório

---

## 🔧 Tecnologias Utilizadas

- **Framework**: Spring Boot
- **Banco de Dados**: MongoDB
- **Build Tool**: Maven
- **Java Version**: 17+
- **Validações**: Jakarta Validation
- **Serialização**: JSON (Jackson)

---

## 🚀 Como Testar

1. Inicie a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```

2. Use ferramentas como **Postman**, **Insomnia** ou **curl** para testar os endpoints

3. Exemplo com curl:
   ```bash
   curl -X POST http://localhost:8081/evento/registrar \
     -H "Content-Type: application/json" \
     -d '{
       "userId": "user123",
       "nome": "Meu Evento",
       "data": "2026-04-10T14:30:00",
       "descricao": "Descrição do evento"
     }'
   ```


