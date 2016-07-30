FROM alpine:latest

# Gradle environment variables
ENV GRADLE_VERSION 2.12
ENV GRADLE_HOME /usr/local/gradle
ENV PATH ${PATH}:${GRADLE_HOME}/bin
ENV GRADLE_USER_HOME /gradle

# Tomcat environment variables
ENV TOMCAT_MAJOR 8
ENV TOMCAT_VERSION 8.0.35
ENV TOMCAT_TGZ_URL https://www.apache.org/dist/tomcat/tomcat-$TOMCAT_MAJOR/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz
ENV CATALINA_HOME /usr/local/tomcat
ENV PATH $CATALINA_HOME/bin:$PATH

RUN apk update && \
    apk add --no-cache openjdk8 curl tar && \

    # Install Tomcat
    mkdir -p "$CATALINA_HOME" && \
    cd $CATALINA_HOME && \
    set -x && \
    curl -fSL "$TOMCAT_TGZ_URL" -o tomcat.tar.gz && \
    curl -fSL "$TOMCAT_TGZ_URL.asc" -o tomcat.tar.gz.asc && \
    tar -xvf tomcat.tar.gz --strip-components=1 && \
    rm bin/*.bat && \
    rm tomcat.tar.gz* && \
    rm -rf /usr/local/tomcat/webapps/*

# Add app files
COPY . /app

WORKDIR /usr/local

RUN apk add --no-cache wget bash libstdc++ && \

    # Install Gradle
    wget  https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip --no-check-certificate && \
    unzip gradle-$GRADLE_VERSION-bin.zip && \
    rm -f gradle-$GRADLE_VERSION-bin.zip && \
    ln -s gradle-$GRADLE_VERSION gradle && \
    echo -ne "- with Gradle $GRADLE_VERSION\n" >> /root/.built && \

    # Build WAR file
    cd /app && \
    mkdir -p /gradle && \
    gradle war && \

    # Copy WAR into Tomcat
    mv build/libs/alfred*?.war /usr/local/tomcat/webapps/ROOT.war && \

    # Remove Gradle and working directory
    cd /usr/local/tomcat && \
    rm /usr/local/gradle && \
    rm -rf /usr/local/gradle-$GRADLE_VERSION && \
    rm -rf /usr/local/share && \
    rm -rf /gradle && \
    apk del wget bash libstdc++ && \
    rm -rf /var/cache/apk/* && \
    rm -rf /app

# Expose port and volume
EXPOSE 8080
VOLUME ["/usr/local/tomcat/nets"]

CMD ["catalina.sh", "run"]
