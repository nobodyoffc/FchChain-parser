package parse;

import java.util.ArrayList;

import data.*;

public class ReadyBlock {
	
	private BlockMark blockMark;
	private Block block;
	private BlockHas blockHas;
	private ArrayList<Tx> txList;
	private ArrayList<TxHas> txHasList;
	private ArrayList<Txo> inList;
	private ArrayList<Txo> outList;
	private ArrayList<OpReturn> opReturnList;
	private ArrayList<Address> addrList;
	private ArrayList<Txo> outWriteList;
	
	public BlockMark getBlockMark() {
		return blockMark;
	}
	public void setBlockMark(BlockMark blockMark) {
		this.blockMark = blockMark;
	}
	public Block getBlock() {
		return block;
	}
	public void setBlock(Block block) {
		this.block = block;
	}
	public BlockHas getBlockHas() {
		return blockHas;
	}
	public void setBlockHas(BlockHas blockHas) {
		this.blockHas = blockHas;
	}
	public ArrayList<Tx> getTxList() {
		return txList;
	}
	public void setTxList(ArrayList<Tx> txList) {
		this.txList = txList;
	}
	public ArrayList<TxHas> getTxHasList() {
		return txHasList;
	}
	public void setTxHasList(ArrayList<TxHas> txHasList) {
		this.txHasList = txHasList;
	}
	public ArrayList<Txo> getInList() {
		return inList;
	}
	public void setInList(ArrayList<Txo> inList) {
		this.inList = inList;
	}
	public ArrayList<Txo> getOutList() {
		return outList;
	}
	public void setOutList(ArrayList<Txo> outList) {
		this.outList = outList;
	}
	public ArrayList<OpReturn> getOpReturnList() {
		return opReturnList;
	}
	public void setOpReturnList(ArrayList<OpReturn> opReturnList) {
		this.opReturnList = opReturnList;
	}
	public ArrayList<Address> getAddrList() {
		return addrList;
	}
	public void setAddrList(ArrayList<Address> addrList) {
		this.addrList = addrList;
	}
	public ArrayList<Txo> getOutWriteList() {
		return outWriteList;
	}
	public void setOutWriteList(ArrayList<Txo> outWriteList) {
		this.outWriteList = outWriteList;
	}
	

}
