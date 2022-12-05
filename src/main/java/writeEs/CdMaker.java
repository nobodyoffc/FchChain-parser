package writeEs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import data.Address;
import esClient.EsTools;
import parse.Preparer;
import start.Indices;

public class CdMaker {

	public void makeUtxoCd(ElasticsearchClient esClient)
			throws ElasticsearchException, IOException, InterruptedException {

		long now = System.currentTimeMillis() / 1000;
		long bestHeight = Preparer.mainList.get(Preparer.mainList.size() - 1).getHeight();

		for (int i = 0;; i += 5000) {
			long fromHeight = i;
			esClient.updateByQuery(u -> u.index(Indices.TxoIndex)
					.query(q -> q.bool(b -> b.filter(f -> f.term(t -> t.field("utxo").value(true)))
							.must(m -> m.range(r -> r.field("birthHeight").gte(JsonData.of(fromHeight))
									.lt(JsonData.of(fromHeight + 5000))))))
					.sort("height:asc")
					.script(s -> s.inline(i1 -> i1.source(
							"ctx._source.cd = (long)((((long)(params.now - ctx._source.blockTime)/86400)*ctx._source.value)/100000000)")
							.params("now", JsonData.of(now)))));
			if (fromHeight + 5000 > bestHeight)
				break;
			TimeUnit.SECONDS.sleep(3);
		}
	}

	public void makeAddrCd(ElasticsearchClient esClient) throws Exception {
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

	private ArrayList<Address> getResultAddrList(SearchResponse<Address> response) {
		// TODO Auto-generated method stub
		ArrayList<Address> addrList = new ArrayList<Address>();
		for (Hit<Address> hit : response.hits().hits()) {
			addrList.add(hit.source());
		}
		return addrList;
	}

	private Map<String, Long> makeAddrList(ElasticsearchClient esClient, ArrayList<Address> addrOldList)
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

	private void updateAddrMap(ElasticsearchClient esClient, Map<String, Long> addrNewMap) throws Exception {
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
}
