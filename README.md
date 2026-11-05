# Radar CiudadLab · API

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-green?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-blue?style=flat-square&logo=postgresql)

Backend do Radar, ferramenta interna da [CiudadLab](https://ciudadlab.com.br) com duas frentes:

* **Acompanhamento dos sites entregues:** puxa os dados do Google Analytics 4 de cada site, guarda o histórico e gera leituras semanais e mensais. É a base do [acompanhamento mensal](https://ciudadlab.com.br/radar) oferecido aos clientes.
* **Funil de prospecção:** importa os leads da ferramenta de busca que eu uso, calcula um score e organiza o funil comercial em kanban.

Projeto desenvolvido por mim. O frontend em Next.js fica num repositório separado.

## O que o sistema faz

**Analytics**
* Coleta diária dos dados do GA4 de cada site, às 6h, além de sincronização manual por site ou de todos
* Métricas diárias, quebras por dimensão e usuários por período, comparando com o período anterior
* Leituras automáticas do site (insights) e status de acesso ao GA de cada propriedade
* Controle do consumo de cota da Google Analytics Data API

**Leads**
* Importação de CSV: linhas inválidas são puladas e listadas no relatório da importação
* Normalização de telefone (libphonenumber), texto e endereço
* Score de prospecção por critérios, com o nicho-alvo configurável
* Funil com as etapas Novo, Contatado, Reunião, Proposta, Fechado e Perdido, histórico de status, filtros, busca e detalhe do lead

## Decisões técnicas

**Arquitetura hexagonal.** O domínio define as portas, e o Google Analytics, o banco e o leitor de CSV são adapters. A regra de score (`LeadScorer`) é uma classe de domínio pura, testada sem banco.

**JDBC onde é relatório, JPA onde é entidade.** As tabelas de analytics são escritas e consultadas com `NamedParameterJdbcTemplate`, porque são agregações e upserts em lote. O módulo de leads usa JPA, porque tem entidade com ciclo de vida e histórico. As consultas do funil, que são leitura pesada, também vão por JDBC.

**Segurança como resource server.** Login próprio que emite JWT HS256, validado pelo `oauth2-resource-server` do Spring Security. Papéis ADMIN e VIEWER, com a regra "nega por padrão": toda rota exige ADMIN, e o VIEWER só acessa a própria sessão (`/me` e logout). Ele fica bloqueado até existir o vínculo usuário → cliente, para um cliente nunca ver o painel de outro. O token carrega uma versão conferida no banco a cada requisição, então logout e desativação revogam na hora. O login tem limite de tentativas por conta. Os erros 401 e 403 seguem o formato `ProblemDetail`. O primeiro admin é criado na subida a partir de variáveis de ambiente.

**Credenciais fora do repositório.** O acesso ao GA4 é por service account com papel de leitor em cada propriedade. A chave JSON fica numa pasta ignorada pelo git.

## Stack

* Java 17, Spring Boot 4.1, Spring Security (OAuth2 resource server)
* PostgreSQL e Flyway
* Google Analytics Data API
* Apache Commons CSV, libphonenumber
* JUnit 5, com teste de integração do funil contra PostgreSQL

## Estrutura

```
src/main/java/com/eduar/radarciudadlab
├── domain
│   ├── model         # Métricas, insights, sites e o modelo de leads
│   └── port/out      # Portas para analytics, persistência e CSV
├── application       # Sincronização, consultas do dashboard, importação e funil
├── adapter
│   ├── in/web        # Controllers e tratamento de erros
│   ├── in/scheduler  # Coleta diária do GA4
│   └── out           # Google Analytics, persistência JDBC e JPA, CSV, telefone
└── config            # Configuração e segurança
```

## Como rodar

Pré-requisitos: Java 17 e PostgreSQL com um banco `radarciudad_db`.

| Variável | Uso |
|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | Banco |
| `JWT_SECRET` | Segredo do JWT em Base64, com pelo menos 32 bytes |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Cria o primeiro admin na subida (senha com 12 caracteres ou mais) |
| `GA_CREDENTIALS_PATH` | Caminho da chave JSON da service account do Google |

```bash
./mvnw spring-boot:run
./mvnw test
```

## Endpoints

| Área | Rotas |
|---|---|
| Autenticação | `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout` |
| Sites (ADMIN) | `GET /api/sites`, `GET /api/sites/{id}/dashboard`, `GET /api/sites/{id}/insights` |
| Sincronização (ADMIN) | `POST /api/sites/{id}/sync`, `POST /api/sites/sync` |
| Leads (ADMIN) | `POST /api/leads/import`, `GET /api/leads/imports`, `GET /api/leads/board`, `GET /api/leads`, `GET /api/leads/filters`, `GET /api/leads/{id}`, `PATCH /api/leads/{id}`, `POST /api/leads/rescore` |

## Estado atual

Analytics, funil de leads e segurança da API prontos; em preparação para produção. Ficam para quando clientes tiverem acesso ao painel: vínculo usuário → cliente, gestão de usuários VIEWER e refresh token.

## Autor

Eduardo · [CiudadLab](https://ciudadlab.com.br) · [LinkedIn](https://www.linkedin.com/in/eduardociudadf) · [GitHub](https://github.com/eduardo-Ciudad)