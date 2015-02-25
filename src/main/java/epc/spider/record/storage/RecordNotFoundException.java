package epc.spider.record.storage;

public class RecordNotFoundException extends RuntimeException{

	private static final long serialVersionUID = -4842357477828677591L;

	public RecordNotFoundException(String message) {
		super(message);
	}

}
