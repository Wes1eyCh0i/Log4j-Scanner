FROM tomcat:8.0.36-jre8

RUN rm -rf /usr/local/tomcat/webapps/*
ADD target/log4shell-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
RUN sed -i 's/port="8080"/port="8081"/' /usr/local/tomcat/conf/server.xml
EXPOSE 8081
CMD ["catalina.sh", "run"]
