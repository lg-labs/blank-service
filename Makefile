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

run-unit-test: clean
	mvn test

run-integration-test: install-skip-test-jib
	mvn failsafe:integration-test failsafe:verify -Dit.test="**/*IT.java" -Dfailsafe.failIfNoSpecifiedTests=false

run-acceptance-test: install-skip-test
	mvn failsafe:integration-test failsafe:verify -Dit.test="**/*AcceptanceT*.java" -Dfailsafe.failIfNoSpecifiedTests=false

run-test-spec-base:
	mvn failsafe:integration-test failsafe:verify -Dit.test=${TEST_NAME} -Dfailsafe.failIfNoSpecifiedTests=false

run-test-spec: install-skip-test run-test-spec-base

run-ut-spec: install-skip-test-jib run-test-spec-base

run-it-spec: install-skip-test-jib run-test-spec-base

run-at-spec: run-test-spec

# SETUP INFRASTRUCTURE
zookeeper-down:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/zookeeper.yml down --volumes
kafka-cluster-down:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/kafka_cluster.yml down --volumes
kafka-init-down:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/init_kafka.yml down --volumes
kafka-mngr-down:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/kafka_mngr.yml down --volumes
ddbb-down:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/postgres-ddbb.yml down --volumes --volumes
spec-ui-down:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/spec-ui.yml down --volumes --volumes
spec-generator-down:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/spec-generator.yml down --volumes --volumes
graph-generator-down:
	docker-compose -f ${INFRA}/graph/docker-compose.yml down --volumes

zookeeper-up:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/zookeeper.yml up -d
kafka-cluster-up:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/kafka_cluster.yml up -d
kafka-init-up:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/init_kafka.yml up -d
kafka-mngr-up:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/kafka_mngr.yml up -d
ddbb-up:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/postgres-ddbb.yml up -d
spec-ui-up:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/spec-ui.yml up -d
spec-generator-up:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/spec-generator.yml up -d
graph-generator-up:
	docker-compose -f ${INFRA}/graph/docker-compose.yml up -d


# DOWN ALL
docker-down: zookeeper-down kafka-cluster-down kafka-init-down kafka-mngr-down ddbb-down spec-ui-down spec-generator-down

# UP ALL
docker-up: zookeeper-up kafka-cluster-up kafka-init-up kafka-mngr-up ddbb-up spec-ui-up spec-generator-up


## APPs
run-app:
	mvn -f blank-container/pom.xml spring-boot:run


run-happy-path: docker-down docker-up run-app


# KAFKA MODELS from Avro Model definition
# If add a new Avro model, REMEMBER execute kafka model again.
run-avro-model:
	mvn -pl ${AVRO_MODEL} clean install

APP = blank-container
INFRA = blank-report/docker
AVRO_MODEL = blank-message/blank-message-model

