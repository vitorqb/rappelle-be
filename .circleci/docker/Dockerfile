FROM openjdk:14-buster

# Install 
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
    software-properties-common \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Python
RUN python3 -V

# Coursier
RUN curl -fLo /usr/bin/coursier https://git.io/coursier-cli-linux && chmod +x /usr/bin/coursier
RUN coursier install sbt

# Docker
ENV DOCKERVERSION=18.03.1-ce
RUN curl -fsSLO https://download.docker.com/linux/static/stable/x86_64/docker-${DOCKERVERSION}.tgz \
  && tar xzvf docker-${DOCKERVERSION}.tgz --strip 1 \
                 -C /usr/local/bin docker/docker \
        && rm docker-${DOCKERVERSION}.tgz
RUN docker --version

# Prepares env vars
ENV PYTHONUNBUFFERED=1
ENV RAPPELLE_BE_REPO_ROOT=/root/project/
ENV PATH="/root/project/dev:${PATH}"
ENV PATH="/root/.local/share/coursier/bin:${PATH}"
