FROM tomcat:8.0.36-jre8

RUN rm -rf /usr/local/tomcat/webapps/*
ADD target/log4shell-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080 
CMD ["catalina.sh", "run"]

FROM python:3-alpine

WORKDIR /app

COPY requirements.txt requirements.txt

RUN apk add gcc g++ make libffi-dev openssl-dev
RUN pip3 install -r requirements.txt

COPY . .

ENTRYPOINT ["python", "log4j-scan.py" ]