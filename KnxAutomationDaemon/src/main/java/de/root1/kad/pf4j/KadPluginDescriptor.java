/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.kad.pf4j;

import com.github.zafarkhaja.semver.Parser;
import com.github.zafarkhaja.semver.Version;
import static com.github.zafarkhaja.semver.expr.CompositeExpression.Helper.gte;
import com.github.zafarkhaja.semver.expr.Expression;
import com.github.zafarkhaja.semver.expr.ExpressionParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginDependency;
import ro.fortsoft.pf4j.PluginDescriptor;
import ro.fortsoft.pf4j.util.StringUtils;

/**
 *
 * @author achristian
 */
public class KadPluginDescriptor extends PluginDescriptor {

    private Logger log = LoggerFactory.getLogger(getClass());
    
    private final String pluginId;
    private final String pluginDescription;
    private final String pluginClass;
    private final Version pluginVersion;
    private final Expression requires;
    private final String pluginProvider;
    private final List<PluginDependency> dependencies;

    public KadPluginDescriptor(String pluginId, String pluginDescription, String pluginClass, Version pluginVersion, String requires, String pluginProvider, String dependencies) {
        
        this.pluginId = pluginId;
        this.pluginDescription = pluginDescription;
        this.pluginClass = pluginClass;
        this.pluginVersion = pluginVersion;
        
        if (!StringUtils.isEmpty(requires)) {
            Parser<Expression> parser = ExpressionParser.newInstance();
            this.requires = parser.parse(requires);
        } else {
            this.requires = gte("0.0.0");
        }
        
        this.pluginProvider = pluginProvider;
        
        if (dependencies != null) {
    		dependencies = dependencies.trim();
    		if (dependencies.isEmpty()) {
    			this.dependencies = Collections.emptyList();
    		} else {
	    		this.dependencies = new ArrayList<>();
	    		String[] tokens = dependencies.split(",");
	    		for (String dependency : tokens) {
	    			dependency = dependency.trim();
	    			if (!dependency.isEmpty()) {
	    				this.dependencies.add(new PluginDependency(dependency));
	    			}
	    		}
                        // macht wenig Sinn?!
//	    		if (this.dependencies.isEmpty()) {
//	    			this.dependencies = Collections.emptyList();
//	    		}
    		}
    	} else {
    		this.dependencies = Collections.emptyList();
    	}
        
    }

    /**
     * Returns the unique identifier of this plugin.
     * @return 
     */
    @Override
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Returns the description of this plugin.
     * @return 
     */
    @Override
    public String getPluginDescription() {
        return pluginDescription;
    }

    /**
     * Returns the name of the class that implements Plugin interface.
     * @return 
     */
    @Override
    public String getPluginClass() {
        return pluginClass;
    }

    /**
     * Returns the pluginVersion of this plugin.
     * @return 
     */
    @Override
    public Version getVersion() {
        return pluginVersion;
    }

    /**
     * Returns the requires of this plugin.
     * @return 
     */
    @Override
    public Expression getRequires() {
        return requires;
    }

    /**
     * Returns the pluginProvider name of this plugin.
     * @return 
     */
    @Override
    public String getProvider() {
        return pluginProvider;
    }

    /**
     * Returns all dependencies declared by this plugin. Returns an empty array
     * if this plugin does not declare any require.
     * @return 
     */
    @Override
    public List<PluginDependency> getDependencies() {
        return dependencies;
    }

    @Override
    public String toString() {
        return "KadPluginDescriptor{" + "pluginId=" + pluginId + ", pluginDescription=" + pluginDescription + ", pluginClass=" + pluginClass + ", pluginVersion=" + pluginVersion + ", requires=" + requires + ", pluginProvider=" + pluginProvider + ", dependencies=" + dependencies + '}';
    }

    

}
