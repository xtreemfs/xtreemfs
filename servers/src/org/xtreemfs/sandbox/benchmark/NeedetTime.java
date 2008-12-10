package org.xtreemfs.sandbox.benchmark;

/**
 *
 * @author clorenz
 */
public class NeedetTime {
	private static NeedetTime self = new NeedetTime();

	private long start;
	private long end;
	private String name;

	/**
	 * fabric
	 * @return
	 */
	public static NeedetTime getNeedetTime(){
		return self;
	}

	private NeedetTime() {
		super();
	}

	public long start(String s){
		System.out.println("##### Begin: " + s);
		this.name = s;
		this.start = System.currentTimeMillis();
		return this.start;
	}

	public long end(){
		this.end = System.currentTimeMillis();
		long time;
		time = this.end-this.start;
		System.out.println("##### End: " + this.name + " takes " + time + "ms (" + time/1000 + "s)");
		reset();
		return time;
	}

	private void reset(){
		this.start = 0;
		this.end = 0;
		this.name = "";
	}
}
