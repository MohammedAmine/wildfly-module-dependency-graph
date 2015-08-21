package com.jamesnetherton.rest;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class ModuleService {

    public static final String MODULE_LOADER_PATH = "jboss.modules:type=ModuleLoader,name=LocalModuleLoader-2";
    private MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    @GET
    @Path("/module")
    public Module getLoadedModules() {
        Module rootModule = new Module("modules");

        try {
            ObjectName objectName = new ObjectName(MODULE_LOADER_PATH);
            String[] loadedModules = (String[]) server.invoke(objectName, "queryLoadedModuleNames", null, null);
            for(String moduleName : loadedModules) {
                rootModule.addToChildren(moduleName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rootModule;
    }

    @GET
    @Path("/module/{moduleName}")
    public List<Module> getModuleDescription(@PathParam("moduleName") String moduleName) {
        List<Module> moduleDependencies = new ArrayList<>();
        try {
            ObjectName objectName = new ObjectName(MODULE_LOADER_PATH);
            CompositeData composite = (CompositeData) server.invoke(objectName, "getModuleDescription",
                    new Object[]{moduleName}, new String[]{String.class.getName()});

            CompositeData[] dependencies = (CompositeData[]) composite.get("dependencies");
            for (CompositeData dependency : dependencies) {
                String name = (String) dependency.get("moduleName");
                if (name != null) {
                    moduleDependencies.add(new Module(name));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return moduleDependencies;
    }

    private static class Module {
        private String name;
        private String slot;
        private List<Module> children = new ArrayList<>();

        public Module(String name) {
            String[] nameParts = name.split(":");
            this.name = nameParts[0];
            if (nameParts.length > 1) {
                this.slot = nameParts[1];
            }
        }

        public void addToChildren(String moduleName) {
            children.add(new Module(moduleName));
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSlot() {
            return slot;
        }

        public void setSlot(String slot) {
            this.slot = slot;
        }

        public List<Module> getChildren() {
            return this.children;
        }
    }
}
