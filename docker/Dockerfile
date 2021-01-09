FROM openjdk:11

ARG APP_VERSION

EXPOSE 8000

# Prepares the work dir
RUN mkdir -p /apps/rappelle
WORKDIR /apps/rappelle

# Copies the entrypoint
COPY entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh

# Copies the generated .tgz artifact file to the image
COPY ./target/universal/rappelle-${APP_VERSION}.tgz ./rappelle-${APP_VERSION}.tgz

# Untars it
RUN tar -vzxf rappelle-${APP_VERSION}.tgz && rm -rf rappelle-${APP_VERSION}.tgz
RUN ln -s ./rappelle-${APP_VERSION}/bin/rappelle ./run
RUN chmod +x ./run

ENTRYPOINT ["/apps/rappelle/entrypoint.sh"]
CMD []
