package writeEs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import data.OpReturn;
import start.Indices;
import tools.BytesTools;

public class RollBacker {
	private static final Logger log = LoggerFactory.getLogger(RollBacker.class);

	public boolean rollback(ElasticsearchClient esClient, long lastHeight) throws Exception {
		
		long bestHeight = getBestHeight(esClient);
		if(bestHeight==lastHeight) {
			System.out.println("The height you are rollbacking to equales to the best height:" +bestHeight );
			return true;
		}
		
		recoverAddress(esClient, lastHeight);
		
		recoverStxoToUtxo(esClient, lastHeight);

		try {
			deleteBlocks(esClient, lastHeight);
		} catch (Exception e) {
			log.error("Error when deleting in rollback",e);
		}
		try {
			deleteBlockHas(esClient, lastHeight);
		} catch (Exception e) {
			log.error("Error when deleting in rollback",e);
		}
		try {
			deleteTxs(esClient, lastHeight);
		} catch (Exception e) {
			log.error("Error when deleting in rollback",e);
		}
		try {
			deleteTxHas(esClient, lastHeight);
		} catch (Exception e) {
			log.error("Error when deleting in rollback",e);
		}
		try {
			deleteOpReturns(esClient, lastHeight);
		} catch (Exception e) {
			log.error("Error when deleting in rollback",e);
		}
		try {
			deleteUtxos(esClient, lastHeight);
		} catch (Exception e) {
			log.error("Error when deleting in rollback",e);
		}
		try {
			deleteNewAddresses(esClient, lastHeight);
		} catch (Exception e) {
			log.error("Error when deleting in rollback",e);
		}
		try {
			deleteBlockMarks(esClient, lastHeight);
		} catch (Exception e) {
			log.error("Error when deleting in rollback",e);
		}
		
		
		
		recordInOpReturnFile(lastHeight);
		return true;
	}
	
	public long getBestHeight(ElasticsearchClient esClient) throws ElasticsearchException, IOException {
		SearchResponse<Void> response = esClient.search(s->s.index(Indices.BlockIndex).aggregations("bestHeight", a->a.max(m->m.field("height"))), void.class);		
		long bestHeight = (long)response.aggregations().get("bestHeight").max().value();
		return bestHeight;
	}
	private void recoverAddress(ElasticsearchClient esClient, long lastHeight) throws Exception {

		ArrayList<String> addrList = readAllAddrs(esClient,lastHeight);
		
		Map<String, Map<String, Long>> aggsMaps = aggsTxoByAddrs(esClient,lastHeight, addrList);
		
		bulkUpdateAddr(esClient,aggsMaps);
	}
	private void bulkUpdateAddr(ElasticsearchClient esClient, Map<String, Map<String, Long>> aggsMaps) throws ElasticsearchException, IOException {
		// TODO Auto-generated method stub

		
		Map<String, Long> utxoSumMap = aggsMaps.get("income");
		Map<String, Long> stxoSumMap = aggsMaps.get("spend");
		Map<String, Long> stxoCddMap = aggsMaps.get("cdd");
		Map<String, Long> utxoCountMap = aggsMaps.get("utxoCount");
		
		
		Set<String> ukset = utxoSumMap.keySet();
		Set<String> skset = stxoSumMap.keySet();
		Set<String> addrSet = new HashSet<String>();
		addrSet.addAll(ukset);
		addrSet.addAll(skset);
		
		if(addrSet.isEmpty())return;
		
		BulkRequest.Builder br = new BulkRequest.Builder();
		
		for(String addr : addrSet) {
			
			Map<String,Object> updateMap = new HashMap<String,Object>();
			
			if(utxoSumMap.get(addr)!=null) {
				updateMap.put("balance", utxoSumMap.get(addr));
				updateMap.put("utxo", utxoCountMap.get(addr));
			}else {
				updateMap.put("balance", 0);
			}
			
			if(stxoSumMap.get(addr)!=null) {
				updateMap.put("expend", stxoSumMap.get(addr));
			}else {
				updateMap.put("expend", 0);
			}
			
			if(stxoCddMap.get(addr)!=null) {
				updateMap.put("cdd", stxoCddMap.get(addr));
			}else {
				updateMap.put("cdd", 0);
			}

			if(utxoSumMap.get(addr)!=null) {
				if(stxoSumMap.get(addr)!=null) {
					updateMap.put("income", utxoSumMap.get(addr)+stxoSumMap.get(addr));
				}else {
					updateMap.put("income", utxoSumMap.get(addr)+0);
				}
			}else {
				if(stxoSumMap.get(addr)!=null) {
					updateMap.put("income", 0+stxoSumMap.get(addr));
				}
			}
			
			br.operations(o1->o1.update(u->u
					.index(Indices.AddressIndex)
					.id(addr)
					.action(a->a
							.doc(updateMap)))
					);
		}
		br.timeout(t->t.time("600s"));	
		esClient.bulk(br.build());
	}

	private ArrayList<String> readAllAddrs(ElasticsearchClient esClient, long lastHeight) throws ElasticsearchException, IOException {
		
		SearchResponse<Void> response = esClient.search(s->s
				.index(Indices.AddressIndex)
				.query(q->q
						.bool(b->b.must(s1->s1.range(r->r.field("spentHeight").gt(JsonData.of(lastHeight))))
								.must(s2->s2.range(r1->r1.field("birthHeight").lte(JsonData.of(lastHeight))))
								)
						)
				.size(0)
				.aggregations("addr",a->a
						.terms(t->t.field("addr")
								.size(200000)))
				, void.class);
		
		List<StringTermsBucket> buckets = response.aggregations().get("addr").sterms().buckets().array();
		
		ArrayList<String> addrAllList = new ArrayList<String> ();
		for (StringTermsBucket bucket: buckets) {
			String addr = bucket.key();		
			addrAllList.add(addr);
		}
		return addrAllList;
	}
	private Map<String, Map<String, Long>> aggsTxoByAddrs(ElasticsearchClient esClient, long lastHeight, List<String> addrAllList) throws ElasticsearchException, IOException {

		List<FieldValue> fieldValueList = new ArrayList<FieldValue>();
		
		Iterator<String> iter = addrAllList.iterator();
		while(iter.hasNext()) 
			fieldValueList.add(FieldValue.of(iter.next()));
		
		SearchResponse<Void> response = esClient.search(s->s
				.index(Indices.TxoIndex)
				.query(q->q.bool(b->b
						.must(m->m.range(r->r.field("spentHeight").lte(JsonData.of(lastHeight))))
						.must(m1->m1.range(r1->r1.field("birthHeight").lte(JsonData.of(lastHeight)))))
						)
				.size(0)
				.aggregations("addrFilterAggs",a->a
						.filter(f->f.terms(t->t
								.field("addr")
								.terms(t1->t1
										.value(fieldValueList))))
						.aggregations("utxoFilterAggs",a0->a0
								.filter(f1->f1.match(m->m.field("spentHeight").query(0)))
								.aggregations("incomeAggs",a3->a3
									.terms(t2->t2
											.field("addr")
											.size(200000))
									.aggregations("incomeSum",t5->t5
											.sum(s1->s1
													.field("value"))))
								)	
						.aggregations("stxoFilterAggs",a0->a0
								.filter(f1->f1.range(r4->r4.field("spentHeight").gt(JsonData.of(0))))
								.aggregations("expendAggs",a1->a1
										.terms(t2->t2
												.field("addr")
												.size(200000))
										.aggregations("spendSum",t3->t3
												.sum(s1->s1
														.field("value")))
										.aggregations("cddSum",t4->t4
												.sum(s1->s1
														.field("cdd")))
										)
								)
						)
				, void.class);
	
		Map<String, Long> incomeMap = new HashMap<String, Long>();
		Map<String, Long> expendMap = new HashMap<String, Long>();
		Map<String, Long> cddMap = new HashMap<String, Long>();
		Map<String, Long> utxoCountMap = new HashMap<String, Long>();
		
		List<StringTermsBucket> utxoBuckets = response.aggregations()
			    .get("addrFilterAggs")
			    .filter()
			    .aggregations()
			    .get("utxoFilterAggs")
			    .filter()
			    .aggregations()
			    .get("incomeAggs")
			    .sterms()
			    .buckets().array(); 
		
		for (StringTermsBucket bucket: utxoBuckets) {
			String addr = bucket.key();		
			long value1 = (long)bucket.aggregations().get("incomeSum").sum().value();
			utxoCountMap.put(addr, bucket.docCount());
			incomeMap.put(addr, value1);
		}
		
		List<StringTermsBucket> stxoBuckets = response.aggregations()
			    .get("addrFilterAggs")
			    .filter()
			    .aggregations()
			    .get("stxoFilterAggs")
			    .filter()
			    .aggregations()
			    .get("expendAggs")
			    .sterms()
			    .buckets().array(); 
		
		for (StringTermsBucket bucket: stxoBuckets) {
			String addr = bucket.key();		
			long value1 = (long)bucket.aggregations().get("spendSum").sum().value();
			expendMap.put(addr, value1);
			long cddSum = (long)bucket.aggregations().get("cddSum").sum().value();
			cddMap.put(addr, cddSum);
		}
		
		Map<String,Map<String, Long>> resultMapMap = new HashMap<String,Map<String, Long>>();	
		resultMapMap.put("income",incomeMap);
		resultMapMap.put("spend",expendMap);
		resultMapMap.put("cdd",cddMap);
		resultMapMap.put("utxoCount",utxoCountMap);
		return resultMapMap;
	}

	private void recoverStxoToUtxo(ElasticsearchClient esClient, long lastHeight) throws Exception {		
		esClient.updateByQuery(u->u
				.index(Indices.TxoIndex)
				.query(q->q.bool(b->b
						.must(m->m.range(r->r.field("spentHeight").gt(JsonData.of(lastHeight))))
						.must(m1->m1.range(r1->r1.field("birthHeight").lte(JsonData.of(lastHeight))))))
				.script(s->s.inline(i->i.source(
						"ctx._source.spentTime=0;"
						+ "ctx._source.spentTxId=null;"
						+ "ctx._source.spentHeight=0;"
						+ "ctx._source.spentIndex=0;"
						+ "ctx._source.unlockScript=null;"
						+ "ctx._source.sigHash=null;"
						+ "ctx._source.sequence=null;"
						+ "ctx._source.cdd=0;"
						+ "ctx._source.utxo=true;"
						)))
				);
	}

	private void deleteOpReturns(ElasticsearchClient esClient, long lastHeight) throws Exception {
		deleteHeigherThan(esClient,Indices.OpReturnIndex,"height",lastHeight);
	}
	private void deleteBlocks(ElasticsearchClient esClient, long lastHeight) throws Exception {
		deleteHeigherThan(esClient,Indices.BlockIndex,"height",lastHeight);
	}
	private void deleteBlockHas(ElasticsearchClient esClient, long lastHeight) throws Exception {
		deleteHeigherThan(esClient,Indices.BlockHasIndex,"height",lastHeight);
	}
	private void deleteTxHas(ElasticsearchClient esClient, long lastHeight) throws Exception {
		deleteHeigherThan(esClient,Indices.TxHasIndex,"height",lastHeight);
	}
	private void deleteTxs(ElasticsearchClient esClient, long lastHeight) throws Exception {
		deleteHeigherThan(esClient,Indices.TxIndex,"height",lastHeight);
	}
	private void deleteUtxos(ElasticsearchClient esClient, long lastHeight) throws Exception {
		deleteHeigherThan(esClient,Indices.TxoIndex,"birthHeight",lastHeight);
	}
	private void deleteNewAddresses(ElasticsearchClient esClient, long lastHeight) throws Exception {
		deleteHeigherThan(esClient,Indices.AddressIndex,"birthHeight",lastHeight);
	}
	private void deleteBlockMarks(ElasticsearchClient esClient, long lastHeight) throws Exception {
		// TODO Auto-generated method stub
		deleteHeigherThan(esClient,Indices.BlockMarkIndex,"height",lastHeight);
	}
	private void deleteHeigherThan(ElasticsearchClient esClient, String index, String rangeField,long lastHeight) throws Exception {

		esClient.deleteByQuery(d->d
								.index(index)
								.query(q->q
										.range(r->r
												.field(rangeField)
												.gt(JsonData.of(lastHeight))))
				);

	}
	
	private void recordInOpReturnFile(long lastHeight) throws IOException {
		
		OpReturn opRollBack = new OpReturn();//rollbackMarkInOpreturn 
		opRollBack.setHeight(lastHeight);

		File opFile = new File("opreturn.byte");
		FileOutputStream opos = new FileOutputStream(opFile,true);
		
		ArrayList<byte[]> opArrList = new ArrayList<byte[]>();
		opArrList.add(BytesTools.intToByteArray(32));
		opArrList.add(BytesTools.longToBytes(opRollBack.getHeight()));
		
		opos.write(BytesTools.bytesMerger(opArrList));
		opos.close();
	}

}
