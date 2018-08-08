FROM navikt/java:8
COPY build/install/* /app
ENV JAVA_OPTS="'-Dlogback.configurationFile=logback-remote.xml'"
#RUN keytool -genkey -keystore mq_keystore.p12 -storetype pkcs12 -keyalg RSA -keysize 4096 -validity 3650 -alias mqkey -dname "cn=Unknown, ou=Unknown, o=Unknown, c=Unknown" -storepass changeit -keypass changeit
ENTRYPOINT ["/app/bin/paop"]
