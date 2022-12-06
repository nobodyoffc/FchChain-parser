package writeEs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import data.Address;
import data.Block;
import data.BlockHas;
import data.OpReturn;
import data.Tx;
import data.TxHas;
import data.TxMark;
import data.Txo;
import data.TxoMark;
import esClient.EsTools;
import esClient.Indices;
import esClient.EsTools.MgetResult;
import parse.ReadyBlock;
import tools.FchTools;

public class BlockMaker {

	public ReadyBlock makeReadyBlock(ElasticsearchClient esClient, ReadyBlock rawBlock) throws Exception {

		if (rawBlock.getInList() == null || rawBlock.getInList().isEmpty()) {

			ReadyBlock txTxHasOpMadeBlock = makeTxTxHasOpReturn(rawBlock);
			ReadyBlock blockBlockHasMadeBlock = makeBlockBlockHas(txTxHasOpMadeBlock);
			ReadyBlock addrMadeBlock = makeAddress(esClient, blockBlockHasMadeBlock);
			addrMadeBlock.setOutWriteList(addrMadeBlock.getOutList());
			return addrMadeBlock;

		} else {
			ReadyBlock inputMadeBlock = makeInputList(esClient, rawBlock);
			inputMadeBlock.setOutList(rawBlock.getOutList());
			ReadyBlock txTxHasOpMadeBlock = makeTxTxHasOpReturn(inputMadeBlock);
			ReadyBlock blockBlockHasMadeBlock = makeBlockBlockHas(txTxHasOpMadeBlock);
			ReadyBlock addrMadeBlock = makeAddress(esClient, blockBlockHasMadeBlock);
			ReadyBlock readyBlock = addrMadeBlock;

			return readyBlock;
		}

	}

	private ReadyBlock makeInputList(ElasticsearchClient esClient, ReadyBlock rawBlock) throws Exception {
		// TODO Auto-generated method stub
		ReadyBlock inListMadeBlock = rawBlock;

		ArrayList<Txo> inList = inListMadeBlock.getInList();
		Map<String, Txo> inMap = new HashMap<String, Txo>();
		List<String> inStrList = new ArrayList<String>();
		for (Txo in : inList) {
			inMap.put(in.getId(), in);
			inStrList.add(in.getId());
		}

		ArrayList<Txo> outList = inListMadeBlock.getOutList();
		Map<String, Txo> outMap = new HashMap<String, Txo>();
		for (Txo out : outList) {
			outMap.put(out.getId(), out);
		}

		MgetResult<Txo> inMgetResult = EsTools.getMultiByIdList(esClient, Indices.TxoIndex, inStrList, Txo.class);
		ArrayList<Txo> inOldList = (ArrayList<Txo>) inMgetResult.getResultList();
		List<String> inNewIdList = inMgetResult.getMissList();

		ArrayList<Txo> inMadeList = new ArrayList<Txo>();
		ArrayList<Txo> outWriteList = new ArrayList<Txo>();

		for (Txo out : inOldList) {
			Txo in = inMap.get(out.getId());
			in.setAddr(out.getAddr());
			in.setOutIndex(out.getOutIndex());
			in.setType(out.getType());
			in.setValue(out.getValue());
			in.setLockScript(out.getLockScript());
			in.setTxId(out.getTxId());
			in.setTxIndex(out.getTxIndex());
			in.setBirthTime(out.getBirthTime());
			in.setBirthHeight(out.getBirthHeight());
			in.setCdd(FchTools.cdd(in.getValue(), in.getBirthTime(), in.getSpentTime()));

			inMadeList.add(in);
		}

		for (String id : inNewIdList) {
			Txo in = inMap.get(id);
			Txo out = outMap.get(id);
			in.setAddr(out.getAddr());
			in.setOutIndex(out.getOutIndex());
			in.setType(out.getType());
			in.setValue(out.getValue());
			in.setLockScript(out.getLockScript());
			in.setTxId(out.getTxId());
			in.setTxIndex(out.getTxIndex());
			in.setBirthTime(out.getBirthTime());
			in.setBirthHeight(out.getBirthHeight());
			in.setCdd(FchTools.cdd(in.getValue(), in.getBirthTime(), in.getSpentTime()));
			outMap.remove(id);
			inMadeList.add(in);
		}

		Set<String> idSet = outMap.keySet();
		for (String id : idSet) {
			outWriteList.add(outMap.get(id));
		}

		/*
		 * 
		 * for(String id : inNewIdList) { boolean found = false;
		 * 
		 * Iterator<Txo> iterIn = inList.iterator(); while(iterIn.hasNext()) { Txo in =
		 * iterIn.next(); if(id.equals(in.getId())) {
		 * 
		 * Iterator<Txo> iterOut = outList.iterator(); while(iterOut.hasNext()) { Txo
		 * out = iterOut.next(); if(id.equals(out.getId())){
		 * in.setOutIndex(out.getOutIndex()); in.setType(out.getType());
		 * in.setValue(out.getValue()); in.setLockScript(out.getLockScript());
		 * in.setTxId(out.getTxId()); in.setTxIndex(out.getTxIndex());
		 * in.setBirthTime(out.getBirthTime()); in.setBirthHeight(out.getBirthHeight());
		 * in.setCdd(FchTools.cdd(in.getValue(), in.getBirthTime(), in.getSpentTime()));
		 * 
		 * inMadeList.add(in); iterOut.remove(); found = true; break; } }
		 * if(found)break; } } }
		 * 
		 * for(Txo in: inList) { for(Txo inOld: inOldList) {
		 * if(in.getId().equals(inOld.getId())) { in.setOutIndex(inOld.getOutIndex());
		 * in.setType(inOld.getType()); in.setValue(inOld.getValue());
		 * in.setLockScript(inOld.getLockScript()); in.setTxId(inOld.getTxId());
		 * in.setBirthTime(inOld.getBirthTime());
		 * in.setBirthHeight(inOld.getBirthHeight());
		 * in.setCdd(FchTools.cdd(in.getValue(), in.getBirthTime(), in.getSpentTime()));
		 * 
		 * inMadeList.add(in); break; } } }
		 */

		inListMadeBlock.setInList(inMadeList);
		inListMadeBlock.setOutWriteList(outWriteList);
		inListMadeBlock.setOutList(outList);
		return inListMadeBlock;
	}

	private ReadyBlock makeTxTxHasOpReturn(ReadyBlock blockForMaking) {

		ArrayList<Txo> outList = blockForMaking.getOutList();
		ArrayList<Tx> txList = blockForMaking.getTxList();
		ArrayList<OpReturn> opList = blockForMaking.getOpReturnList();

		Map<String, Tx> txMap = new HashMap<String, Tx>();
		Map<String, TxHas> txHasMap = new HashMap<String, TxHas>();
		

		for (Tx tx : txList) {
			txMap.put(tx.getId(), tx);

			TxHas txHas = new TxHas();
			ArrayList<TxoMark> inMarks = new  ArrayList<TxoMark>();
			ArrayList<TxoMark> outMarks = new  ArrayList<TxoMark>();
			txHas.setId(tx.getId());
			txHas.setHeight(tx.getHeight());
			txHas.setInMarks(inMarks);
			txHas.setOutMarks(outMarks);
			txHasMap.put(tx.getId(), txHas);
		}

		if (blockForMaking.getInList() != null)
			for (Txo in : blockForMaking.getInList()) {
				long value = in.getValue();
				long cdd = in.getCdd();

				Tx tx = txMap.get(in.getSpentTxId());
				tx.setInValueT(tx.getInValueT() + value);
				tx.setCdd(tx.getCdd() + cdd);

				TxHas txHas = txHasMap.get(in.getSpentTxId());
				TxoMark inMark = new TxoMark();
				inMark.setId(in.getId());
				inMark.setAddr(in.getAddr());
				inMark.setValue(in.getValue());
				inMark.setCdd(in.getCdd());
				inMark.setIndex(in.getSpentIndex());
				
				txHas.getInMarks().add(inMark);
			}

		for (Txo out : outList) {
			long value = out.getValue();

			Tx tx = txMap.get(out.getTxId());
			tx.setOutValueT(tx.getOutValueT() + value);

			TxHas txHas = txHasMap.get(out.getTxId());
			TxoMark outMark = new TxoMark();
			outMark.setId(out.getId());
			outMark.setAddr(out.getAddr());
			outMark.setValue(out.getValue());
			outMark.setIndex(out.getOutIndex());

			txHas.getOutMarks().add(outMark);
		}
		

		if(opList!=null && !opList.isEmpty())
			for (OpReturn opReturn : opList) {
				String txId = opReturn.getId();
	
				opReturn.setCdd(txMap.get(txId).getCdd());
				String signer = txHasMap.get(txId).getInMarks().get(0).getAddr();
				opReturn.setSigner(signer);
	
				for (TxoMark txoB : txHasMap.get(txId).getOutMarks()) {
					String addr = txoB.getAddr();
					if (!addr.equals(signer) && !addr.equals("unknown") && addr.equals("OpReturn")) {
						opReturn.setRecipient(addr);
						break;
					}
				}
				if(opReturn.getRecipient() == null)opReturn.setRecipient("nobody");
			}

		Set<String> txIdSet = txMap.keySet();
		ArrayList<Tx> txGoodList = new ArrayList<Tx>();
		Iterator<String> itertx = txIdSet.iterator();
		while (itertx.hasNext()) {
			Tx tx = txMap.get(itertx.next());
			if(tx.getInCount()!=0)
				tx.setFee(tx.getInValueT()-tx.getOutValueT());
			txGoodList.add(tx);
		}

		ArrayList<TxHas> txHasGoodList = new ArrayList<TxHas>();
		Iterator<String> itertxhas = txIdSet.iterator();
		while (itertxhas.hasNext()) {
			txHasGoodList.add(txHasMap.get(itertxhas.next()));
		}

		ReadyBlock txAndTxHasMadeBlock = blockForMaking;

		txAndTxHasMadeBlock.setTxList(txGoodList);
		txAndTxHasMadeBlock.setTxHasList(txHasGoodList);
		txAndTxHasMadeBlock.setOpReturnList(opList);

		return txAndTxHasMadeBlock;
	}

	private ReadyBlock makeBlockBlockHas(ReadyBlock txAndTxHasMadeBlock) {
		// TODO Auto-generated method stub
		ArrayList<Tx> txList = txAndTxHasMadeBlock.getTxList();
		Block block = txAndTxHasMadeBlock.getBlock();
		BlockHas blockHas = new BlockHas();
		blockHas.setId(block.getId());
		blockHas.setHeight(block.getHeight());
		blockHas.setTxMarks(new ArrayList<TxMark>());

		for (Tx tx : txList) {
			block.setInValueT(block.getInValueT() + tx.getInValueT());
			block.setOutValueT(block.getOutValueT() + tx.getOutValueT());
			block.setFee(block.getFee()+tx.getFee());
			block.setCdd(block.getCdd()+tx.getCdd());

			TxMark txMark = new TxMark();
			txMark.setId(tx.getId());
			txMark.setOutValue(tx.getOutValueT());
			txMark.setIndex(tx.getTxIndex());

			if (tx.getInCount() != 0) {
				long fee = tx.getFee();
				txMark.setFee(fee);
				txMark.setCdd(tx.getCdd());
			}
			blockHas.getTxMarks().add(txMark);
		}

		ReadyBlock blockMadeBlock = txAndTxHasMadeBlock;

		blockMadeBlock.setBlock(block);
		blockMadeBlock.setBlockHas(blockHas);

		return blockMadeBlock;
	}

	private ReadyBlock makeAddress(ElasticsearchClient esClient, ReadyBlock readyBlock) throws Exception {
		// TODO Auto-generated method stub
		List<String> addrStrList = getAddrStrList(readyBlock);
		ArrayList<Address> addrList = readAddrListFromEs(esClient, addrStrList);

		Map<String, Address> addrMap = new HashMap<String, Address>();

		for (Address addr : addrList) {
			addrMap.put(addr.getId(), addr);
		}

		ArrayList<TxHas> txHasList = readyBlock.getTxHasList();

		for (TxHas txHas : txHasList) {

			if (txHas.getInMarks() != null && !txHas.getInMarks().isEmpty()) {
				for (TxoMark inb : txHas.getInMarks()) {
					String inAddr = inb.getAddr();
					long inValue = inb.getValue();
					long cdd = inb.getCdd();

					Address addr = addrMap.get(inAddr);
					addr.setExpend(addr.getExpend() + inValue);
					addr.setBalance(addr.getBalance() - inValue);
					addr.setCdd(addr.getCdd() + cdd);
					addr.setLastHeight(txHas.getHeight());
					addr.setUtxo(addr.getUtxo() - 1);

					if (addr.getPubkey() == null) {
						ArrayList<Txo> inList = readyBlock.getInList();
						Iterator<Txo> iter = inList.iterator();
						while (iter.hasNext()) {
							Txo in = iter.next();
							if (in.getAddr().equals(addr.getId()) && in.getType() == "P2PKH") {
								setPKAndMoreAddrs(addr, in.getUnlockScript());
								break;
							}
						}
					}
				}
			}
			for (TxoMark outb : txHas.getOutMarks()) {
				String outAddr = outb.getAddr();
				long outValue = outb.getValue();
				Address addr = addrMap.get(outAddr);
				addr.setIncome(addr.getIncome() + outValue);
				addr.setBalance(addr.getBalance() + outValue);
				addr.setLastHeight(txHas.getHeight());
				addr.setUtxo(addr.getUtxo() + 1);

				if (addr.getBirthHeight() == 0 && (!addr.getId().equals("FTqiqAyXHnK7uDTXzMap3acvqADK4ZGzts")))
					addr.setBirthHeight(txHas.getHeight());

				if (addr.getGuide() == null) {
					if (txHas.getInMarks() != null && !txHas.getInMarks().isEmpty()) {
						addr.setGuide(txHas.getInMarks().get(0).getAddr());
					} else
						addr.setGuide("coinbase");
				}
			}
		}

		Collection<Address> addrs = addrMap.values();
		ArrayList<Address> readyAddrList = new ArrayList<Address>();

		Iterator<Address> iter = addrs.iterator();
		while (iter.hasNext()) {
			readyAddrList.add(iter.next());
		}

		ReadyBlock addrMadeBlock = readyBlock;
		addrMadeBlock.setAddrList(readyAddrList);

		return addrMadeBlock;
	}

	private List<String> getAddrStrList(ReadyBlock readyBlock) {
		// TODO Auto-generated method stub
		ArrayList<Txo> inList = readyBlock.getInList();
		ArrayList<Txo> outList = readyBlock.getOutList();

		Set<String> addrStrSet = new HashSet<String>();

		for (Txo in : inList)
			addrStrSet.add(in.getAddr());
		for (Txo out : outList)
			addrStrSet.add(out.getAddr());

		List<String> addrStrList = new ArrayList<String>(addrStrSet);

		return addrStrList;
	}

	private ArrayList<Address> readAddrListFromEs(ElasticsearchClient esClient, List<String> addrStrList)
			throws Exception {
		// TODO Auto-generated method stub
		MgetResult<Address> addrMgetResult = EsTools.getMultiByIdList(esClient, Indices.AddressIndex, addrStrList,
				Address.class);

		ArrayList<Address> addrOldList = (ArrayList<Address>) addrMgetResult.getResultList();

		List<String> addrNewStrList = addrMgetResult.getMissList();
		ArrayList<Address> addrNewList = new ArrayList<Address>();

		for (String addrStr : addrNewStrList) {
			Address addr = new Address();
			addr.setId(addrStr);
			addrNewList.add(addr);
		}

		ArrayList<Address> addrList = new ArrayList<Address>();
		addrList.addAll(addrOldList);
		addrList.addAll(addrNewList);
		return addrList;
	}

	private Address setPKAndMoreAddrs(Address addr1, String unLockScript) {
		// TODO Auto-generated method stub
		Address addr = addr1;
		String pk = FchTools.parsePkFromUnlockScript(unLockScript);
		addr.setPubkey(pk);
		addr.setBtcAddr(FchTools.pubKeyToBtcAddr(pk));
		addr.setEthAddr(FchTools.pubKeyToEthAddr(pk));
		addr.setLtcAddr(FchTools.pubKeyToLtcAddr(pk));
		addr.setDogeAddr(FchTools.pubKeyToDogeAddr(pk));
		addr.setTrxAddr(FchTools.pubKeyToTrxAddr(pk));
		return addr;
	}
}
