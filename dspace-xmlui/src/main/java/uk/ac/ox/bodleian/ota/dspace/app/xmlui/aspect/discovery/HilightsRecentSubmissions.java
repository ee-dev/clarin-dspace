/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package uk.ac.ox.bodleian.ota.dspace.app.xmlui.aspect.discovery;

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.app.xmlui.aspect.discovery.AbstractRecentSubmissionTransformer;

import org.dspace.core.ConfigurationManager;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.content.Collection;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Renders a list of recently submitted items to the highlights collection
 * Based on org.dspace.app.xmlui.aspect.discovery.SiteRecentSubmissions
 * org.dspace.app.xmlui.aspect.discovery.CollectionRecentSubmissions
 
 * @author Mark Rogerson (mark dot rogerson at bodleian dot ox dot ac to uk
 */
public class HilightsRecentSubmissions extends AbstractRecentSubmissionTransformer {

    private static final Message T_head_highlights =
            message("xmlui.homepage.highlights");

	public static final String highlightsHandle = ConfigurationManager.getProperty("ota","ota.xmlui.homepage.highlightsCollection");

    /**
     * Display a single community (and reference any subcommunites or
     * collections)
     */
    public void addBody(Body body) throws SAXException, WingException,
            SQLException, IOException, AuthorizeException {

   			context = new Context();
			// This is the handle of the colection we want to on homepage
			DSpaceObject dso = HandleManager.resolveToObject(context, highlightsHandle);

        // Set up the major variables
        Collection collection = (Collection) dso;
        if(!(dso instanceof Collection))
        {
            return;
        }

        getRecentlySubmittedItems(collection);

        //Only attempt to render our result if we have one.
        if (queryResults == null)  {
            return;
        }

        if (0 < queryResults.getDspaceObjects().size()) {
            Division home = body.addDivision("site-home", "primary repository");

            Division lastSubmittedDiv = home
                    .addDivision("site-recent-submission", "secondary recent-submission");

            lastSubmittedDiv.setHead(T_head_highlights);

            ReferenceSet lastSubmitted = lastSubmittedDiv.addReferenceSet(
                    "site-last-submitted", ReferenceSet.TYPE_SUMMARY_LIST,
                    null, "recent-submissions");

            for (DSpaceObject resultObj : queryResults.getDspaceObjects()) {
                if(resultObj != null){
                    lastSubmitted.addReference(resultObj);
                }
            }
          //  addViewMoreLink(lastSubmittedDiv, null);
        }

    }
}
