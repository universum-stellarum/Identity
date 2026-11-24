# Antares

> **Antares** é uma supergigante vermelha, uma das estrelas mais brilhantes do céu do hemisfério sul,
> no coração da constelação do Escorpião. O nome vem do grego *anti-Ares*, "rival de Ares" (Marte):
> sua cor avermelhada lembra a do planeta. Se estivesse no lugar do Sol, sua superfície ultrapassaria
> a órbita de Marte. Está a cerca de 550 anos-luz da Terra.

Serviço de **identidade** da API Universum. Cadastra usuários, autentica com e-mail e senha,
emite os tokens de acesso e expõe a chave pública usada pelo gateway e pelos demais serviços
para validar esses tokens.

Java 21 · Spring Boot 4.1.2 · Spring Security · Spring Data JPA · PostgreSQL · Redis · Flyway · Maven.

## Responsabilidades

- Cadastro, leitura, atualização e remoção de usuários (`/identity/api/v1/users`).
- Login, renovação e logout (`/identity/api/v1/auth`).
- Assinatura dos access tokens JWT com RS256. Somente este serviço tem a chave privada.
- Publicação das chaves públicas em `/.well-known/jwks.json`.
- Bloqueio temporário de conta após tentativas de senha inválidas.

O gateway (Polaris, no repositório `Polaris`) valida os access tokens localmente, pela chave
pública, e não chama este serviço a cada requisição. Chamadas ao identity acontecem só em
login, renovação e logout.

## Arquitetura

Arquitetura limpa, em quatro camadas. As dependências apontam sempre para dentro:

```
presentation ──┐
               ├──► application ──► domain
infrastructure ┘
```

| Camada | Pacote | Conteúdo |
| --- | --- | --- |
| Domínio | `domain` | Entidades e regras (`User`, `RefreshToken`, `SigningKey`), exceções de negócio e eventos. Só depende do padrão JPA (`jakarta.persistence`). |
| Aplicação | `application` | Um caso de uso por classe (`usecase`), os ports que eles precisam (`port`) e os DTOs de entrada e saída (`dto`). Sem Spring. |
| Infraestrutura | `infrastructure` | Implementação dos ports: repositórios Spring Data (`persistence`), JWT, BCrypt e SHA-256 (`security`), eventos e configuração do Spring (`config`). |
| Apresentação | `presentation` | Controllers REST, requests e responses HTTP, tratamento de erros e OpenAPI. |

- Os modelos do domínio são as próprias entidades JPA, mapeados só com anotações do padrão
  (`jakarta.persistence`): nada de Hibernate ou Spring Data no domínio. `created_at`/`updated_at`
  são preenchidos por `@PrePersist`/`@PreUpdate`. Os repositórios Spring Data ficam na
  infraestrutura, atrás dos ports da aplicação.
- Os casos de uso não têm anotações. O `UseCaseConfig` os registra pelo pacote, e cada um recebe
  pelo construtor os adapters dos seus ports.
- A transação é o port `UnitOfWork`. Uma `DomainException` **commita** o que o caso de uso já
  gravou e depois é relançada: login com senha errada mantém o contador de tentativas e reuso de
  refresh token mantém a revogação das sessões. Qualquer outra exceção faz rollback.
- Lombok é usado em todas as camadas, inclusive no domínio (`@Getter` e construtor privado; sem
  `@Setter`/`@Data`, para que o estado só mude pelos métodos de negócio). Como atua só na
  compilação, não cria dependência em tempo de execução.
- O `ArchitectureTest` (ArchUnit) quebra o build se alguma dependência apontar para fora, ou se o
  domínio ou a aplicação passarem a depender de framework.

## Endpoints

| Método | Caminho | Acesso | Descrição |
| --- | --- | --- | --- |
| `POST` | `/identity/api/v1/users/register` | público | Cadastro de usuário |
| `POST` | `/identity/api/v1/auth/login` | público | Retorna access token e refresh token |
| `POST` | `/identity/api/v1/auth/refresh` | público | Rotaciona o refresh token e emite novo par |
| `POST` | `/identity/api/v1/auth/logout` | público | Revoga o refresh token informado |
| `GET` | `/.well-known/jwks.json` | público | Chave pública para validação dos JWT |
| `GET` | `/identity/api/v1/users/me` | autenticado | Dados do usuário do token |
| `PUT` | `/identity/api/v1/users/me` | autenticado | Atualiza o próprio usuário |
| `GET` | `/identity/api/v1/users` | autenticado | Lista usuários |
| `GET` / `PUT` / `DELETE` | `/identity/api/v1/users/{id}` | autenticado | Consulta, altera ou remove um usuário |
| `POST` | `/identity/api/v1/auth/keys` | autenticado | Gerenciamento de chaves de assinatura |
| `GET` | `/health` | público | Health check |

## Tokens

- **Access token (JWT, RS256):** válido por 15 minutos. Claims: `sub` (id do usuário), `iss`,
  `aud`, `iat`, `exp`, `email`, `name` e `scope` (papéis).
- **Refresh token:** valor aleatório, válido por 7 dias. O banco guarda somente o hash SHA-256.
- **Rotação:** cada renovação revoga o refresh token usado e emite um novo.
- **Reuso:** apresentar um refresh token já revogado revoga todas as sessões do usuário.
- **Sessão única:** um novo login revoga os refresh tokens anteriores do mesmo usuário.
- **Logout:** revoga o refresh token. O access token expira sozinho em até 15 minutos.

## Configuração

Os valores específicos de cada ambiente vêm do Config Server (Aldebaran). A aplicação precisa de:

| Variável | Uso |
| --- | --- |
| `APP_ENV` | Perfil Spring: `dev` ou `prod` (também na máquina local) |
| `CONFIG_SERVER_URL` | URL base do Config Server, por exemplo `http://config-server:8888` na rede do ambiente |

No repositório de configurações, o arquivo `identity-<perfil>.yaml` precisa definir, entre outros:

- `app.jwt.issuer`: emissor dos tokens. Deve coincidir com o `issuer-uri` configurado no gateway.
- `app.jwt.audience`: público dos tokens. Deve coincidir com `gateway.security.jwt-audience` no gateway.
- Conexão com PostgreSQL e Redis. Os segredos ficam criptografados no repositório de configurações.

Se `app.jwt.audience` não estiver definido, a aplicação não inicia.

CORS não fica aqui: é aplicado pelo gateway, que é o único ponto de entrada
(`gateway.cors.allowed-origins` em `gateway-<perfil>.yaml`).

### Rodando na máquina (IntelliJ)

Use `APP_ENV=dev` e `CONFIG_SERVER_URL` nas variáveis de ambiente da configuração de execução.
Os endereços dos outros serviços (`universum.services.*.url`) já são as URIs públicas, então
funcionam igual no servidor e na máquina.

- Banco e Redis de dev ficam no Tailscale: a máquina precisa estar conectada.
- Chamadas a outros serviços passam pelo gateway e precisam de um JWT válido.

## Rede

O serviço entra na rede `universum-<ambiente>` com o alias `identity`, e o alias `config-server`
fica disponível para ele na mesma rede. Não é publicado no host. A porta interna é `8080`.

## Execução

```bash
./mvnw clean verify                    # Windows: .\mvnw.cmd clean verify
docker compose config --quiet          # valida o compose
APP_ENV=dev CONFIG_SERVER_URL=http://config-server:8888 docker compose up -d --build
```

Os testes não dependem do Config Server nem do PostgreSQL: usam H2 em modo PostgreSQL e um
`application.yaml` próprio em `src/test/resources`, com os valores de JWT do repositório
`configurations`. As funções do PostgreSQL usadas pelas migrations ficam emuladas em
`H2PostgresFunctions`.
