package org.myrobotlab.service;

import static org.myrobotlab.arduino.Msg.MAGIC_NUMBER;
import static org.myrobotlab.arduino.VirtualMsg.MAX_MSG_SIZE;
import static org.myrobotlab.arduino.VirtualMsg.MRLCOMM_VERSION;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.Msg;
import org.myrobotlab.arduino.VirtualMsg;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.PortQueue;
import org.myrobotlab.service.Arduino.AckLock;
import org.myrobotlab.service.interfaces.RecordControl;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.slf4j.Logger;

public class VirtualArduino extends Service implements SerialDataListener, RecordControl {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(VirtualArduino.class);

	Serial uart;
	String portName = "COM42";
	final BoardInfo boardInfo = new BoardInfo();
	String boardType;

	// The mighty device List. This contains all active devices that are
	// attached to the arduino.
	LinkedList<Device> deviceList = new LinkedList<Device>();

	// list of pins currently being read from - can contain both digital and
	// analog
	LinkedList<Pin> pinList = new LinkedList<Pin>();

	// MRLComm message buffer and current count from serial port ( MAGIC |
	// MSGSIZE | FUNCTION | PAYLOAD ...
	// unsigned char ioCmd[MAX_MSG_SIZE]; // message buffer for all inbound
	// messages
	String config;
	// performance metrics and load timing
	// global debug setting, if set to true publishDebug will write to the
	// serial port.
	int byteCount;
	int msgSize;
	boolean boardStatusEnabled;
	int publishBoardStatusModulus; // the frequency in which to report the load
									// timing metrics (in number of main loops)
	long lastMicros; // timestamp of last loop (if stats enabled.)

	boolean heartbeatEnabled = false;
	long lastHeartbeatUpdate;

	int[] customMsgBuffer = new int[Msg.MAX_MSG_SIZE];
	int customMsgSize;

	transient FileOutputStream record = null;
	// for debuging & developing - need synchronized - both send & recv threads
	transient StringBuffer recordRxBuffer = new StringBuffer();
	transient StringBuffer recordTxBuffer = new StringBuffer();

	private int loopCount;
	
	transient MrlComm mrlCommThread;

	public class Device {
		public int id;
		public int type;

		public void update() {
			// TODO Auto-generated method stub

		}
	}

	public class Pin {
		public Pin(Integer address, Integer type, Integer rate) {
			this.address = address;
			this.type = type;
			this.rate = rate;
		}
		
		public int rate;
		public long lastUpdate;
		public int type;
		public int value;
		public int address;
	}

	public class MrlComm extends Thread {
		VirtualArduino mrlComm;
		public boolean isRunning = false;

		public MrlComm(VirtualArduino virtual) {
			super(virtual.getName() + ".MrlComm");
			this.mrlComm = virtual;
		}

		public void run() {
			isRunning = true;
			// loop !
			while (isRunning) {
				// increment how many times we've run
				// TODO: handle overflow here after 32k runs, i suspect this might blow up?
				mrlComm.loopCount++;
				// get a command and process it from
				// the serial port (if available.)
				// if (mrlComm.readMsg()) {
				//	mrlComm.processCommand();
				// }
				// update devices
				mrlComm.updateDevices();
				// send back load time and memory
				mrlComm.publishBoardStatus();

				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public VirtualArduino(String n) {
		super(n);
		uart = (Serial) createPeer("uart");
		setBoardUno();
		boardInfo.setVersion(MRLCOMM_VERSION);
	}

	public long micros() {
		return System.currentTimeMillis();
	}

	/***********************************************************************
	 * UPDATE DEVICES BEGIN updateDevices updates each type of device put on the
	 * device list depending on their type. This method processes each loop.
	 * Typically this "back-end" processing will read data from pins, or change
	 * states of non-blocking pulses, or possibly regulate a motor based on pid
	 * values read from pins
	 */
	void updateDevices() {

		// update self - the first device which
		// is type Arduino
		update();

		// iterate through our device list and call update on them.
		for (int i = 0; i < deviceList.size(); ++i) {
			Device node = deviceList.get(i);
			node.update();
		}
	}

	private void update() {
		long now = millis();
		if ((now - lastHeartbeatUpdate > 1000) && heartbeatEnabled) {
			softReset();
			lastHeartbeatUpdate = now;
			return;
		}

	  if (pinList.size() > 0) {

			// size of payload - 1 byte for address + 2 bytes per pin read
			// this is an optimization in that we send back "all" the read pin data in a
			// standard 2 byte package - digital reads don't need both bytes, but the
			// sending it all back in 1 msg and the simplicity is well worth it
			// msg.addData(pinList.size() * 3 /* 1 address + 2 read bytes */);

		  	int[] buffer = new int[pinList.size() * 3];
			
			// iterate through our device list and call update on them.
			boolean dataCount = false;
			for(int i = 0; i < pinList.size(); ++i){
				Pin pin = pinList.get(i);
				if (pin.rate == 0 || (now > pin.lastUpdate + (1000 / pin.rate))) {
					pin.lastUpdate = now;
					// TODO: move the analog read outside of this method and pass it in!
					if (pin.type == Arduino.ANALOG) {
						pin.value = analogRead(pin.address);
					} else {
						pin.value = digitalRead(pin.address);
					}

					// loading both analog & digital data
					buffer[3*i] = pin.address;// 1 byte
					buffer[3*i + 1] = pin.value  >> 8 & 0xFF;// 2 byte b16 value			
					buffer[3*i + 2] = pin.value & 0xFF;// 2 byte b16 value
					// ++dataCount;
					dataCount = true;
				}
				// node = node.next;
			}
			if (dataCount) {
				msg.publishPinArray(buffer);
			}
		}
	}
	
	public int getRandom(int min, int max){
		return min + (int)(Math.random() * ((max - min) + 1));
	}

	private int digitalRead(int address) {
		return getRandom(0, 1);
	}

	private int analogRead(int address) {
		return getRandom(0, 1024);
	}

	private long millis() {
		return System.currentTimeMillis();
	}

	public void publishBoardStatus() {
		// protect against a divide by zero in the division.
		if (publishBoardStatusModulus == 0) {
			publishBoardStatusModulus = 10000;
		}

		int avgTiming = 0;
		long now = micros();

		avgTiming = (int) (now - lastMicros) / publishBoardStatusModulus;

		// report board status
		if (boardStatusEnabled && (loopCount % publishBoardStatusModulus == 0)) {
 			int[] deviceSummary = new int[deviceList.size() * 2];
			for (int i = 0; i < deviceList.size(); ++i) {
				deviceSummary[i] = deviceList.get(i).id;
				deviceSummary[i + 1] = deviceList.get(i).type;
			}
			msg.publishBoardStatus(avgTiming, getFreeRam(), deviceSummary);
		}
		// update the timestamp of this update.
		lastMicros = now;

	}

	private Integer getFreeRam() {
		return 925;
	}

	@Override
	public void startService() {
		super.startService();
		uart = (Serial) startPeer("uart");
		uart.addByteListener(this);
		msg = new VirtualMsg(this, uart);
		startMrlComm();
	}

	public Serial getSerial() {
		return uart;
	}

	public void setPortName(String portName) {
		this.portName = portName;
	}

	/**
	 * This static method returns all the details of the class without it having
	 * to be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(VirtualArduino.class.getCanonicalName());
		meta.addDescription("virtual hardware of for the Arduino!");
		meta.setAvailable(true); // false if you do not want it viewable in a
									// gui
		meta.addPeer("uart", "Serial", "serial device for this Arduino");
		meta.addCategory("simulator");
		return meta;
	}

	public SerialDevice connectVirtualUart(String myPort, String uartPort) throws IOException {

		BlockingQueue<Integer> left = new LinkedBlockingQueue<Integer>();
		BlockingQueue<Integer> right = new LinkedBlockingQueue<Integer>();

		// add our virtual port
		PortQueue vPort = new PortQueue(myPort, left, right);
		Serial.ports.put(myPort, vPort);

		PortQueue uPort = new PortQueue(uartPort, right, left);
		uart.connectPort(uPort, uart);

		log.info(String.format("connectToVirtualUart - creating uart %s <--> %s", myPort, uartPort));
		return uart;
	}

	transient int[] ioCmd = new int[MAX_MSG_SIZE];

	transient VirtualMsg msg;

	int error_arduino_to_mrl_rx_cnt = 0;

	int error_mrl_to_arduino_rx_cnt = 0;

	boolean ackEnabled = false;

	transient AckLock ackRecievedLock = new AckLock();

	Boolean debug = false;

	@Override
	public Integer onByte(Integer newByte) {
		try {
			/**
			 * Archtype InputStream read - rxtxLib does not have this
			 * straightforward design, but the details of how it behaves is is
			 * handled in the Serial service and we are given a unified
			 * interface
			 *
			 * The "read()" is data taken from a blocking queue in the Serial
			 * service. If we want to support blocking functions in Arduino then
			 * we'll "publish" to our local queues
			 */
			// TODO: consider reading more than 1 byte at a time ,and make this
			// callback onBytes or something like that.

			++byteCount;
			if (log.isDebugEnabled()) {
				log.info("onByte {} \tbyteCount \t{}", newByte, byteCount);
			}
			if (byteCount == 1) {
				if (newByte != MAGIC_NUMBER) {
					byteCount = 0;
					msgSize = 0;
					Arrays.fill(ioCmd, 0); // FIXME - optimize - remove
					warn(String.format("Arduino->MRL error - bad magic number %d - %d rx errors", newByte, ++error_arduino_to_mrl_rx_cnt));
					// dump.setLength(0);
				}
				return newByte;
			} else if (byteCount == 2) {
				// get the size of message
				if (newByte > 64) {
					byteCount = 0;
					msgSize = 0;
					error(String.format("Arduino->MRL error %d rx sz errors", ++error_arduino_to_mrl_rx_cnt));
					return newByte;
				}
				msgSize = newByte.intValue();
				// dump.append(String.format("MSG|SZ %d", msgSize));
			} else if (byteCount > 2) {
				// remove header - fill msg data - (2) headbytes -1
				// (offset)
				// dump.append(String.format("|P%d %d", byteCount,
				// newByte));
				ioCmd[byteCount - 3] = newByte.intValue();
			} else {
				// the case where byteCount is negative?! not got.
				error(String.format("Arduino->MRL error %d rx negsz errors", ++error_arduino_to_mrl_rx_cnt));
				return newByte;
			}
			if (byteCount == 2 + msgSize) {
				// we've received a full message

				msg.processCommand(ioCmd);

				if (ackEnabled) {
					/*
					 * we doon need no stink'ing ack'ing synchronized
					 * (ackRecievedLock) { try { ackRecievedLock.wait(2000); }
					 * catch (InterruptedException e) {// don't care} }
					 * 
					 * if (!ackRecievedLock.acknowledged) { log.error(
					 * "Ack not received : {} {}", Msg.methodToString(ioCmd[0]),
					 * numAck); } }
					 */
				}

				// clean up memory/buffers
				msgSize = 0;
				byteCount = 0;
				Arrays.fill(ioCmd, 0); // optimize remove
			}
		} catch (Exception e) {
			++error_mrl_to_arduino_rx_cnt;
			error("msg structure violation %d", error_mrl_to_arduino_rx_cnt);
			log.warn("msg_structure violation byteCount {} buffer {}", byteCount, Arrays.copyOf(ioCmd, byteCount));
			// try again (clean up memory buffer)
			msgSize = 0;
			byteCount = 0;
			Logging.logError(e);
		}
		return newByte;
	}

	public void connect(String portName) throws IOException {
		connectVirtualUart(portName, portName + ".UART");
	}

	@Override
	public String onConnect(String portName) {
		for (int i = 0; i < 3; ++i) {
			// TODO msg.publishBoardInfo();
			/*
			 * MrlMsg msg = new MrlMsg(PUBLISH_BOARD_INFO);
			 * msg.append(boardInfo.getVersion()).append(boardInfo.getBoardType(
			 * )); sendMsg(msg);
			 */
		}
		return portName;
	}

	@Override
	public String onDisconnect(String portName) {
		return portName;
	}

	@Override
	public void record() throws Exception {
		if (record == null) {
			record = new FileOutputStream(String.format("%s.ard", getName()));
		}
	}

	@Override
	public void stopRecording() {
		if (record != null) {
			try {
				record.close();
			} catch (Exception e) {
			}
			record = null;
		}
	}

	@Override
	public boolean isRecording() {
		return record != null;
	}

	public String setBoard(String board) {
		log.info("setting board to type {}", board);

		boardInfo.setType(board);
		this.boardType = board;
		// createPinList();
		broadcastState();
		return board;
	}

	/**
	 * easy way to set to a 54 pin arduino
	 *
	 * @return
	 */
	public String setBoardMega() {
		return setBoard(Arduino.BOARD_TYPE_MEGA);
	}

	public String setBoardMegaADK() {
		return setBoard(Arduino.BOARD_TYPE_MEGA_ADK);
	}

	public String setBoardUno() {
		return setBoard(Arduino.BOARD_TYPE_UNO);
	}

	public void getBoardInfo() {
		msg.publishBoardInfo(MRLCOMM_VERSION, boardInfo.getBoardType());
	}

	public void enableBoardStatus(Boolean enabled) {
		boardStatusEnabled = enabled;
	}

	public void enablePin(Integer address, Integer type, Integer rate) {
		log.info("enablePin {} {} {}", address, type, rate);
		// don't add it twice
		for (int i = 0; i < pinList.size(); ++i) {
			Pin pin = pinList.get(i);
			if (pin.address == address) {
				// TODO already exists error?
				return;
			}
		}

		if (type == Arduino.DIGITAL) {
			pinMode(address, Arduino.INPUT);
		}
		Pin p = new Pin(address, type, rate);
		p.lastUpdate = 0;
		pinList.add(p);
	}

	public void setDebug(Boolean enabled) {
		debug = enabled;
	}

	public void setSerialRate(Integer rate) {
		// TODO Auto-generated method stub

	}

	public void softReset() {
		// TODO Auto-generated method stub

	}

	public void enableAck(Boolean enabled) {
		// TODO Auto-generated method stub

	}

	public void enableHeartbeat(Boolean enabled) {
		// TODO Auto-generated method stub

	}

	public void heartbeat() {
		msg.publishHeartbeat();
	}

	public void echo(Long sInt) {
		log.info("varduino.echo {}", sInt);
		msg.publishEcho(sInt);
	}

	public void controllerAttach(Integer serialPort) {
		// TODO Auto-generated method stub

	}

	public void customMsg(int[] msg2) {
		msg.publishCustomMsg(msg2);
	}

	public void i2cBusAttach(Integer deviceId, Integer i2cBus) {
		// TODO Auto-generated method stub

	}

	public void i2cRead(Integer deviceId, Integer deviceAddress, Integer size) {
		// TODO Auto-generated method stub

	}

	public void i2cWrite(Integer deviceId, Integer deviceAddress, int[] data) {
		// TODO Auto-generated method stub

	}

	public void i2cWriteRead(Integer deviceId, Integer deviceAddress, Integer readSize, Integer writeValue) {
		// TODO Auto-generated method stub

	}

	public void neoPixelAttach(Integer deviceId, Integer pin, Integer numPixels) {
		// TODO Auto-generated method stub

	}

	public void analogWrite(Integer pin, Integer value) {
		log.info("analogWrite({}, {})", pin, value);
	}

	public void digitalWrite(Integer pin, Integer value) {
		// TODO Auto-generated method stub

	}

	public void disablePin(Integer pin) {
		// TODO Auto-generated method stub

	}

	public void disablePins() {
		// TODO Auto-generated method stub

	}

	public void pinMode(Integer pin, Integer mode) {
		// TODO Auto-generated method stub

	}

	public void servoAttach(Integer deviceId, Integer pin, Integer initPos, Integer initVelocity) {
		// TODO Auto-generated method stub

	}

	public void servoEnablePwm(Integer deviceId, Integer pin) {
		// TODO Auto-generated method stub

	}

	public void servoDisablePwm(Integer deviceId) {
		// TODO Auto-generated method stub

	}

	public void servoSetMaxVelocity(Integer deviceId, Integer maxVelocity) {
		// TODO Auto-generated method stub

	}

	public void servoSetVelocity(Integer deviceId, Integer velocity) {
		// TODO Auto-generated method stub

	}

	public void servoSweepStart(Integer deviceId, Integer min, Integer max, Integer step) {
		// TODO Auto-generated method stub

	}

	public void servoSweepStop(Integer deviceId) {
		// TODO Auto-generated method stub

	}

	public void servoWrite(Integer deviceId, Integer target) {
		// TODO Auto-generated method stub

	}

	public void servoWriteMicroseconds(Integer deviceId, Integer ms) {
		// TODO Auto-generated method stub

	}

	public void serialAttach(Integer deviceId, Integer relayPin) {
		// TODO Auto-generated method stub

	}

	public void serialRelay(Integer deviceId, int[] data) {
		// TODO Auto-generated method stub

	}

	public void ultrasonicSensorAttach(Integer deviceId, Integer triggerPin, Integer echoPin) {
		// TODO Auto-generated method stub

	}

	public void ultrasonicSensorStartRanging(Integer deviceId) {
		// TODO Auto-generated method stub

	}

	public void deviceDetach(Integer deviceId) {
		// TODO Auto-generated method stub

	}

	public void neoPixelSetAnimation(Integer deviceId, Integer animation, Integer red, Integer green, Integer blue, Integer speed) {
		// TODO Auto-generated method stub

	}

	public void neoPixelWriteMatrix(Integer deviceId, int[] buffer) {
		// TODO Auto-generated method stub

	}

	public void setDebounce(Integer pin, Integer delay) {
		// TODO Auto-generated method stub

	}

	public void setTrigger(Integer pin, Integer triggerValue) {
		// TODO Auto-generated method stub

	}

	public void ultrasonicSensorStopRanging(Integer deviceId) {
		// TODO Auto-generated method stub

	}
	
	public void stopMrlComm(){
		if (mrlCommThread != null){
			mrlCommThread.isRunning = false;
		}
	}
	
	public void startMrlComm(){	
		if (mrlCommThread != null){
			stopMrlComm();
		}
		mrlCommThread = new MrlComm(this);
		mrlCommThread.start();
	}
	
	public void stopService(){
		super.stopService();
		stopMrlComm();
	}

	public static void main(String[] args) {
		try {

			LoggingFactory.init(Level.INFO);

			VirtualArduino varduino = null;

			String port = "COM42";
			boolean useVirtual = true;

			Runtime.start("gui", "GUIService");
			Runtime.start("webgui", "WebGui");
			// Runtime.start("python", "Python");

			Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			arduino.record();
			log.info("ports " + Arrays.toString(arduino.getSerial().getPortNames().toArray()));
			if (useVirtual) {
				varduino = (VirtualArduino) Runtime.create("varduino", "VirtualArduino");
				varduino.setPortName(port);
				Runtime.start("varduino", "VirtualArduino");
				varduino.setBoardMega();// .setBoardUno();
			}
			arduino.connect(port);
			arduino.enablePin(54);

		} catch (Exception e) {
			log.error("main threw", e);
		}
	}

}
