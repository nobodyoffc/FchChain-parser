package tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.google.gson.Gson;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import data.Block;
import esClient.Indices;
import io.netty.buffer.Unpooled;

public class ParseTools {
	
	public static VarintResult parseVarint(ByteArrayInputStream blockInputStream) throws IOException {	
		//Byte[] List for merge all bytes readed./用于保存所读取字节数组的列表。
		ArrayList<byte[]> bl = new ArrayList<byte[]>();
		//Read 1 byte and turn it into unsigned./读1个字节并转换成整型。
		byte[] b = new byte[1];
		blockInputStream.read(b);
		bl.add(b);
		
		//log.debug("Paring varint. first byte is :{}",(int)b[0]);

		int size = Byte.toUnsignedInt(b[0]);
		long number = 0;
		
		if(size>=0 && size<=252) {
			number=(long)size;
			
		}else if(size==253){
			byte[] f = new byte[2];
			blockInputStream.read(f);
			bl.add(f);
			number = Unpooled.wrappedBuffer(f).readUnsignedShortLE();
			
		}else if(size==254) {
			byte[] f = new byte[4];
			blockInputStream.read(f);
			bl.add(f);
			number = Unpooled.wrappedBuffer(f).readUnsignedIntLE();
		}else {
			byte[] f = new byte[8];
			blockInputStream.read(f);
			bl.add(f);
			number = Unpooled.wrappedBuffer(f).readLongLE();
			System.exit(0);
		}
		//For return./将要返回的值。
		byte[] mergeBytes = BytesTools.bytesMerger(bl);
		
		VarintResult varint = new VarintResult();
		varint.rawBytes = mergeBytes;
		varint.number = number;
		
		return varint;
	}

	public static class VarintResult{
		public long number;
		public byte[] rawBytes;	
	}
	
	public static String calcTxoIdFromBytes(byte[] b36PreTxIdAndIndex) {
		String txoId = BytesTools.bytesToHexStringLE(Hash.Sha256x2(b36PreTxIdAndIndex));
		return txoId;
	}

	public static String calcTxoId(String txId, int j) {
		byte[] txIdBytes  = BytesTools.invertArray(BytesTools.hexToByteArray(txId));
		byte[] b4OutIndex = new byte[4];
		b4OutIndex = BytesTools.invertArray(BytesTools.intToByteArray(j));			
		String outId = BytesTools.bytesToHexStringLE(
			Hash.Sha256x2(
				BytesTools.bytesMerger(txIdBytes,b4OutIndex)
			));	
		return outId;
	}
	
	public static void gsonPrint(Object ob) {
		Gson gson = new Gson();
		System.out.println("***********\n"+ob.getClass().toString()+": "+gson.toJson(ob)+"\n***********");
		return ;
	}

	public static Block getBestBlock(ElasticsearchClient esClient) throws ElasticsearchException, IOException {
		SearchResponse<Block> result = esClient.search(s->s
				.index(Indices.BlockIndex)
				.size(1)
				.sort(so->so.field(f->f.field("height").order(SortOrder.Desc)))
				, Block.class);
		return result.hits().hits().get(0).source();
	}
}
