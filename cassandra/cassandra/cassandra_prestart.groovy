/*******************************************************************************
* Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
import org.cloudifysource.utilitydomain.context.ServiceContextFactory
import groovy.util.ConfigSlurper;
import java.util.concurrent.TimeUnit


	def String parseHostAddress(address)
	{
		def retAddress
		if(address.contains("/")) {
			retAddress = address.split('/')[0];
		}
		else {
			retAddress = ""+address;
		}
		return ""+retAddress;
	}
config = new ConfigSlurper().parse(new File("cassandra.properties").toURL())

//configure the YAML

	serviceContext = ServiceContextFactory.getServiceContext()
	service = serviceContext.waitForService("cassandra", 20, TimeUnit.SECONDS)
	numOfInstances = service.getNumberOfPlannedInstances()
	println("numOfInstances: " + numOfInstances)
	service.waitForInstances(numOfInstances, 20, TimeUnit.SECONDS);
  
	agents = service.getInstances()
	agentlist = "- 127.0.0.1\n"
	agents.each { agentlist += "    - " + parseHostAddress(it.getHostAddress()) + "\n"; println("adding agent for instance id " + it.getInstanceID()); }
	println "agentlist: " + agentlist
	instanceID = serviceContext.getInstanceId()
	ip = parseHostAddress(serviceContext.getPrivateAddress());

	
	numOfSeeds ="${config.numOfSeeds}".toInteger();
	seedsList = parseHostAddress(agents[0].getHostAddress());
	seedsLoop = numOfSeeds - 1 
	println "this is the seed looop : "  + seedsLoop
	if (seedsLoop >0)
	{
		for (int i in (1..seedsLoop)) {
			 seedsList += "," + parseHostAddress(agents[i].getHostAddress())
		}
	}
	
	installDir = System.properties["user.home"]+ "/.cloudify/${config.serviceName}" + instanceID
	home = "${serviceContext.serviceDirectory}/${config.unzipFolder}"
	conf = "${home}/conf"
	yaml = new File("${conf}/cassandra.yaml")
	println "cassandra yaml location: " + yaml.getAbsolutePath()
	yamltext = yaml.text
	backup = new File("${conf}/cassandra.yaml_bak")
	backup.write yamltext
	token =(BigInteger)((instanceID -1)*(2**127)/numOfInstances)
	println "setting token: " + token
	yamltext = yamltext.replaceAll("initial_token:", "initial_token: " + token)
	yamltext = yamltext.replaceAll("- 127.0.0.1\n", agentlist)
	yamltext = yamltext.replaceAll("127.0.0.1", seedsList)
	yamltext = yamltext.replaceAll("listen_address: localhost", "listen_address: " + ip)
	yamltext = yamltext.replaceAll("rpc_address: localhost", "rpc_address: 0.0.0.0")
//	yamltext = yamltext.replaceAll("endpoint_snitch: org.apache.cassandra.locator.SimpleSnitch", "endpoint_snitch: org.apache.cassandra.locator.RackInferringSnitch")
	yamltext = yamltext.replaceAll("/var/lib/cassandra/data", "../lib/cassandra/data")
	yamltext = yamltext.replaceAll("/var/lib/cassandra/commitlog", "../lib/cassandra/commitlog")
	yamltext = yamltext.replaceAll("/var/lib/cassandra/saved_caches", "../lib/cassandra/saved_caches")
	yaml.write yamltext
	println "wrote new yaml"
	logprops = new File(conf + "/log4j-server.properties")
	logpropstext = logprops.text
	logpropstext = logpropstext.replaceAll("/var/log/cassandra/system.log", "../log/cassandra/system.log")
	logprops.write logpropstext
