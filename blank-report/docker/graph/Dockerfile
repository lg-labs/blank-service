FROM ubuntu:latest

# Instalar dependencias necesarias
RUN apt-get update && \
    apt-get install -y curl zip unzip

# Instalar SDKMAN
RUN curl -s "https://get.sdkman.io" | bash

# Inicializar SDKMAN en el script de inicio
RUN echo "source $HOME/.sdkman/bin/sdkman-init.sh" >> ~/.bashrc

# Instalar JDK 21
RUN /bin/bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && \
    sdk install java 21.0.2-amzn" \
RUN sdk use java 21.0.2-amzn

# Instalar GraphViz y Maven
RUN apt-get install -y graphviz maven openjdk-21-jdk

RUN java --version

# Copiar el código fuente del proyecto
WORKDIR /app
COPY . /app

# Generar el grafo del proyecto
CMD mvn clean compile && mvn site
