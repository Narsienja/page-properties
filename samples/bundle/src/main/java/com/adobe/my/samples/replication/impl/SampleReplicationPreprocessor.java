

package com.adobe.my.samples.replication.impl;

import java.util.Map;

import com.day.cq.replication.Preprocessor;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true, metatype=true, label="PreProcessor to validate Page properties values")
@Service({Preprocessor.class})
public class SampleReplicationPreprocessor implements Preprocessor {
    private static final Logger log = LoggerFactory.getLogger(SampleReplicationPreprocessor.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Property( unbounded= PropertyUnbounded.ARRAY)
    private static final String PageProperties = "config.multiproperty";
    
    private static  String [] pagePropetiesAuthored;
    
    @Override
    public void preprocess(final ReplicationAction replicationAction,
                           final ReplicationOptions replicationOptions) throws ReplicationException {

        if (replicationAction == null || !ReplicationActionType.ACTIVATE.equals(replicationAction.getType())) {
            // Do nothing
            return;
        }

        // Get the path of the replication payload
        final String path = replicationAction.getPath();
		if(!path.trim().startsWith("/content/")){
			return;
		}

        ResourceResolver resourceResolver = null;

        try {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            // Get the payload as a resource; In this case getting the jcr:content node since we'll
            // be writing a custom value to it (this will fail if writing to cq:Page resource)
            final Resource resource = resourceResolver.getResource(path).getChild("jcr:content");

            if (resource == null) {
                // Remember; ALL replications go through this; so check to make sure that what
                // you're doing is Universal OR put your checks in early.
                log.warn("Could not find jcr:content node for resource to apply checksum!");
                return;
            }

            // Get the resource's properties for modification
            final ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);
 
                String primaryType =  properties.get("jcr:primaryType",String.class);
				if(!primaryType.equals("cq:PageContent")) {					return;
				}
            // Apply some business logic; in this case we write a checksum based on some properties we care about
          for(String property : pagePropetiesAuthored) {
        	  if(!properties.containsKey(property)){
        		  throw new ReplicationException("Page should not be published without authoring property"+property);
        	  }
          }
            resourceResolver.commit();
        } catch (LoginException e) {
            // To prevent Replication from happening, throw a ReplicationException
            throw new ReplicationException(e);
        } catch (PersistenceException e) {
            // To prevent Replication from happening, throw a ReplicationException
            throw new ReplicationException(e);
        } finally {
                if (resourceResolver != null && resourceResolver.isLive()) {
                // Always close resource resolver you open
                resourceResolver.close();
            }
        }
    }
   
        	@Activate
            @Modified
            protected void init(final Map<String, Object> props) {
                if (props != null) {
                	pagePropetiesAuthored = PropertiesUtil.toStringArray(props.get(PageProperties));  
                	}
   
    }

    private Long checksum(final ValueMap properties) {
        // Stub method; Compute a checksum of certain properties values you care bout
        return 1L;
    }
}
