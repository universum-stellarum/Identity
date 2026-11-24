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

Três camadas, com as dependências sempre apontando para dentro:

```
infrastructure ──► application ──► domain
```

```
domain/
  model/            User, RefreshToken, SigningKey: entidades JPA (Lombok, Hibernate, auditoria)
  exception/        exceções de negócio
  event/            SigningKeyRotated
application/
  abstractions/     interfaces que a aplicação precisa (UserRepository, AccessTokenIssuer, TokenHasher...)
  usecases/
    auth/           login, refresh e logout
    users/          cadastro, consulta, alteração e remoção de usuários
    keys/           geração, consulta e rotação da chave de assinatura
      request/      entrada dos casos de uso do assunto (com Bean Validation)
      response/     saída dos casos de uso do assunto
infrastructure/
  endpoints/        controllers REST
  web/              tratamento de erros, usuário autenticado (@CurrentUserId), OpenAPI
  persistence/      repositórios Spring Data (JpaUserRepository...), que implementam as abstrações
  security/         segurança HTTP, JWT, chaves RSA, SHA-256 e geração dos refresh tokens
  config/           Clock, PasswordEncoder e auditoria JPA
```

- **Casos de uso:** uma classe por caso, `@Service` com `@Transactional` e um método `execute`. Os
  controllers chamam os casos de uso diretamente e usam os requests e responses deles como corpo
  HTTP, sem DTOs intermediários.
- **Validação:** os requests carregam Bean Validation (`@NotBlank`, `@Email`, `@Size`) e os
  controllers usam `@Valid`; erros viram 400 com o mapa `errors`. As entidades ainda verificam os
  próprios invariantes como última linha de defesa.
- **Transações:** login e refresh usam `noRollbackFor = BusinessException.class`, para que a tentativa
  falha e a revogação das sessões em caso de reuso sejam gravadas mesmo com a exceção.
- **Nomes:** a interface fica com o nome limpo e a implementação leva a tecnologia
  (`UserRepository` ← `JpaUserRepository`, `TokenHasher` ← `Sha256TokenHasher`), como no padrão do
  Java (`List` ← `ArrayList`).
- **Repositórios:** os repositórios Spring Data estendem as abstrações e as implementam diretamente:
  `findById`, `findAll(Pageable)`, `save`, `delete` e as consultas derivadas do nome são gerados pelo
  Spring; bloqueios e queries explícitas são redeclarados no repositório.
- **Regras de arquitetura:** o `ArchitectureTest` (ArchUnit) quebra o build se a aplicação depender da
  infraestrutura ou de tecnologias dela (web, JPA, JWT), ou se um controller usar repositórios ou
  entidades em vez dos casos de uso.

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
