# Verify if docker compose modern is installed
APP = blank-container
INFRA = blank-support/docker
AVRO_MODEL = blank-message/blank-message-model
ATDD = blank-acceptance-test
FILE_LOG ?=false
DOCKER_COMPOSE := docker-compose

build_to_arm:
	 mvn clean install -Parch-aarch64
build_to_amd:
	mvn clean install -Pamd

clean:
	mvn clean
# INSTALL ARTIFACT
install: clean
	mvn install

install-skip-test: clean
	mvn install -DskipTests

install-skip-test-jib: clean
	mvn install -DskipTests -Djib.skip=true
# TESTING
run-checkstyle:
	mvn validate
run-verify: clean
	mvn verify -Dit.test="**/*IT.java,**/*Test.java" -Dfailsafe.failIfNoSpecifiedTests=false -Djib.skip=true

run-unit-test: clean
	mvn test

run-integration-test: install-skip-test-jib
	mvn failsafe:integration-test failsafe:verify -Dit.test="**/*IT.java" -Dfailsafe.failIfNoSpecifiedTests=false

run-acceptance-test-alone: install-skip-test-jib
	mvn failsafe:integration-test failsafe:verify -Dit.test="**/*AcceptanceT*.java" -Dfailsafe.failIfNoSpecifiedTests=false

run-acceptance-test: install-skip-test
	mvn failsafe:integration-test failsafe:verify -Dit.test="**/*IT.java" -Dfailsafe.failIfNoSpecifiedTests=false

run-test-spec-base:
	mvn failsafe:integration-test failsafe:verify -Dit.test=${TEST_NAME} -Dfailsafe.failIfNoSpecifiedTests=false

run-at-by-tag: run-acceptance-test
	 -Dcucumber.filter.tags=${TAG_NAME}

run-test-spec: install-skip-test run-test-spec-base

run-ut-spec: install-skip-test-jib run-test-spec-base

run-it-spec: install-skip-test-jib run-test-spec-base

run-at-spec: run-test-spec

# SETUP INFRASTRUCTURE
# Docker Commands
docker-kill:
	@echo "🛑 Killing all Docker containers..."
	@docker ps -aq | xargs -r docker rm -f

docker-prune:
	@echo "🛑 Cleaning Docker..."
	@docker system prune --volumes --force

docker-down: docker-kill
	@echo "🛑 Removing all Volumes..."
	@docker volume prune -f

kafka-down: docker-kill
	@echo "🛑 Stopping Docker Compose..."
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/kafka_cluster.yml down --volumes --remove-orphans
ddbb-down: docker-kill
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/postgres-ddbb.yml down --volumes --remove-orphans
spec-ui-down: docker-kill
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/spec-ui.yml down --volumes --remove-orphans
spec-generator-down: docker-kill
	@echo "spec-generator stack removed (CDN-based templates do not need a container)"
graph-generator-down: docker-kill
	docker-compose -f ${INFRA}/graph/docker-compose.yml down --volumes --remove-orphans

kafka-up:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/kafka_cluster.yml up -d
ddbb-up:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/postgres-ddbb.yml up -d
spec-ui-up:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/spec-ui.yml up -d

# DOCS GENERATOR
spec-generator-up:
	@echo "spec-generator no longer runs containers (asyncapi/openapi sites are CDN-based static HTML)"
graph-generator-up:
	docker-compose -f ${INFRA}/graph/docker-compose.yml up -d

asyncapi-gen-html-up:
	@echo "AsyncAPI docs are now assembled directly in CI from blank-support/asyncapi-template/index.html"
	@echo "(uses @asyncapi/web-component via CDN — same renderer as https://studio.asyncapi.com)"

openapi-gen-html-up:
	@echo "OpenAPI docs are now assembled directly in CI from blank-support/openapi-template/index.html"
	@echo "(uses Swagger UI via CDN — same renderer as https://petstore.swagger.io/)"


# DOWN ALL
docker-down: kafka-down ddbb-down spec-ui-down spec-generator-down docker-prune
d-down: docker-down

# UP ALL
docker-up: d-down kafka-up ddbb-up spec-ui-up spec-generator-up
d-up: d-down docker-up


## APPs
run-app:
	mvn -f blank-container/pom.xml spring-boot:run


run-happy-path: docker-down docker-up run-app


# KAFKA MODELS from Avro Model definition
# If add a new Avro model, REMEMBER execute kafka model again.
run-avro-model:
	mvn -pl ${AVRO_MODEL} clean install

run-atdd-module:
	mvn -pl ${ATDD} clean install -Dapplication.traces.file.enabled=${FILE_LOG}



