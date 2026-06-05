FROM apache/spark:4.0.2-scala2.13-java21-python3-ubuntu

USER root

RUN mkdir -p /opt/spark/app
COPY target/dataworks-spark-runner.jar /opt/spark/app/dataworks-spark-runner.jar

RUN chown -R spark:spark /opt/spark/app

USER spark
