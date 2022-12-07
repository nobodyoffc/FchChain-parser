package mainTest;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;

//import com.xwc1125.chain5j.abi.datatypes.Address;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.cat.HealthResponse;
import co.elastic.clients.elasticsearch.cat.health.HealthRecord;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.MgetRequest;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateByQueryResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.json.JsonData;
import data.Address;
import data.Block;
import data.BlockMark;
import data.Txo;
import esClient.EsTools;
import esClient.Indices;
import esClient.EsTools.MgetResult;
import parse.Preparer;
import esClient.StartClient;
import testDataMaker.BlockParts;
import testDataMaker.DataMaker;
import tools.BytesTools;
import tools.FchTools;
import tools.Hash;
import tools.ParseTools;
import writeEs.CdMaker;

public class mainTest {
	public static final int FILE_END = -1;
	public static final int WRONG = -2;
	public static final int HEADER_FORK = -3;
	public static final int REPEAT = -4;
	public static final int WAIT_MORE = 0;
	public static final String MAGIC = "f9beb4d9";

	static final Logger log = LoggerFactory.getLogger(mainTest.class);

	public static void main(String[] args) throws Exception {

		////////////////////
		StartClient sc = new StartClient();
		sc.setParams("192.168.31.193", 9200);

		ElasticsearchClient esClient = sc.getClientHttp();
		// createIndex(client,"test");
		HealthResponse ch = esClient.cat().health();
		List<HealthRecord> vb = ch.valueBody();
		System.out.println("ES Client was created. The cluster is: " + vb.get(0).cluster());
		////////////////////


			System.out.println("Make all cd of UTXOs...");
			log.info("Make all cd of UTXOs...");
			
			//TimeUnit.MINUTES.sleep(1);
			
			Block bestBlock = new Block();
			bestBlock.setTime(System.currentTimeMillis());
			bestBlock.setHeight(389036l);
			makeUtxoCd(esClient);

		Gson gson = new Gson();

	//	System.out.println("result:" + gson.toJson(result));

		esClient.shutdown();

	}
	
	public static void makeUtxoCd(ElasticsearchClient esClient)
			throws ElasticsearchException, IOException, InterruptedException {
		long bestBlockTime = System.currentTimeMillis();

		UpdateByQueryResponse response = esClient.updateByQuery(u -> u
					.conflicts(Conflicts.Proceed)
					.timeout(Time.of(t->t.time("1800s")))
					.index(Indices.TxoIndex)
					.query(q -> q.bool(b -> b
							.filter(f -> f.term(t -> t.field("utxo").value(true)))))
					.script(s -> s.inline(i1 -> i1.source(
							"ctx._source.cd = (long)((((long)(params.bestBlockTime - ctx._source.birthTime)/86400)*ctx._source.value)/100000000)")
							.params("bestBlockTime", JsonData.of(bestBlockTime)))));
		
		System.out.println(response.updated()+" utxo updated within "
				+response.took()/1000+" seconds. Version confilcts: "+response.versionConflicts());
	}

	public static void makeUtxoCd1(ElasticsearchClient esClient)
			throws ElasticsearchException, IOException, InterruptedException {

		long now = System.currentTimeMillis() / 1000;
		long bestHeight = 900000;//Preparer.mainList.get(Preparer.mainList.size() - 1).getHeight();

		for (int i = 0;; i += 5000) {
			long fromHeight = i;
			esClient.updateByQuery(u -> u.index(Indices.TxoIndex)
					.query(q -> q.bool(b -> b.filter(f -> f.term(t -> t.field("utxo").value(true)))
							.must(m -> m.range(r -> r.field("birthHeight").gte(JsonData.of(fromHeight))
									.lt(JsonData.of(fromHeight + 5000))))))
					.sort("birthHeight:asc")
					.script(s -> s.inline(i1 -> i1
							.source("ctx._source.cd = (long)((((long)(params.now - ctx._source.birthTime)/86400)*ctx._source.value)/100000000)")
							.params("now", JsonData.of(now)))));
			if (fromHeight + 5000 > bestHeight)
				break;
			TimeUnit.SECONDS.sleep(5);
		}
	}
	
	public static void makeAddrCd(ElasticsearchClient esClient) throws Exception {
		SearchResponse<Address> response = esClient.search(
				s -> s.index(Indices.AddressIndex).size(EsTools.READ_MAX).sort(sort -> sort.field(f -> f.field("id"))),
				Address.class);

		ArrayList<Address> addrOldList = getResultAddrList(response);
		Map<String, Long> addrNewMap = makeAddrList(esClient, addrOldList);
		updateAddrMap(esClient, addrNewMap);

		while (true) {
			if (response.hits().hits().size() < EsTools.READ_MAX)
				break;
			Hit<Address> last = response.hits().hits().get(response.hits().hits().size() - 1);
			String lastId = last.id();
			response = esClient.search(s -> s.index(Indices.AddressIndex).size(5000)
					.sort(sort -> sort.field(f -> f.field("id"))).searchAfter(lastId), Address.class);

			addrOldList = getResultAddrList(response);
			addrNewMap = makeAddrList(esClient, addrOldList);
			updateAddrMap(esClient, addrNewMap);
		}

	}

	private static ArrayList<Address> getResultAddrList(SearchResponse<Address> response) {
		// TODO Auto-generated method stub
		ArrayList<Address> addrList = new ArrayList<Address>();
		for (Hit<Address> hit : response.hits().hits()) {
			addrList.add(hit.source());
		}
		return addrList;
	}

	private static Map<String, Long> makeAddrList(ElasticsearchClient esClient, ArrayList<Address> addrOldList)
			throws ElasticsearchException, IOException {
		// TODO Auto-generated method stub

		List<FieldValue> fieldValueList = new ArrayList<FieldValue>();
		for (Address addr : addrOldList) {
			fieldValueList.add(FieldValue.of(addr.getId()));
		}

		SearchResponse<Address> response = esClient.search(
				s -> s.index(Indices.TxoIndex).size(0).query(q -> q.term(t -> t.field("utxo").value(true)))
						.aggregations("filterByAddr",
								a -> a.filter(f -> f.terms(t -> t.field("addr").terms(t1 -> t1.value(fieldValueList))))
										.aggregations("termByAddr",
												a1 -> a1.terms(t3 -> t3.field("addr").size(addrOldList.size()))
														.aggregations("cdSum", a2 -> a2.sum(su -> su.field("cd"))))),
				Address.class);

		Map<String, Long> addrCdMap = new HashMap<String, Long>();

		List<StringTermsBucket> utxoBuckets = response.aggregations().get("filterByAddr").filter().aggregations()
				.get("termByAddr").sterms().buckets().array();

		for (StringTermsBucket bucket : utxoBuckets) {
			String addr = bucket.key();
			long value1 = (long) bucket.aggregations().get("cdSum").sum().value();
			addrCdMap.put(addr, value1);
		}
		return addrCdMap;
	}

	private static void updateAddrMap(ElasticsearchClient esClient, Map<String, Long> addrNewMap) throws Exception {
		// TODO Auto-generated method stub
		Set<String> addrSet = addrNewMap.keySet();
		BulkRequest.Builder br = new BulkRequest.Builder();

		for (String addr : addrSet) {
			Map<String, Long> updateMap = new HashMap<String, Long>();
			updateMap.put("cd", addrNewMap.get(addr));
			br.operations(o -> o.update(u -> u.index(Indices.AddressIndex).id(addr).action(a -> a.doc(updateMap))));
		}
		EsTools.bulkWithBuilder(esClient, br);
	}
	
	
	public void bytesToLong() {
		long a = 14215752192L;
		byte[] bytes= new byte[8];
		bytes = longToBytes(a);
		long longLE =  bytes8ToLong(bytes,true);
		long longBE =  bytes8ToLong(bytes,false);
		String hexBE = BytesTools.bytesToHexStringBE(bytes);
		String hexLE = BytesTools.bytesToHexStringLE(bytes);
		
		System.out.println("a: "+a);
		System.out.println("bytes hexLE: "+hexLE);
		System.out.println("bytes hexBE: "+hexBE);
		System.out.println("long LE: "+longLE);
		System.out.println("long BE: "+longBE);
	}

    public static byte[] longToBytes(long x) {
    	ByteBuffer buffer = ByteBuffer.allocate(8);  
        buffer.putLong(0, x);
        return buffer.array();
    }
	public static long bytes8ToLong(byte[] input, boolean littleEndian){
	    long value=0;
	    for(int  count=0;count<8;++count){
	        int shift=(littleEndian?count:(7-count))<<3;
	        value |=((long)0xff<< shift) & ((long)input[count] << shift);
	    }
	    return value;
	}

	public void pkToETH() {
		String unLockScript = "41fd184fce132dece2f23faaf394409df2feadff5c61d695a5406179c5af34e0bb7fafd48d0b1e819ccb96a3656fd972ba14ddf37ee1ffbb57de0751e711e164f2412102f62c5ec00bfbcaa71f4de400f54c6a1c1dad220f34246cf5561c292971641791";
		String pk = FchTools.parsePkFromUnlockScript(unLockScript);
		System.out.println(FchTools.pubKeyToEthAddr(pk));

	}

	private static <T> MgetResult<T> mgetWithNull(ElasticsearchClient esClient, String index, List<String> idList,
			Class<T> classType) throws ElasticsearchException, IOException {

		ArrayList<T> resultList = new ArrayList<T>();
		ArrayList<String> missList = new ArrayList<String>();

		MgetRequest.Builder mgetRequestBuilder = new MgetRequest.Builder();
		mgetRequestBuilder.index(index).ids(idList);
		MgetRequest mgetRequest = mgetRequestBuilder.build();
		MgetResponse<T> mgetResponse = null;

		mgetResponse = esClient.mget(mgetRequest, classType);

		List<MultiGetResponseItem<T>> items = mgetResponse.docs();

		ListIterator<MultiGetResponseItem<T>> iter = items.listIterator();
		while (iter.hasNext()) {
			MultiGetResponseItem<T> item = iter.next();
			if (item.result().found()) {
				resultList.add(item.result().source());

				// TODO
				System.out.println("found address: " + item.result().id());
			} else {
				missList.add(item.result().id());
				// TODO
				System.out.println("miss address: " + item.result().id());
			}
		}
		MgetResult<T> result = new MgetResult<T>();
		result.setMissList(missList);
		result.setResultList(resultList);

		return result;
	}

	public static long checkBlock(FileInputStream fis, long pointer) throws Exception {

		// TODO Auto-generated method stub

		BlockMark blockMark = new BlockMark();
		blockMark.set_pointer(pointer);
		blockMark.set_fileOrder(0);

		byte[] b8 = new byte[8];
		byte[] b4 = new byte[4];

		if (fis.read(b8) == FILE_END) {
			System.out.println("File end when reading magic. pointer: " + Preparer.Pointer);
			log.error("File end when reading magic. ");

			return FILE_END;
		}

		if (b8[0] == 0) {
			System.out.println("wait more...");

			return WAIT_MORE;
		}

		b4 = Arrays.copyOfRange(b8, 0, 4);
		String magic = BytesTools.bytesToHexStringBE(b4);
		if (!magic.equals(MAGIC)) {
			System.out.println("wrong magic.");
			return WRONG;
		}

		b4 = Arrays.copyOfRange(b8, 4, 8);
		int blockSize = (int) BytesTools.bytes4ToLongLE(b4);
		blockMark.setSize(blockSize);

		// Check valid header fork
		if (blockSize == 80) {
			System.out.println("head fork");
			return HEADER_FORK;
		}

		byte[] blockBytes = new byte[blockSize];
		if (fis.read(blockBytes) == FILE_END) {
			System.out.println("File end when reading block. pointer: " + Preparer.Pointer);
			log.error("File end when reading block. Pointer:" + Preparer.Pointer);
			return FILE_END;
		}

		ByteArrayInputStream blockInputStream = new ByteArrayInputStream(blockBytes);

		byte[] blockHeadBytes = new byte[80];
		blockInputStream.read(blockHeadBytes);

		String blockId = BytesTools.bytesToHexStringLE(Hash.Sha256x2(blockHeadBytes));
		blockMark.setId(blockId);

		String preId = BytesTools.bytesToHexStringLE(Arrays.copyOfRange(blockHeadBytes, 4, 4 + 32));
		blockMark.setPreId(preId);

		System.out.println("blockId: " + blockId + " PreId: " + preId);

		byte[] blockBodyBytes = new byte[blockSize - 80];
		blockInputStream.read(blockBodyBytes);

		// Check valid header fork
		b4 = Arrays.copyOfRange(blockBodyBytes, 0, 4);
		String b4Hash = BytesTools.bytesToHexStringBE(b4);
		if (b4Hash.equals(MAGIC)) {
			System.out.println("Found valid header fork. Pointer: " + Preparer.Pointer);
			log.info("Found valid header fork. Pointer: {}", Preparer.Pointer);
			System.out.println("wrong magic...");
			return HEADER_FORK;
		}

		// TODO
		System.out.println("BlockMark checked. Pointer: " + Preparer.Pointer + " BlockId: " + blockMark.getId());
		return blockSize + 8;
	}

	public class CheckResult {
		int blockLength;
		BlockMark blockMark;
		byte[] blockBytes;

		public int getBlockLength() {
			return blockLength;
		}

		public void setBlockLength(int blockLength) {
			this.blockLength = blockLength;
		}

		public BlockMark getBlockMark() {
			return blockMark;
		}

		public void setBlockMark(BlockMark blockMark) {
			this.blockMark = blockMark;
		}

		public byte[] getBlockBytes() {
			return blockBytes;
		}

		public void setBlockBytes(byte[] blockBytes) {
			this.blockBytes = blockBytes;
		}
	}

	public void deleteAllIndices() throws ElasticsearchException, IOException {

		////////////////////
		StartClient sc = new StartClient();
		sc.setParams("192.168.31.193", 9200);

		ElasticsearchClient esClient = sc.getClientHttp();
		// createIndex(client,"test");
		HealthResponse ch = esClient.cat().health();
		List<HealthRecord> vb = ch.valueBody();
		System.out.println("ES Client was created. The cluster is: " + vb.get(0).cluster());
		////////////////////

		try {
			DeleteIndexResponse req = esClient.indices().delete(c -> c.index(Indices.BlockMarkIndex));

			if (req.acknowledged()) {
				log.info("Index  block_Mark deleted.");
			}
		} catch (ElasticsearchException e) {
			log.info("Index block_mark deleting failed.", e);
		}

		try {
			DeleteIndexResponse req = esClient.indices().delete(c -> c.index(Indices.BlockIndex));

			if (req.acknowledged()) {
				log.info("Index  block deleted.");
			}
		} catch (ElasticsearchException e) {
			log.info("Index block deleting failed.", e);
		}

		try {
			DeleteIndexResponse req = esClient.indices().delete(c -> c.index(Indices.TxIndex));
			if (req.acknowledged()) {
				log.info("Index tx deleted.");
			}
		} catch (ElasticsearchException e) {
			log.info("Index tx deleting failed.", e);
		}

		try {
			DeleteIndexResponse req = esClient.indices().delete(c -> c.index(Indices.TxoIndex));
			if (req.acknowledged()) {
				log.info("Index txo delted.");
			}
		} catch (ElasticsearchException e) {
			log.info("Index txo deleting failed.", e);
		}

		try {
			DeleteIndexResponse req = esClient.indices().delete(c -> c.index(Indices.AddressIndex));
			if (req.acknowledged()) {
				log.info("Index address deleted.");
			}
		} catch (ElasticsearchException e) {
			log.info("Index address deleting failed.", e);
		}

		try {
			DeleteIndexResponse req = esClient.indices().delete(c -> c.index(Indices.BlockHasIndex));
			if (req.acknowledged()) {
				log.info("Index block_has deleted.");
			}
		} catch (ElasticsearchException e) {
			log.info("Index block_has deleting failed.", e);
		}

		try {
			DeleteIndexResponse req = esClient.indices().delete(c -> c.index(Indices.TxHasIndex));
			if (req.acknowledged()) {
				log.info("Index tx_has deleted.");
			}
		} catch (ElasticsearchException e) {
			log.info("Index tx_has deleting failed.", e);
		}

		try {
			DeleteIndexResponse req = esClient.indices().delete(c -> c.index(Indices.OpReturnIndex));
			if (req.acknowledged()) {
				log.info("Index opreturn deleted.");
			}
		} catch (ElasticsearchException e) {
			log.info("Index opreturn creating failed.", e);
		}

		return;
	}

	public void createAllIndices() throws ElasticsearchException, IOException {

		////////////////////
		StartClient sc = new StartClient();
		sc.setParams("192.168.31.193", 9200);

		ElasticsearchClient esClient = sc.getClientHttp();
		// createIndex(client,"test");
		HealthResponse ch = esClient.cat().health();
		List<HealthRecord> vb = ch.valueBody();
		System.out.println("ES Client was created. The cluster is: " + vb.get(0).cluster());
		////////////////////

		String blockMarkJsonStr = "{\"mappings\":{\"properties\":{\"id\":{\"type\":\"keyword\"},\"preId\":{\"type\":\"keyword\"},\"height\":{\"type\":\"long\"},\"size\":{\"type\":\"long\"},\"status\":{\"type\":\"keyword\"},\"_fileOrder\":{\"type\":\"short\"},\"_pointer\":{\"type\":\"long\"}}}}";
		String blockJsonStr = "{\"mappings\":{\"properties\":{\"size\":{\"type\":\"long\"},\"time\":{\"type\":\"long\"},\"cdd\":{\"type\":\"long\"},\"diffTarget\":{\"type\":\"long\"},\"fee\":{\"type\":\"long\"},\"height\":{\"type\":\"long\"},\"id\":{\"type\":\"keyword\"},\"inValueT\":{\"type\":\"long\"},\"merkleRoot\":{\"type\":\"keyword\"},\"nonce\":{\"type\":\"long\"},\"outValueT\":{\"type\":\"long\"},\"preId\":{\"type\":\"keyword\"},\"txCount\":{\"type\":\"long\"},\"version\":{\"type\":\"keyword\"}}}}";
		String blockHasJsonStr = "{\"mappings\":{\"properties\":{\"id\":{\"type\":\"keyword\"},\"height\":{\"type\":\"long\"},\"txIds\":{\"type\":\"keyword\"}}}}";
		String txJsonStr = "{\"mappings\":{\"properties\":{\"blockId\":{\"type\":\"keyword\"},\"blockTime\":{\"type\":\"long\"},\"cdd\":{\"type\":\"long\"},\"coinbase\":{\"type\":\"text\"},\"fee\":{\"type\":\"long\"},\"height\":{\"type\":\"long\"},\"id\":{\"type\":\"keyword\"},\"inCount\":{\"type\":\"long\"},\"inValueT\":{\"type\":\"long\"},\"lockTime\":{\"type\":\"long\"},\"opReBrief\":{\"type\":\"text\"},\"outCount\":{\"type\":\"long\"},\"outValueT\":{\"type\":\"long\"},\"txIndex\":{\"type\":\"long\"},\"version\":{\"type\":\"long\"}}}}";
		String txHasJsonStr = "{\"mappings\":{\"properties\":{\"cdds\":{\"type\":\"long\"},\"height\":{\"type\":\"long\"},\"id\":{\"type\":\"keyword\"},\"inAddrs\":{\"type\":\"wildcard\"},\"inIds\":{\"type\":\"keyword\"},\"inValues\":{\"type\":\"long\"},\"outAddrs\":{\"type\":\"wildcard\"},\"outIds\":{\"type\":\"keyword\"},\"outValues\":{\"type\":\"long\"}}}}";
		String txoJsonStr = "{\"mappings\":{\"properties\":{\"addr\":{\"type\":\"wildcard\"},\"birthTime\":{\"type\":\"long\"},\"cdd\":{\"type\":\"long\"},\"birthHeight\":{\"type\":\"long\"},\"id\":{\"type\":\"keyword\"},\"lockScript\":{\"type\":\"text\"},\"outIndex\":{\"type\":\"long\"},\"sequence\":{\"type\":\"keyword\"},\"sigHash\":{\"type\":\"keyword\"},\"spentHeight\":{\"type\":\"long\"},\"spentIndex\":{\"type\":\"long\"},\"spentTime\":{\"type\":\"long\"},\"spentTxId\":{\"type\":\"keyword\"},\"txId\":{\"type\":\"keyword\"},\"txIndex\":{\"type\":\"long\"},\"type\":{\"type\":\"keyword\"},\"unlockScript\":{\"type\":\"text\"},\"value\":{\"type\":\"long\"}}}}";
		String addressJsonStr = "{\"mappings\":{\"properties\":{\"balance\":{\"type\":\"long\"},\"birthHeight\":{\"type\":\"long\"},\"btcAddr\":{\"type\":\"wildcard\"},\"cd\":{\"type\":\"long\"},\"cdd\":{\"type\":\"long\"},\"dogeAddr\":{\"type\":\"wildcard\"},\"ethAddr\":{\"type\":\"wildcard\"},\"expend\":{\"type\":\"long\"},\"guide\":{\"type\":\"wildcard\"},\"id\":{\"type\":\"wildcard\"},\"income\":{\"type\":\"long\"},\"lastHeight\":{\"type\":\"long\"},\"ltcAddr\":{\"type\":\"wildcard\"},\"pubkey\":{\"type\":\"wildcard\"},\"trxAddr\":{\"type\":\"wildcard\"}}}}";
		String opreturnJsonStr = "{\"mappings\":{\"properties\":{\"cdd\":{\"type\":\"long\"},\"height\":{\"type\":\"long\"},\"id\":{\"type\":\"keyword\"},\"opReturn\":{\"type\":\"text\"},\"recipient\":{\"type\":\"wildcard\"},\"signer\":{\"type\":\"wildcard\"},\"txIndex\":{\"type\":\"long\"}}}}";

		InputStream blockMarkJsonStrIs = new ByteArrayInputStream(blockMarkJsonStr.getBytes());
		InputStream blockJsonStrIs = new ByteArrayInputStream(blockJsonStr.getBytes());
		InputStream blockHasJsonStrIs = new ByteArrayInputStream(blockHasJsonStr.getBytes());
		InputStream txJsonStrIs = new ByteArrayInputStream(txJsonStr.getBytes());
		InputStream txHasJsonStrIs = new ByteArrayInputStream(txHasJsonStr.getBytes());
		InputStream txoJsonStrIs = new ByteArrayInputStream(txoJsonStr.getBytes());
		InputStream addressJsonIs = new ByteArrayInputStream(addressJsonStr.getBytes());
		InputStream opreturnJsonStrIs = new ByteArrayInputStream(opreturnJsonStr.getBytes());

		try {
			CreateIndexResponse req = esClient.indices()
					.create(c -> c.index(Indices.BlockMarkIndex).withJson(blockMarkJsonStrIs));
			blockMarkJsonStrIs.close();
			if (req.acknowledged()) {
				log.info("Index  block_mark created.");
			}
		} catch (ElasticsearchException e) {
			log.info("Index block_mark creating failed.", e);
			return;
		}

		try {
			CreateIndexResponse req = esClient.indices()
					.create(c -> c.index(Indices.BlockIndex).withJson(blockJsonStrIs));
			blockJsonStrIs.close();
			if (req.acknowledged()) {
				log.info("Index  block created.");
			}
		} catch (ElasticsearchException e) {
			log.info("Index block creating failed.", e);
			return;
		}

		try {
			CreateIndexResponse req = esClient.indices()
					.create(c -> c.index(Indices.BlockHasIndex).withJson(blockHasJsonStrIs));
			blockHasJsonStrIs.close();

			if (req.acknowledged()) {
				log.info("Index block_has created.");
			} else {
				log.info("Index block_has creating failed.");
				return;
			}
		} catch (ElasticsearchException e) {
			log.info("Index block_has creating failed.", e);
			return;
		}

		try {
			CreateIndexResponse req = esClient.indices().create(c -> c.index(Indices.TxIndex).withJson(txJsonStrIs));
			txJsonStrIs.close();

			if (req.acknowledged()) {
				log.info("Index tx created.");
			}
		} catch (ElasticsearchException e) {
			log.info("Index tx creating failed.", e);
			return;
		}

		try {
			CreateIndexResponse req = esClient.indices()
					.create(c -> c.index(Indices.TxHasIndex).withJson(txHasJsonStrIs));
			txHasJsonStrIs.close();

			if (req.acknowledged()) {
				log.info("Index tx_has created.");
			}
		} catch (ElasticsearchException e) {
			log.info("Index tx_has creating failed.", e);
			return;
		}

		try {
			CreateIndexResponse req = esClient.indices().create(c -> c.index(Indices.TxoIndex).withJson(txoJsonStrIs));
			txoJsonStrIs.close();

			if (req.acknowledged()) {
				log.info("Index stxo created.");
			}
		} catch (ElasticsearchException e) {
			log.info("Index stxo creating failed.", e);
			return;
		}

		try {
			CreateIndexResponse req = esClient.indices()
					.create(c -> c.index(Indices.AddressIndex).withJson(addressJsonIs));
			addressJsonIs.close();

			if (req.acknowledged()) {
				log.info("Index address created.");
			}
		} catch (ElasticsearchException e) {
			log.info("Index address creating failed.", e);
			return;
		}

		try {
			CreateIndexResponse req = esClient.indices()
					.create(c -> c.index(Indices.OpReturnIndex).withJson(opreturnJsonStrIs));
			opreturnJsonStrIs.close();
			if (req.acknowledged()) {
				log.info("Index opreturn created.");
			}
		} catch (ElasticsearchException e) {
			log.info("Index opreturn creating failed.", e);
			return;
		}
		return;
	}

	public void utxoCount() throws ElasticsearchException, IOException {
		////////////////////
		StartClient sc = new StartClient();
		sc.setParams("192.168.31.193", 9200);

		ElasticsearchClient esClient = sc.getClientHttp();
		// createIndex(client,"test");
		HealthResponse ch = esClient.cat().health();
		List<HealthRecord> vb = ch.valueBody();
		System.out.println("ES Client was created. The cluster is: " + vb.get(0).cluster());
		////////////////////

		ArrayList<String> addrAllList = new ArrayList<String>();
		addrAllList.add("FTqiqAyXHnK7uDTXzMap3acvqADK4ZGzts");
		addrAllList.add("F6WQbxLghfYvB36t8y44xySyxJ2KKANAWC");
		long lastHeight = 1000;
		List<FieldValue> fieldValueList = new ArrayList<FieldValue>();

		Iterator<String> iter = addrAllList.iterator();
		while (iter.hasNext())
			fieldValueList.add(FieldValue.of(iter.next()));

		SearchResponse<Void> response = esClient.search(
				s -> s.index(Indices.TxoIndex)
						.query(q -> q.bool(b -> b
								.must(m -> m.range(r -> r.field("spentHeight").lte(JsonData.of(lastHeight))))
								.must(m1 -> m1.range(r1 -> r1.field("birthHeight").lte(JsonData.of(lastHeight))))))
						.size(0)
						.aggregations("addrFilterAggs", a -> a
								.filter(f -> f.terms(t -> t.field("addr").terms(t1 -> t1.value(fieldValueList))))
								.aggregations("utxoFilterAggs", a0 -> a0
										.filter(f1 -> f1.match(m -> m.field("spentHeight").query(0)))
										.aggregations("incomeAggs",
												a3 -> a3.terms(t2 -> t2.field("addr").size(200000)).aggregations(
														"incomeSum", t5 -> t5.sum(s1 -> s1.field("value")))))
								.aggregations("stxoFilterAggs", a0 -> a0
										.filter(f1 -> f1.range(r4 -> r4.field("spentHeight").gt(JsonData.of(0))))
										.aggregations("expendAggs",
												a1 -> a1.terms(t2 -> t2.field("addr").size(200000))
														.aggregations("spendSum", t3 -> t3.sum(s1 -> s1.field("value")))
														.aggregations("cddSum", t4 -> t4.sum(s1 -> s1.field("cdd")))))),
				void.class);
		ParseTools.gsonPrint(response);
	}

	public void preIdReplace() throws IOException {
		BlockParts blockParts = new BlockParts();

		int fileOrder = 0;
		long pointer = 0;
		String preId = "";
		// 3个初始区块
		for (int i = 0; i < 2; i++) {
			blockParts = DataMaker.readBlock(fileOrder, pointer);
			pointer += blockParts.getLength();
			preId = DataMaker.getLastId(blockParts);
			System.out.println("1. Id:" + preId);
		}

		blockParts = DataMaker.readBlock(fileOrder, pointer);
		pointer += blockParts.getLength();
		blockParts = DataMaker.readBlock(fileOrder, pointer);
		pointer += blockParts.getLength();
		String oldPreId = BytesTools.bytesToHexStringLE(Arrays.copyOfRange(blockParts.getB80(), 4, 4 + 32));
		System.out.println("2. oldPreId:" + oldPreId);

		blockParts = DataMaker.replacePreId(blockParts, preId);
		String newPreId = BytesTools.bytesToHexStringLE(Arrays.copyOfRange(blockParts.getB80(), 4, 4 + 32));
		System.out.println("3. newPreId:" + newPreId);
	}

	public void test() throws ElasticsearchException, IOException {

		////////////////////
		StartClient sc = new StartClient();
		sc.setParams("192.168.31.193", 9200);

		ElasticsearchClient client = sc.getClientHttp();
		// createIndex(client,"test");
		HealthResponse ch = client.cat().health();
		List<HealthRecord> vb = ch.valueBody();
		System.out.println("ES Client was created. The cluster is: " + vb.get(0).cluster());
		////////////////////

		long lastHeight = 1460550;
		client.updateByQuery(u -> u.index("stxo")
				.query(q -> q.bool(b -> b.must(m -> m.range(r -> r.field("spentHeight").gt(JsonData.of(lastHeight))))))
				.script(s -> s.inline(i -> i.source(
						"ctx._source.spentTime=0;" + "ctx._source.spentTxId=0;" + "ctx._source.spentHeight=null;"
								+ "ctx._source.spentIndex=0;" + "ctx._source.unlockScript=null;"
								+ "ctx._source.sigHash=null;" + "ctx._source.sequence=0;" + "ctx._source.cdd=0;"))));
	}

	public void cdtest() throws ElasticsearchException, IOException {

		////////////////////
		StartClient sc = new StartClient();
		sc.setParams("192.168.31.193", 9200);

		ElasticsearchClient client = sc.getClientHttp();
		// createIndex(client,"test");
		HealthResponse ch = client.cat().health();
		List<HealthRecord> vb = ch.valueBody();
		System.out.println("ES Client was created. The cluster is: " + vb.get(0).cluster());
		////////////////////

		long lastHeight = 0;
		long now = System.currentTimeMillis() / 1000;
		System.out.println("now:" + now);

		Txo txo = new Txo();
		txo.setCd(100);

		client.updateByQuery(u -> u.index("utxo")
				.query(q -> q.range(r -> r.field("height").gt(JsonData.of(lastHeight)))).sort("height:asc")
				.script(s -> s.inline(i -> i.source(
						"ctx._source.cd = (long)((((long)(params.now - ctx._source.blockTime)/86400)*ctx._source.value)/100000000)")
						.params("now", JsonData.of(now)))));
	}

	public void requst() throws ElasticsearchException, IOException, InterruptedException {

		////////////////////
		StartClient sc = new StartClient();
		sc.setParams("192.168.31.193", 9200);

		ElasticsearchClient esClient = sc.getClientHttp();
		// createIndex(client,"test");
		HealthResponse ch = esClient.cat().health();
		List<HealthRecord> vb = ch.valueBody();
		System.out.println("ES Client was created. The cluster is: " + vb.get(0).cluster());
		////////////////////

		SearchResponse<Address> res = esClient
				.search(s -> s.index("address").size(5000).sort(sort -> sort.field(f -> f.field("id"))), Address.class);

		String total = res.hits().toString();
		System.out.println(total);
		TimeUnit.SECONDS.sleep(5);

	}

	public void addr() throws Exception {

		////////////////////
		StartClient sc = new StartClient();
		sc.setParams("192.168.31.193", 9200);

		ElasticsearchClient esClient = sc.getClientHttp();
		HealthResponse ch = esClient.cat().health();
		List<HealthRecord> vb = ch.valueBody();
		System.out.println("ES Client was created. The cluster is: " + vb.get(0).cluster());
		////////////////////
		esClient.delete(d -> d.index("utxo").id("1"));

		SearchResponse<Address> response = esClient.search(
				s -> s.index("address").size(EsTools.READ_MAX).sort(sort -> sort.field(f -> f.field("id"))),
				Address.class);

		ArrayList<Address> addrOldList = getResultAddrList(response);
		System.out.println(addrOldList.get(0).getId());

		Map<String, Long> addrNewMap = makeAddrList(esClient, addrOldList);

		System.out.println("made.");
		updateAddrMap(esClient, addrNewMap);

		System.out.println("writed.");

		while (true) {
			if (response.hits().hits().size() < EsTools.READ_MAX)
				break;
			Hit<Address> last = response.hits().hits().get(response.hits().hits().size() - 1);
			String lastId = last.id();
			response = esClient.search(
					s -> s.index("address").size(5000).sort(sort -> sort.field(f -> f.field("id"))).searchAfter(lastId),
					Address.class);

			addrOldList = getResultAddrList(response);
			addrNewMap = makeAddrList(esClient, addrOldList);
			updateAddrMap(esClient, addrNewMap);
		}
	}

//	private ArrayList<Address> getResultAddrList(SearchResponse<Address> response) {
//		// TODO Auto-generated method stub
//		ArrayList<Address> addrList = new ArrayList<Address>();
//		for (Hit<Address> hit : response.hits().hits()) {
//			addrList.add(hit.source());
//		}
//		return addrList;
//	}
//
//	private Map<String, Long> makeAddrList(ElasticsearchClient esClient, ArrayList<Address> addrOldList)
//			throws ElasticsearchException, IOException {
//		// TODO Auto-generated method stub
//
//		List<FieldValue> fieldValueList = new ArrayList<FieldValue>();
//		for (Address addr : addrOldList) {
//			fieldValueList.add(FieldValue.of(addr.getId()));
//		}
//
//		SearchResponse<Address> response = esClient.search(s -> s.index("utxo").size(0)
//				// .query(q->q.term(t->t.field("utxo").value(true)))
//				.aggregations("filterByAddr",
//						a -> a.filter(f -> f.terms(t -> t.field("addr").terms(t1 -> t1.value(fieldValueList))))
//								.aggregations("termByAddr",
//										a1 -> a1.terms(t3 -> t3.field("addr").size(addrOldList.size()))
//												.aggregations("cdSum", a2 -> a2.sum(su -> su.field("cd"))))),
//				Address.class);
//
//		Map<String, Long> addrCdMap = new HashMap<String, Long>();
//
//		List<StringTermsBucket> utxoBuckets = response.aggregations().get("filterByAddr").filter().aggregations()
//				.get("termByAddr").sterms().buckets().array();
//
//		for (StringTermsBucket bucket : utxoBuckets) {
//			String addr = bucket.key();
//			long value1 = (long) bucket.aggregations().get("cdSum").sum().value();
//			addrCdMap.put(addr, value1);
//		}
//		return addrCdMap;
//	}
//
//	private void updateAddrMap(ElasticsearchClient esClient, Map<String, Long> addrNewMap) throws Exception {
//		// TODO Auto-generated method stub
//		Set<String> addrSet = addrNewMap.keySet();
//		BulkRequest.Builder br = new BulkRequest.Builder();
//
//		for (String addr : addrSet) {
//			Map<String, Long> updateMap = new HashMap<String, Long>();
//			updateMap.put("cd", addrNewMap.get(addr));
//			br.operations(o -> o.update(u -> u.index("address").id(addr).action(a -> a.doc(updateMap))));
//		}
//		EsTools.bulkWithBuilder(esClient, br);
//	}

	public void cd() throws ElasticsearchException, IOException {

		////////////////////
		StartClient sc = new StartClient();
		sc.setParams("192.168.31.193", 9200);

		ElasticsearchClient client = sc.getClientHttp();
		// createIndex(client,"test");
		HealthResponse ch = client.cat().health();
		List<HealthRecord> vb = ch.valueBody();
		System.out.println("ES Client was created. The cluster is: " + vb.get(0).cluster());
		////////////////////

		long lastHeight = 1460550;
		long now = System.currentTimeMillis();

		Txo txo = new Txo();
		txo.setCd(100);

		client.updateByQuery(
				u -> u.index("utxo").query(q -> q.range(r -> r.field("height").gt(JsonData.of(lastHeight)))).script(
						s -> s.inline(i -> i.source("ctx._source.cd = (long)(ctx._source.value*(long)/100000000) ")
								.params("now", JsonData.of(now)))));
	}
	
	public ArrayList<BlockMark> readForkList(ElasticsearchClient esClient, long bestHeight) throws ElasticsearchException, IOException {
		// TODO Auto-generated method stub
		//, MARK_FORK, "height",REOTG_PROTECT
		SearchResponse<BlockMark> response = esClient.search(s->s.index(Indices.BlockMarkIndex)
				.query(q->q
						.range(r->r
								.field("height")
								.gt(JsonData.of(bestHeight-30))))
				.size(EsTools.READ_MAX)
				.sort(so->so.field(f->f
						.field("height")
						.order(SortOrder.Asc)))
				, BlockMark.class);
		
		List<Hit<BlockMark>> hitList = response.hits().hits();
		
		ArrayList<BlockMark> readList = new ArrayList<BlockMark>();
		
		Iterator<Hit<BlockMark>> iter = hitList.iterator();
		while(iter.hasNext()) {
			Hit<BlockMark> hit = iter.next();
			readList.add(hit.source());
		}
		
		return readList;
	}
	public ArrayList<BlockMark> readOrPhanList(ElasticsearchClient esClient) throws ElasticsearchException, IOException {
		// TODO Auto-generated method stub
		SearchResponse<BlockMark> response = esClient.search(s->s.index(Indices.BlockMarkIndex)
				.query(q->q
						.term(t->t
								.field("status")
								.value(Preparer.ORPHAN)))
				.size(EsTools.READ_MAX)
				.sort(so->so
						.field(f->f
								.field("_fileOrder").order(SortOrder.Asc)
								.field("_pointer").order(SortOrder.Asc))
						)
				, BlockMark.class);
		
		List<Hit<BlockMark>> hitList = response.hits().hits();
		
		ArrayList<BlockMark> readList = new ArrayList<BlockMark>();
		
		Iterator<Hit<BlockMark>> iter = hitList.iterator();
		while(iter.hasNext()) {
			Hit<BlockMark> hit = iter.next();
			readList.add(hit.source());
		}
		
		return readList;
	}
	public ArrayList<BlockMark> readMainList(ElasticsearchClient esClient) throws ElasticsearchException, IOException {
		// TODO Auto-generated method stub
		SearchResponse<BlockMark> response = esClient.search(s->s.index(Indices.BlockMarkIndex)
				.query(q->q
						.term(t->t
								.field("status")
								.value(Preparer.MAIN)))
				.size(Preparer.CHECK_POINTS_NUM)
				.sort(so->so.field(f->f
						.field("height")
						.order(SortOrder.Asc)))
				, BlockMark.class);
		
		List<Hit<BlockMark>> hitList = response.hits().hits();
		
		ArrayList<BlockMark> readList = new ArrayList<BlockMark>();
		
		Iterator<Hit<BlockMark>> iter = hitList.iterator();
		while(iter.hasNext()) {
			Hit<BlockMark> hit = iter.next();
			readList.add(hit.source());
		}
		
		return readList;
	}
}
