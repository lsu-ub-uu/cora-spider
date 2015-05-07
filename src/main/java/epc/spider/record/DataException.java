package epc.spider.record;

public class DataException extends RuntimeException {

	private static final long serialVersionUID = -5355036186089708149L;

	public DataException(String message) {
		super(message);
	}

	public DataException(String message, Exception exception) {
		super(message, exception);
	}

}
