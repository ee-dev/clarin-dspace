/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

/**
 * Class contains parameters used for the configuration of discovery
 *
 * based on class by Kevin Van de Velde (kevin at atmire dot com)
 * modified for LINDAT/CLARIN
 */
public class DiscoveryConfigurationParameters {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_DATE = "date";
    public static final String TYPE_HIERARCHICAL = "hierarchical";
    public static final String TYPE_AC = "ac";
    public static final String TYPE_AUTHORITY = "authority";
    public static final String TYPE_STANDARD = "standard";
    public static final String TYPE_RAW = "raw_values";
    public static final String TYPE_ISO_LANG = "iso_language";
    public static final String TYPE_OTA_IDENTIFIER = "ota_identifier";
    public static final String TYPE_STC_IDENTIFIER = "stc_identifier";

    public static enum SORT {VALUE, COUNT}


}
