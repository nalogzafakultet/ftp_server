package ftp_server;



public class Response {

	private int code;
	private String message;

	public Response(int code, String message){
		this.code = code;
		this.message = message;
	}

	public Response(String response){
		String[] partedResponse = response.split(" ", 2);
		code = Integer.parseInt(partedResponse[0]);
		message = partedResponse[1];
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return code + " " + message + "\n";
	}
}