### Spring Cloud Data Flow Scaffolding and Kafka(for Binder) in a "box"

1. Running `mvn clean install -PgenerateApps` will generate the binder specific source code for your application assembled from 
`spring-cloud/spring-cloud-stream-app-starters/your_app` in the `apps` directory created upon successful installation.
Each application will have `kafka-11` suffix appended to its artifact name.

2. The _dev_ directory includes Vagrantfile for setting up a single broker apache-kafka environment useful for 
local SCDF deployment during development phase. By default, executing `vagrant up` 
installs additional software as defined in the `dev/scripts/bootstrap.sh` 
Modify the `dev/servers.yml` as needed depending on your host environment.

```
- name: dev
  box: centos/7
  ip: "192.168.0.11"
  netmask: "255.255.255.0"
  memory: 8192
  cpus: 4
  provision: "scripts/bootstrap.sh"
  forwarded_ports: [80,80,2181,2181,9092,9092,3306,3306,9393,9393]
```

3. `vagrant ssh` into the virtual machine. 
Alternatively, the same can be achieved by `ssh -i .vagrant/machines/dev/virtualbox/private_key vagrant@192.168.0.11`
Within the virtual machine environment download the Spring Cloud Data Flow Server and shell.   

`wget https://repo.spring.io/release/org/springframework/cloud/spring-cloud-dataflow-server/2.1.0.RELEASE/spring-cloud-dataflow-server-2.1.0.RELEASE.jar
wget https://repo.spring.io/release/org/springframework/cloud/spring-cloud-dataflow-shell/2.1.0.RELEASE/spring-cloud-dataflow-shell-2.1.0.RELEASE.jar
`
Download Skipper by running the following 

`wget https://repo.spring.io/release/org/springframework/cloud/spring-cloud-skipper-server/2.0.2.RELEASE/spring-cloud-skipper-server-2.0.2.RELEASE.jar`


4. go to `dev/scripts` and run `start_zk` followed by `start_kafka`
Confirm that zookeeper is started before you start kafka by `jps`

When you move files to `dev` it will be available in `/vagrant/` and this is how you place your apps into the virtual machine.

launch the `spring-cloud-skipper-server-2.0.2.RELEASE.jar` followed by `spring-cloud-dataflow-server-2.1.0.RELEASE.jar` 
and `spring-cloud-dataflow-shell-2.1.0.RELEASE.jar`

5. Test your app with SCDF.

