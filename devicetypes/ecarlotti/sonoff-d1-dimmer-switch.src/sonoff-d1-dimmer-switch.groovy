/**
 *  SONOFF D1 Dimmer Switch
 *
 *  Copyright 2020 Edson Carlotti
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
metadata {
	definition (name: "SONOFF D1 Dimmer Switch", namespace: "ecarlotti", author: "Edson Carlotti", cstHandler: true) {
		capability "Switch"
		capability "Switch Level"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale:2) {
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
			}
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
	    }
	}
    
	preferences {
        input "device_ip", "text", title: "Device IP", required: true
        input "device_port", "text", title: "Device Port", required: true
        input "device_id", "text", title: "Device ID", required: true
    }
    
    main "switch"
    details (["switch"])
}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                  Installation and update                                //
/////////////////////////////////////////////////////////////////////////////////////////////
def installed() {
	log.debug("installed()")
	sendEvent(name: "level", value: 0, unit: "%")
	sendEvent(name: "switch", value: "off")
}

def updated() {
	log.debug("updated()")
}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                     Event Generation                                    //
/////////////////////////////////////////////////////////////////////////////////////////////

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'switch' attribute
	// TODO: handle 'level' attribute

}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                 Device-Specific Methods                                 //
/////////////////////////////////////////////////////////////////////////////////////////////
def on() {
	log.debug "Executing 'on'"
    sonoff_on()
	sendEvent(name: "switch", value: "on", descriptionText: "$device.displayName was turned on")
}

def off() {
	log.debug "Executing 'off'"
    sonoff_off()
	sendEvent(name: "switch", value: "off", descriptionText: "$device.displayName was turned off")
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

	sonoff_setLevel(level == 99 ? 100 : level)
	sendEvent(name: "level", value: level == 99 ? 100 : level , unit: "%", descriptionText: "$device.displayName level was set to ${percent}%")
	if (level>0 && level<100) {
		if (device.currentValue("switch")=="off") sendEvent(name: "switch", value: "on", descriptionText: "$device.displayName was turned on")
		runIn(1, "levelChanging", [data: [upDown: options.upDown, level: level]])
	} else if (level==0) {
		if (device.currentValue("switch")=="on") sendEvent(name: "switch", value: "off", descriptionText: "$device.displayName was turned off")
	}
}

def setLevel(percent, rate=null) {
	log.debug("setLevel(level:${percent})")
	sonoff_setLevel(level == 99 ? 100 : level)
    sendEvent(name: "level", value: percent == 99 ? 100 : percent , unit: "%", descriptionText: "$device.displayName level was set to ${percent}%")
}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                  Local (private) Methods                                //
/////////////////////////////////////////////////////////////////////////////////////////////
private sonoff_on() {

    def result = new physicalgraph.device.HubAction (
        method: "POST",
        path: "/zeroconf/switch",
        body: '{ "deviceid": "${device_id}", "data": { "switch": "on" } }',
        headers: [ HOST: "${device_ip}:${device_port}" ]
    )
    sendHubCommand(result)

}

private sonoff_off() {

    def result = new physicalgraph.device.HubAction (
        method: "POST",
        path: "/zeroconf/switch",
        body: '{ "deviceid": "${device_id}", "data": { "switch": "off" } }',
        headers: [ HOST: "${device_ip}:${device_port}" ]
    )
    sendHubCommand(result)

}

private sonoff_setLevel(level) {

    def result = new physicalgraph.device.HubAction (
        method: "POST",
        path: "/zeroconf/dimmable",
        body: '{ "deviceid": "${device_id}", "data": { "switch": "on", "brightness": ${level} } }',
        headers: [ HOST: "${device_ip}:${device_port}" ]
    )
    sendHubCommand(result)

}
