package writeEs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;

public class Indices {
	
	static final Logger log = LoggerFactory.getLogger(Indices.class);
	

	public static final String BlockIndex = "block";
	public static final String BlockHasIndex = "block_has";
	public static final String TxIndex = "tx";
	public static final String TxHasIndex = "tx_has";
	public static final String CashIndex = "cash";
	public static final String AddressIndex = "address";
	public static final String OpReturnIndex = "opreturn";
	public static final String BlockMarkIndex = "block_mark";
			
	public static void createAllIndices(ElasticsearchClient esClient) throws ElasticsearchException, IOException {
			
		if(esClient==null) {
			System.out.println("Create a Java client for ES first.");
			return;
		}

		String blockMarkJsonStr = "{\"mappings\":{\"properties\":{\"_fileOrder\":{\"type\":\"short\"},\"_pointer\":{\"type\":\"long\"},\"height\":{\"type\":\"long\"},\"id\":{\"type\":\"keyword\"},\"preId\":{\"type\":\"keyword\"},\"size\":{\"type\":\"long\"},\"status\":{\"type\":\"keyword\"}}}}";
		String blockJsonStr = "{\"mappings\":{\"properties\":{\"cdd\":{\"type\":\"long\"},\"diffTarget\":{\"type\":\"long\"},\"fee\":{\"type\":\"long\"},\"height\":{\"type\":\"long\"},\"id\":{\"type\":\"keyword\"},\"inValueT\":{\"type\":\"long\"},\"merkleRoot\":{\"type\":\"keyword\"},\"nonce\":{\"type\":\"long\"},\"outValueT\":{\"type\":\"long\"},\"preId\":{\"type\":\"keyword\"},\"size\":{\"type\":\"long\"},\"time\":{\"type\":\"long\"},\"txCount\":{\"type\":\"long\"},\"version\":{\"type\":\"keyword\"}}}}";
		String blockHasJsonStr = "{\"mappings\":{\"properties\":{\"height\":{\"type\":\"long\"},\"id\":{\"type\":\"keyword\"},\"txMarks\":{\"properties\":{\"cdd\":{\"type\":\"long\"},\"fee\":{\"type\":\"long\"},\"id\":{\"type\":\"keyword\"},\"outValue\":{\"type\":\"long\"}}}}}}";
		String txJsonStr = "{\"mappings\":{\"properties\":{\"blockId\":{\"type\":\"keyword\"},\"blockTime\":{\"type\":\"long\"},\"cdd\":{\"type\":\"long\"},\"coinbase\":{\"type\":\"text\"},\"fee\":{\"type\":\"long\"},\"height\":{\"type\":\"long\"},\"id\":{\"type\":\"keyword\"},\"inCount\":{\"type\":\"long\"},\"inValueT\":{\"type\":\"long\"},\"lockTime\":{\"type\":\"long\"},\"opReBrief\":{\"type\":\"text\"},\"outCount\":{\"type\":\"long\"},\"outValueT\":{\"type\":\"long\"},\"txIndex\":{\"type\":\"long\"},\"version\":{\"type\":\"long\"}}}}";
		String txHasJsonStr = "{\"mappings\":{\"properties\":{\"height\":{\"type\":\"long\"},\"id\":{\"type\":\"keyword\"},\"inMarks\":{\"properties\":{\"addr\":{\"type\":\"wildcard\"},\"cdd\":{\"type\":\"long\"},\"id\":{\"type\":\"keyword\"},\"value\":{\"type\":\"long\"}}},\"outMarks\":{\"properties\":{\"addr\":{\"type\":\"wildcard\"},\"cdd\":{\"type\":\"long\"},\"id\":{\"type\":\"keyword\"},\"value\":{\"type\":\"long\"}}}}}}";
		String cashJsonStr = "{\"mappings\":{\"properties\":{\"addr\":{\"type\":\"wildcard\"},\"birthHeight\":{\"type\":\"long\"},\"birthTime\":{\"type\":\"long\"},\"cdd\":{\"type\":\"long\"},\"id\":{\"type\":\"keyword\"},\"lockScript\":{\"type\":\"text\"},\"outIndex\":{\"type\":\"long\"},\"sequence\":{\"type\":\"keyword\"},\"sigHash\":{\"type\":\"keyword\"},\"spentHeight\":{\"type\":\"long\"},\"spentIndex\":{\"type\":\"long\"},\"spentTime\":{\"type\":\"long\"},\"spentTxId\":{\"type\":\"keyword\"},\"txId\":{\"type\":\"keyword\"},\"txIndex\":{\"type\":\"long\"},\"type\":{\"type\":\"keyword\"},\"unlockScript\":{\"type\":\"text\"},\"valid\":{\"type\":\"boolean\"},\"value\":{\"type\":\"long\"}}}}";
		String addressJsonStr = "{\"mappings\":{\"properties\":{\"balance\":{\"type\":\"long\"},\"birthHeight\":{\"type\":\"long\"},\"btcAddr\":{\"type\":\"wildcard\"},\"cd\":{\"type\":\"long\"},\"cdd\":{\"type\":\"long\"},\"dogeAddr\":{\"type\":\"wildcard\"},\"ethAddr\":{\"type\":\"wildcard\"},\"expend\":{\"type\":\"long\"},\"guide\":{\"type\":\"wildcard\"},\"id\":{\"type\":\"wildcard\"},\"income\":{\"type\":\"long\"},\"lastHeight\":{\"type\":\"long\"},\"ltcAddr\":{\"type\":\"wildcard\"},\"pubkey\":{\"type\":\"wildcard\"},\"trxAddr\":{\"type\":\"wildcard\"}}}}";
		String opreturnJsonStr = "{\"mappings\":{\"properties\":{\"cdd\":{\"type\":\"long\"},\"height\":{\"type\":\"long\"},\"id\":{\"type\":\"keyword\"},\"opReturn\":{\"type\":\"text\"},\"recipient\":{\"type\":\"wildcard\"},\"signer\":{\"type\":\"wildcard\"},\"time\":{\"type\":\"long\"},\"txIndex\":{\"type\":\"long\"}}}}";		
		
		InputStream blockMarkJsonStrIs = new ByteArrayInputStream(blockMarkJsonStr.getBytes());
		InputStream blockJsonStrIs = new ByteArrayInputStream(blockJsonStr.getBytes());
		InputStream blockHasJsonStrIs = new ByteArrayInputStream(blockHasJsonStr.getBytes());
		InputStream txJsonStrIs = new ByteArrayInputStream(txJsonStr.getBytes());
		InputStream txHasJsonStrIs = new ByteArrayInputStream(txHasJsonStr.getBytes());
		InputStream cashJsonStrIs = new ByteArrayInputStream(cashJsonStr.getBytes());
		InputStream addressJsonIs = new ByteArrayInputStream(addressJsonStr.getBytes());
		InputStream opreturnJsonStrIs = new ByteArrayInputStream(opreturnJsonStr.getBytes());

		try {
			CreateIndexResponse req = esClient.indices().create(c -> c.index(Indices.BlockMarkIndex).withJson(blockMarkJsonStrIs));
			blockMarkJsonStrIs.close();
			if(req.acknowledged()) {
			log.info("Index  block_mark created.");
			}
		}catch(ElasticsearchException e) {
			log.info("Index block_mark creating failed.",e);
			return;
		}
		
		try {
			CreateIndexResponse req = esClient.indices().create(c -> c.index(Indices.BlockIndex).withJson(blockJsonStrIs));
			blockJsonStrIs.close();
			if(req.acknowledged()) {
			log.info("Index  block created.");
			}
		}catch(ElasticsearchException e) {
			log.info("Index block creating failed.",e);
			return;
		}
		
		try {
			CreateIndexResponse req = esClient.indices().create(c -> c.index(Indices.BlockHasIndex).withJson(blockHasJsonStrIs));
			blockHasJsonStrIs.close();
			
			if(req.acknowledged()) {
			log.info("Index block_has created.");
			}else {log.info("Index block_has creating failed.");
			return;
			}
		}catch(ElasticsearchException e) {
			log.info("Index block_has creating failed.",e);
			return;
		}
		
		try {
			CreateIndexResponse req = esClient.indices().create(c -> c.index(Indices.TxIndex).withJson(txJsonStrIs));
			txJsonStrIs.close();
			
			if(req.acknowledged()) {
			log.info("Index tx created.");
			}
		}catch(ElasticsearchException e) {
			log.info("Index tx creating failed.",e);
			return;
		}
		
		try {
			CreateIndexResponse req = esClient.indices().create(c -> c.index(Indices.TxHasIndex).withJson(txHasJsonStrIs));
			txHasJsonStrIs.close();
			
			if(req.acknowledged()) {
			log.info("Index tx_has created.");
			}
		}catch(ElasticsearchException e) {
			log.info("Index tx_has creating failed.",e);
			return;
		}
		
		try {
			CreateIndexResponse req = esClient.indices().create(c -> c.index(Indices.CashIndex).withJson(cashJsonStrIs));
			cashJsonStrIs.close();
			
			if(req.acknowledged()) {
			log.info("Index stxo created.");
			}
		}catch(ElasticsearchException e) {
			log.info("Index stxo creating failed.",e);
			return;
		}
		
		try {
			CreateIndexResponse req = esClient.indices().create(c -> c.index(Indices.AddressIndex).withJson(addressJsonIs));
			addressJsonIs.close();
			
			if(req.acknowledged()) {
			log.info("Index address created.");
			}
		}catch(ElasticsearchException e) {
			log.info("Index address creating failed.",e);
			return;
		}
		
		try {
			CreateIndexResponse req = esClient.indices().create(c -> c.index(Indices.OpReturnIndex).withJson(opreturnJsonStrIs));
			opreturnJsonStrIs.close();	
			if(req.acknowledged()) {
			log.info("Index opreturn created.");
			}
		}catch(ElasticsearchException e) {
			log.info("Index opreturn creating failed.",e);
			return;
		}
		return;
	}
	
	public static void deleteAllIndices(ElasticsearchClient esClient) throws ElasticsearchException, IOException {

		if(esClient==null) {
			System.out.println("Create a Java client for ES first.");
			return;
		}
		
		
		try {
			DeleteIndexResponse req = esClient.indices().delete(c -> c.index(Indices.BlockMarkIndex));

			if(req.acknowledged()) {
			log.info("Index  block_Mark deleted.");
			}
		}catch(ElasticsearchException e) {
			log.info("Index block_mark deleting failed.",e);
		}

		try {
			DeleteIndexResponse req = esClient.indices().delete(c -> c.index(Indices.BlockIndex));

			if(req.acknowledged()) {
			log.info("Index  block deleted.");
			}
		}catch(ElasticsearchException e) {
			log.info("Index block deleting failed.",e);
		}
		
		try {
			DeleteIndexResponse req = esClient.indices().delete(c -> c.index(Indices.TxIndex));
			if(req.acknowledged()) {
			log.info("Index tx deleted.");
			}
		}catch(ElasticsearchException e) {
			log.info("Index tx deleting failed.",e);
		}
		
		try {
			DeleteIndexResponse req = esClient.indices().delete(c -> c.index(Indices.CashIndex));
			if(req.acknowledged()) {
			log.info("Index cash delted.");
			}
		}catch(ElasticsearchException e) {
			log.info("Index cash deleting failed.",e);
		}
		
		try {
			DeleteIndexResponse req = esClient.indices().delete(c -> c.index(Indices.AddressIndex));
			if(req.acknowledged()) {
			log.info("Index address deleted.");
			}
		}catch(ElasticsearchException e) {
			log.info("Index address deleting failed.",e);
		}
		
		try {
			DeleteIndexResponse req = esClient.indices().delete(c -> c.index(Indices.BlockHasIndex));
			if(req.acknowledged()) {
			log.info("Index block_has deleted.");
			}
		}catch(ElasticsearchException e) {
			log.info("Index block_has deleting failed.",e);
		}
		
		try {
			DeleteIndexResponse req = esClient.indices().delete(c -> c.index(Indices.TxHasIndex));
			if(req.acknowledged()) {
			log.info("Index tx_has deleted.");
			}
		}catch(ElasticsearchException e) {
			log.info("Index tx_has deleting failed.",e);
		}
		
		try {
			DeleteIndexResponse req = esClient.indices().delete(c -> c.index(Indices.OpReturnIndex));
			if(req.acknowledged()) {
			log.info("Index opreturn deleted.");
			}
		}catch(ElasticsearchException e) {
			log.info("Index opreturn creating failed.",e);
		}
		return;
	}	
}
