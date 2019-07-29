/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package uk.ac.ox.bodleian.ota.dspace.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.*;
import javax.inject.Inject;
import org.dspace.kernel.DSpaceKernel;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to tidy and parse dates for ota.
 * Separate class so we can process browse and discovery in exactly the same way
 *
 * Dates are parsed as being in the UTC zone.
 *
 * @author mrogerson
 */
public class OTADate
{
    private static final Logger log = LoggerFactory.getLogger(OTADate.class);

    private static final TimeZone UTC_ZONE = TimeZone.getTimeZone("UTC");

    /** Format for displaying a result of testing. */
    private static final DateFormat formatter;
    static
    {
        formatter = SimpleDateFormat.getDateTimeInstance();
        formatter.setTimeZone(UTC_ZONE);
    }

    static public String clean(String dateString)
	{
	
		log.info("OTADate "+dateString);

		if(dateString==null)
		{
			return null;
		}

		if(dateString.contains("BCE"))
		{		
			dateString="BCE";
			return dateString;
		}

		if(dateString.matches("^\\[.*\\]$"))
		{
		// Strip enclosing []
			dateString = dateString.substring(1, dateString.length()-1);
		}

		if(dateString.matches("^.*[–-]$"))
		{
		// Strip trailing - e.g. "1966-"
			dateString = dateString.substring(0, dateString.length()-1);
		}

		switch (dateString.toLowerCase()) {
		case "Unspecified":
		case "Date unknown":
		case "s.d.":
		case "n.d":
		case "s.d":
		case "s.l":
		case "unknown":
			dateString="Unknown";
			return dateString;

		}

		if(dateString.matches("^\\d{4}[–-][01]\\d([–-][0123]\\d)?$"))
		{
		// real date yyyy-mm or yyy-mm-dd
			return dateString;
		}

		// Not a real date (see above) must be a range so just take upto "-"
		Pattern p = Pattern.compile("^(\\d+)[–-].*$", Pattern.CASE_INSENSITIVE);  
        Matcher matcher = p.matcher(dateString); 
		if(matcher.find())
		{
			dateString=matcher.group(1);
			return dateString;
		}

		
		return dateString;
	}

    static public String sort(String dateString)
	{
		if(dateString==null)
		{
			return null;
		}
		String sortDateString = dateString;
		if(dateString.contains("BCE"))
		{		
			sortDateString="-BCE";
			return sortDateString;
		}

		switch(dateString.length())
		{
			case 3:
			sortDateString = "0"+dateString;
			break;
			case 2:
			sortDateString = "00"+dateString;
			break;
			case 1:
			sortDateString = "000"+dateString;
			break;
			default:
			sortDateString = dateString;
		}
		return sortDateString;
	}
 }
