package uk.ac.ox.bodleian.ota.dspace.discovery;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Collection;
import org.dspace.content.Metadatum;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.discovery.SolrServiceSearchPlugin;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows us to experiment with index without change core code
 * 
 * @author Mark Rogerson
 *
 */
public class SolrServiceIndexDebuggerPlugin implements SolrServiceIndexPlugin
{
    private static final Logger log = LoggerFactory
            .getLogger(SolrServiceIndexDebuggerPlugin.class);

    @Override
    public void additionalIndex(Context context, DSpaceObject dso,
            SolrInputDocument document)
    {
        if (dso.getType() == Constants.ITEM)
        {
            Item item = (Item) dso;

    		log.info("we have an item to index");

            //create our filter values
            List<DiscoveryConfiguration> discoveryConfigurations;
            try
            {
                discoveryConfigurations = this.getAllDiscoveryConfigurations(item);

                for (DiscoveryConfiguration discoveryConfiguration : discoveryConfigurations)
                {
                    		log.info("discoveryConfiguration "+discoveryConfiguration.getId());
                }
                

            }
            catch (SQLException e)
            {
                log.error(e.getMessage());
            }


            	document.addField("SolrServiceIndexDebuggerPlugin_s", "Proof we loaded");
            }
        }

    /**
     * Method that retrieves a list of all the configuration objects from the given item
     * A configuration object can be returned for each parent community/collection
     * @param item the DSpace item
     * @return a list of configuration objects
     */
    public static List<DiscoveryConfiguration> getAllDiscoveryConfigurations(Item item) throws SQLException {
        Map<String, DiscoveryConfiguration> result = new HashMap<String, DiscoveryConfiguration>();

       Collection[] collections = item.getCollections();
        for (Collection collection : collections) {
			log.info("configuration for parent collection id:" + collection.getID());

            DiscoveryConfiguration configuration = SearchUtils.getDiscoveryConfiguration(collection);
            if(!result.containsKey(configuration.getId())){
                result.put(configuration.getId(), configuration);
            }
        }

        //Also add one for the default
        DiscoveryConfiguration configuration = SearchUtils.getDiscoveryConfiguration(null);
        if(!result.containsKey(configuration.getId())){
			log.info("configuration for default");
			result.put(configuration.getId(), configuration);
        }
        
        
        /* configuration.getId is returning always null its a bug */
        // forcefully adding our search configuration to display extra facets on search page
        DiscoveryConfigurationService configurationService = SearchUtils.getConfigurationService();
        DiscoveryConfiguration search = configurationService.getMap().get("search");
        if(!result.containsKey("search")){
                    		log.info("configuration for search");

            result.put("search", search);
        }        
        ArrayList<String> list = new ArrayList<String>(result.keySet());   

        return Arrays.asList(result.values().toArray(new DiscoveryConfiguration[result.size()]));
    }

}
