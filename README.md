### JSUG勉強会 2018年その7 Spring Cloud Data FlowとSpring Bootで実装するエンタープライズ統合パターン

【第1部】Spring Bootで実装するエンタープライズ統合パターン
　勉強会の資料と、デモで使ったアプリケーションです。

cloneしたディレクトで
`mvn clean install -PgenerateApps`を実行すると、`apps`ディレクトリに、
デモで使った`Source`と`Processor`のアプリケーションがbuildされます。

`dev`はVagrantで起動するLinux環境で、vagrant upでkafkaとmariadbがすぐに使える状態になります。
こちらの環境を使って、できたSpring Cloud Streamのアプリケーションをlocal版のSCDFで実行して試すことができます。
SCDFは別途Vagrant環境にインストールしてください。

この`maven`構成は、`spring-cloud-stream-app-maven-plugin`が、`spring-cloud-stream-app-starters`
ディレクトリにある`demo-load-generator`と`demo-webflux`をそれぞれ、`Spring Boot`アプリケーションにしてくれます。
このmaven pluginをカスタマイズすることで、全ての`Spring Boot`アプリケーションが必ず使う機能(Annotation)を、自動生成される
`Spring Boot`アプリケーションにつけることができます。
