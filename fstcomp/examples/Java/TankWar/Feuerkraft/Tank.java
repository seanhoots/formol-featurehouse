
public class Tank {

	protected long feuerkraftTimer;
	protected boolean feuerkraft = false;
	
	protected void toolKontroller(){
		original();
		if (tankManager.status == GameManager.PAUSE || tankManager.status == GameManager.EXIT) {
			if (feuerkraft) {
				feuerkraftTimer += elapsedTime;
			}
		}
		if (feuerkraft && System.currentTimeMillis() - feuerkraftTimer > 15000) {
			feuernHaufigkeit = feuernHaufigkeit + 500l;
			feuerkraft = false;
		}
	}
	
	protected void toolBehandeln(int toolType) {
		original(toolType);
		switch (toolType) {
		case 372:// 0,255,0
			if (!feuerkraft) {
				this.feuerkraftTimer = System.currentTimeMillis();
				this.feuerkraft = true;
				this.feuernHaufigkeit = this.feuernHaufigkeit - 500l;
			}
			break;
		}
	}

}