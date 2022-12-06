# FreeChain
 Basic data of Freecash blockchain

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

If you want run FreeChain to establish your own database service, you need install it as following.
If you just need query information of FreeChain, you need't install it. You should find a service provider, get an account, and query as showed in [Query Freecash Blockchain](query-freecash-blockchain) .
1. Install freecash full node and synchronize all blocks
* Download freecash full node
* The path of block files
* Deal with the confilct of freecash node of Docker, ES and FreeChain
1. Install Java
2. Install ElasticSearch and run it
3. Run the jar file of FreeChain to parse blocks
## Parse Freecash Blockchain
## ES Indices
## Query Freecash Blockchain

by No1_NrC7