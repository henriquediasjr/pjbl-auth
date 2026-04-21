# PjBL-Auth — API de Autenticação JWT

API REST de autenticação em **Java puro** (sem frameworks) com JWT, BCrypt e persistência em JSON.

---

## 📋 Pré-requisitos

| Ferramenta | Versão mínima | Download |
|------------|--------------|---------|
| Java (JDK) | 8+ | [adoptium.net](https://adoptium.net) |
| Maven | 3.6+ | [maven.apache.org](https://maven.apache.org/download.cgi) |

Verifique as instalações:
```bash
java -version
mvn -version
```

---

## ⚙️ Configuração obrigatória — variável de ambiente

A chave secreta JWT **nunca deve estar no código**. Defina-a antes de subir o servidor:

### Linux / macOS
```bash
export JWT_SECRET="minha_chave_super_secreta_com_32_chars!!"
```

### Windows (CMD)
```cmd
set JWT_SECRET=minha_chave_super_secreta_com_32_chars!!
```

### Windows (PowerShell)
```powershell
$env:JWT_SECRET = "minha_chave_super_secreta_com_32_chars!!"
```

> ⚠️ A chave deve ter **pelo menos 32 caracteres** (256 bits) para o algoritmo HS256.

---

## 🚀 Como rodar

### 1. Instalar dependências e compilar
```bash
cd BackEnd/PjBL-Auth
mvn package
```

### 2. Iniciar o servidor
```bash
java -cp "target/PjBL-Auth-1.0-SNAPSHOT.jar:target/libs/*" com.pucpr.Main
```
> **Windows:** use `;` no lugar de `:` no classpath:
> ```cmd
> java -cp "target\PjBL-Auth-1.0-SNAPSHOT.jar;target\libs\*" com.pucpr.Main
> ```

Saída esperada:
```
Servidor iniciado na porta 8080...
```

### 3. Abrir o Frontend
Abra diretamente no navegador:
```
Frontend/index.html
```
Ou use uma extensão como **Live Server** no VS Code.

---

## 🌐 Endpoints da API

Base URL: `http://localhost:8080`

### POST `/api/auth/register` — Cadastro

**Body JSON:**
```json
{
  "name": "João Silva",
  "email": "joao@example.com",
  "password": "Senha123!",
  "role": "PACIENTE"
}
```
> `role` é opcional. Padrão: `"PACIENTE"`. Outros valores: `"MEDICO"`, `"ADMIN"`.

**Resposta 201:**
```json
{ "mensagem": "Usuário cadastrado com sucesso." }
```

**Resposta 400 (email duplicado):**
```json
{ "erro": "E-mail já cadastrado." }
```

---

### POST `/api/auth/login` — Login

**Body JSON:**
```json
{
  "email": "joao@example.com",
  "password": "Senha123!"
}
```

**Resposta 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWI..."
}
```

**Resposta 401 (credenciais inválidas):**
```json
{ "erro": "E-mail ou senha inválidos." }
```

> ⚠️ A mensagem de erro é **intencional e genérica** — não revela se o problema é no e-mail ou na senha (proteção contra enumeração de usuários).

---

### GET `/api/protected` — Rota protegida (requer token)

**Header obrigatório:**
```
Authorization: Bearer <TOKEN>
```

**Resposta 200:**
```json
{ "mensagem": "Acesso autorizado.", "email": "joao@example.com" }
```

**Resposta 401 (sem token ou token inválido):**
```json
{ "erro": "Token não fornecido." }
```
```json
{ "erro": "Token inválido ou expirado." }
```

---

### POST `/api/auth/logout` — Logout

Não requer body. Retorna 200. O token é apagado pelo frontend (JWT é stateless).

---

## 🧪 Testando com curl

### Cadastro
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Maria","email":"maria@test.com","password":"Senha123!","role":"MEDICO"}'
```

### Login (guarda o token)
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"maria@test.com","password":"Senha123!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

echo "Token: $TOKEN"
```

### Acessar rota protegida
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/protected
```

### Testar token inválido
```bash
curl -H "Authorization: Bearer tokenfalso" http://localhost:8080/api/protected
```

---

## 🗂️ Estrutura do projeto

```
PjBL-Auth/
├── pom.xml                          # Dependências Maven
├── usuarios.json                    # Banco de dados (criado automaticamente)
└── src/main/java/com/pucpr/
    ├── Main.java                    # Servidor HTTP + registro de rotas
    ├── model/
    │   └── Usuario.java             # Entidade usuário
    ├── repository/
    │   └── UsuarioRepository.java   # Persistência em JSON (Jackson)
    ├── service/
    │   └── JwtService.java          # Geração e validação de JWT (JJWT)
    └── handlers/
        └── AuthHandler.java         # Handlers HTTP (register/login/protected/logout)
```

---

## 🔐 Decisões de segurança

| Prática | Por quê |
|---------|---------|
| `BCrypt.hashpw(senha, BCrypt.gensalt(12))` | Hash com salt aleatório — protege contra rainbow tables |
| `BCrypt.checkpw(senha, hash)` | Comparação segura em tempo constante — nunca `.equals()` |
| Mensagem genérica no login | Não vaza se o e-mail existe ou não (proteção contra enumeração) |
| `JWT_SECRET` via variável de ambiente | Segredo nunca comitado no código |
| Token expira em 15 min | Limita janela de ataque em caso de roubo |
| Verificação de assinatura no JWT | Impede tokens forjados (`alg: none` rejeitado pela biblioteca) |

---

## 📦 Dependências

| Biblioteca | Versão | Uso |
|-----------|--------|-----|
| `jjwt-api` / `jjwt-impl` / `jjwt-jackson` | 0.12.5 | Geração e validação de JWT |
| `jackson-databind` | 2.17.0 | Serialização JSON (usuarios.json) |
| `jbcrypt` | 0.4 | Hash de senha com BCrypt |

---

## ❓ Problemas comuns

### `IllegalStateException: JWT_SECRET não definida`
Você esqueceu de exportar a variável antes de rodar:
```bash
export JWT_SECRET="sua_chave_aqui_com_minimo_32_chars"
```

### `BindException: Endereço já em uso`
A porta 8080 está ocupada. Veja o que está usando:
```bash
# Linux
ss -tlnp | grep 8080

# macOS
lsof -i :8080
```

### `BUILD FAILURE` no Maven
Verifique se o Java está instalado:
```bash
java -version   # deve ser 8 ou superior
mvn -version
```

### Frontend não conecta
Verifique se o servidor está rodando e se a URL está correta:
- `index.html` usa `http://localhost:8080/api`
- O backend deve estar rodando na porta 8080
