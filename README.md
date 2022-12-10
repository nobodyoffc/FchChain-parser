# FreeChain Introduction

## What's FreeChain

FreeChain is a database of Freecash blockchain information.

Besides general blockchain information, some important special information is also reached form FreeChain (see [Features](features)).

The data of FreeChain is parsed from Freecash blockchain data files begining with blk00000.dat, and keeping up to date.

This database is builded on ElasticSearch.

The project of FreeChain is wrote with Java.

## Features

1. Take `TXO`(Transaction Output)) as the minimum `entity` with ID of double sha256 hash value of txid and the index of utxo in the tx in which this txo was born.
2. full-text index of `OpReturn` content.
3. CoinDays(`cd`) for UTXO (Unspent Transaction Output) and address.
4. CoinDays Destroied(`cdd`) for STXO (Spent Transaction Output), address, tx, block, and OpReturn.
5. `Guide` for address. Guide is the address who bring the new address to freecash world.
6. `BirthHeight` for address, utxo, stxo.
7. `BTC, ETH, LTC, DOGE, TRX` address from the same public key of freecash address.

## Install

If you only want to query information of FreeChain, you need't install FreeChain. You could find a service provider, get an account, and query as showed in [Query Freecash Blockchain](query-Freecash-Blockchain) .

If you want run FreeChain to establish your own database service, you need install it as following:

1. Install freecash full node and synchronize all blocks
* Download freecash full node from: https://github.com/freecashorg/freecash
* The default path of block files
	- MacOs: ~/Library/Application Support/freecash/blocks
	- Windows: 
	- docker: ~/fc_data/blocks
* Deal with the confilct of freecash node of Docker, ES and FreeChain
	- Problem: ES can't be install by user root. But the block file of freecash docker node will be defaultly belong to root. Inorder to operate ES, FreeChain should run by a new user instead root. You have to create the freecash block files with this new user. To deal with this, you can do as [Install with docker freecash node](install-with-docker-freecash-node)
2. Install Java
3. Install ElasticSearch and run it
4. Run the jar file of FreeChain to parse blocks

## Parse Freecash Blockchain
	config
	http or https
	restart from interrupt
	manual start
## ES Indices
	7 indices
	blockmark
	address
## Query Freecash Blockchain
	basic auth
	port
	es dsl sample
	cd 
## Deal with problems
### Install with docker freecash node
When install freecash docker node, you should do as below:

1. With root, copy block dir to newuser's home:
```
	cp -r /root/fc_data/* /home/carmx/fc_data
```
2. Change the owner to newuser
```
	chown -R carmx:carmx /home/carmx/fc_data
	```
	see the result:
```
	ls -l /home/carmx
	drwxr-xr-x. 3 carmx  carmx   22 11月  4 12:21 fc_data
	```
3. Check and remember your id
```
	id carmx
	uid=1000(carmx) gid=1000(carmx) group=100(carmx)
	```
4. Run docker container
```
	docker run -dit --name fc_miner --net=host -v /home/carmx/fc_data:/opt/newcoin fc.io:latest /bin/bash
	```
	If cointainer has been existed, restart it:
```
	docker ps -a
	```
	Get the id,then:
```
	docker start [id]
	```
5. Add the same name of newuser within the container
```
	useradd newuser
```
6. Ensure newuser has the same id and password as the newuser out container: 
```
	vi /etc/group
	```
	Find the line where newuser locate in, and make sure id is 1000. If not change it.
```
	vi /etc/passwd
	```
	Find the line where newuser locate in, and make sure id is 1000. If not change it.
7. login with newuser
```
	su carmx
	```
8. Start freecash node
```
	freecashd -listen=0 -datadir=/opt/newcoin -logtimemicros -gen=0 -daemon
	```
9. Check the blockchain info after a while of the node started 
```
	freecash-cli -datadir=/opt/newcoin getblockchaininfo
	```
	

by No1_NrC7