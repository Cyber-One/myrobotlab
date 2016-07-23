#ifndef ArduinoMsgCodec_h
#define ArduinoMsgCodec_h

/*******************************************************************
 * MRLCOMM FUNCTION GENERATED INTERFACE
 * this file was generated by ArduinoMsgCodec and ArduinoMsgCodec.h.template
 */

///// INO GENERATED DEFINITION BEGIN //////
// {publishMRLCommError Integer} 
#define PUBLISH_MRLCOMM_ERROR		1
// {getVersion} 
#define GET_VERSION		2
// {publishVersion Integer} 
#define PUBLISH_VERSION		3
// {analogWrite int int} 
#define ANALOG_WRITE		4
// {createI2cDevice I2CControl int int} 
#define CREATE_I2C_DEVICE		5
// {deviceAttach DeviceControl Object[]} 
#define DEVICE_ATTACH		6
// {deviceDetach DeviceControl} 
#define DEVICE_DETACH		7
// {digitalWrite int int} 
#define DIGITAL_WRITE		8
// {disableBoardStatus} 
#define DISABLE_BOARD_STATUS		9
// {disablePinEvents int} 
#define DISABLE_PIN_EVENTS		10
// {enableBoardStatus int} 
#define ENABLE_BOARD_STATUS		11
// {enablePinEvents int int} 
#define ENABLE_PIN_EVENTS		12
// {i2cRead I2CControl int int byte[] int} 
#define I2C_READ		13
// {i2cWrite I2CControl int int byte[] int} 
#define I2C_WRITE		14
// {i2cWriteRead I2CControl int int byte[] int byte[] int} 
#define I2C_WRITE_READ		15
// {intsToString int[] int int} 
#define INTS_TO_STRING		16
// {isAttached} 
#define IS_ATTACHED		17
// {motorMove MotorControl} 
#define MOTOR_MOVE		18
// {motorMoveTo MotorControl} 
#define MOTOR_MOVE_TO		19
// {motorReset MotorControl} 
#define MOTOR_RESET		20
// {motorStop MotorControl} 
#define MOTOR_STOP		21
// {neoPixelWriteMatrix NeoPixel List} 
#define NEO_PIXEL_WRITE_MATRIX		22
// {pinMode String String} 
#define PIN_MODE		23
// {publishAttachedDevice String} 
#define PUBLISH_ATTACHED_DEVICE		24
// {publishBoardInfo BoardInfo} 
#define PUBLISH_BOARD_INFO		25
// {publishBoardStatus BoardStatus} 
#define PUBLISH_BOARD_STATUS		26
// {publishDebug String} 
#define PUBLISH_DEBUG		27
// {publishMessageAck} 
#define PUBLISH_MESSAGE_ACK		28
// {publishPinEvent PinEvent} 
#define PUBLISH_PIN_EVENT		29
// {publishPulse Long} 
#define PUBLISH_PULSE		30
// {publishPulseStop Integer} 
#define PUBLISH_PULSE_STOP		31
// {publishSensorData Object} 
#define PUBLISH_SENSOR_DATA		32
// {publishServoEvent Integer} 
#define PUBLISH_SERVO_EVENT		33
// {publishTrigger Pin} 
#define PUBLISH_TRIGGER		34
// {pulse int int int int} 
#define PULSE		35
// {pulseStop} 
#define PULSE_STOP		36
// {read int} 
#define READ		37
// {releaseI2cDevice I2CControl int int} 
#define RELEASE_I2C_DEVICE		38
// {sensorActivate SensorControl Object[]} 
#define SENSOR_ACTIVATE		39
// {sensorDeactivate SensorControl} 
#define SENSOR_DEACTIVATE		40
// {sensorPollingStart String} 
#define SENSOR_POLLING_START		41
// {sensorPollingStop String} 
#define SENSOR_POLLING_STOP		42
// {servoAttach ServoControl int} 
#define SERVO_ATTACH		43
// {servoDetach ServoControl} 
#define SERVO_DETACH		44
// {servoEventsEnabled ServoControl boolean} 
#define SERVO_EVENTS_ENABLED		45
// {servoSetSpeed ServoControl} 
#define SERVO_SET_SPEED		46
// {servoSweepStart ServoControl} 
#define SERVO_SWEEP_START		47
// {servoSweepStop ServoControl} 
#define SERVO_SWEEP_STOP		48
// {servoWrite ServoControl} 
#define SERVO_WRITE		49
// {servoWriteMicroseconds ServoControl int} 
#define SERVO_WRITE_MICROSECONDS		50
// {setDebounce int} 
#define SET_DEBOUNCE		51
// {setDebug boolean} 
#define SET_DEBUG		52
// {setDigitalTriggerOnly Boolean} 
#define SET_DIGITAL_TRIGGER_ONLY		53
// {setPWMFrequency Integer Integer} 
#define SET_PWMFREQUENCY		54
// {setSampleRate int} 
#define SET_SAMPLE_RATE		55
// {setSerialRate int} 
#define SET_SERIAL_RATE		56
// {setTrigger int int int} 
#define SET_TRIGGER		57
// {softReset} 
#define SOFT_RESET		58
// {write int int} 
#define WRITE		59
///// INO GENERATED DEFINITION END //////


/*******************************************************************
 * serial protocol functions
 */
#define MAGIC_NUMBER            170 // 10101010


/*******************************************************************
 * BOARD TYPE
 */
#define BOARD_TYPE_ID_UNKNOWN 0
#define BOARD_TYPE_ID_MEGA    1
#define BOARD_TYPE_ID_UNO     2

#if defined(ARDUINO_AVR_MEGA2560)
  #define BOARD BOARD_TYPE_ID_MEGA
#elif defined(ARDUINO_AVR_UNO)
  #define BOARD BOARD_TYPE_ID_UNO
#else
  #define BOARD BOARD_TYPE_ID_UNKNOWN
#endif

#endif