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
| `APP_ENV` | Perfil Spring: `dev` ou `prod`; para rodar na máquina, `dev,local` (ver abaixo) |
| `CONFIG_SERVER_URL` | URL base do Config Server, por exemplo `http://config-server:8888` na rede do ambiente |

No repositório de configurações, o arquivo `identity-<perfil>.yaml` precisa definir, entre outros:

- `app.jwt.issuer`: emissor dos tokens. Deve coincidir com o `issuer-uri` configurado no gateway.
- `app.jwt.audience`: público dos tokens. Deve coincidir com `gateway.security.jwt-audience` no gateway.
- `app.cors.allowed-origins`: origens do frontend aceitas pelo CORS, separadas por vírgula. Não há variável de ambiente para isso.
- Conexão com PostgreSQL e Redis. Os segredos ficam criptografados no repositório de configurações.

Se `app.jwt.audience` não estiver definido, a aplicação não inicia.

### Rodando na máquina (IntelliJ)

Use `APP_ENV=dev,local`. O config server entrega `identity-dev.yaml` (banco, Redis, JWT, CORS) e
`application-local.yaml`, que, por ser o último perfil, troca os endereços dos serviços
(`universum.services.*.service`) pelas URIs públicas de desenvolvimento. Para conferir o que chega:
`<CONFIG_SERVER_URL>/identity/dev,local`.

- Banco e Redis de dev ficam no Tailscale: a máquina precisa estar conectada.
- Chamadas a outros serviços passam pelo gateway público e precisam de um JWT válido.
- Só `local` não basta: não existe `identity-local.yaml`, e o serviço subiria sem banco nem JWT.

## Rede

O serviço entra na rede `universum-<ambiente>` com o alias `identity`, e o alias `config-server`
fica disponível para ele na mesma rede. Não é publicado no host. A porta interna é `8080`.

## Execução

```bash
./mvnw clean verify                    # Windows: .\mvnw.cmd clean verify
docker compose config --quiet          # valida o compose
APP_ENV=dev CONFIG_SERVER_URL=http://config-server:8888 docker compose up -d --build
```

Os testes de integração de autenticação exigem PostgreSQL e Redis conforme o perfil de teste.
