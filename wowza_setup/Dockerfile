#Simple docker container to get required jar if one does not exist
FROM ubuntu:latest

RUN apt-get update && \
    apt-get install -y wget curl && \
    rm -rf /var/lib/apt/lists/*

COPY setup_script.sh /wowza_setup/

ENTRYPOINT ["/wowza_setup/setup_script.sh"]