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

# Sbt
RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
RUN apt-get --no-install-recommends -y update
RUN apt-get --no-install-recommends -y install sbt
RUN sbt --version

# Prepares env vars
ENV RAPPELLE_BE_REPO_ROOT=/root/project/
ENV PATH="/root/project/dev:${PATH}"

# Coursier
RUN curl -fLo /usr/bin/coursier https://git.io/coursier-cli-linux && chmod +x /usr/bin/coursier
RUN coursier --version
