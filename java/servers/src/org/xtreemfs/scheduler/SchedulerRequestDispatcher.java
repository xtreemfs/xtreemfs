package org.xtreemfs.scheduler;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.xtreemfs.babudb.BabuDBFactory;
import org.xtreemfs.babudb.api.BabuDB;
import org.xtreemfs.babudb.api.DatabaseManager;
import org.xtreemfs.babudb.api.SnapshotManager;
import org.xtreemfs.babudb.api.StaticInitialization;
import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.common.config.PolicyContainer;
import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.foundation.CrashReporter;
import org.xtreemfs.foundation.LifeCycleListener;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.SSLOptions.TrustManager;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.TimeSync.ExtSyncSource;
import org.xtreemfs.foundation.VersionManagement;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.MessageType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader;
import org.xtreemfs.foundation.pbrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequestListener;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.SchedulerServiceConstants;
import org.xtreemfs.scheduler.algorithm.ReservationScheduler;
import org.xtreemfs.scheduler.algorithm.ReservationSchedulerFactory;
import org.xtreemfs.scheduler.data.OSDDescription;
import org.xtreemfs.scheduler.data.OSDPerformanceDescription;
import org.xtreemfs.scheduler.data.store.ReservationStore;
import org.xtreemfs.scheduler.operations.*;

public class SchedulerRequestDispatcher extends LifeCycleThread implements
		RPCServerRequestListener, LifeCycleListener {

	public static final int INDEX_ID_RESERVATIONS = 0;
	public static final int INDEX_ID_OSDS = 1;
	private static final int RPC_TIMEOUT = 15000;
	private static final int CONNECTION_TIMEOUT = 5 * 60 * 1000;
	public static final int DB_VERSION = 1;

	private SSLOptions sslOptions;
	private SchedulerConfig config;
	private RPCNIOSocketClient clientStage;
	private static final String DB_NAME = "scheddb";
    private final HttpServer httpServ;
	private DIRClient dirClient;
	private List<OSDDescription> osds;
	private ReservationScheduler reservationScheduler;
	private final BlockingQueue<RPCServerRequest> queue;
	private volatile boolean quit;
	private final BabuDB database;
	private final RPCNIOSocketServer server;
	private final Map<Integer, SchedulerOperation> registry;
	private ReservationStore reservationStore;
    private OSDMonitor osdMonitor;

	public SchedulerRequestDispatcher(SchedulerConfig config,
			final BabuDBConfig dbsConfig) throws BabuDBException, IOException {

		super("Sched RqDisp");
		this.config = config;

		Logging.logMessage(Logging.LEVEL_INFO, this,
				"XtreemFS Reservation Scheduler version "
						+ VersionManagement.RELEASE_VERSION);

		this.registry = new HashMap<Integer, SchedulerOperation>();
        this.osds = new ArrayList<OSDDescription>();

		// start up babudb
		database = BabuDBFactory.createBabuDB(dbsConfig,
				new StaticInitialization() {
					@Override
					public void initialize(DatabaseManager dbMan,
							SnapshotManager sMan) {
						initDB(dbMan, sMan);
					}
				});
		this.reservationStore = new ReservationStore(
				this.getSchedulerDatabase(), INDEX_ID_RESERVATIONS);

		registerOperations();

		sslOptions = null;
		if (this.config.isUsingSSL()) {

			PolicyContainer policyContainer = new PolicyContainer(config);
			TrustManager tm;
			try {
				tm = policyContainer.getTrustManager();
			} catch (Exception e) {
				throw new IOException(e);
			}

			sslOptions = new SSLOptions(new FileInputStream(
					config.getServiceCredsFile()),
					config.getServiceCredsPassphrase(),
					config.getServiceCredsContainer(), new FileInputStream(
							config.getTrustedCertsFile()),
					config.getTrustedCertsPassphrase(),
					config.getTrustedCertsContainer(), false,
					config.isGRIDSSLmode(), tm);

			if (Logging.isInfo() && tm != null) {
				Logging.logMessage(Logging.LEVEL_INFO, Category.misc, this,
						"using custom trust manager '%s'", tm.getClass()
								.getName());
			}
		}

		try {
			InetSocketAddress bindPoint = config.getAddress() != null ? new InetSocketAddress(
					config.getAddress(), 0) : null;

			if (Logging.isInfo() && bindPoint != null)
				Logging.logMessage(Logging.LEVEL_INFO, Category.misc, this,
						"outgoing server connections will be bound to '%s'",
						config.getAddress());

			clientStage = new RPCNIOSocketClient(sslOptions, RPC_TIMEOUT,
					CONNECTION_TIMEOUT, -1, -1, bindPoint,
					"SchedulerRequestDispatcher");
			clientStage.setLifeCycleListener(this);
			DIRServiceClient dirRpcClient = new DIRServiceClient(clientStage,
					config.getDirectoryService());
			dirClient = new DIRClient(dirRpcClient,
					config.getDirectoryServices(),
					config.getFailoverMaxRetries(), config.getFailoverWait());

			TimeSync.initialize(dirClient, config.getRemoteTimeSync(),
					config.getLocalClockRenew());
		} catch (Exception ex) {
			Logging.logMessage(Logging.LEVEL_ERROR, this, "STARTUP FAILED!");
			Logging.logError(Logging.LEVEL_ERROR, this, ex);
			System.exit(1);
		}

		quit = false;
		queue = new LinkedBlockingQueue<RPCServerRequest>();

		server = new RPCNIOSocketServer(config.getPort(), config.getAddress(),
				this, sslOptions);
		server.setLifeCycleListener(this);

        osdMonitor = new OSDMonitor(this);
        osdMonitor.setLifeCycleListener(this);

        httpServ = HttpServer.create(new InetSocketAddress(config.getHttpPort()), 0);

        final HttpContext ctx = httpServ.createContext("/", new HttpHandler() {
            public void handle(HttpExchange httpExchange) throws IOException {
                byte[] content;
                content = SchedulerStatusPage.getStatusPage(SchedulerRequestDispatcher.this).getBytes("ascii");
                httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                httpExchange.sendResponseHeaders(200, content.length);
                httpExchange.getResponseBody().write(content);
                httpExchange.getResponseBody().close();
            }
        });
	}

	public void startup() {
		this.start();
		try {
			TimeSync.getInstance().init(ExtSyncSource.XTREEMFS_DIR, dirClient,
					null, config.getRemoteTimeSync(),
					config.getLocalClockRenew());
			
			clientStage.start();
			clientStage.waitForStartup();

			osdMonitor.start();
            osdMonitor.waitForStartup();
			
			// TODO(ckleineweber): Get scheduler parameters from config
			reservationScheduler = ReservationSchedulerFactory.getScheduler(
                    getOsds(), 0.0, 1.0, 1.0, 1.0, true);
			
	        server.start();
	        server.waitForStartup();
		} catch (Exception ex) {
			Logging.logMessage(Logging.LEVEL_ERROR, this, "STARTUP FAILED!");
			Logging.logError(Logging.LEVEL_ERROR, this, ex);
			System.exit(1);
		}
	}

	@Override
	public void shutdown() throws Exception {
        httpServ.stop(0);
		server.shutdown();
		server.waitForShutdown();
		database.shutdown();
		clientStage.shutdown();

		this.quit = true;
		this.interrupt();
		this.waitForShutdown();
	}

	@Override
	public void receiveRecord(RPCServerRequest rq) {
		if (Logging.isDebug()) {
			Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this,
					"received new request: %s", rq.toString());
		}
		this.queue.add(rq);
	}

	@Override
	public void crashPerformed(Throwable cause) {
		final String report = CrashReporter.createCrashReport(
				"SchedulerRequestDispatcher",
				VersionManagement.RELEASE_VERSION, cause);
		System.out.println(report);
		CrashReporter.reportXtreemFSCrash(report);
		try {
			shutdown();
		} catch (Exception e) {
			Logging.logError(Logging.LEVEL_ERROR, this, e);
		}
	}

	@Override
	public void startupPerformed() {
	}

	@Override
	public void shutdownPerformed() {
	}
	
	public void reloadOSDs() throws InterruptedException, IOException {
		ServiceSet.Builder knownOSDs = dirClient
				.xtreemfs_service_get_by_type(null,
						RPCAuthentication.authNone,
						RPCAuthentication.userService,
						ServiceType.SERVICE_TYPE_OSD).toBuilder();
		
		for (Service osd : knownOSDs.getServicesList()) {
			boolean osdFound = false;

            for(OSDDescription o: this.getOsds()) {
                if(o.getIdentifier().equals(osd.getName())) {
                    osdFound = true;
                    break;
                }
            }

            if(!osdFound) {
                try {
                    byte[] bytes = getSchedulerDatabase().lookup(INDEX_ID_OSDS, osd.getUuid().getBytes(), null).get();
                    if(bytes != null) {
                        OSDDescription osdDescription = new OSDDescription(bytes);
                        getOsds().add(osdDescription);
                        osdFound = true;
                    }
                } catch(BabuDBException ex) {
                    Logging.logError(Logging.LEVEL_DEBUG, this, ex);
                }
            }
			
			if(!osdFound) {
				// TODO(ckleineweber): Benchmark osd
				OSDPerformanceDescription osdPerf = new OSDPerformanceDescription();
				
				// Determine free OSD capacity

				String totalStr = KeyValuePairs.getValue(osd.getData().getDataList(), "free");
				osdPerf.setCapacity(Double.valueOf(totalStr));
				
				// TODO(ckleineweber): Determine osd type
				OSDDescription.OSDType type = OSDDescription.OSDType.UNKNOWN;
				OSDDescription osdDescription = new OSDDescription(osd.getUuid(), osdPerf, type);
				getOsds().add(osdDescription);
				try {
					getSchedulerDatabase().singleInsert(INDEX_ID_OSDS, osd.toByteArray(), osdDescription.getBytes(), null);
				} catch(BabuDBException ex) {
					Logging.logError(Logging.LEVEL_ERROR, this, ex);
				}
			}
		}
	}

	private void registerOperations() throws BabuDBException {
		SchedulerOperation op;

		op = new ScheduleReservationOperation(this);
		registry.put(op.getProcedureId(), op);

		op = new GetScheduleOperation(this);
		registry.put(op.getProcedureId(), op);

		op = new GetVolumesOperation(this);
		registry.put(op.getProcedureId(), op);

		op = new RemoveReservationOperation(this);
		registry.put(op.getProcedureId(), op);
		
		op = new GetAllVolumesOperation(this);
		registry.put(op.getProcedureId(), op);

        op = new GetFreeResourcesOperation(this);
        registry.put(op.getProcedureId(), op);
	}

	private void initDB(DatabaseManager dbMan, SnapshotManager sMan) {
		final byte[] versionKey = "version".getBytes();
		try {
			Database db = dbMan.createDatabase("scheddbver", 2);
			ReusableBuffer rb = null;
			try {
				byte[] keyData = new byte[4];
				rb = ReusableBuffer.wrap(keyData);
				rb.putInt(DB_VERSION);
				db.singleInsert(0, versionKey, keyData, null).get();
			} catch (Exception ex) {
				ex.printStackTrace();
				System.err.println("cannot initialize database");
				System.exit(1);
			} finally {
				if (rb != null) {
					BufferPool.free(rb);
				}
			}
		} catch (BabuDBException ex) {
			// database exists: check version
			if (ex.getErrorCode() == BabuDBException.ErrorCode.DB_EXISTS) {
				ReusableBuffer rb = null;
				try {
					Database db = dbMan.getDatabase("scheddbver");

					byte[] value = db.lookup(0, versionKey, null).get();
					int ver = -1;
					if ((value != null) && (value.length == 4)) {
						rb = ReusableBuffer.wrap(value);
						ver = rb.getInt();
					}
					if (ver != DB_VERSION) {
						Logging.logMessage(Logging.LEVEL_ERROR, this,
								"OUTDATED DATABASE VERSION DETECTED!");
						Logging.logMessage(
								Logging.LEVEL_ERROR,
								this,
								"the database was created contains data with version no %d, this scheduler uses version %d.",
								ver, DB_VERSION);
						Logging.logMessage(Logging.LEVEL_ERROR, this,
								"please start an older version of the scheduler or remove the old database");
						System.exit(1);
					}
				} catch (Exception ex2) {
					ex2.printStackTrace();
					System.err.println("cannot initialize database");
					System.exit(1);
				} finally {
					if (rb != null) {
						BufferPool.free(rb);
					}
				}
			} else {
				ex.printStackTrace();
				System.err.println("cannot initialize database");
				System.exit(1);
			}
		}

		try {
			dbMan.createDatabase(DB_NAME, 3);
		} catch (BabuDBException ex) {
			// database already created
		}
	}

	private void processRequest(RPCServerRequest rq) {
		RPCHeader hdr = rq.getHeader();

		if (hdr.getMessageType() != MessageType.RPC_REQUEST) {
			rq.sendError(
					ErrorType.GARBAGE_ARGS,
					POSIXErrno.POSIX_ERROR_EIO,
					"expected RPC request message type but got "
							+ hdr.getMessageType());
			return;
		}

		RPCHeader.RequestHeader rqHdr = hdr.getRequestHeader();

		if (rqHdr.getInterfaceId() != SchedulerServiceConstants.INTERFACE_ID) {
			rq.sendError(
					ErrorType.INVALID_INTERFACE_ID,
					POSIXErrno.POSIX_ERROR_EIO,
					"Invalid interface id. This is a Scheduler service. You probably wanted to contact another service. Check the used address and port.");
			return;
		}

		SchedulerOperation op = registry.get(rqHdr.getProcId());
		if (op == null) {
			rq.sendError(ErrorType.INVALID_PROC_ID, POSIXErrno.POSIX_ERROR_EIO,
					"unknown procedure id requested");
			return;
		}

		SchedulerRequest schedRq = new SchedulerRequest(rq);
		try {
			op.parseRPCMessage(schedRq);
			op.startRequest(schedRq);
		} catch (IOException ex) {
			ex.printStackTrace();
			rq.sendError(ErrorType.INTERNAL_SERVER_ERROR,
					POSIXErrno.POSIX_ERROR_EIO,
					"internal server error: " + ex.toString(),
					OutputUtils.stackTraceToString(ex));
		}
	}

	public Database getSchedulerDatabase() throws BabuDBException {
		return database.getDatabaseManager().getDatabase(DB_NAME);
	}

	public ReservationScheduler getReservationScheduler() {
		return reservationScheduler;
	}

	@Override
	public void run() {
		try {
			notifyStarted();
			while (!quit) {
				final RPCServerRequest rq = queue.take();

				synchronized (database) {
					processRequest(rq);
				}
			}
		} catch (InterruptedException ex) {
			quit = true;
		}
		notifyStopped();
	}

	public ReservationStore getStore() {
		return reservationStore;
	}

    public List<OSDDescription> getOsds() {
        return osds;
    }
}
