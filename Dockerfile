FROM adoptopenjdk:8-jre-hotspot

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ADD target/datakit-1.0.0.Beta.tar.gz  /

USER root

RUN chmod u+x /datakit-1.0.0.Beta/bin/docker-startup.sh

CMD  ["/datakit-1.0.0.Beta/bin/docker-startup.sh"]