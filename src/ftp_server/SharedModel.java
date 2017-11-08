package ftp_server;

public class SharedModel {
	public boolean isPASVReady() {
		return PASVReady;
	}

	public void setPASVReady(boolean pASVReady) {
		PASVReady = pASVReady;
	}

	private volatile boolean PASVReady;
	public boolean isPASVAlive() {
		return PASVAlive;
	}

	public void setPASVAlive(boolean pASVAlive) {
		PASVAlive = pASVAlive;
	}
	
	
	private volatile boolean PASVAlive;
	

}

