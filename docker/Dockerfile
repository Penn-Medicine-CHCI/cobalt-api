# See https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/docker-install.html

# Build image
#
# docker build . -t cobalt-api -f docker/Dockerfile

# Run image
#
# docker run -e COBALT_API_ENV='local' -e COBALT_API_PORT='8080' -p 8080:8080 cobalt-api

# Interactive shell
#
# docker run -it cobalt-api sh

# Stop image
#
# docker stop $(docker ps -q --filter ancestor=cobalt-api)

FROM amazoncorretto:11

EXPOSE 8080

ARG MAVEN_VERSION=3.6.3

# Set up our env vars

ENV COBALT_API_LAUNCHED_IN_DOCKER=true
ENV PATH="/home/appuser/apache-maven-${MAVEN_VERSION}/bin:${PATH}"
ENV MAVEN_HOME="/home/appuser/apache-maven-${MAVEN_VERSION}"

# Install basic system tools

RUN yum install -y wget unzip which tar gzip make gcc-c++ libpng-devel shadow-utils procps nmap-ncat git-core

# Switch to non-root user

RUN useradd --create-home appuser
WORKDIR /home/appuser
USER appuser

# Install Maven
RUN wget https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.zip
RUN unzip apache-maven-${MAVEN_VERSION}-bin.zip

# Copy over application code

COPY --chown=appuser:appuser . /home/appuser/app/

WORKDIR /home/appuser/app

RUN ./archive && unzip cobalt-api.zip && chmod +x cobalt-api/start-api

WORKDIR /home/appuser/app/cobalt-api

CMD ["sh", "-c", "./start-backend"]