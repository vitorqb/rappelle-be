FROM openjdk:14-buster

# Prepare
ENV PYTHONUNBUFFERED=1
RUN apt-get --no-install-recommends -y update
RUN apt-get --no-install-recommends install -y software-properties-common
RUN apt-get --no-install-recommends -y update

# Curl
RUN apt-get --no-install-recommends -y install curl

# Python
RUN python3 -V

# Prepares env vars
ENV RAPPELLE_BE_REPO_ROOT=/root/project/
ENV PATH="/root/project/dev:${PATH}"
ENV PATH="/root/.local/share/coursier/bin:${PATH}"

# Coursier
RUN curl -fLo /usr/bin/coursier https://git.io/coursier-cli-linux && chmod +x /usr/bin/coursier
RUN coursier install sbt
