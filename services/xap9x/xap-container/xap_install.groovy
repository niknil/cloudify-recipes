/*******************************************************************************
* Copyright (c) 2014 GigaSpaces Technologies Ltd. All rights reserved
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
import groovy.text.SimpleTemplateEngine
import groovy.util.ConfigSlurper;
import org.cloudifysource.dsl.utils.ServiceUtils
import org.cloudifysource.utilitydomain.context.ServiceContextFactory


context=ServiceContextFactory.serviceContext
config = new ConfigSlurper().parse(new File(context.serviceName+"-service.properties").toURL())

if (!context.isLocalCloud()) {
    new AntBuilder().sequential {
        mkdir(dir: "${config.installDir}")
        get(src: config.downloadPath, dest: "${config.installDir}/${config.zipName}", skipexisting: true)
        unzip(src: "${config.installDir}/${config.zipName}", dest: config.installDir, overwrite: true)
        chmod(dir: "${config.installDir}/${config.xapDir}/bin", perm: "+x", includes: "*.sh")
    }

// Set license if defined
    if (config.license != null && config.license.size() > 0) {
        def binding = ["license": config.license]
        def engine = new SimpleTemplateEngine()
        def gslicense = new File("${context.serviceDirectory}/overwrite/gslicense.xml")
        def template = engine.createTemplate(gslicense).make(binding)

        new File("${config.installDir}/${config.xapDir}/gslicense.xml").withWriter { out ->
            out.write(template.toString())
        }
    } else {
        new AntBuilder().sequential {
            delete(file: "${config.installDir}/${config.xapDir}/gslicense.xml")
        }
    }

    //xap-overrides
    new AntBuilder().sequential {
        copy(todir:"${config.installDir}/${config.xapDir}/", overwrite:true) {
            fileset(dir:"${context.serviceDirectory}/xap-overrides/")
        }
    }
}