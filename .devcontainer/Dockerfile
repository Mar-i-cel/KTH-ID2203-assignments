FROM ubuntu:22.04
ENV DEBIAN_FRONTEND=noninteractive


RUN apt update -y
RUN apt upgrade -y
#RUN unminimize -y

RUN apt install zsh  git sudo man    -y
#RUN apt install python3 pip3 -y

RUN apt install software-properties-common -y
RUN apt update -y 

#install java 8
RUN apt install openjdk-8-jdk -y
RUN apt install openjdk-8-jre -y
# set JAVA_HOME
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

# install sbt
RUN apt install curl -y
RUN apt install gzip -y

COPY .zshrc /root/.zshrc
#use zsh as main shell
RUN chsh -s /usr/bin/zsh root

# install scala -> uncomment one of the following lines

# x86_64
# RUN curl -fL https://github.com/coursier/coursier/releases/latest/download/cs-x86_64-pc-linux.gz | gzip -d > cs && chmod +x cs && ./cs setup -y && rm -f cs

# For ARM64
RUN curl -fL https://github.com/VirtusLab/coursier-m1/releases/latest/download/cs-aarch64-pc-linux.gz | gzip -d > cs && chmod +x cs && ./cs setup -y && rm -f cs