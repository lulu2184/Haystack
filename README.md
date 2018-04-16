# Haystack Photo Distributed File System
Authored by: You Xu (youx@andrew.cmu.edu), Lu Liu(lul3@andrew.cmu.edu)
### Machine List
You need 8 machines to deploy this distributed file system. To run our project, you need follow this machine list for your servers:
* Cassandra seed - unix4 (128.2.13.137:9045)
* Cassandra replica - unix8 (128.2.13.188:9045)
* Web server - unix7 (128.2.13.138:8080)
* Cache server - ghc32 (128.2.100.165:4442)
* Redis cluster
    * ghc32 (128.2.100.165:7000-7002)
    * ghc33 (128.2.100.166:7003-7005)
    * ghc34 (128.2.100.167:7006-7008)
* Store server - unix6 (128.2.13.145:4443)

### Deploy on Andrew Machines
1. Install Cassandra on two different machines. One for Cassandra seed and one for replica. (To run on Andrew File System, remember to keep two different copies of installed Cassandra. One copy for one Cassandra server)
2. Change `conf/cassandra.yaml` in Cassandra folder for the two Cassandra servers.
    * Change `listen_port` (on line 599) to the dns of the according machine.
    * Change `seeds` (on line 425) to the dns of the Cassandra seed.
3. Copy webserver java code to web server with command `scp -r webserver <web_server_dns>:<project_path>`
4. Login to the web server machine and build the raw code with command `mvn package`
5. Start the web server with `mvn exec:java`
6. NGINX?
7. ....
8.
