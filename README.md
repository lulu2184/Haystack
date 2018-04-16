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
#### Directory
1. Install Cassandra on two different machines. One for Cassandra seed and one for replica. (To run on Andrew File System, remember to keep two different copies of installed Cassandra. One copy for one Cassandra server)
2. Change `conf/cassandra.yaml` in Cassandra folder for the two Cassandra servers.
    * Change `listen_port` (on line 599) to the dns of the according machine.
    * Change `seeds` (on line 425) to the dns of the Cassandra seed.
3. Use `bin/cqlsh` to create tables in Cassandra with following commands.
```
CREATE KEYSPACE IF NOT EXISTS haystack
           WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};

USE haystak;

CREATE TABLE volumes (
    lid int primary key,
    physical_machine list<varchar>,
    writable boolean
) WITH caching = { 'keys' : 'ALL', 'rows_per_partition' : 'ALL' };

CREATE TABLE photo_entries (
	pid bigint primary key,
	cache_url varchar,
	lid int,
	physical_machine list<varchar>
) WITH caching = { 'keys' : 'ALL', 'rows_per_partition' : 'ALL' };
```
#### Web server
1. Copy webserver java code to web server with command `scp -r webserver <web_server_dns>:<project_path>`
2. Login to the web server machine and build the raw code with command `mvn package`
3. Start the web server with `mvn exec:java`
4. Install nginx and change the nginx.conf file. You can do this change based on the config file we give and you just need to modify the urls in `upstream directories`. Then run nginx with `nginx -p <YOUR_NGINX_HOME_PATH>` 
#### Cache
1. Copy cache java code to cache server with command `scp -r Cache <cache_server_dns>:<project_path>`
2. Login to the cache server machine and build the raw code with command `mvn package`
3. Start the cache server with `mvn exec:java`. If you want the server run as daemon, you can start the service with `nohup mvn exec:java &`
4. You can just run the code with the redis cluster we give.
#### Store
1. Copy store java code to store server with command `scp -r Store <store_server_dns>:<project_path>`
2. Login to the store server machine and build the raw code with command `mvn package`
3. Make sure you start the local redis server on store server
4. Create the photo directory: `<YOUR_HOME_PATh>/photos`
4. Start the store server with `mvn exec:java`. If you want the server run as daemon, you can start the service with `nohup mvn exec:java &`
