/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  On/Off Button Tile
 *
 *  Author: SmartThings
 *
 *  Date: 2013-05-01
 */
metadata {
	definition (name: "On/Off/Toggle Button Tile", namespace: "ecarlotti", author: "SmartThings") {
		capability "Actuator"
		capability "Switch"
		capability "Sensor"
	}

	// simulator metadata
	simulator {
	}

	// UI tile definitions
	tiles {
		standardTile("button", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: 'Off', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "on"
			state "on", label: 'On', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC", nextState: "off"
		}
		main "button"
		details "button"
	}
}

def parse(String description) {
}

def on() {
	log.debug("Turning switch ON...")
	sendEvent(name: "switch", value: "on")
}

def off() {
	log.debug("Turning switch OFF...")
	sendEvent(name: "switch", value: "off")
}

def off_nostatechange() {
	log.debug("Turning switch OFF...")
	sendEvent(name: "switch", value: "off", isStateChange: false, displayed: false)
}

def toggle() {
	log.debug("toggle() called...")

	def lval = device.latestValue("switch")
    def lstate = device.latestState("switch")
    
	log.debug("device.latestValue(switch)=${lval}")
	log.debug("device.latestState(switch)=${lstate}")
    
	if (lval == "off") {
     	on()
    } else {
    	off()
    }
}