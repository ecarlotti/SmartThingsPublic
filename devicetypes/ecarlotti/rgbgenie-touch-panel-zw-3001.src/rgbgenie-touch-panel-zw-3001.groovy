/*
 *	Touch Panel Driver
 *	Code adapted from the one written for RGBGenie by Bryan Copeland
 *
 *  Updated on 2020-07-03 by ELC - Adapted for RGBGenie ZW-3001 only
 */
metadata {
	definition (name: "RGBGenie Touch Panel ZW-3001", namespace: "ecarlotti", author: "RGBGenie") {
        capability "Refresh"
        capability "Actuator"
        capability "Configuration"

		attribute "associationsG1", "string"
        attribute "associationsG2", "string"
        attribute "associationsG3", "string"
        attribute "associationsG4", "string"

		fingerprint mfr:"0330", prod:"0301", model:"A109", deviceJoinName: "Touch Panel"
    }
    
    simulator {
    
    }
    
    tiles(scale: 2) {
/*
		valueTile("associationsG1", "device.associationsG1", decoration: "flat", width: 3, height: 1) {
            state "associationsG1", label:"G1: ${currentValue}", defaultState: true
        }    
        valueTile("associationsG2", "device.associationsG2", decoration: "flat", width: 3, height: 1) {
            state "associationsG2", label:"Z1: ${currentValue}", defaultState: true
        }
        valueTile("associationsG3", "device.associationsG3", decoration: "flat", width: 3, height: 1) {
            state "associationsG3", label:"Z2: ${currentValue}", defaultState: true
        }        
        valueTile("associationsG4", "device.associationsG4", decoration: "flat", width: 3, height: 1) {
            state "associationsG4", label:"Z2: ${currentValue}", defaultState: true
        }

		standardTile("associations", "device.status", decoration: "flat", width: 3, height:1) {
        	state "default", label:'Association'        
        }
        standardTile("states", "device.status", decoration: "flat", width: 3, height:1) {
        	state "default", label:'States'
        }
    	standardTile("zone1", "device.status", decoration: "flat", width: 3, height:1) {
        	state "default", label:'Zone 1'
        }
        standardTile("zone2", "device.status", decoration: "flat", width: 3, height:1) {
        	state "default", label:'Zone 2'
        }
        standardTile("zone3", "device.status", decoration: "flat", width: 3, height:1) {
        	state "default", label:'Zone 3'
        }
        
        childDeviceTile("zone1switch", "zone1", decoration: "flat", width: 3, height:1, childTileName: "switch") 
        childDeviceTile("zone1level", "zone1", decoration: "flat", width: 3, height:1, childTileName: "level") 
        childDeviceTile("zone2switch", "zone2", decoration: "flat", width: 3, height:1, childTileName: "switch") 
        childDeviceTile("zone2level", "zone2", decoration: "flat", width: 3, height:1, childTileName: "level") 
        childDeviceTile("zone3switch", "zone3", decoration: "flat", width: 3, height:1, childTileName: "switch") 
        childDeviceTile("zone3level", "zone3", decoration: "flat", width: 3, height:1, childTileName: "level")
        
    	details(["associations","states","associationsG2", "associationsG3", "associationsG4", "associationsG1", 
        	"zone1", "states", 
            "zone1switch", "zone1level",  
        	"zone2", "states", 
            "zone2switch", "zone2level",             
        	"zone3", "states", 
            "zone3switch", "zone3level",             
        ])
*/        

		standardTile("status", "device.status", decoration: "flat", width: 3, height:1) {
        	state "default", label:'Status'
        }
		childDeviceTiles("zones")
 
 		main "status"
  		details(["status", "zones"])
    }
    
    preferences {
        input name: "associationsZ1", type: "string", description: "To add nodes to zone associations use the Hexidecimal nodeID from the IDE device list separated by commas into the space below", title: "Zone 1 Associations", required: false
        input name: "associationsZ2", type: "string", description: "To add nodes to zone associations use the Hexidecimal nodeID from the IDE device list separated by commas into the space below", title: "Zone 2 Associations", required: false
        input name: "associationsZ3", type: "string", description: "To add nodes to zone associations use the Hexidecimal nodeID from the IDE device list separated by commas into the space below", title: "Zone 3 Associations", required: false
	}
}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                  Installation and update                                //
/////////////////////////////////////////////////////////////////////////////////////////////
def installed() {
	log.debug("BEGIN installed()")
    createChildDevices()
    initialize()
    log.debug("END installed()")
}

def updated() {
	log.debug("BEGIN updated()")
    def cmds=[]

	for (int i = 1 ; i <= 3; i++) {
        def child=null
        children.each { it->
            if (it.deviceNetworkId=="${device.deviceNetworkId}-$i") {
                child=it
            }
        }
        if(child) {
            log.debug "updating child: ${child.deviceNetworkId} setting sceneMode: " + settings."sceneMode$i"
            child.enableSceneCapture(settings."sceneMode$i")
        }
        addHubMultiChannel(i).each { cmds << it }
    }

	processAssociations().each { cmds << it }
    pollAssociations().each { cmds << it }
    log.debug "updated: ${cmds}"
	log.debug("END updated()")
    response(commands(cmds))
}

def configure() {
	log.debug("BEGIN configure()")
    initialize()
	log.debug("END configure()")
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
 * 0x60 V4 COMMAND_CLASS_MULTI_CHANNEL
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

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    if (logEnable) log.debug "skip:${cmd}"
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
    if (logEnable) log.debug "${device.label?device.label:device.name}: ${cmd}"
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
    if (logEnable) log.debug "${device.label?device.label:device.name}: ${cmd}"
    sendEvent(name: "groups", value: cmd.supportedGroupings)
    log.info "${device.label?device.label:device.name}: Supported association groups: ${cmd.supportedGroupings}"
    state.associationGroups = cmd.supportedGroupings
}

/////////////////////////////////////////////////////////////////////////////////////////////
//                                 Device-Specific Methods                                 //
/////////////////////////////////////////////////////////////////////////////////////////////
def createChildDevices() {
	def cmds=[]
	for (i in 1..3) {
        def child=addChildDevice("RGBGenie Touch Panel Child ZW-3001", 
        						 "${device.deviceNetworkId}-$i", 
                                 null, 
                                 [
                                 	completedSetup: true, 
                                 	label: "${device.displayName} (Zone$i)", 
                                    isComponent: true, 
                                    componentName: "zone$i", 
                                    componentLabel: "Zone $i"
                                 ])
                                 
        if (child) {
            child.defineMe()
        } else {
        	log.error("ERROR adding child device $i")
        }

		addHubMultiChannel(i).each { cmds << it }
    }
    
	def children = getChildDevices()
    children.each { child ->
    	log.debug "child ${child.displayName} has deviceNetworkId ${child.deviceNetworkId}"
	}    
}

def initialize() {
	log.debug("BEGIN initialize()")
    def cmds=[]
    cmds+=pollAssociations()
    commands(cmds)
	log.debug("END initialize()")
}

def refresh() {
	log.debug("BEGIN refresh()")
    def cmds=[]
    cmds+=pollAssociations()
    response(commands(cmds))    
	log.debug("END initializa()")
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

/////////////////////////////////////////////////////////////////////////////////////////////
//                                  Local (private) Methods                                //
/////////////////////////////////////////////////////////////////////////////////////////////
private getDRIVER_VER() { "0.001" }
private getCMD_CLASS_VERS() { [0x33:3,0x26:3,0x85:2,0x8E:2,0x71:8,0x20:1] }

private command(physicalgraph.zwave.Command cmd) {
    return cmd.format()
}

private commands(commands, delay=200) {
    delayBetween(commands.collect{ command(it) }, delay)
}

