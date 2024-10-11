## 🛠️ Blank μ-service with Hex Arch, DDD, SAGA, Outbox&Kafka

For full code visit [Repository][11].


> <h1> ⚠️ Replace _blank_ with the Domain Name ⚠️</h1>  
# Using Lg5 Spring `1.0.0-alpha`, JDK 21

[More details][4]


## 🚀 Build project

Install 1/1: Setup JDK 21.

```bash
sdk use java 21.0.2-amzn 
```

Install 1/2: Install the dependencies in your project.

```bash
mvn clean install 
```
## 🚀 Deploy with K8s

Use the infra repository [blank-infra][8] to deploy with **K8s**

## 📚Contents

* [blank-acceptance-test](blank-acceptance-test)
* [blank-api](blank-api)
* [blank-container](blank-container)
* [blank-data-access](blank-data-access)
* [blank-domain](blank-domain)
    * [blank-domain-core](blank-domain%2Fblank-domain-core)
    * [blank-application-service](blank-domain%2Fblank-application-service)
* [blank-external](blank-external)
* [blank-message](blank-message)
    * [blank-message-core](blank-message%2Fblank-message-core)
    * [blank-message-model](blank-message%2Fblank-message-model)



## 🚀 Run locally

### You can ...
Using `makefile`

### Start with infrastructure
😀 To **start** the Kafka Cluster and Postgres.

```shell
make docker-up
```

⛔️ To the Kafka Cluster and Postgres **stop** or **destroy**:
```shell
make docker-down
```

### Run APP
😀 To **start** the blank Service.

```shell
make run-app
```

### blank API `1.0.0-alpha`
> 👋  **[blank API, Port:8181][5]**
>
> Username: `None`  
> Password: `None`

### Database UI
> 👋  **[PgAdmin, Port:5013][9]**
>
> Username: `blanksystem@db.com`  
> Password: `blanksystem-db`

### Kafka UI
> 👋  **[Kafka UI, Port:9080][10]**
>
> Username: `None`  
> Password: `None`
>
# Contracts

1. [Open API][6]
2. [Async API][7]

## AVRO MODELS from Avro Model definition
> If you add a new Avro model, REMEMBER execute avro model again.
```shell
make run-avro-model
```
## Logger & ELK
This project is prepared to send log files and process visualization with filebeat.
You can specify the directory for stored the *.log files. Now, genera two file logs.

> Simple log
>* [log.path]/[application_name]-simple.log
>
> Complex log
>* [log.path]/[application_name]-complex.log
>
- Specify the directory with `log.path` property.

**_Simple_**: `Simple details about application logs.`
**_Complex_**:  `More details about application logs.`

## 🧪 Testing Project
![Sonar Results][img2]

> **✅ Checkstyle 1/4:** configuration that checks the Google coding conventions from Google Java Style.
>
> ```bash
> make run-checkstyle 
> ```

>**🧪 Running Unit Test 2/4:** Using JUnit 5.
>
>```bash
>make run-unit-test 
>```

>**🌾 Running Integration Test 3/4:** Using Test Containers, JUnit 5 and Rest-Assured.
>
>```bash
>make run-integration-test 
>```

>**🥒Running Acceptance Test 4/4:** Using Cucumber, Test Containers, JUnit 5 and Rest-Assured.
>
>```bash
>make run-acceptance-test
>```
### Interaction with tests one-to-one

> 🧪 Run a Unit Test
>```bash
>make run-ut-spec TEST_NAME=BlankMessageListenerImplTest
>```
>Details: `make run-ut-spec TEST_NAME=[TestNameTest]`

> 🌾Run an Integration Test
>```bash
>make run-it-spec TEST_NAME=OtherRepositoryIT
>```
>Details: `make run-it-spec TEST_NAME=[TestNameTest|TestNameIT]`

> 🥒 Run an Acceptance Test
>```bash
>make run-at-spec TEST_NAME=StartAcceptanceT
>```
>Details: `make run-at-spec TEST_NAME=[TestNameAcceptanceT]`

> ⚠️ Any Test: _Be careful, this option is slower._
>```bash
>make run-test-spec TEST_NAME=BlankMessageListenerImplTest
>```
>Details: `make run-test-spec TEST_NAME=[TestNameTest|TestNameIT|TestNameAcceptanceT]`
>

## [🥒 Acceptance Test Report][13]

You can show the [Acceptance Test Report Online][13].

## ⚖️ License

The MIT License (MIT). Please see [License][LIC] for more information.


[0]: https://img.shields.io/badge/LgLabs-community-blue?style=flat-square
[1]: https://lufgarciaqu.medium.com
[2]: https://img.shields.io/badge/license-MIT-green?style=flat-square
[4]: https://github.com/lg-labs-pentagon/lg5-spring

[5]: http://localhost:8181
[6]: blank-api/src/main/resources/spec/openapi.yaml
[7]: blank-message/blank-message-model/src/main/resources/spec/asyncapi.yaml
[8]: https://github.com/lg-labs/blank-infra
[9]: http://localhost:5013
[10]: http://localhost:9080
[11]: https://github.com/lg-labs/blank-service
[13]: https://lg-labs.github.io/blank-service

[LIC]: LICENSE

[img1]: https://github.com/lg-labs-pentagon/lg-labs-boot-parent/assets/105936384/31c27db8-1e77-478d-a38e-7acf6ba2571c
[img2]: sonar-results.png
