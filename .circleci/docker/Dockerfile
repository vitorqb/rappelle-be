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

# Prepares env vars
ENV PYTHONUNBUFFERED=1
ENV RAPPELLE_BE_REPO_ROOT=/root/project/
ENV PATH="/root/project/dev:${PATH}"
ENV PATH="/root/.local/share/coursier/bin:${PATH}"
