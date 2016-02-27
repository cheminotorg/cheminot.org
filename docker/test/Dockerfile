FROM debian:8.3

MAINTAINER Sebastien Renault <srenault.contact@gmail.com>

RUN apt-get -y update && apt-get -y upgrade

RUN apt-get -y install wget

RUN apt-get -y install apt-transport-https ca-certificates

RUN wget -O - https://debian.neo4j.org/neotechnology.gpg.key | apt-key add -

RUN echo 'deb http://debian.neo4j.org/repo stable/' > /tmp/neo4j.list

RUN mv /tmp/neo4j.list /etc/apt/sources.list.d

RUN apt-get -y update

RUN apt-get -y install neo4j=2.3.1

WORKDIR /db

RUN wget 'https://www.dropbox.com/s/hger98hlxarq8fp/cheminotdb-test.tar.gz?dl=1' -O cheminotdb-test.tar.gz

RUN tar -xvf cheminotdb-test.tar.gz

RUN chmod u+rw -R /db/cheminot.db

RUN chown neo4j:adm -R /db/cheminot.db

WORKDIR /etc/neo4j/

RUN cp neo4j-server.properties neo4j-server.properties.bk

RUN awk -F"=" -v value="/db/cheminot.db" 'BEGIN {OFS="=";} /org.neo4j.server.database.location/{$2=value;print;next}1' neo4j-server.properties.bk > neo4j-server.properties

RUN rm neo4j-server.properties.bk

RUN cp neo4j-server.properties neo4j-server.properties.bk

RUN awk -F"=" -v value="false" 'BEGIN {OFS="=";} /dbms.security.auth_enabled/{$2=value;print;next}1' neo4j-server.properties.bk > neo4j-server.properties

RUN rm neo4j-server.properties.bk

RUN cp neo4j-server.properties neo4j-server.properties.bk

RUN awk -F"=" -v value="false" 'BEGIN {OFS="=";} /org.neo4j.server.webserver.https.enabled/{$2=value;print;next}1' neo4j-server.properties.bk > neo4j-server.properties

RUN rm neo4j-server.properties.bk

RUN echo "org.neo4j.server.webserver.address=0.0.0.0" > neo4j-server.properties

EXPOSE 7474

CMD [ "/var/lib/neo4j/bin/neo4j", "console"]
