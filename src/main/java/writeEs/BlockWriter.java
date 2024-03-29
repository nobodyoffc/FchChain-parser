package writeEs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkRequest.Builder;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import data.Address;
import data.Block;
import data.BlockHas;
import data.BlockMark;
import data.OpReturn;
import data.Tx;
import data.TxHas;
import data.Cash;
import esClient.EsTools;
import parser.Preparer;
import parser.ReadyBlock;
import redis.clients.jedis.Jedis;
import parser.OpReFileTools;

public class BlockWriter {

	private static final Logger log = LoggerFactory.getLogger(BlockWriter.class);
	private static Jedis jedis = new Jedis();

	public void writeIntoEs(ElasticsearchClient esClient, ReadyBlock readyBlock1,OpReFileTools opReFile) throws Exception {
		ReadyBlock readyBlock = readyBlock1;	

		Block block = readyBlock.getBlock();
		BlockHas blockHas = readyBlock.getBlockHas();
		ArrayList<Tx> txList = readyBlock.getTxList();
		ArrayList<TxHas> txHasList = readyBlock.getTxHasList();
		ArrayList<Cash> inList = readyBlock.getInList();
		ArrayList<Cash> outList = readyBlock.getOutWriteList();
		ArrayList<OpReturn> opReturnList = readyBlock.getOpReturnList();
		BlockMark blockMark = readyBlock.getBlockMark();
		ArrayList<Address> addrList = readyBlock.getAddrList();
		
		opReFile.writeOpReturnListIntoFile(opReturnList);


		Builder br = new BulkRequest.Builder();
		putBlock(esClient, block, br);
		putBlockHas(blockHas, br);
		putTx(esClient, txList, br);
		putTxHas(esClient, txHasList, br);
		putUtxo(esClient, outList, br);
		putStxo(esClient, inList, br);
		putOpReturn(esClient, opReturnList, br);
		putAddress(esClient, addrList, br);
		putBlockMark(esClient, blockMark, br);
		BulkResponse response = EsTools.bulkWithBuilder(esClient, br);

		try {
			jedis.set("bestHeight", String.valueOf(block.getHeight()));
			jedis.set("bestBlockId", block.getId());
		}catch(Exception e){
			log.warn("Redis isn't ready. Reading redis is ignored.");
		}

		System.out.println("Main chain linked. "
				+"Orphan: "+Preparer.orphanList.size()
				+" Fork: "+Preparer.forkList.size()
				+" id: "+blockMark.getId()
				+" file: "+Preparer.CurrentFile
				+" pointer: "+Preparer.Pointer
				+" Height:"+blockMark.getHeight());

		
		response.items().iterator();

		if (response.errors()) {
			log.error("bulkWriteToEs error");
			for(BulkResponseItem item:response.items()) {
				if(item.error()!=null) {
					System.out.println("index: "+item.index()+", Type: "+item.error().type()+"\nReason: "+item.error().reason());
				}
			}	
			throw new Exception("bulkWriteToEs error");
		};

		Preparer.mainList.add(blockMark);
		if (Preparer.mainList.size() > EsTools.READ_MAX) {
			Preparer.mainList.remove(0);
		}
		Preparer.BestHash = blockMark.getId();
		Preparer.BestHeight = blockMark.getHeight();
	}

	private void putBlockMark(ElasticsearchClient esClient, BlockMark blockMark, Builder br) throws Exception {

		// BulkRequest.Builder br = new BulkRequest.Builder();
		br.operations(op -> op.index(i -> i.index(IndicesFCH.BlockMarkIndex).id(blockMark.getId()).document(blockMark)));
		// EsTools.bulkWithBuilder(esClient, br);
	}

	private void putAddress(ElasticsearchClient esClient, ArrayList<Address> addrList, Builder br) throws Exception {

		if (addrList.size() > EsTools.WRITE_MAX / 5) {
			Iterator<Address> iter = addrList.iterator();
			ArrayList<String> idList = new ArrayList<String>();
			while (iter.hasNext())
				idList.add(iter.next().getId());
			EsTools.bulkWriteList(esClient, IndicesFCH.AddressIndex, addrList, idList, Address.class);
			TimeUnit.SECONDS.sleep(3);
		} else {
			Iterator<Address> iterAd = addrList.iterator();
			while (iterAd.hasNext()) {
				Address am = iterAd.next();
				br.operations(op -> op.index(i -> i.index(IndicesFCH.AddressIndex).id(am.getId()).document(am)));
			}
		}
	}

	private void putOpReturn(ElasticsearchClient esClient, ArrayList<OpReturn> opReturnList, Builder br)
			throws Exception {

		if (opReturnList != null) {
			if (opReturnList.size() > 100) {
				Iterator<OpReturn> iter = opReturnList.iterator();
				ArrayList<String> idList = new ArrayList<String>();
				while (iter.hasNext())
					idList.add(iter.next().getId());
				EsTools.bulkWriteList(esClient, IndicesFCH.OpReturnIndex, opReturnList, idList, OpReturn.class);
				TimeUnit.SECONDS.sleep(3);
			} else {
				Iterator<OpReturn> iterOR = opReturnList.iterator();
				while (iterOR.hasNext()) {
					OpReturn or = iterOR.next();
					br.operations(op -> op.index(i -> i.index(IndicesFCH.OpReturnIndex).id(or.getId()).document(or)));
				}
			}
		}
	}

	private void putStxo(ElasticsearchClient esClient, ArrayList<Cash> inList, Builder br) throws Exception {
		if (inList != null) {
			if (inList.size() > EsTools.WRITE_MAX / 5) {
				Iterator<Cash> iter = inList.iterator();
				ArrayList<String> idList = new ArrayList<String>();
				while (iter.hasNext())
					idList.add(iter.next().getId());
				EsTools.bulkWriteList(esClient, IndicesFCH.CashIndex, inList, idList, Cash.class);
				TimeUnit.SECONDS.sleep(3);
			} else {
				Iterator<Cash> iterTxo = inList.iterator();
				while (iterTxo.hasNext()) {
					Cash om = iterTxo.next();
					br.operations(op -> op.index(i -> i.index(IndicesFCH.CashIndex).id(om.getId()).document(om)));
				}
			}
		}
	}

	private void putUtxo(ElasticsearchClient esClient, ArrayList<Cash> outList, Builder br) throws Exception {
		if (outList.size() > EsTools.WRITE_MAX / 5) {
			Iterator<Cash> iter = outList.iterator();
			ArrayList<String> idList = new ArrayList<String>();
			while (iter.hasNext())
				idList.add(iter.next().getId());
			EsTools.bulkWriteList(esClient, IndicesFCH.CashIndex, outList, idList, Cash.class);
			TimeUnit.SECONDS.sleep(3);
		} else {
			Iterator<Cash> iterTxo = outList.iterator();
			while (iterTxo.hasNext()) {
				Cash om = iterTxo.next();
				br.operations(op -> op.index(i -> i.index(IndicesFCH.CashIndex).id(om.getId()).document(om)));
			}
		}
	}

	private void putTxHas(ElasticsearchClient esClient, ArrayList<TxHas> txHasList, Builder br) throws Exception {
		if (txHasList != null) {
			if (txHasList.size() > EsTools.WRITE_MAX / 5) {
				Iterator<TxHas> iter = txHasList.iterator();
				ArrayList<String> idList = new ArrayList<String>();
				while (iter.hasNext())
					idList.add(iter.next().getId());
				EsTools.bulkWriteList(esClient, IndicesFCH.TxHasIndex, txHasList, idList, TxHas.class);
				TimeUnit.SECONDS.sleep(3);
			} else {
				Iterator<TxHas> iterOInTx = txHasList.iterator();
				while (iterOInTx.hasNext()) {
					TxHas ot = iterOInTx.next();
					br.operations(op -> op.index(i -> i.index(IndicesFCH.TxHasIndex).id(ot.getId()).document(ot)));
				}
			}
		}
	}

	private void putTx(ElasticsearchClient esClient, ArrayList<Tx> txList, Builder br) throws Exception {
		if (txList.size() > EsTools.WRITE_MAX / 5) {
			Iterator<Tx> iter = txList.iterator();
			ArrayList<String> idList = new ArrayList<String>();
			while (iter.hasNext())
				idList.add(iter.next().getId());
			EsTools.bulkWriteList(esClient, IndicesFCH.TxIndex, txList, idList, Tx.class);
			TimeUnit.SECONDS.sleep(3);
		} else {
			Iterator<Tx> iterTx = txList.iterator();
			while (iterTx.hasNext()) {
				Tx tm = iterTx.next();
				br.operations(op -> op.index(i -> i.index(IndicesFCH.TxIndex).id(tm.getId()).document(tm)));
			}
		}
	}

	private void putBlockHas(BlockHas blockHas, Builder br) {
		br.operations(op -> op.index(i -> i.index(IndicesFCH.BlockHasIndex).id(blockHas.getId()).document(blockHas)));
	}

	private void putBlock(ElasticsearchClient esClient, Block block, Builder br) throws Exception {
		br.operations(op -> op.index(i -> i.index(IndicesFCH.BlockIndex).id(block.getId()).document(block)));
	}
}
