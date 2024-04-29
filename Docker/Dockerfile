# 基于Ubuntu 20.04的基础镜像
FROM ubuntu:20.04

# 设置非交互式环境变量
ENV DEBIAN_FRONTEND=noninteractive

# 更新软件源并安装必要的软件包
RUN apt-get update && \
    apt-get install -y software-properties-common && \
    add-apt-repository ppa:deadsnakes/ppa && \
    apt-get install -y openjdk-8-jdk git wget unzip curl libdbi-perl subversion vim

# 安装 Defects4j v2.0
RUN git clone https://github.com/rjust/defects4j.git && \
    cd defects4j && \
    ./init.sh && \
    echo "export PATH=\$PATH:/defects4j/framework/bin" >> ~/.bashrc && \
    export PATH=$PATH:/defects4j/framework/bin && \
    defects4j checkout -p Chart -v 26b -w /defects4j_checkout/Chart-26 && \
    cd /defects4j_checkout/Chart-26 && \
    defects4j compile

# 安装 CatenaD4J
RUN git clone https://github.com/universetraveller/CatenaD4J.git && \
    echo "export PATH=\$PATH:/CatenaD4J" >> ~/.bashrc && \
    export PATH=$PATH:/CatenaD4J

# 设置工作目录
WORKDIR /

# 容器启动后默认执行的命令
CMD ["/bin/bash && source ~/.bashrc"]
