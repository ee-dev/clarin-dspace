package uk.ac.ox.bodleian.ota.dspace.discovery;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.discovery.SolrServiceSearchPlugin;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dspace.util.MultiFormatDateParser;
import uk.ac.ox.bodleian.ota.dspace.util.OTADate;
import org.apache.commons.lang.time.DateFormatUtils;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;

import cz.cuni.mff.ufal.IsoLangCodes;

/**
 * Keeps most of our search query/index tweaks
 * 
 * @author LINDAT/CLARIN team
 *
 */
public class SolrServiceTweaksPlugin implements SolrServiceIndexPlugin,
        SolrServiceSearchPlugin
{
    private static final Logger log = LoggerFactory
            .getLogger(SolrServiceTweaksPlugin.class);

    @Override
    public void additionalSearchParameters(Context context,
            DiscoverQuery discoveryQuery, SolrQuery solrQuery)
    {
        String query = discoveryQuery.getQuery();
        if (query == null)
        {
            query = "*:*";
        }
		String q = solrQuery.getQuery();

        // only search title if there is user search text
		if (!"*:*".equals(query))
        {
			q = q + " OR title:(" + query + ")^5";
        }

 		// PREMOTE NOT isreplacedby 
        q = q + " OR ((" + q + ") AND -dc.relation.isreplacedby:*)^5";
        // PREMOTE replacements
		q = q + " OR ((" + q + ") AND dc.relation.replaces:*)^15";
        solrQuery.setQuery(q);

    }

    @Override
    public void additionalIndex(Context context, DSpaceObject dso,
            SolrInputDocument document)
    {
        if (dso.getType() == Constants.ITEM)
        {
            Item item = (Item) dso;
            //create our filter values
            List<DiscoveryConfiguration> discoveryConfigurations;
            try
            {
                discoveryConfigurations = SearchUtils
                        .getAllDiscoveryConfigurations(item);
                Map<String, List<DiscoverySearchFilter>> searchFilters = new HashMap<String, List<DiscoverySearchFilter>>();
                // read config
                // partly yanked from SolrServiceImpl
                for (DiscoveryConfiguration discoveryConfiguration : discoveryConfigurations)
                {
                    for (int i = 0; i < discoveryConfiguration
                            .getSearchFilters().size(); i++)
                    {
                        DiscoverySearchFilter discoverySearchFilter = discoveryConfiguration
                                .getSearchFilters().get(i);
                        for (int j = 0; j < discoverySearchFilter
                                .getMetadataFields().size(); j++)
                        {
                            String metadataField = discoverySearchFilter
                                    .getMetadataFields().get(j);
                            List<DiscoverySearchFilter> resultingList;
                            String type = discoverySearchFilter.getType();
                            // Process only our new types
                            if (type.equals(DiscoveryConfigurationParameters.TYPE_OTA_IDENTIFIER)
                                    || type.equals(DiscoveryConfigurationParameters.TYPE_STC_IDENTIFIER)
                                    || type.equals(DiscoveryConfigurationParameters.TYPE_RAW)
                                    || type.equals(DiscoveryConfigurationParameters.TYPE_ISO_LANG)
                                    || type.equals(DiscoveryConfigurationParameters.TYPE_ERA_DATE)
                                    )
                            {
                                if (searchFilters.get(metadataField) != null)
                                {
                                    resultingList = searchFilters.get(metadataField);
                                }
                                else
                                {
                                    // New metadata field, create a new list for
                                    // it
                                    resultingList = new ArrayList<DiscoverySearchFilter>();
                                }
                                resultingList.add(discoverySearchFilter);

                                searchFilters.put(metadataField, resultingList);
                            }
                        }
                    }
                }
                
                for (Map.Entry<String, List<DiscoverySearchFilter>> entry : searchFilters
                        .entrySet())
                {
                	//clear any input document fields we are about to add lower
                    //String metadataField = entry.getKey();
                    List<DiscoverySearchFilter> filters = entry.getValue();
                    for (DiscoverySearchFilter filter : filters)
                    {
                    	String name = filter.getIndexFieldName();
                    	String[] names = {name, name + "_filter", name + "_keyword",
                    			name + "_ac"};
                    	for(String fieldName : names){
                    		document.removeField(fieldName);
                    		log.info("remove :"+fieldName);

                    	}
                    }
                }

                for (Map.Entry<String, List<DiscoverySearchFilter>> entry : searchFilters
                        .entrySet())
                {
                    String metadataField = entry.getKey();
                    List<DiscoverySearchFilter> filters = entry.getValue();
                    Metadatum[] mds = item
                            .getMetadataByMetadataString(metadataField);
                    for (Metadatum md : mds)
                    {
                        String value = md.value;
                        for (DiscoverySearchFilter filter : filters)
                        {
                        
                    		log.info("filter.getIndexFieldName :"+filter.getIndexFieldName());
                    		log.info("filter.getFilterType :"+filter.getFilterType());
                    		log.info("filter.getType :"+filter.getType());

                            if (filter
                                    .getFilterType()
                                    .equals(DiscoverySearchFilterFacet.FILTER_TYPE_FACET))
                            {
                                String convertedValue = null;
                                if (filter
                                        .getType()
                                        .equals(DiscoveryConfigurationParameters.TYPE_RAW))
                                {
                                    // no lowercasing and separators for this
                                    // type
                                    convertedValue = value;
                                }
                                else if (filter
                                        .getType()
                                        .equals(DiscoveryConfigurationParameters.TYPE_OTA_IDENTIFIER))
                                {
                                    // TYPE_OTA_IDENTIFIER remove ota: from field value
                                    // type
                                    if(value.indexOf("ota:")==0)
                                    {
										convertedValue = value.substring(4);
                                    }
                                }
                                else if (filter
                                        .getType()
                                        .equals(DiscoveryConfigurationParameters.TYPE_ISO_LANG))
                                {
                                    String langName = IsoLangCodes
                                            .getLangForCode(value);
                                    if (langName != null)
                                    {
                                        convertedValue = langName.toLowerCase()
                                                + SolrServiceImpl.FILTER_SEPARATOR
                                                + langName;
                                    }
                                    else
                                    {
                                        log.error(String
                                                .format("No language found for iso code %s",
                                                        value));
                                    }
                                }
                                
// Now set the fields
                                if (convertedValue != null)
                                {
                                    document.addField( filter.getIndexFieldName(), convertedValue);
                                    document.addField( filter.getIndexFieldName() + "_keyword", convertedValue);
                                    document.addField( filter.getIndexFieldName() + "_filter", convertedValue);

                                }
                                 else
                                 {
                                    document.addField( filter.getIndexFieldName(), value);
                                    document.addField( filter.getIndexFieldName() + "_keyword", value);
                                }
                            }
//MR always — even if not a facet, based on lindat code IsoLangCodes
                                if (filter
                                        .getType()
                                        .equals(DiscoveryConfigurationParameters.TYPE_ERA_DATE))
                                {
                                String indexField = filter.getIndexFieldName();
									//For our search filters that are Created dates we need to handle BCE dates
									log.info("process TYPE_ERA_DATE: "+indexField);

									// remove existing field's values
									// toSortFieldIndex returns '_year_sort' for TYPE_ERA_DATE not '_dt' as for 'real' dates
									// Debug start -- did previous index add any values for TYPE_ERA_DATE 
                                        if(document.getField(indexField) != null)
										{
											document.removeField(indexField);
										}
                                        if(document.getField(indexField + "_sort") != null)
										{
											document.removeField(indexField+"_sort");
										}
                                       if(document.getField(indexField + "_year_sort") != null)
										{
											document.removeField(indexField+"_year_sort");
										}
                                       if(document.getField(indexField + "_dt") != null)
										{
											document.removeField(indexField+"_dt");
										}
                                       if(document.getField(indexField + "_ac") != null)
										{
											document.removeField(indexField+"_ac");
										}
                                       if(document.getField(indexField + "_keyword") != null)
										{
											document.removeField(indexField+"_keyword");
										}
                                       if(document.getField(indexField + ".year") != null)
										{
											document.removeField(indexField+".year");
										}
									// Debug end

									Date date = null;
// Let's tidy up the date a bit here


									if(value != null)
									{
										log.info("Input Date: "+value);

										value = OTADate.clean(value);
										log.info("Clean Date: "+value);
									//MultiFormatDateParser needs a 4 digit year for now
										switch(value.length())
											{
												case 3:
												value = "0"+value;
												break;
												case 2:
												value = "00"+value;
												break;
												case 1:
												value = "000"+value;
												break;
												default:
												value = value;
											}
									}
						
									if(value != null)
									{
										date = MultiFormatDateParser.parse(value);
									}

									if(date != null)
									{
										// can't parse a null date!
										String year = DateFormatUtils.formatUTC(date,"yyyy");
										log.info("Date "+value+ " parsed as "+year);

								/*
										// Get the era
										Calendar cal = Calendar.getInstance();
										cal.setTime(date);
										// Do we want to explicitly add 'CE' for current era dates?
										String era = cal.get(Calendar.ERA)==0?" BCE":" CE";
										String yearString = yearUTC  + era;
										
										Integer sign = cal.get(Calendar.ERA)==0?-1:1;
										Integer year = Integer.parseInt(yearUTC);
										year = year * sign;
									*/	
										document.addField(indexField + ".year", year);
										

										// Autocomplete without leading zeros
										// default TYPE_DATE adds both leading zero and non-zero copies (ugh!), we don't.
                                    	if (year.startsWith("0"))
                                        {
        									// add date without starting zeros for autocomplete e filtering
											document.addField(indexField+ "_ac",year.replaceFirst("0*", ""));
											document.addField(indexField+ "_keyword",year.replaceFirst("0*", ""));
										}
										else
										{
											document.addField(indexField + "_ac", year);
											document.addField(indexField + "_keyword", year);
										}

                                    	//Also save a sort value of this year, this is required for determining the upper & lower bound year of our facet
                                        if(document.getField(indexField + "_year_sort") == null)
                                        {
                                        	//We can only add one year so take the first one
                                        	document.addField(indexField + "_year_sort", year);
                                    	}

										document.addField(indexField, year);
										document.addField(indexField + "_filter", year);
										document.addField(indexField + "_dt", date);

									}
									else
									{

										log.info("Date parse failed for :"+value+"");
										// Catch some special values
										if(value != null && value.equals("Unknown"))
										{
											document.addField(indexField, "Unknown");
											document.addField(indexField + "_keyword", "Unknown");
											document.addField(indexField + "_filter", "Unknown");
                                        	document.addField(indexField + "_year_sort", 0);
										}
										else if(value != null && value.equals("BCE"))
										{
											document.addField(indexField, "BCE");
											document.addField(indexField + "_keyword", "BCE");
											document.addField(indexField + "_filter", "BCE");
                                        	document.addField(indexField + "_year_sort", 0);
										}

									}

                              }

                            if (filter
                                    .getType()
                                    .equals(DiscoveryConfigurationParameters.TYPE_ISO_LANG))
                            {
                    		log.info("TYPE_ISO_LANG again! sureley we get duplicates from this code line 220");

                                String langName = IsoLangCodes
                                        .getLangForCode(value);
                                if (langName != null)
                                {
                                    document.addField(
                                            filter.getIndexFieldName(),
                                            langName);
                                    document.addField(
                                            filter.getIndexFieldName()
                                                    + "_keyword", langName);
                                    document.addField(
                                            filter.getIndexFieldName() + "_ac",
                                            langName);
                                    //this should ensure it's copied into the default search field
                                    document.addField(
                                            "dc.language.name",
                                            langName);
                                }
                                else
                                {
                                    log.error(String
                                            .format("No language found for iso code %s",
                                                    value));
                                }
                            }


                    
                        } //loop filters
                    }
                }
            }
            catch (SQLException e)
            {
                log.error(e.getMessage());
            }
            //process item metadata
            //just add _comp to local*
            Metadatum[] mds = item.getMetadata("local", Item.ANY, Item.ANY, Item.ANY);
            for(Metadatum meta : mds){
            	String field = meta.schema + "." + meta.element;
                String value = meta.value;
                if (value == null) {
                    continue;
                }
                if (meta.qualifier != null && !meta.qualifier.trim().equals("")) {
                    field += "." + meta.qualifier;
                }
            	document.addField(field + "_comp", value);
            }
        }
    }
}
