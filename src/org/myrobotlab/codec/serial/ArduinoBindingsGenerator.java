package org.myrobotlab.codec.serial;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class ArduinoBindingsGenerator {

  public transient final static Logger log = LoggerFactory.getLogger(ArduinoBindingsGenerator.class);

  static TreeMap<String, Method> sorted = new TreeMap<String, Method>();
  static TreeMap<String, Integer> sensorTypes = new TreeMap<String, Integer>();
  static TreeMap<String, Integer> errorTypes = new TreeMap<String, Integer>();

  static StringBuilder inoTemplate = new StringBuilder("///// INO GENERATED DEFINITION BEGIN //////\n");
  static StringBuilder pythonTemplate = new StringBuilder("##### PYTHON GENERATED DEFINITION BEGIN ######\n");

  static StringBuilder javaStaticImports = new StringBuilder("\t///// java static import definition - DO NOT MODIFY - Begin //////\n");

  static StringBuilder javaDefines = new StringBuilder("\t///// java ByteToMethod generated definition - DO NOT MODIFY - Begin //////\n");
  static StringBuilder javaBindingsInit = new StringBuilder();
  static StringBuilder javaFunctionToString = new StringBuilder();

  /**
   * called for each method - java method is the "source" of reflected data so
   * all method context code should be generated in this function
   * 
   * @param key
   * @param index
   */
  public static void createBindingsFor(String key, int index) {

    Method method = sorted.get(key);
    StringBuilder msb = new StringBuilder(method.getName());
    Class<?>[] params = method.getParameterTypes();
    for (int j = 0; j < params.length; ++j) {
      msb.append(String.format(" %s", params[j].getSimpleName()));
    }

    String methodSignatureComment = String.format("// {%s} \n", msb.toString());
    String pythonSignatureComment = String.format("  # {%s} \n", msb.toString());

    inoTemplate.append(methodSignatureComment);
    pythonTemplate.append(pythonSignatureComment);
    String underscore = CodecUtils.toUnderScore(method.getName());
    inoTemplate.append(String.format("#define %s\t\t%d\n", underscore, index));
    pythonTemplate.append(String.format("  %s = %d\n\n", underscore, index));

    javaStaticImports.append(String.format("\timport static org.myrobotlab.codec.serial.ArduinoMsgCodec.%s;\n", underscore));
    javaDefines.append(String.format("\t%s", methodSignatureComment));
    javaDefines.append(String.format("\tpublic final static int %s =\t\t%d;\n\n", underscore, index));
    javaBindingsInit.append(String.format("\t\tbyteToMethod.put(%s,\"%s\");\n", underscore, method.getName()));
    javaBindingsInit.append(String.format("\t\tmethodToByte.put(\"%s\",%s);\n\n", method.getName(), underscore));
    
    javaFunctionToString.append(String.format("\tcase ArduinoMsgCodec.%s:{\n\t\treturn \"%s\";\n\n\t}\n", underscore ,underscore));
  
  }

  /**)
   * master method which gets source material for generating msg bindings - the
   * source is the Arduino class itself - its method should have a near 1 to 1
   * relation to the msgs sent to and from MRLComm.ino These msgs are examined
   * and msg bindings are created
   * 
   * @throws IOException
   */
  public static void generateDefinitions() throws IOException {

    HashMap<String, String> snr = new HashMap<String, String>();

    Arduino arduino = (Arduino) Runtime.create("arduino", "Arduino");
    Method[] m = arduino.getDeclaredMethods();
    for (int i = 0; i < m.length; ++i) {
      // String signature = Encoder.getMethodSignature(m[i]);
      Method method = m[i];
    
      StringBuilder msb = new StringBuilder(m[i].getName());
      Class<?>[] params = method.getParameterTypes();
      for (int j = 0; j < params.length; ++j) {
        msb.append(String.format(" %s", params[j].toString()));
      }

      // log.info(String.format("[%s]", msb.toString())); // hmmm
      // "class someclass" :(
      // get rid of overloads by taking the method with maximum
      // complexity(ish) :)
      if (sorted.containsKey(method.getName())) {
        // overloaded - who has more parameters
        Method complex = (sorted.get(method.getName()).getParameterTypes().length < method.getParameterTypes().length) ? method : sorted.get(method.getName());
        sorted.put(method.getName(), complex);
      } else {
        sorted.put(method.getName(), method);
      }
      // FIXME non Arduino service method protocol defitions added
      // like errorTypes
      // and sensorTypes
    }

    HashSet<String> exclude = new HashSet<String>();

    // filter out methods which do not get relayed
    // from Arduino to MRLComm or Back
    // these are all methods 'only' used in Java-Land
    // exclude.add("addCustomMsgListener");
    exclude.add("connect");
    exclude.add("disconnect");
    exclude.add("getDescription");
    exclude.add("createPinList");

    exclude.add("getDeviceId");

    exclude.add("setController");
    exclude.add("getController");
    exclude.add("getMrlDeviceType");
    
    exclude.add("getServoIndex");
    exclude.add("getBoardType");
    exclude.add("getCategories");
    exclude.add("getPeers");
    exclude.add("getPinList");
    exclude.add("getSerial");
    exclude.add("getStepperData");// WTF
    exclude.add("isConnected");
    exclude.add("main");
    exclude.add("onByte");
    exclude.add("onCustomMsg");
    exclude.add("releaseService");
    exclude.add("sendMsg");
    exclude.add("setBoardType");
    exclude.add("startService");
    exclude.add("test");
    exclude.add("attach");
    exclude.add("detach");

    exclude.add("getPin");
    exclude.add("setMode");
    exclude.add("uploadSketch");

    exclude.add("publishStepperEvent");
    exclude.add("setStepperSpeed");
    exclude.add("stepperAttach");

    // additionally force getversion and publishMRLCommError
    // so that mis-matches of version are quickly reported...
    
    exclude.add("getVersion");
    exclude.add("publishVersion");
    exclude.add("publishMRLCommError");
    
    // except.add("E");

    // getter & setters
    exclude.add("setRXFormatter");
    exclude.add("setTXFormatter");
    exclude.add("getRXFormatter");
    exclude.add("getTXFormatter");
    exclude.add("getRXCodecKey");
    exclude.add("getTXCodecKey");
    exclude.add("connectVirtualUART");
    exclude.add("getSketch");
    exclude.add("onConnect");
    exclude.add("onDisconnect");
    exclude.add("setBoard");
    exclude.add("setSketch");

    // stuff which never gets down to the uC
    exclude.add("getDataSinkType");
    exclude.add("getSensorConfig");
    exclude.add("getSensorType");
    exclude.add("update");
    exclude.add("stopService");
    exclude.add("refresh");
    exclude.add("pinNameToAddress");

    exclude.add("refreshVersion");
    exclude.add("getPortName");
    exclude.add("setBoardMega");
    exclude.add("setBoardUno");

    exclude.add("createVirtual");
    exclude.add("getMetaData");
    exclude.add("processMessage");
    exclude.add("enabledHeartbeat");

    int index = 0;

    // forcing 3 methods to be always the same index publishMRLCommError 0, getVersion 2 publishVersion 3
    // that way you can get a meaningful error if you have the wrong version or mrlcomm
    ++index;
    createBindingsFor("publishMRLCommError", index);
    ++index;
    createBindingsFor("getVersion", index);
    ++index;
    createBindingsFor("publishVersion", index);
    ++index;

    for (String key : sorted.keySet()) {
      if (exclude.contains(key)) {
        continue;
      }

   
      createBindingsFor(key, index);

      ++index;
      // log.info(); // hmmm "class someclass" :(
    }

    inoTemplate.append("///// INO GENERATED DEFINITION END //////\n");
    pythonTemplate.append("##### PYTHON GENERATED DEFINITION END #####\n");

    snr.put("<%=javaStaticImports%>", javaStaticImports.toString());
    snr.put("<%=java.defines%>", javaDefines.toString());
    snr.put("<%=java.bindings.init%>", javaBindingsInit.toString());
    snr.put("<%=javaFunctionToString%>", javaFunctionToString.toString());
    
    
    snr.put("<%=mrlcomm.defines%>", inoTemplate.toString());
    snr.put("<%=python.defines%>", pythonTemplate.toString());

    log.info(inoTemplate.toString());
    log.info(javaBindingsInit.toString());

    // file template filtering
    String java = FileIO.toString("src/resource/generate/ArduinoMsgCodec.java.template");
    String python = FileIO.toString("src/resource/generate/pythonTemplate.txt");
    String MRLComm = FileIO.toString("src/resource/generate/ArduinoMsgCodec.h.template");

    // merge data with template
    for (String key : snr.keySet()) {
      log.info(snr.get(key));
      java = java.replace(key, snr.get(key));
      python = python.replace(key, snr.get(key));
      MRLComm = MRLComm.replace(key, snr.get(key));
    }

    long ts = System.currentTimeMillis();
    FileIO.toFile(String.format("ArduinoMsgCodec.%d.py", ts), python);
    FileIO.toFile("src/org/myrobotlab/codec/serial/" + String.format("ArduinoMsgCodec.java", ts), java);
    FileIO.toFile(String.format("src/resource/Arduino/MRLComm/ArduinoMsgCodec.h", ts), MRLComm);

    // String ret = String.format("%s\n\n%s", ino.toString(),
    // java.toString());
    // log.info(ret);
    // to define (upper with underscore)

    Runtime.exit();
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.getInstance().configure();
      LoggingFactory.getInstance().setLevel(Level.INFO);

      String t = "testMethod";
      // camelback to underscore
      /*
       * String regex = "[A-Z\\d]"; String replacement = "$1_";
       * log.info(t.replaceAll(regex, replacement)); log.info(t);
       */

      log.info(CodecUtils.toUnderScore(t));
      log.info(CodecUtils.toCamelCase(CodecUtils.toUnderScore(t)));

      ArduinoBindingsGenerator.generateDefinitions();

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
