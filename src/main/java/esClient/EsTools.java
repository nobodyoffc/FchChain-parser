package esClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkRequest.Builder;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.MgetRequest;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;

public class EsTools {
	
	public static final int READ_MAX = 5000;
	public static final int WRITE_MAX = 3000;
	
	final static Logger log = LoggerFactory.getLogger(EsTools.class);
	
	public static <T> MgetResult<T> getMultiByIdList(
			ElasticsearchClient esClient,
			String index, 
			List<String> idList, 
			Class<T> classType
			) throws Exception{
		MgetResult<T> result = new MgetResult<T>();
		
		ArrayList<T> resultList = new ArrayList<T>();
		ArrayList<String> missList = new ArrayList<String>();
		
		if(idList.size()>READ_MAX) {

			Iterator<String> iter = idList.iterator();
			for(int i=0; i< idList.size()/READ_MAX+1 ;i++){
				
				ArrayList<String> idSubList = new ArrayList<String>();
				for(int j = 0; j<idList.size()-i*READ_MAX && j< READ_MAX;j++) {
					idSubList.add(iter.next());
				}	
				
				MgetResult<T> mgetResult = mgetWithNull(esClient,index,idSubList,classType);
				
				resultList.addAll(mgetResult.getResultList());
				missList.addAll(mgetResult.getMissList());
			}
			result.setResultList(resultList);
			result.setMissList(missList);
		}else {
			result =  mgetWithNull(esClient,index,idList,classType);
			}
		return result;
		}
	
	
	
	private static <T> MgetResult<T> mgetWithNull(ElasticsearchClient esClient,String index, List<String> idList,  Class<T> classType) throws ElasticsearchException, IOException{
		
		
		ArrayList<T> resultList = new ArrayList<T>();
		ArrayList<String> missList = new ArrayList<String>();
		
		MgetRequest.Builder mgetRequestBuilder = new MgetRequest.Builder();
		mgetRequestBuilder
			.index(index)
			.ids(idList);
		MgetRequest mgetRequest = mgetRequestBuilder.build();
		MgetResponse<T> mgetResponse = null;
		
		mgetResponse = esClient.mget(mgetRequest,classType);
		
		
		List<MultiGetResponseItem<T>> items = mgetResponse.docs();
		
		ListIterator<MultiGetResponseItem<T>> iter = items.listIterator();
		while(iter.hasNext()) {
			MultiGetResponseItem<T> item = iter.next();
			if(item.result().found()) {
			resultList.add(item.result().source());
			}else {
			missList.add(item.result().id());
			}
		}
		MgetResult<T> result = new MgetResult<T>();
		result.setMissList(missList);
		result.setResultList(resultList);
		
		return result;
	}
	public static class MgetResult<E>{
		private List<String> missList;
		private List<E> resultList;
		
		public List<String> getMissList() {
			return missList;
		}
		public void setMissList(List<String> missList) {
			this.missList = missList;
		}
		public List<E> getResultList() {
			return resultList;
		}
		public void setResultList(List<E> resultList) {
			this.resultList = resultList;
		}
	}
			
	public static <T> BulkResponse bulkWriteList(ElasticsearchClient esClient
			,String indexT, ArrayList<T> tList
			,ArrayList<String> idList
			,Class<T> classT) throws Exception {
		
		if(tList.isEmpty()) return null;
		if(tList.isEmpty())return null;
		BulkResponse response = null;
		
		Iterator<T> iter = tList.iterator();	
		Iterator<String> iterId = idList.iterator();
		for(int i=0;i<tList.size()/READ_MAX+1;i++) {
			
			BulkRequest.Builder br = new BulkRequest.Builder();
			
			for(int j = 0; j< READ_MAX && i*READ_MAX+j<tList.size();j++) {
				T t = iter.next();
				String tid = iterId.next();
				br.operations(op->op.index(in->in
						.index(indexT)
						.id(tid)
						.document(t)));
			}
			response = bulkWithBuilder(esClient,br);
			if(response.errors()) return response;
		}
		return response;
	}
	
	public static BulkResponse bulkDeleteList(ElasticsearchClient esClient,String index, ArrayList<String> idList) throws Exception
			 {
		if(idList==null) return null;
		if(idList.isEmpty())return null;
		BulkResponse response = null;
		
		Iterator<String> iterId = idList.iterator();
		for(int i=0;i<(Math.ceil(idList.size()/READ_MAX));i++) {
			
			BulkRequest.Builder br = new BulkRequest.Builder();
			
			for(int j = 0; j< READ_MAX && i*READ_MAX+j<idList.size();j++) {
				String tid = iterId.next();
				br.operations(op->op.delete(in->in
						.index(index)
						.id(tid)));
			}
			br.timeout(t->t.time("600s"));			
			response = esClient.bulk(br.build());
			
			if(response.errors()) return response;
		}
		return response;
	}
	
	public static BulkResponse bulkWithBuilder(ElasticsearchClient esClient, Builder br) throws Exception {
		br.timeout(t->t.time("600s"));			
		BulkResponse response = esClient.bulk(br.build());
		return response;
	}
}
