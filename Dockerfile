FROM java:8
EXPOSE 8080
VOLUME ["/usr/src", "/usr/src/myapp"]
ADD /target/xml-compare-0.0.1-SNAPSHOT-jar-with-dependencies.jar /usr/src/myapp/xml-compare-0.0.1-SNAPSHOT-jar-with-dependencies.jar
WORKDIR /usr/src/myapp
ENTRYPOINT ["java","-jar","xml-compare-0.0.1-SNAPSHOT-jar-with-dependencies.jar"]
CMD ["/usr/src/${1}", "/usr/src/${2}", "/usr/src/${3}"]