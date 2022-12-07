package parse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import data.Block;
import data.BlockMark;
import esClient.Indices;
import tools.BytesTools;
import tools.FileTools;
import tools.Hash;
import tools.OpReFile;
import tools.ParseTools;
import writeEs.BlockMaker;
import writeEs.BlockWriter;
import writeEs.CdMaker;
import writeEs.RollBacker;

public class MainParser {
	
	public static final int FILE_END = -1;
	public static final int WRONG = -2;
	public static final int HEADER_FORK = -3;
	public static final int REPEAT = -4;
	public static final int WAIT_MORE = 0;
	public static final String MAGIC = "f9beb4d9";
	
	private static final Logger log = LoggerFactory.getLogger(MainParser.class);
	
	private OpReFile opReFile = new OpReFile();
	
	public int startParse(ElasticsearchClient esClient) throws Exception {
		
		System.out.println("Started parsing file:  "+Preparer.CurrentFile+" ...");
		log.info("Started parsing file: {} ...",Preparer.CurrentFile);
		
		File file = new File(Preparer.Path,Preparer.CurrentFile);
		FileInputStream fis = new FileInputStream(file);
		fis.skip(Preparer.Pointer);
		
		int blockLength;

		long cdMakeTime = System.currentTimeMillis();

		while(true) {
			CheckResult checkResult = checkBlock(fis);
			BlockMark blockMark = checkResult.getBlockMark();
			byte[] blockBytes = checkResult.getBlockBytes();	
			
			blockLength = checkResult.getBlockLength();
			
			if(blockLength == FILE_END) {
				String nextFile = FileTools.getNextFile(Preparer.CurrentFile);
				if(new File(Preparer.Path, nextFile).exists()) {
					System.out.println("file "+Preparer.CurrentFile+" finished.");	
					log.info("Parsing file {} finished.",Preparer.CurrentFile);
					Preparer.CurrentFile = nextFile;
					Preparer.Pointer = 0;
					fis.close();
					file = new File(Preparer.Path,Preparer.CurrentFile);
					fis = new FileInputStream(file);
					continue;
				}else {
				System.out.println("Waiting 30 seconds for new file ...");	
				TimeUnit.SECONDS.sleep(30);
				fis.close();
				fis = new FileInputStream(file);
				fis.skip(Preparer.Pointer);
				}
			}else 
			if(blockLength == WRONG ) {
				System.out.println("Read Magic wrong. pointer: "+Preparer.Pointer);
				log.info("Read Magic wrong. pointer: {}",Preparer.Pointer);
				opReFile.close();
				return WRONG;
				
			}else if(blockLength == HEADER_FORK) {
				Preparer.Pointer = Preparer.Pointer + 88;
				fis.close();
				fis = new FileInputStream(file);
				fis.skip(Preparer.Pointer);
				
			}else if(blockLength == WAIT_MORE) {
				System.out.println("Waiting 30 seconds. pointer: "+Preparer.Pointer);
				TimeUnit.SECONDS.sleep(30);
				fis.close();
				fis = new FileInputStream(file);
				fis.skip(Preparer.Pointer);
				
			}else {
				
				linkToChain(esClient, blockMark,blockBytes);	
				
				recheckOrphans(esClient);
				
				Preparer.Pointer += blockLength;
			}
			
			Block bestBlock = ParseTools.getBestBlock(esClient);
			long bestBlockTime = bestBlock.getTime();
			if(bestBlockTime*1000 - cdMakeTime > (1000*60*60*12)) {
				CdMaker cdMaker = new CdMaker();

				cdMaker.makeUtxoCd(esClient,bestBlock);
				log.info("All cd of UTXOs updated.");
				TimeUnit.MINUTES.sleep(2);

				cdMaker.makeAddrCd(esClient);
				log.info("All cd of addresses updated.");
				TimeUnit.MINUTES.sleep(1);
				
				cdMakeTime = bestBlockTime;
			}
		}
	}

	private CheckResult checkBlock(FileInputStream fis) throws Exception {
		
		BlockMark blockMark = new BlockMark();	
		blockMark.set_pointer(Preparer.Pointer);
		blockMark.set_fileOrder(getFileOrder());
		
		CheckResult checkResult = new CheckResult();
		
		byte []	b8 = new byte[8];
		byte []	b4 = new byte[4];	

		if(fis.read(b8) == FILE_END) {
			System.out.println("File end when reading magic. pointer: "+Preparer.Pointer);
			log.error("File end when reading magic. ");
			checkResult.setBlockLength(FILE_END);
			return checkResult;
		} 

		if(b8[0]==0) {
			checkResult.setBlockLength(WAIT_MORE); 
			return checkResult;
		}

		b4 = Arrays.copyOfRange(b8, 0, 4);
		String magic = BytesTools.bytesToHexStringBE(b4) ;
		if(!magic.equals(MAGIC)) {
			checkResult.setBlockLength(WRONG);
			return checkResult;
		}
		
		b4 = Arrays.copyOfRange(b8, 4, 8);
		int blockSize = (int) BytesTools.bytes4ToLongLE(b4);
		blockMark.setSize(blockSize);
		
		if(blockSize==0) {
			checkResult.setBlockLength(WAIT_MORE); 
			return checkResult;
		}
		
		//Check valid header fork
		if(blockSize == 80) {
			checkResult.setBlockLength(HEADER_FORK);
			return checkResult;
		}

		byte[] blockBytes = new byte[blockSize];
		if(fis.read(blockBytes)== FILE_END) {
			System.out.println("File end when reading block. pointer: "+Preparer.Pointer);
			log.error("File end when reading block. Pointer:"+ Preparer.Pointer);
			checkResult.setBlockLength(FILE_END);
			return checkResult;
		}

		ByteArrayInputStream blockInputStream = new ByteArrayInputStream(blockBytes);
		
		byte[] blockHeadBytes = new byte[80];
		blockInputStream.read(blockHeadBytes);
		
		String blockId = BytesTools.bytesToHexStringLE(Hash.Sha256x2(blockHeadBytes));
		blockMark.setId(blockId);
		
		String preId =  BytesTools.bytesToHexStringLE(Arrays.copyOfRange(blockHeadBytes, 4, 4+32));
		blockMark.setPreId(preId);
		
		byte[] blockBodyBytes = new byte[blockSize-80];
		blockInputStream.read(blockBodyBytes);
		
		//Check valid header fork
		b4 = Arrays.copyOfRange(blockBodyBytes, 0, 4);
		String b4Hash = BytesTools.bytesToHexStringBE(b4) ;
		if(b4Hash.equals(MAGIC)) {
			System.out.println("Found valid header fork. Pointer: "+Preparer.Pointer);
			log.info("Found valid header fork. Pointer: {}",Preparer.Pointer);
			checkResult.setBlockLength(HEADER_FORK);
			return checkResult;
		}
		checkResult.setBlockLength(blockSize+8);
		checkResult.setBlockMark(blockMark);
		checkResult.setBlockBytes(blockBytes);
		return checkResult;
	}
	private int getFileOrder() {
		return FileTools.getFileOrder(Preparer.CurrentFile);
	}
	private class CheckResult{
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
	
	private void linkToChain(ElasticsearchClient esClient, BlockMark blockMark1, byte[] blockBytes) throws Exception {
		BlockMark blockMark = blockMark1;
		
		if(isRepeatBlcokIgnore(blockMark)) 
			return;
		if(isLinkToMainChainWriteItToEs(esClient, blockMark, blockBytes))
			return; 	
		if(isNewForkAddMarkToEs(esClient, blockMark)) 
			return;
		if(isLinkedToForkWriteMarkToEs(esClient, blockMark)){
			if(isForkOverMain(blockMark)) {
				ArrayList<BlockMark> winList = findTheBlockBeforeFork(blockMark);
				reorganize(esClient,winList);
			}
			return;
		}
		writeOrphanMark(esClient, blockMark);
		return; 
	}
	private void writeBlockMark(ElasticsearchClient esClient,BlockMark blockMark) throws ElasticsearchException, IOException {
		esClient.index(i->i.index(Indices.BlockMarkIndex).id(blockMark.getId()).document(blockMark));
	}	
	private void reorganize(ElasticsearchClient esClient, ArrayList<BlockMark> winList) throws Exception {

		if(winList == null || winList.isEmpty()) return;
			BlockMark blockMarkBeforeFork = winList.get(winList.size()-1);
			mainToFork(esClient, blockMarkBeforeFork);
			new RollBacker().rollback(esClient,blockMarkBeforeFork.getHeight());
			writeWinListToEs(esClient,winList);
			System.out.println("Reorganized. Fork: "+Preparer.forkList.size()+"BlockId before fork: "+blockMarkBeforeFork.getId()+" Height: "+blockMarkBeforeFork.getHeight());
	}
	private boolean isRepeatBlcokIgnore(BlockMark blockMark) {

		if(Preparer.mainList==null || Preparer.mainList.isEmpty())return false;
		Iterator<BlockMark> iter = Preparer.mainList.iterator();
		while(iter.hasNext()) {
			if(blockMark.getId().equals(iter.next().getId())){
				return true;
			}
		}
		return false;
	}
	private boolean isLinkToMainChainWriteItToEs(ElasticsearchClient esClient, BlockMark blockMark1, byte[] blockBytes) throws Exception {

		BlockMark blockMark = blockMark1;
		
		if(blockMark.getPreId().equals(Preparer.BestHash)){
			blockMark.setStatus(Preparer.MAIN);
			long newHeight = Preparer.BestHeight+1;
			blockMark.setHeight(newHeight);
			ReadyBlock rawBlock = new BlockParser().parseBlock(blockBytes,blockMark);
			ReadyBlock readyBlock = new BlockMaker().makeReadyBlock(esClient, rawBlock);
			new BlockWriter().writeIntoEs(esClient, readyBlock,opReFile);
			dropOldFork(newHeight);
			return true;
		}
		return false;
	}
	private void dropOldFork(long newHeight) {

		Iterator<BlockMark> iter = Preparer.forkList.iterator();
		while(iter.hasNext()) {
			BlockMark bm = iter.next();
			if(bm.getHeight() < newHeight-30) {
				iter.remove();
			}
		}
	}

	private boolean isNewForkAddMarkToEs(ElasticsearchClient esClient,BlockMark blockMark1) throws ElasticsearchException, IOException {

		BlockMark blockMark = blockMark1;
		Iterator<BlockMark> iter = Preparer.mainList.iterator();
		while(iter.hasNext()) {
			BlockMark bm = iter.next();
			if(blockMark.getPreId().equals(bm.getId())){
				blockMark.setHeight(bm.getHeight()+1);
				blockMark.setStatus(Preparer.FORK);
				writeBlockMark(esClient, blockMark);
				Preparer.forkList.add(blockMark);
				return true;
			}
		}
		return false;
	}
	private boolean isLinkedToForkWriteMarkToEs(ElasticsearchClient esClient,BlockMark blockMark1) throws ElasticsearchException, IOException {

		BlockMark blockMark = blockMark1;
		Iterator<BlockMark> iter = Preparer.forkList.iterator();
		while(iter.hasNext()) {
			BlockMark bm = iter.next();
			if(blockMark.getPreId().equals(bm.getId())){
				blockMark.setHeight(bm.getHeight()+1);
				blockMark.setStatus(Preparer.FORK);
				writeBlockMark(esClient, blockMark);
				Preparer.forkList.add(blockMark);
				return true;
			}
		}
		
		return false;
	}

	private boolean isForkOverMain(BlockMark blockMark) {

		long bestHeight = Preparer.mainList.get(Preparer.mainList.size()-1).getHeight();
		if(blockMark.getHeight() > bestHeight) return true;
		return false;
	}
	private ArrayList<BlockMark> findTheBlockBeforeFork(BlockMark blockMark1) {

		BlockMark blockMark = blockMark1;
		
		String preId = blockMark.getPreId();
		BlockMark mainBlock = new BlockMark();
		BlockMark forkBlock = new BlockMark();

		ArrayList<BlockMark> findList = new ArrayList<BlockMark>();
		blockMark.setStatus(Preparer.MAIN);
		findList.add(blockMark);
		
		boolean found = false;
		
		while(true) {
			found = false;
			for(BlockMark bm : Preparer.forkList) {
				if(bm.getId().equals(preId)) {
					bm.setStatus(Preparer.MAIN);
					findList.add(forkBlock);
					
					for(int i=Preparer.mainList.size()-2; i>=Preparer.mainList.size()-31; i--) {
						mainBlock = Preparer.mainList.get(i);
						
						if(bm.getPreId().equals(mainBlock.getId())){
							findList.add(mainBlock);
							return findList;
						}
					}
					found = true;
					preId = bm.getPreId();
				}
				if(found)break;
			}
			if(!found) {
				Preparer.forkList.removeAll(findList);
				return null;
			}
		}
	}
	private static void mainToFork(ElasticsearchClient esClient,BlockMark blockMarkBeforeFork) throws Exception {
		// TODO Auto-generated method stub
		
		BulkRequest.Builder br = new BulkRequest.Builder();
		
		for(int i=Preparer.mainList.size()-1; i>=Preparer.mainList.size()-31; i--) {
			BlockMark mainBlockMark = Preparer.mainList.get(i);
			
			ArrayList<BlockMark> mainToForkList = new ArrayList<BlockMark>();
			
			if(blockMarkBeforeFork.getId().equals(mainBlockMark.getId())) {
				
				esClient.bulk(br.build());
				
				Preparer.forkList.addAll(mainToForkList);
				Preparer.mainList.removeAll(mainToForkList);
				return;
			}
			mainBlockMark.setStatus(Preparer.FORK);
			br.operations(op->op.index(in->in
					.index(Indices.BlockMarkIndex)
					.id(mainBlockMark.getId())
					.document(mainBlockMark)));	
			mainToForkList.add(mainBlockMark);
		}

		log.error("The fork block is not found in mainBlockMarkList!!!");
		throw new Exception("The fork block is not found in mainBlockMarkList!!!");
	}

	private boolean writeWinListToEs(ElasticsearchClient esClient, ArrayList<BlockMark> winList) throws Exception {
		// TODO Auto-generated method stub
		for(int i=winList.size()-2;i>=0;i--) {
			BlockMark blockMark = winList.get(i);
			byte[] blockBytes = getBlockBytes(blockMark);
			ReadyBlock rawBlock = new BlockParser().parseBlock(blockBytes,blockMark);
			ReadyBlock readyBlock = new BlockMaker().makeReadyBlock(esClient, rawBlock);
			new BlockWriter().writeIntoEs(esClient, readyBlock,opReFile);
		}
		dropOldFork(winList.get(0).getHeight());
		return false;
	}
	private byte[] getBlockBytes(BlockMark bm) throws IOException {
		
		File file = new File(Preparer.Path, FileTools.getFileNameWithOrder(bm.get_fileOrder()));
		FileInputStream fis = new FileInputStream(file);
		fis.skip(bm.get_pointer()+8);
		byte[] blockBytes = new byte[(int) bm.getSize()];
		fis.read(blockBytes);
		fis.close();
		return blockBytes;
	}

	private void writeOrphanMark(ElasticsearchClient esClient,BlockMark blockMark) throws ElasticsearchException, IOException {

		blockMark.setStatus(Preparer.ORPHAN);
		writeBlockMark(esClient, blockMark);
		Preparer.orphanList.add(blockMark);
	}
	private void recheckOrphans(ElasticsearchClient esClient) throws Exception {

		boolean found = false;
		
		BlockMark bestBlockMark = new BlockMark();;
		
		while(!found) {	
			//If linked to main;
			for(int i= 0;i<Preparer.orphanList.size();i++) {
				BlockMark blockMark = Preparer.orphanList.get(i);

				if(blockMark.getPreId().equals(Preparer.BestHash)) {
					blockMark.setHeight(Preparer.BestHeight+1);
					blockMark.setStatus(Preparer.MAIN);
					byte[] blockBytes = getBlockBytes(blockMark);
					
					ReadyBlock rawBlock = new BlockParser().parseBlock(blockBytes,blockMark);
					ReadyBlock readyBlock = new BlockMaker().makeReadyBlock(esClient, rawBlock);
					new BlockWriter().writeIntoEs(esClient, readyBlock,opReFile);
					
					bestBlockMark = Preparer.mainList.get(Preparer.mainList.size()-1);
					
					//TODO
					if(bestBlockMark.getId()!= Preparer.BestHash) {
						System.out.println("BestHash "+Preparer.BestHash+" is not the same as mainList:"+bestBlockMark.getId());
						throw new Exception("BestHash "+Preparer.BestHash+" is not the same as mainList:"+bestBlockMark.getId());
					}
					Preparer.orphanList.remove(i);
					found = true;
					break;
				}
				//If linked to a fork;
				for(BlockMark fm: Preparer.forkList) {
					if(blockMark.getPreId().equals(fm.getId())) {
						blockMark.setHeight(fm.getHeight()+1);
						blockMark.setStatus(Preparer.FORK);
						Preparer.forkList.add(blockMark);
						Preparer.orphanList.remove(i);
						if(isForkOverMain(blockMark)) {
							ArrayList<BlockMark> winList = findTheBlockBeforeFork(blockMark);
							reorganize(esClient,winList);
						}
						found = true;
						break;
					}	
				}
			}
			if(!found)break;
		}			
	}
}
