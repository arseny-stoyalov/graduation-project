FROM debian:10 as base

RUN apt update --fix-missing && \
    apt install -y apt-transport-https ca-certificates curl gnupg2 \
                   openjdk-11-jre-headless procps unzip && \
    apt clean

FROM base AS toolchain

ARG SBT_VER=1.3.6
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" > /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add - && \
    apt update && apt install -y sbt=${SBT_VER}

FROM toolchain AS builder

WORKDIR /usr/src/app

COPY build.sbt .scalafmt.conf ./
COPY project project/
RUN java -version && sbt help

COPY src src/
RUN sbt reload update assembly

COPY docker docker/

FROM base AS runtime-image

EXPOSE 9091

RUN mkdir -p /usr/share/gp /etc/gp /var/lib/gp /var/log/gp && \
    addgroup --system gp && \
    adduser --system --home /var/lib/gp --no-create-home --ingroup gp --disabled-password --shell /bin/false gp && \
    chown -R gp:gp /var/lib/gp/ /var/log/gp/

COPY docker/logback.xml etc/gp/
COPY docker/gp-exec.sh /usr/local/bin/gp-exec
COPY --from=builder /usr/src/app/target/scala-2.*/graduation-project-*.jar /usr/share/gp/gp.jar

USER gp:gp
ENTRYPOINT ["/usr/local/bin/gp-exec"]
