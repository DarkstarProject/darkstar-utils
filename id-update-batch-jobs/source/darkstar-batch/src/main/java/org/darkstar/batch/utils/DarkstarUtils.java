package org.darkstar.batch.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/** 
 * Copyright (c) 2010-2014 Darkstar Dev Teams
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 * 
 * This file is part of DarkStar-server source code.
 */

/**
 * Utility Methods for the Darkstar Batch Processes
 */
public class DarkstarUtils {
	private static List<String> badCharacterDataLines;
	
	public static final String FIELD_CLOSING_TAG = "</field>";
	public static final String FIELD_ID_OPENING_TAG = "<field name=\"id\">";
	public static final String FIELD_INDEX_OPENING_TAG = "<field name=\"index\">";
	public static final String FIELD_NAME_OPENING_TAG = "<field name=\"name\">";
	public static final String FIX_ME = "FIXME: ";
	public static final Logger LOG = Logger.getLogger(DarkstarUtils.class);
	
	public static final String NPC_LIST_INSERT_START = "INSERT INTO `npc_list` VALUES (";
		
	/**
	 * Method to Collapse a String into one line, and Format It By Removing Escaped Characters
	 * @param xmlString String to Collapse & Format
	 * @return
	 */
	public static String collapseAndFormatXmlString(final String xmlString){
		String newString = xmlString;
		newString = newString.replaceAll("("+System.getProperty("line.separator")+")", " ");
		newString = StringEscapeUtils.unescapeXml(newString);
		return newString;
	}
	
	/**
	 * Method to PolUtils Npc Id to Shortened Darkstar Sql Npc Id
	 * @param polUtilsNpcId Long POLUtils NPC Id
	 * @return Short Darkstar SQL Npc Id
	 */
	public static int convertPolUtilsNpcIdToDarkstar(final int polUtilsNpcId){
		final String hexString = Integer.toHexString(polUtilsNpcId);
		final String shortHexString = hexString.substring(hexString.length()-3, hexString.length());
		final int npcId = Integer.parseInt(shortHexString, 16);
		LOG.debug(String.format("convertPolUtilsNpcIdToDarkstar: %d -> %s -> %s -> %d", polUtilsNpcId, hexString, shortHexString, npcId));
		return npcId; 
	}
	
	/**
	 * Method to Filter Out Known Bad Character Patterns
	 * @param text Text to Filter
	 * @return Filtered Text
	 */
	public static String filterBadCharacters(final String text){
		loadBadCharacterData();
		
		String newText = text;
		
		for(final String regex : badCharacterDataLines){
			LOG.debug(String.format("Filtering Bad Character Pattern: %s", regex));
			newText = newText.replaceAll(regex,"");
		}
		
		return newText;
	}
	
	/**
	 * Method to Find the Current Npc Id in the Npc List
	 * @param npcListSqlLine Line in the Npc List File
	 * @return Short Darkstar Npc Id of the Npc on this Line
	 */
	public static int findCurrentNpcIdInNpcList(final String npcListSqlLine){		
		final int insertIndex = npcListSqlLine.indexOf(NPC_LIST_INSERT_START);
		
		if(insertIndex == -1 || npcListSqlLine.startsWith("--")){
			return -1;
		}
		
		final int endIndex = npcListSqlLine.indexOf(",", insertIndex);
		String npcIdString = npcListSqlLine.substring(NPC_LIST_INSERT_START.length(), endIndex);
		
		if(npcIdString.startsWith("'")){
			npcIdString = npcIdString.substring(1,npcIdString.length()-1);
			
		}
		return Integer.valueOf(npcIdString);
	}
	
	/**
	 * Method to Find the Name of the Npc on the Current Line
	 * @param npcListSqlLine Current Line of the Npc List SQL
	 * @return Name of the NPC as Listed in the SQL
	 */
	public static String findCurrentNpcNameInNpcList(final String npcListSqlLine){
		final int insertIndex = npcListSqlLine.indexOf(NPC_LIST_INSERT_START);
		
		if(insertIndex == -1 || npcListSqlLine.startsWith("--")){
			return null;
		}
		
		int endIndex = npcListSqlLine.indexOf(",", insertIndex);
		endIndex = npcListSqlLine.indexOf(",", endIndex+1);
		final int npcNameStartIndex = npcListSqlLine.indexOf("'",endIndex);
		int npcNameEndIndex = npcListSqlLine.indexOf("'",npcNameStartIndex+1);
		
		if(npcListSqlLine.charAt(npcNameEndIndex-1) == '\\'){
			npcNameEndIndex = npcListSqlLine.indexOf("'",npcNameEndIndex+1);
		}
		
		final String name = npcListSqlLine.substring(npcNameStartIndex+1, npcNameEndIndex);
		return name.replaceAll("\\\\\'","'");
	}
	
	/**
	 * Method to Find the Index of a Specified ID In the POLUtils Moblist
	 * @param polUtilsMobListString String Representation of POLUtils Mob List
	 * @param polUtilsNpcId POLUtils NPC ID
	 * @return Index
	 */
	public static int findPolUtilsIndexById(final String polUtilsMobListString, final int polUtilsNpcId){
		return polUtilsMobListString.indexOf(String.valueOf(polUtilsNpcId));
	}

	/**
	 * Method to Find a POLUtils NPC's Name By Id (Starting @ Beginning of File)
	 * @param polUtilsMobListString String Representation of the Mob List File
	 * @param npcId Npc Id to Find
	 * @return Name of the Npc In POLUtils
	 */
	public static String findPolUtilsNpcNameById(final String polUtilsMobListString, final int npcId){
		return findPolUtilsNpcNameById(polUtilsMobListString, npcId, 0);
	}
	
	/**
	 * Method to Find a POLUtils NPC's Name By Id (Starting at Specified Index)
	 * @param polUtilsMobListString String Representation of the Mob List File
	 * @param npcId Npc Id to Find
	 * @param lastIndex Starting Index
	 * @param searchBackwards TRUE if searching backwards, FALSE otherwise.
	 * @return Name of the Npc In POLUtils
	 */
	public static String findPolUtilsNpcNameById(final String polUtilsMobListString, final int npcId, final int lastIndex){
		final int nameIndex = polUtilsMobListString.indexOf(String.valueOf(npcId), lastIndex);
		
		if(nameIndex==-1){
			return "";
		}
		
		final int nameStartIndex = polUtilsMobListString.indexOf(FIELD_NAME_OPENING_TAG, nameIndex);
		final int nameEndIndex = polUtilsMobListString.indexOf(FIELD_CLOSING_TAG, nameStartIndex);
		final String polUtilsNpcName = polUtilsMobListString.substring(nameStartIndex+FIELD_NAME_OPENING_TAG.length(), nameEndIndex);
				
		LOG.debug(String.format("findPolUtilsNpcNameById: %d -> %s", npcId, polUtilsNpcName));
		return polUtilsNpcName;
	}

	/**
	 * Scans a String Representation of <npc_list.sql> for the beginning of a specific zone's data
	 * @param zoneName Zone Name
	 * @param npcListSql String Representation of the SQL File
	 * @return Starting Index
	 */
	public static int findZoneInNpcListSql(final String zoneName, final List<String> npcListSqlLines){		
		int zoneLine = -1;
			
		for(int lineIndex = 0; lineIndex < npcListSqlLines.size(); lineIndex++){
			final String npcListSqlLine = npcListSqlLines.get(lineIndex);
			
			int zoneIndex = npcListSqlLine.indexOf("-- "+zoneName);
		
			// Don't Mistake a WoTG Zone for the Regular Type
			if(npcListSqlLine.indexOf("-- "+zoneName+" [S]")==zoneIndex && zoneName.indexOf("[S]")==-1){
				zoneIndex = npcListSqlLine.indexOf("-- "+zoneName, zoneIndex+("-- "+zoneName+" [S]").length());
			}
			
			if(zoneIndex>=0){
				zoneLine = lineIndex + 2; // +2 gets us past the closing comment
				break;
			}
		}
		
		return zoneLine;
	}
	
	/**
	 * Method to Get the Path to the Dialog Table File By Zone ID
	 * @param configProperties Handle to Batch Configuration Properties
	 * @param zoneId Zone ID
	 * @return Path to the Mob List File
	 */
	public static String getDialogTablePathByZoneId(final Properties configProperties, final int zoneId){
		final StringBuilder polUtilsMobListPath = new StringBuilder();
		polUtilsMobListPath.append(configProperties.getProperty("polutils_dump","./polutils_dump/"));
		polUtilsMobListPath.append("dialog-table-");
		polUtilsMobListPath.append(String.format("%03d", zoneId));
		polUtilsMobListPath.append(".xml");		
		return polUtilsMobListPath.toString();
	}
	
	/**
	 * Method to get the Mapping File Key
	 * @param zoneId Zone ID
	 * @param npcName Npc Name
	 * @return Mapping File Key
	 */
	public static String getMappingKey(final Map<String,Integer> keyCountMap, 
			final int zoneId, final String npcName){
		final StringBuilder mappingKeyBuilder = new StringBuilder();
		mappingKeyBuilder.append(String.format("%03d",zoneId));
		mappingKeyBuilder.append('|');
		mappingKeyBuilder.append(npcName);
		
		final String keyWithoutCount = mappingKeyBuilder.toString();
		Integer keyCount = keyCountMap.get(keyWithoutCount);
		
		if(keyCount==null){
			keyCount = 1;
		}
		else {
			keyCount++;
		}
		
		mappingKeyBuilder.append('|');
		mappingKeyBuilder.append(keyCount);
		
		keyCountMap.put(keyWithoutCount, keyCount);
				
		final String mappingKey = mappingKeyBuilder.toString();
		
		LOG.debug(String.format("getMappingKey: %s", mappingKey));
		
		return mappingKey;
	}
	
	/**
	 * Method to Get the Path to the Mob List File By Zone ID
	 * @param configProperties Handle to Batch Configuration Properties
	 * @param zoneId Zone ID
	 * @return Path to the Mob List File
	 */
	public static String getMobListPathByZoneId(final Properties configProperties, final int zoneId){
		final StringBuilder polUtilsMobListPath = new StringBuilder();
		polUtilsMobListPath.append(configProperties.getProperty("polutils_dump","./polutils_dump/"));
		polUtilsMobListPath.append("mob-list-");
		polUtilsMobListPath.append(String.format("%03d", zoneId));
		polUtilsMobListPath.append(".xml");		
		return polUtilsMobListPath.toString();
	}
	
	/**
	 * Method to Get Read the NPC List SQL
	 * @param configProperties Handle to Batch Configuration Properties
	 * @return List of Strings Representing Each Line of the File
	 */
	public static List<String> getNpcListFileLines(final Properties configProperties){
		final StringBuilder npcListSqlPathBuilder = new StringBuilder();
		npcListSqlPathBuilder.append(configProperties.getProperty("darkstar_root",""));
		npcListSqlPathBuilder.append("sql/npc_list.sql");
		
		final File npcIdFile = new File(npcListSqlPathBuilder.toString());
		
		List<String> npcListSqlLines = null;
		
		try {
			npcListSqlLines = FileUtils.readLines(npcIdFile);
		} 
		catch (final IOException e) {
			throw new RuntimeException("Error Reading Npc List SQL!",e);
		}
		
		return npcListSqlLines;
	}
	
	/**
	 * Method to get the Zone Comment as Mapped to the NPC List SQL File
	 * @param configProperties Handle to Batch Configuration Properties
	 * @param zoneId Zone ID
	 * @return Zone Comment
	 */
	public static String getNpcListSqlZoneCommentByZoneId(final Properties configProperties, final int zoneId){
		final StringBuilder polUtilsMobListPath = new StringBuilder();
		polUtilsMobListPath.append("mob-list-");
		polUtilsMobListPath.append(String.format("%03d", zoneId));
		polUtilsMobListPath.append(".xml");
		return configProperties.getProperty(polUtilsMobListPath.toString());
	}
	
	/**
	 * Method to Get the Text Id File Path for a Given Zone
	 * @param configProperties Handle to the Batch Configuration Properties
	 * @param zoneId Zone ID
	 * @return Path to Text ID File
	 */
	public static String getTextIdFilePath(final Properties configProperties, final int zoneId){
		final StringBuilder textIdPath = new StringBuilder();
		textIdPath.append(getZonePath(configProperties, zoneId));
		textIdPath.append("IDs.lua");
		return textIdPath.toString();
	}
	
	/**
	 * Method to Get the Zone Path
	 * @param configProperties Handle to the Batch Configuration Properties
	 * @param zoneId Zone ID
	 * @return Path to the Zone's Root
	 */
	public static String getZonePath(final Properties configProperties, final int zoneId){
		final String darkstarRoot = configProperties.getProperty("darkstar_root","../../");
		final String zoneIdString = String.format("%03d", zoneId);
		final String relativeZonePath = configProperties.getProperty(zoneIdString);
		return (darkstarRoot + relativeZonePath);
	}
	
	/**
	 * Checks if a Zone is Mapped in the Batch Config Properties
	 * @param configProperties Handle to the Batch Config Properties
	 * @param zoneId Zone ID
	 * @return TRUE if mapped, FALSE otherwise.
	 */
	public static boolean isZoneMapped(final Properties configProperties, final int zoneId){
		boolean isMapped = false;
		
		final String zoneIdString = String.format("%03d", zoneId);
		
		if(configProperties.containsKey(zoneIdString)){
			isMapped = true;
		}
		
		return isMapped;
	}
	
	/**
	 * Method to Load the Bad Character Data Resource
	 */
	private static void loadBadCharacterData(){
		synchronized(DarkstarUtils.class){
			if(badCharacterDataLines==null){
				final InputStream configStream = DarkstarUtils.class.getResourceAsStream("/badcharacters.dat");
				try {
					badCharacterDataLines = IOUtils.readLines(configStream);
				} 
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				IOUtils.closeQuietly(configStream);
			}
		}
	}
	
	/**
	 * Method to Load the Batch Configuration Properties, Creating a Config From the Embedded Default If Necessary
	 * @return Configuration Properties
	 */
	public static Properties loadBatchConfiguration(){
		Properties config = new Properties();
		
		try {
			final File configFile = new File("config.properties");
		
			if(!configFile.exists()){
				final InputStream configStream = DarkstarUtils.class.getResourceAsStream("/defaultconfig.properties");
				final byte[] configBytes = new byte[configStream.available()];
				IOUtils.readFully(configStream, configBytes);
				IOUtils.closeQuietly(configStream);
				FileUtils.writeByteArrayToFile(configFile, configBytes);
			}
			
			final FileInputStream configStream = new FileInputStream(configFile);
			config.load(configStream);
			IOUtils.closeQuietly(configStream);
			
		}
		catch(final Exception e){
			throw new RuntimeException("Error Reading Config File!",e);
		}
		
		return config;
	}	
			
	/**
	 * Method to Load the Npc Id Mapping Properties
	 * @return Mapping Properties
	 */
	public static Properties loadMappingProperties(final Properties configProperties){
		final Properties mappingProperties = new Properties() {
			private static final long serialVersionUID = 665397466355935210L;

		    @Override
		    public synchronized Enumeration<Object> keys() {
		        return Collections.enumeration(new TreeSet<Object>(super.keySet()));
		    }

		    @Override
		    public Set<Object> keySet(){
		        return Collections.unmodifiableSet(new TreeSet<Object>(super.keySet()));
		    }
		};
		
		try {
			final File mappingFile = new File(configProperties.getProperty("mapping_file","./npcid-mapping.properties"));
			if(mappingFile.exists()){
				final FileInputStream mappingStream = new FileInputStream(mappingFile);
				mappingProperties.load(mappingStream);
				IOUtils.closeQuietly(mappingStream);
			}
		}
		catch(final Exception e){
			throw new RuntimeException("Error Reading Mapping File!",e);
		}
		
		return mappingProperties;
	}

	/**
	 * Method to Load the Npc Id Shift Properties
	 * @return Shift Properties
	 */
	public static Properties loadShiftProperties(final Properties configProperties){
		final Properties shiftProperties = new Properties();
		
		try {
			final File shiftFile = new File(configProperties.getProperty("shift_file","npcid-shift.properties"));
			if(shiftFile.exists()){
				final FileInputStream mappingStream = new FileInputStream(shiftFile);
				shiftProperties.load(mappingStream);
				IOUtils.closeQuietly(mappingStream);				
			}
			
			// Shift Properties are 1 time use.
			shiftFile.deleteOnExit();
		}
		catch(final Exception e){
			throw new RuntimeException("Error Reading Mapping File!",e);
		}
		
		return shiftProperties;
	}	
	
	/**
	 * Method to Reconstruct the Long Npc Id as Used by POLUtils
	 * @param hexPrefix Hex Prefix for this File
	 * @param sqlNpcId Short NPC ID From Darkstar SQL
	 * @return Long POLUtils NPC ID
	 */
	public static int reconstructPolUtilsNpcId(final String hexPrefix, final int sqlNpcId){
		String polUtilsHexId = Integer.toHexString(sqlNpcId);
		
		while(polUtilsHexId.length()<3){
			polUtilsHexId = "0"+polUtilsHexId;
		}
		
		polUtilsHexId = hexPrefix + polUtilsHexId;
		
		final int polUtilsNpcId = Integer.parseInt(polUtilsHexId, 16);
		LOG.debug(String.format("reconstructPolUtilsNpcId: %d -> %s -> %d", sqlNpcId, polUtilsHexId, polUtilsNpcId));
		
		return polUtilsNpcId;
	}
	
	/**
	 * Method to Replace the NPC ID on the Current Line w/ a New One
	 * @param npcListSqlLines Line Array of the NPC List SQL
	 * @param lineIndex Index of Current Line
	 * @param newNpcId New Short NPC ID
	 */
	public static void replaceNpcId(final List<String> npcListSqlLines, final int lineIndex, final int newNpcId, final boolean markAsGuess){
		final String npcIdLine = npcListSqlLines.get(lineIndex);
		final int endIndex = npcIdLine.indexOf(",");
		final String insertStringFragment = npcIdLine.substring(0, NPC_LIST_INSERT_START.length());
		final String postIdFragment = npcIdLine.substring(endIndex, npcIdLine.length());
		String newNpcIdLine = insertStringFragment + newNpcId + postIdFragment;
		
		if(markAsGuess){
			newNpcIdLine = "FIXME: "+newNpcIdLine;
		}
		
		npcListSqlLines.set(lineIndex, newNpcIdLine);
	}
	
	public static void replacePolUtilsName(final List<String> npcListSqlLines, final int lineIndex, final String name){
		final String npcIdLine = npcListSqlLines.get(lineIndex);
		
		int startIndex = npcIdLine.indexOf(",");
		startIndex = npcIdLine.indexOf(",", startIndex+1);
		int nextQuote = npcIdLine.indexOf("'", startIndex+2);
		int endIndex = npcIdLine.indexOf(",", startIndex+1);
		
		if(endIndex < nextQuote){
			endIndex = npcIdLine.indexOf(",", endIndex+1);
		}
		
		final String newLine;
		
		if(name==null){
			newLine = npcIdLine.substring(0,startIndex+1) + "\'\'" + npcIdLine.substring(endIndex,npcIdLine.length());
		}
		else {
			newLine = npcIdLine.substring(0,startIndex+1) + '\'' + name.replaceAll("'", "\\\\'") + '\'' + npcIdLine.substring(endIndex,npcIdLine.length());
		}
		
		npcListSqlLines.set(lineIndex, newLine);
	}
	
	public static void removeZoneId(final List<String> npcListSqlLines, final int lineIndex){
		final String npcIdLine = npcListSqlLines.get(lineIndex);
		final int startIndex = npcIdLine.lastIndexOf(",");
		npcListSqlLines.set(lineIndex, npcIdLine.substring(0,startIndex)+");");
	}
	
	/**
	 * Method to Save the Mapping Properties
	 * @param configProperties Handle to Batch Configuration Properties
	 * @param mappingProperties Handle to Mapping Properties
	 */
	public static void saveMappingProperties(final Properties configProperties, final Properties mappingProperties){
		try {			
			final File mappingFile = new File(configProperties.getProperty("mapping_file","./npcid-mapping.properties"));
			final FileOutputStream mappingStream = new FileOutputStream(mappingFile);
			mappingProperties.store(mappingStream, "Darkstar<->POLUtils Npc Id Mappings");
			IOUtils.closeQuietly(mappingStream);
		}
		catch(final Exception e){
			throw new RuntimeException("Error Reading Mapping File!",e);
		}				
	}

	/**
	 * Method to Save the Npc Id Shift Properties
	 * @param configProperties Handle to Batch Configuration Properties
	 * @param npcIdShiftProperties Shift Properties
	 */
	public static void saveNpcIdShiftProperties(final Properties configProperties, final Properties npcIdShiftProperties){
		try {			
			final File shiftFile = new File(configProperties.getProperty("shift_file","npcid-shift.properties"));
			final FileOutputStream mappingStream = new FileOutputStream(shiftFile);
			npcIdShiftProperties.store(mappingStream, "Darkstar Shifted Npc Ids");
			IOUtils.closeQuietly(mappingStream);
		}
		catch(final Exception e){
			throw new RuntimeException("Error Reading Mapping File!",e);
		}				

	}
	
	/**
	 * Method to Save the NPC List SQL File
	 * @param configProperties Handle to Batch Configuration Properties
	 * @param npcListSqlLines Array of the NPC List SQL Lines to Save
	 */
	public static void saveNpcListSqlFile(final Properties configProperties, final List<String> npcListSqlLines){
		final StringBuilder npcListSqlPathBuilder = new StringBuilder();
		npcListSqlPathBuilder.append(configProperties.getProperty("darkstar_root",""));
		npcListSqlPathBuilder.append("sql/npc_list.sql");
		
		final File npcIdFile = new File(npcListSqlPathBuilder.toString());
		
		try {
			FileUtils.writeLines(npcIdFile, npcListSqlLines);
		} 
		catch (IOException e) {
			throw new RuntimeException("Failed to Write Npc List SQL File!",e);
		}
	}
	
	/**
	 * Method to Write a Copy of the Filtered Dialog Table (Post RegEx Stripping)
	 * @param configProperties Handle to Batch Configuration File
	 * @param filteredDialogTable Filtered Dialog Table String
	 * @param zoneId Zone ID
	 */
	public static void writeFilteredDialogTable(final Properties configProperties, final String filteredDialogTable, final int zoneId){
		final StringBuilder filteredDialogTablePathBuilder = new StringBuilder();
		filteredDialogTablePathBuilder.append(configProperties.getProperty("filtered_dialog_tables","./filtered_dialog_tables/"));
		filteredDialogTablePathBuilder.append("filtered-dialog-table-");
		filteredDialogTablePathBuilder.append(String.format("%03d", zoneId));
		filteredDialogTablePathBuilder.append(".xml");
		
		final File filteredDialogTableFile = new File(filteredDialogTablePathBuilder.toString());
		
		try {
			FileUtils.writeStringToFile(filteredDialogTableFile, filteredDialogTable);
		} 
		catch (IOException e) {
			throw new RuntimeException("Failed to Write Filtered Dialog Table File!",e);
		}		
	}
}
