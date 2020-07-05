/*
*	Touch Panel Child Driver
*   Code adapted from the one written for RGBGenie by Bryan Copeland
*
*   Updated 2020-07-03 Adapted for RGBGenie ZW-3001 only
*/

metadata {
	definition (name: "RGBGenie Touch Panel Child ZW-3001", namespace: "ecarlotti", author: "RGBGenie") {
		capability "Switch"
		capability "SwitchLevel"
		capability "Button"
		capability "Actuator"
        
        attribute "sceneCapture", "boolean"
	}
    
    simulator {
    
    }
    
    tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "lighting", width: 3, height: 2, canChangeIcon: true) {
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"switch level.setLevel"
            }
        }
    
//        main(["switch"])
//    	details(["switch", "level"])
        
    }    
}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                  Installation and update                                //
/////////////////////////////////////////////////////////////////////////////////////////////
def installed() {
	log.debug("installed()")
//    state.sceneCapture=true
}

def updated() {
	log.debug("updated()")
//	if (sceneCapture && getDataValue("deviceModel")=="41221") { 
//		sendEvent(name: "numberOfButtons", value: 0) 
//	} else if (!sceneCapture && getDataValue("deviceModel")!="41221") {
//		sendEvent(name: "numberOfButtons", value: 3)
//	}
}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                     Event Generation                                    //
/////////////////////////////////////////////////////////////////////////////////////////////
def parse(description) {
	log.debug "parse: ${description}"
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.debug "Command: ${cmd}"
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	log.debug("BasicReport: ${cmd}")
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	log.debug "BasicSet: ${cmd}"
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
	log.debug("SwitchMultilevelReport: ${cmd}")
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelStartLevelChange cmd){
	log.debug("SwitchMultilevelStartLevelChange: ${cmd}")
	runIn(1, "levelChanging", [data: [upDown: cmd.upDown, level: device.currentValue("level")]])
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelStopLevelChange cmd) {
	log.debug("SwitchMultilevelStopLevelChange: ${cmd}")
	unschedule()
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelSet cmd) {
	log.debug("SwitchMultilevelSet: ${cmd}")
//	sendEvent(name: "level", value: cmd.value)
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfSet cmd) {
	log.debug("SceneActuatorConfSet: ${cmd}")
	if (state.sceneCapture) {
		if (!state.scene) { state.scene=[:] }
        state.scene["${cmd.sceneId}"]=["level": device.currentValue("level"), "switch": device.currentValue("switch")]
	} else {
		sendEvent(name: "pushed", value: (cmd.sceneId/16))
	}
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd) {
	log.debug("SceneActivationSet: ${cmd}")
	if (state.sceneCapture) {
		if (!state.scene) { state.scene=[:] }
		def scene=state.scene["${cmd.sceneId}"] 
		scene.each { k, v ->
			sendEvent(name: k, value: v)
		}
	} else {
		sendEvent(name: "held", value: (cmd.sceneId/16))
	}
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	log.debug("SecurityMessageEncapsulation: ${cmd}")
	def encapsulatedCommand = cmd.encapsulatedCommand(cmdClassVers)
	if (encapsulatedCommand) {
		state.sec = 1
		def result = zwaveEvent(encapsulatedCommand)
		result = result.collect {
			if (it instanceof physicalgraph.device.HubAction && !it.toString().startsWith("9881")) {
				response(cmd.CMD + "00" + it.toString())
			} else {
				it
			}
		}
		result
	}
}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                 Device-Specific Methods                                 //
/////////////////////////////////////////////////////////////////////////////////////////////
def enableSceneCapture(value) {
	log.debug("enableSceneCapture: ${value}")
	state.sceneCapture=value
    sendEvent(name: "sceneCapture", value: value) 
//    if (value) {
//    	sendEvent(name: "numberOfButtons", value: 0) 
//    } else {
//    	sendEvent(name: "numberOfButtons", value: 3) 
//    }
}

def defineMe() {
	log.debug("defineMe()")
//	sendEvent(name: "numberOfButtons", value: 3)
	sendEvent(name: "level", value: 0, unit: "%")
	sendEvent(name: "switch", value: "off")
}

def levelChanging(options){
	log.debug("levelChanging: ${options}")
	def level=0
	if (options.upDown) {
		level=options.level-10
	} else {
		level=options.level+10
	}
	if (level>100) level=100
	if (level<0) level=0 

	sendEvent(name: "level", value: level == 99 ? 100 : level , unit: "%")
	if (level>0 && level<100) {
		if (device.currentValue("switch")=="off") sendEvent(name: "switch", value: "on")
		runIn(1, "levelChanging", [data: [upDown: options.upDown, level: level]])
	} else if (level==0) {
		if (device.currentValue("switch")=="on") sendEvent(name: "switch", value: "off")
	}
}

def buildOffOnEvent(cmd){
	[zwave.basicV1.basicSet(value: cmd), zwave.switchMultilevelV3.switchMultilevelGet()]
}

def on() {
	log.debug("on()")
	commands(buildOffOnEvent(0xFF), 3500)
}

def off() {
	log.debug("off()")
	commands(buildOffOnEvent(0x00), 3500)
}

def setLevel(level) {
	log.debug("setLevel(level:${level})")
	setLevel(level, 1)
}

def setLevel(level, duration) {
	log.debug("setLevel(level:${level}, duration:${duration})")
}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                  Local (private) Methods                                //
/////////////////////////////////////////////////////////////////////////////////////////////
private getCOLOR_TEMP_MIN() { 2700 }
private getCOLOR_TEMP_MAX() { 6500 }
private getCOLOR_TEMP_DIFF() { COLOR_TEMP_MAX - COLOR_TEMP_MIN }

private dimmerEvents(physicalgraph.zwave.Command cmd) {
	def value = (cmd.value ? "on" : "off")
	sendEvent(name: "switch", value: value, descriptionText: "$device.displayName was turned $value")
	if (cmd.value) {
		if (cmd.value>100) cmd.value=100
		sendEvent(name: "level", value: cmd.value == 99 ? 100 : cmd.value , unit: "%")
	}
}

private secEncap(physicalgraph.zwave.Command cmd) {
	zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crcEncap(physicalgraph.zwave.Command cmd) {
	zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
}

private command(physicalgraph.zwave.Command cmd) {
    cmd.format()
}

private commands(commands, delay=200) {
	delayBetween(commands.collect{ command(it) }, delay)
}
