/*
 *	Touch Panel Driver
 *	Code adapted from the one written for RGBGenie by Bryan Copeland
 *
 *  Updated on 2020-07-03 by ELC - Adapted for RGBGenie ZW-3001 only
 */
metadata {
	definition (name: "RGBGenie Touch Panel ZW-3001", namespace: "ecarlotti", author: "ecarlotti") {
        capability "Refresh"
        capability "Actuator"
        capability "Configuration"
        capability "Button"
        capability "Switch"
        
        command "SceneCapture"
        command "StopCapture"
        command "SceneToggle"

		attribute "associationsG1", "string"
        attribute "associationsG2", "string"
        attribute "associationsG3", "string"
        attribute "associationsG4", "string"
        attribute "capture1", "enum", ["on", "off"]
        attribute "capture2", "enum", ["on", "off"]
        attribute "capture3", "enum", ["on", "off"]
        attribute "scene1", "enum", ["on", "off"]
        attribute "scene2", "enum", ["on", "off"]
        attribute "scene3", "enum", ["on", "off"]
        
		fingerprint mfr:"0330", prod:"0301", model:"A109", deviceJoinName: "Touch Panel", role: 0x05
    }
    
    simulator {
    
    }
    
    tiles(scale: 2) {

		valueTile("status", "device.status", decoration: "flat", width: 6, height:1) {
        	state "default", label:'${currentValue}'
        }
        
        standardTile("scene1Tile", "device.switch", width:2, height: 2, decoration: "flat") {
            state "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
            state "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
            state "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
            state "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
        }

		childDeviceTiles("zones")
//		childDeviceTile("zone1switch", "zone1", decoration: "flat", width:6, height: 4, childTileName: "switch")
//		childDeviceTile("zone1level", "zone1", decoration: "flat", width:6, height: 1, childTileName: "level")

//		childDeviceTile("zone2switch", "zone2", decoration: "flat", width:6, height: 4, childTileName: "switch")
//		childDeviceTile("zone2level", "zone2", decoration: "flat", width:6, height: 2, childTileName: "level")
        
//		childDeviceTile("zone3switch", "zone3", decoration: "flat", width:6, height: 4, childTileName: "switch")
//		childDeviceTile("zone3level", "zone3", decoration: "flat", width:6, height: 2, childTileName: "level")
        
 
// 		main(["status"])
		details(["zones"])
//		details(["zone1switch", "zone1level", "zone2switch", "zone2level", "zone3switch", "zone3level"])
//		details(["zone1switch", "zone2switch", "zone3switch"])
    }
    
    preferences {
    	section { input "switchesZ1", "capability.switch", title: "Zone 1 Devices", multiple: true, required: false, displayDuringSetup: true }
        input name: "associationsZ1", type: "string", description: "To add nodes to zone associations use the Hexadecimal nodeID from the IDE device list separated by commas into the space below", title: "Zone 1 Associations", required: false
        input name: "associationsZ2", type: "string", description: "To add nodes to zone associations use the Hexadecimal nodeID from the IDE device list separated by commas into the space below", title: "Zone 2 Associations", required: false
        input name: "associationsZ3", type: "string", description: "To add nodes to zone associations use the Hexadecimal nodeID from the IDE device list separated by commas into the space below", title: "Zone 3 Associations", required: false
	}
}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                  Installation and update                                //
/////////////////////////////////////////////////////////////////////////////////////////////
def installed() {
	log.debug("BEGIN installed()")
    initialize()
    log.debug("END installed()")
}

def updated() {
	log.debug("BEGIN updated()")

	def children = getChildDevices()
    children.each { child ->
    	log.debug "child ${child.displayName} has deviceNetworkId ${child.deviceNetworkId}"
	}	
    def cmds=[]
    for (int i = 1 ; i <= 3; i++) {
        if (!children.any { it -> it.deviceNetworkId == "${device.deviceNetworkId}-$i" } ) {
        	// Save the Touch Panel's device name at the time the child devices were created
            state.oldLabel = device.label
            def child=addChildDevice("RGBGenie Touch Panel Child ZW-3001", "${device.deviceNetworkId}-$i", null, [completedSetup: true, label: "${device.displayName} (Zone$i)", isComponent: true, componentName: "zone$i", componentLabel: "Zone $i"])
            if (child) {
                child.defineMe(i)
            }
            def id = i * 16
            addChildDevice("Momentary Button Tile", "${device.deviceNetworkId}-$id", null, [completedSetup: true, label: "${device.displayName} (Scene$i)", isComponent: true, componentName: "scene$i", componentLabel: "Scene $i"])
        } else if (device.label != state.oldLabel) {
        	// The Touch Panel component was RENAMED, rename the child devices accordingly
            children.each {
            	def oldLabel = it.getLabel()
                def newLabel = oldLabel.contains("Zone") ? "${device.displayName} (Zone${zoneNumber(it.deviceNetworkId)})" : "${device.displayName} (Scene${zoneNumber(it.deviceNetworkId)})" 
                it.setLabel(newLabel)
            }
            state.oldLabel = device.label
        }
        addHubMultiChannel(i).each { cmds << it }
    }
    
    for (int id in switchesZ1*.deviceNetworkId) {
    	log.debug("Zone1 has device id: ${it.deviceNetworkId}")
    }

	processAssociations().each { cmds << it }
    pollAssociations().each { cmds << it }
    log.debug "update: ${cmds}"
	log.debug("END updated()")

	response(commands(cmds))
}

def configure() {
	log.debug("BEGIN configure()")
    initialize()
	log.debug("END configure()")
}

def refresh() {
	log.debug("BEGIN refresh()")
    def cmds=[]
    cmds+=pollAssociations()
    response(commands(cmds))    
	log.debug("END refresh()")
}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                     Event Generation                                    //
/////////////////////////////////////////////////////////////////////////////////////////////
def parse(String description) {
/*
 * Supported Command Classes (documented):
 * 
 * 0x5E V2 COMMAND_CLASS_ZWAVEPLUS_INFO
 * 0x98 V1 COMMAND_CLASS_SECURITY
 * 0x9F V1 COMMAND_CLASS_SECURITY_2
 * 0x55 V2 COMMAND_CLASS_TRANSPORT_SERVICE
 * 0x6C V1 COMMAND_CLASS_SUPERVISION
 *
 * 0x26 V4 COMMAND_CLASS_SWITCH_MULTILEVEL
 * 0x72 V2 COMMAND_CLASS_MANUFACTURER_SPECIFIC
 * 0x86 V2 COMMAND_CLASS_VERSION
 * 0x59 V1 COMMAND_CLASS_ASSOCIATION_GRP_INFO
 * 0x8E V3 COMMAND_CLASS_MULTICHANNEL_ASSOCIATION (SmartThings only supports V2)
 * 0x5B V3 COMMAND_CLASS_CENTRAL_SCENE
 * 0x85 V2 COMMAND_CLASS_ASSOCIATION
 * 0x73 V1 COMMAND_CLASS_POWERLEVEL
 * 0x5A V1 COMMAND_CLASS_DEVICE_RESET_LOCALLY
 * 0x7A V4 COMMAND_CLASS_FIRMWARE_UPDATE_MD 
 *
 * Supported Command Classes (UNdocumented):
 *
 * 0x60 V4 COMMAND_CLASS_MULTI_CHANNEL (SmartThings only supports V3)
 *
 */
    log.debug("parse called for: ${description}")
    def result = null
    def cmd = zwave.parse(description, [0x5E:2,0x98:1,0x9F:1,0x55:2,0x6C:1,0x26:4,0x72:2,0x86:2,0x59:1,0x8E:2,0x5B:3,0x85:2,0x73:1,0x5A:1,0x7A:4,0x60:3]) //CMD_CLASS_VERS)
    if (cmd) {
        result = zwaveEvent(cmd)
        log.debug "Parsed ${cmd} to ${result}"
    } else {
        log.debug "Non-parsed event: ${description}"
    }
    return result
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand()
    log.debug "zwaveEvent(MultiChannelCmdEncap): Got multichannel encap for endpoint: ${cmd.destinationEndPoint}"
    if (encapsulatedCommand) {
    	def child=null
        def children=getChildDevices()
        children.each { 
        	if (it.deviceNetworkId=="${device.deviceNetworkId}-${cmd.destinationEndPoint}") {
            	child=it
            }
        }
        if (child) {
        	def childName=child.getDisplayName()
        	log.debug("zwaveEvent(MultiChannelCmdEncap): sending ${encapsulatedCommand} for endpoint ${cmd.destinationEndPoint} to ${childName}")
            child.zwaveEvent(encapsulatedCommand)
	    } else {
        	log.error("zwaveEvent(MultiChannelCmdEncap): No child found to process multichannel encap for endpoint ${cmd.destinationEndPoint}")
        }
    }
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
    if (encapsulatedCommand) {
        state.sec = 1
        zwaveEvent(encapsulatedCommand)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelassociationv2.MultiChannelAssociationReport cmd) {
    log.debug "multichannel association report: ${cmd}"
    state."zwaveAssociationMultiG${cmd.groupingIdentifier}"="${cmd.nodeId}"
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
    log.debug "${device.label?device.label:device.name}: ${cmd}"
    def temp = []
    if (cmd.nodeId != []) {
       cmd.nodeId.each {
          temp += it.toString().format( '%02x', it.toInteger() ).toUpperCase()
       }
    }
    def zone=cmd.groupingIdentifier-1
    log.debug "${cmd.groupingIdentifier} - $zone - $temp"
    def group=cmd.groupingIdentifier
    sendEvent(name: "associationsG$group", value: "$temp")
    state."associationsG$group"="$temp"
    log.debug "Sending Event (name: associationsG$group, value: $temp)" 
	log.debug "associationsG$group: ${state.assocationsG$group}"
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
    log.debug "${device.label?device.label:device.name}: ${cmd}"
    sendEvent(name: "groups", value: cmd.supportedGroupings)
    log.info "${device.label?device.label:device.name}: Supported association groups: ${cmd.supportedGroupings}"
    state.associationGroups = cmd.supportedGroupings
}

// Handles all Z-Wave commands we don't know we are interested in
def zwaveEvent(physicalgraph.zwave.Command cmd) {
    createEvent([:])
}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                Device-Specific Commands                                 //
/////////////////////////////////////////////////////////////////////////////////////////////

def SceneCapture(sceneId) {
	log.debug "Scene ${sceneId} Capture called..."
	sendEvent(name: "capture${sceneId}", value: "on")
}

def StopCapture() {
	log.debug "StopCapture called..."
	sendEvent(name: "capture1", value: "off")
	sendEvent(name: "capture2", value: "off")
	sendEvent(name: "capture3", value: "off")	
}

def sceneActivate(sceneId) {
   	def child=null
    def children=getChildDevices()
    children.each { 
        if (it.deviceNetworkId=="${device.deviceNetworkId}-${sceneId}") {
            child=it
        }
    }
    if (child) {
        def childName=child.getDisplayName()
        log.debug("sceneActivate: Activating scene ${sceneId/16} on ${childName}")
        child.push()
    } else {
        log.error("sceneActivate: No child found to process sceneActivation for endpoint ${sceneId}")
    }
}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                 Device-Specific Methods                                 //
/////////////////////////////////////////////////////////////////////////////////////////////
def initialize() {
	log.debug("BEGIN initialize()")
    def cmds=[]
    cmds+=pollAssociations()
    commands(cmds)
	log.debug("END initialize()")
}

def pollAssociations() {
	log.debug("BEGIN pollAssociations()")
    def cmds=[]
    for(int i = 1;i<=4;i++) {
        cmds << zwave.associationV2.associationGet(groupingIdentifier:i)
        cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: i)
    }
    log.debug "pollAssociations cmds: ${cmds}"
	log.debug("END pollAssociations()")
    return cmds
}

def setDefaultAssociation() {
	log.debug("BEGIN setDefaultAssociation()")
    def cmds=[]
    cmds << zwave.associationV2.associationSet(groupingIdentifier: 1, nodeId: zwaveHubNodeId)
    log.debug "setDefaultAssociation cmds: ${cmds}"
	log.debug("END setDefaultAssociation()")
    return cmds
}

def addHubMultiChannel(zone) {
	log.debug("BEGIN addHubMultiChannel()")
    def cmds=[]
    def group=zone+1
    cmds << zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier: group, nodeId: [0,zwaveHubNodeId,zone as Integer])
    log.debug "addHubMultiChannel cmds: ${cmds}"
	log.debug("END addHubMultiChannel()")
    return cmds
}

def removeHubMultiChannel(zone) {
	log.debug("BEGIN removeHubMultiChannel()")
    def cmds=[]
    def group=zone+1
    cmds << zwave.multiChannelAssociationV2.multiChannelAssociationRemove(groupingIdentifier: group, nodeId: [0,zwaveHubNodeId,zone as Integer])
    log.debug "removeHubMultiChannel cmds: ${cmds}"
	log.debug("END removeHubMultiChannel()")
    return cmds
}

def processAssociations(){
	log.debug("BEGIN processAssociations()")
    def cmds = []
    cmds += setDefaultAssociation()
    def associationGroups = 4
    for (int i = 2 ; i <= associationGroups; i++) {
        def z=i-1
        log.debug "group: $i dataValue: " + getDataValue("zwaveAssociationG$i") + " parameterValue: " + settings."associationsZ$z"
        def parameterInput=settings."associationsZ$z"
        def newNodeList = []
        def oldNodeList = []
        if (state."zwaveAssociationsG$i" != null) {
            state."zwaveAssociationsG$i".minus("[").minus("]").split(",").each {
                if (it!="") {
                    oldNodeList.add(it.minus(" "))
                }
            }
        }
        
        if (parameterInput!=null) {
            parameterInput.minus("[").minus("]").split(",").each {
                if (it!="") {
                    newNodeList.add(it.minus(" "))
                }
            }
        }
        if (oldNodeList.size > 0 || newNodeList.size > 0) {
            if (logEnable) log.debug "${oldNodeList.size} - ${newNodeList.size}"
            oldNodeList.each {
                if (!newNodeList.contains(it)) {
                    // user removed a node from the list
                    log.debug "removing node: $it, from group: $i"
                    cmds << zwave.associationV2.associationRemove(groupingIdentifier:i, nodeId:Integer.parseInt(it,16))
                }        
            }
            newNodeList.each {
                cmds << zwave.associationV2.associationSet(groupingIdentifier:i, nodeId:Integer.parseInt(it,16))   
            }                            
        }
    }
    log.debug "processAssociations cmds: ${cmds}"
	log.debug("END processAssociations()")
    return cmds
}

def getDevices(__zone) {
	// Zones are groups offset by 1
    //
    // Group 1 is the Lifeline, we can't touch it...
    // Zone 1 = Group 2, Zone 2 = Group 3, Zone 3 = Group 4
	def group = ++__zone
	def associationsGRP = device.currentValue("associationsG$group")
    def group_devices = associationsGRP.tokenize(",[]") 
    return group_devices
}
/////////////////////////////////////////////////////////////////////////////////////////////
//                                  Local (private) Methods                                //
/////////////////////////////////////////////////////////////////////////////////////////////
private getDRIVER_VER() { "0.001" }
private getCMD_CLASS_VERS() { [0x33:3,0x26:3,0x85:2,0x8E:2,0x71:8,0x20:1] }

private zoneNumber(String dni) {
	dni.split("-")[-1] as Integer
}

private command(physicalgraph.zwave.Command cmd) {
    return cmd.format()
}

private commands(commands, delay=500) {
    delayBetween(commands.collect{ command(it) }, delay)
}

private buildOffOnEvent(cmd){
	[zwave.basicV1.basicSet(value: cmd), zwave.switchMultilevelV3.switchMultilevelGet()]
}

