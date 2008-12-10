package org.xtreemfs.sandbox.benchmark;


/**
 *
 * @author clorenz
 *
 */
public class StorageStageBenchmark extends OSDBenchmark {
//	private class FileInfo {
//		private String fileId;
//
//		private int objectAmount;
//
//		private int objectsProcessed;
//
//		public FileInfo(String fileId, int objAmount) {
//			super();
//			this.fileId = fileId;
//			if (objAmount != 0)
//				this.objectAmount = objAmount;
//			else
//				this.objectAmount = 1;
//			this.objectsProcessed = 0;
//		}
//
//		public String getFileId() {
//			return fileId;
//		}
//
//		public int getObjectAmount() {
//			return objectAmount;
//		}
//
//		public int getObjectsProcessed() {
//			return objectsProcessed;
//		}
//
//		public void increaseObjectsProcessed() {
//			this.objectsProcessed++;
//		}
//
//		public void resetObjectsProcessed() {
//			this.objectsProcessed = 0;
//		}
//	}
//
//	public static final int WRITE_BENCHMARK = 0;
//
//	public static final int READ_BENCHMARK = 1;
//
//	public static final int DELETE_BENCHMARK = 2;
//
//	protected StorageStage stage;
//
//	protected NeedetTime time;
//
//	protected LinkedList<String> output;
//
//	private boolean randomOrder;
//
//	private String benchConfig;
//
//	private Random shuffleRandom = new Random(1389724);
//
//	/**
//	 * Map contains Files for Requests FileID -> Amount of Objects
//	 */
//	protected LinkedList<FileInfo> files;
//
//	public StorageStageBenchmark(String testDir, boolean randomOrder) throws IOException {
//		super(testDir, new RAID0(1, 1));
//		this.stage = new StorageStage(controller, controller, config, null);
//		this.time = NeedetTime.getNeedetTime();
//		this.randomOrder = randomOrder;
//
//		this.files = new LinkedList<FileInfo>();
//		this.output = new LinkedList<String>();
//	}
//
//	@Override
//	protected void setUp(){
//            try {
//                System.out.println("tidy up the testdir");
//                FSTools.delTree(new File(testDir));
//                stage = new StorageStage(controller, controller, config, null);
//                stage.start();
//            } catch (IOException ex) {
//                return;
//            }
//	}
//
//	@Override
//	protected void tearDown() {
////		System.out.println("tidy up the testdir");
////		FSTools.delTree(new File(testDir));
//		stage.shutdown();
//		stage = null;
//	}
//
//	private void createFileList(int fileAmount, int maxObjAmount) {
//		System.out.println("create file-list");
//		Random objectsRandom = new Random(151684);
//
//		String fileId;
//		FileInfo file;
//		for (int i = 0; i < fileAmount; i++) {
//			fileId = Common.generateFileId(filenameRandom);
//			file = new FileInfo(fileId, objectsRandom.nextInt(maxObjAmount));
//			files.add(file);
//		}
//		benchConfig = fileAmount + " files, max " + maxObjAmount
//				+ " objects, random order " + randomOrder;
//	}
//
//	/*
//	 * Benchmarks
//	 */
//	public void benchWrite() {
//		System.out.println("generate write-requests");
//		bench(WRITE_BENCHMARK, "Write", benchConfig);
//	}
//
//	public void benchRead() {
//		System.out.println("generate read-requests");
//		bench(READ_BENCHMARK, "Read", benchConfig);
//	}
//
//	public void benchDelete() {
//		System.out.println("generate read-requests");
//		bench(DELETE_BENCHMARK, "Delete", benchConfig);
//	}
//
//	/**
//	 * run the requests and measures the time
//	 *
//	 * @param benchmark
//	 * @param info
//	 * @param random
//	 *            TODO
//	 * @param requests
//	 */
//	protected void bench(int benchMode, String benchmark, String info) {
//		OSDRequest request = null;
//		long endTime = 0;
//		Random objToReadInOneLoopRandom = new Random(168465);
//
//		boolean minOneFileWithObjectsRemaining = true;
//		int objNo, objectsToReadInOneLoop;
//		// loop runs until all objects of all files has been processed
//		while (minOneFileWithObjectsRemaining) {
//			if (randomOrder) {
//				Collections.shuffle(files, shuffleRandom);
//			}
//
//			time.start(benchmark);
//			for (FileInfo file : files) { // loop for all files
//				objectsToReadInOneLoop = objToReadInOneLoopRandom.nextInt((file
//						.getObjectAmount() / 2) + 1); // min: 1 object; max:
//														// 1/2 FileObjectAmount
//				// loop runs until a certain number of objects has been
//				// processed or all objects has been processed
//				while (objectsToReadInOneLoop == 0
//						|| file.getObjectsProcessed() <= file.getObjectAmount()) {
//					objNo = file.getObjectsProcessed() + 1;
//
//					request = createRequest(benchMode, file.getFileId(), objNo);
//					stage.enqueueRequest(request);
//					controller.getLastRequest();
//
//					file.increaseObjectsProcessed();
//					objectsToReadInOneLoop--;
//				}
//				if (file.getObjectsProcessed() <= file.getObjectAmount()) {
//					minOneFileWithObjectsRemaining = true;
//				} else {
//					minOneFileWithObjectsRemaining = false;
//				}
//			}
//			endTime += time.end();
//		}
//		for (FileInfo file : files) {
//			file.resetObjectsProcessed();
//		}
//
//		output.add(Common.formatResultForFile(benchmark, info, endTime));
//	}
//
//	private OSDRequest createRequest(int benchMode, String fileId, int objNo) {
//		OSDRequest rq;
//		switch (benchMode) {
//		case 0: {
//			int dataLength = 10;
//			rq = createWriteRequest(fileId, objNo, 0, dataLength);
//			break;
//		}
//		case 1: {
//			rq = createReadRequest(fileId, objNo, 0);
//			break;
//		}
//		case 2: {
//			rq = createDeleteRequest(fileId, objNo, 0);
//			break;
//		}
//		default: {
//			rq = null;
//			break;
//		}
//		}
//		return rq;
//	}
//
//	public void startBenchmarks(int fileAmount, int maxObjectAmount) {
//		setUp();
//		createFileList(fileAmount, maxObjectAmount);
//
//		benchWrite();
//		benchRead();
//		// benchDelete(false); // doesn't work at the moment
//		tearDown();
//
//		Common.writeToFile("StorageStageBenchmark" + "_results.csv", output);
//	}
//
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//		try {
//			if (args.length >= 3) {
//				StorageStageBenchmark bench = new StorageStageBenchmark(args[0],
//						true);
//				bench.startBenchmarks(Integer.parseInt(args[1]), Integer
//						.parseInt(args[2]));
//			} else
//				System.out
//						.println("usage: java <testDir> <file amount> <object/file amount>"
//								+ "\nhint: use the -Xmx option for more needed RAM (e.g.: -Xmx256m)");
//		} catch (NullPointerException e) {
//			System.out
//			.println("usage: java <testDir> <file amount> <object/file amount>"
//					+ "\nhint: use the -Xmx option for more needed RAM (e.g.: -Xmx256m)");
//		} catch (IOException e) {
//                    e.printStackTrace();
//                }
//	}

}
