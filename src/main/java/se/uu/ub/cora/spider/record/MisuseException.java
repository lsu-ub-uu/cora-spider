package se.uu.ub.cora.spider.record;

public class MisuseException extends RuntimeException {
	private static final long serialVersionUID = -6159213811738340300L;

	public MisuseException(String message) {
		super(message);
	}

	public MisuseException(String message, Exception exception) {
		super(message, exception);
	}
}
