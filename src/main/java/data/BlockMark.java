package data;

public class BlockMark {
	private String id;
	private String preId;
	private long height;
	private long size;		//block size
	private String status;
	
	//parsing info
	private int _fileOrder;		//The order number of the file that the block is located in.
	private long _pointer;		//The position of the beginning of the block in the file. 
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getPreId() {
		return preId;
	}
	public void setPreId(String preId) {
		this.preId = preId;
	}
	public long getHeight() {
		return height;
	}
	public void setHeight(long height) {
		this.height = height;
	}
	public int get_fileOrder() {
		return _fileOrder;
	}
	public void set_fileOrder(int _fileOrder) {
		this._fileOrder = _fileOrder;
	}
	public long get_pointer() {
		return _pointer;
	}
	public void set_pointer(long _pointer) {
		this._pointer = _pointer;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
}
