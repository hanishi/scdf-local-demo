#!/usr/bin/env bash

TIME_ZONE_FILE=/usr/share/zoneinfo/Asia/Tokyo

printLog() {
  printf "[$1-$0] $1\n";
}

install_wget() {
  value=$(rpm -qa | grep -c ^wget)
  if [ $value -eq 0 ]; then
    printLog "Installing wget";
    sudo yum --quiet -y install wget
  fi
}

install_openjdk8() {
  value=$(rpm -qa | grep -c ^java)
  if [ $value -eq 0 ]; then
    printLog "Installing java-1.8.0-openjdk-devel";
    sudo yum --quiet -y install java-1.8.0-openjdk-devel
    echo 'export JAVA_HOME=/usr/lib/jvm/java-1.8.0' | sudo tee -a /etc/profile.d/java.sh
    echo 'export JRE_HOME=/usr/lib/jvm/java-1.8.0' | sudo tee -a /etc/profile.d/java.sh
    source /etc/profile
  fi
}

install_kafka() {
  FILE=/home/vagrant/kafka
  if [ ! -f $FILE ]; then
    printLog "Installing Apache Kafka";
    sudo wget -q https://archive.apache.org/dist/kafka/1.0.2/kafka_2.11-1.0.2.tgz
    sudo mkdir -p /home/vagrant/kafka && sudo tar xvf kafka_2.11-1.0.2.tgz -C /home/vagrant/kafka --strip-components=1
    rm -rf kafka_2.11-1.0.2.tgz
  fi
}

install_mariadb() {
  value=$(rpm -qa | grep -c ^MariaDB)
  if [ $value -eq 0 ]; then
    printLog "Installing MariaDB-client MariaDB-server";
    yum --quiet -y install MariaDB-client MariaDB-server
    systemctl enable mariadb.service
    systemctl start mariadb.service
    printLog "Provisioning database";
    mysql -u root -e "CREATE USER 'vagrant'@'localhost' IDENTIFIED BY 'vagrant';"
    mysql -u root -e "GRANT ALL PRIVILEGES ON *.* TO vagrant IDENTIFIED BY 'vagrant' WITH GRANT OPTION; FLUSH PRIVILEGES;"
    sudo systemctl restart mariadb.service
  fi
}

install_dos2unix() {
  value=$(rpm -qa | grep -c ^dos2unix)
  if [ $value -eq 0 ]; then
    printLog "Installing dos2unix";
    sudo yum --quiet -y install dos2unix
  fi
}

node_ip=$1

sudo tee "/vagrant/scripts/common.sh" > /dev/null <<EOF
#!/usr/bin/env bash

node_ip=$node_ip
zk_port=2181
kafka_port=9092

zk=\${node_ip}:\$zk_port
broker=\${node_ip}:\$kafka_port

kafka_home=/home/vagrant/kafka
spark_home=/home/vagrant/spark
EOF

source "/vagrant/scripts/common.sh"

sudo tee "/etc/yum.repos.d/MariaDB.repo" > /dev/null <<EOF
[mariadb]
name = MariaDB
baseurl = http://yum.mariadb.org/10.1/centos7-amd64
gpgkey=https://yum.mariadb.org/RPM-GPG-KEY-MariaDB
gpgcheck=0
EOF

install_wget
install_dos2unix
install_openjdk8
install_kafka
install_mariadb

chown vagrant:vagrant -R /home/vagrant/
chmod u+x /vagrant/scripts/*.sh