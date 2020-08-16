/*
*	Touch Panel Child Driver
*   Code adapted from the one written for RGBGenie by Bryan Copeland
*
*   Updated 2020-07-03 Adapted for RGBGenie ZW-3001 only
*
*   Works with the SmartLighting App using mirror mode
*
*/

metadata {
	definition (name: "RGBGenie Touch Panel Child ZW-3001", namespace: "ecarlotti", author: "ecarlotti") {
		capability "Switch"
		capability "Switch Level"
		capability "Button"
		capability "Actuator"
        
        attribute "sceneCapture", "boolean"
        attribute "zone", "number"
	}
    
    simulator {
    
    }
    
    tiles(scale: 2) {
        multiAttributeTile(name:"rich-switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"switch level.setLevel", defaultState: true
            }
        }
    
        standardTile("switch", "device.switch", decoration: "flat", height: 4, width: 6, canChangeIcon: true) {
            state "off", label:'Off', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
            state "on", label:'On', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
            state "turningOn", label:'Turning on', icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState: "turningOff"
            state "turningOff", label:'Turning off', icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState: "turningOn"
        }
		controlTile("level", "device.level", "slider", decoration: "flat", height: 2, width: 6, inactiveLabel: false) {
			state "level", action:"switch level.setLevel"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "refresh", label:"", action:"refresh.refresh", icon:"st.secondary.refresh", defaultState: true
		}

        main(["rich-switch"])
    	details(["switch", "level", "refresh"])
    }    
}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                  Installation and update                                //
/////////////////////////////////////////////////////////////////////////////////////////////
def installed() {
	log.debug("installed()")
}

def updated() {
	log.debug("updated()")
}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                     Event Generation                                    //
/////////////////////////////////////////////////////////////////////////////////////////////
def parse(description) {
	log.debug "parse: ${description}"
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
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfSet cmd) {
	log.debug("SceneActuatorConfSet: ${cmd}")

/*
	if (!state.scene) { state.scene=[:] }
    def __scene = ["level": device.currentValue("level"), "switch": device.currentValue("switch")]
    state.scene["${cmd.sceneId}"] = __scene
    log.debug("SceneActuatorConfSet: scene[${cmd.sceneId}] = ${__scene}")
*/

	def sceneId = cmd.sceneId as Integer
    parent.SceneCapture(sceneId/16)
    runIn(5, parent.StopCapture)
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd) {
	log.debug("SceneActivationSet: ${cmd}")

/*    
    if (!state.scene) { state.scene=[:] }
    def scene=state.scene["${cmd.sceneId}"]
    scene.each { k, v ->
        sendEvent(name: k, value: v)
        log.debug("SceneActivationSet: sendEvent(name: ${k}, value:${v})")
    }    
*/

    def sceneId = cmd.sceneId as Integer
	parent.sceneActivate(sceneId)    
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

// Handles all Z-Wave commands we don't know we are interested in
def zwaveEvent(physicalgraph.zwave.Command cmd) {
    createEvent([:])
}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                 Device-Specific Methods                                 //
/////////////////////////////////////////////////////////////////////////////////////////////
def enableSceneCapture(value) {
	log.debug("enableSceneCapture: ${value}")
    sendEvent(name: "sceneCapture", value: value) 
}

def defineMe(zoneId) {
	log.debug("defineMe($zoneId)")
	sendEvent(name: "zone", value: zoneId)
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
	sendEvent(name: "switch", value: "on", descriptionText: "$device.displayName was turned $value")
	commands(buildOffOnEvent(0xFF), 3500)
}

def off() {
	log.debug("off()")
	sendEvent(name: "switch", value: "off", descriptionText: "$device.displayName was turned $value")
	commands(buildOffOnEvent(0x00), 3500)
}

def nextLevel() {
	def level = device.latestValue("level") as Integer ?: 0
	if (level <= 100) {
		level = Math.min(25 * (Math.round(level / 25) + 1), 100) as Integer
	}
	else {
		level = 25
	}
	setLevel(level)
}

def setLevel(percent, rate=null) {
	log.debug("setLevel(level:${percent})")
    sendEvent(name: "level", value: percent == 99 ? 100 : percent , unit: "%")
}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                  Local (private) Methods                                //
/////////////////////////////////////////////////////////////////////////////////////////////
private dimmerEvents(physicalgraph.zwave.Command cmd) {
	def zone=device.currentValue("zone")
	def zone_members=parent.getDevices(zone)
    log.debug("${device.label} --> devices on zone $zone are: ${zone_members}")
    
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
