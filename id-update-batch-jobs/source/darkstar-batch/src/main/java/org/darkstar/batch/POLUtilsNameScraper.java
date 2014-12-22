package org.darkstar.batch;

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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.darkstar.batch.utils.DarkstarUtils;

/**
 * Scrapes Latest POLUtils Names
 * 
 * Requires a POLUtils Dump
 * 
 * Requires the ids to be in sync already, or you'll get incorrect results.
 */
public class POLUtilsNameScraper {

	private static final Logger LOG = Logger.getLogger(POLUtilsNameScraper.class);

	private Properties configProperties;
	private int errors = 0;
	private int guesses = 0;	
	private boolean markGuesses;

	/**
	 * NpcIdUpdate - Batch Entry Point
	 */
	public static void main(String[] args) {
		final POLUtilsNameScraper polutilsNameScraper = new POLUtilsNameScraper();
		polutilsNameScraper.updatePOLUtilsNames();
	}

	/**
	 * Main Loop for Generating the Mapping File
	 */
	private void updatePOLUtilsNames(){	
		configProperties = DarkstarUtils.loadBatchConfiguration();
		markGuesses = Boolean.valueOf(configProperties.getProperty("npcIdMarkGuesses","false"));

		final int minZone = Integer.valueOf(configProperties.getProperty("minZoneId", "0"));
		final int maxZone = Integer.valueOf(configProperties.getProperty("maxZoneId", "255"));

		final List<String> npcListSqlLines = DarkstarUtils.getNpcListFileLines(configProperties);

		for (int zoneId = minZone; zoneId <= maxZone; zoneId++){
			final String polUtilsMobListFilePath = DarkstarUtils.getMobListPathByZoneId(configProperties, zoneId);
			final String zoneNameComment = DarkstarUtils.getNpcListSqlZoneCommentByZoneId(configProperties, zoneId);

			if(zoneNameComment==null){
				LOG.info(String.format("Zone ID <%d> Is Not Configured, Skipping...", zoneId));
			}
			else {
				LOG.info(String.format("Processing Zone ID <%d>...", zoneId));
				updateNpcIdsForZone(npcListSqlLines, polUtilsMobListFilePath, zoneId, zoneNameComment);
			}
		}

		LOG.info(String.format("A Total of <%d> Guesses Occurred.", guesses));

		if(markGuesses){
			LOG.info("Guesses are marked with \"FIXME:\" in the SQL. Please search for and fix these before committing.");	
		}

		LOG.info(String.format("A Total of <%d> Unhandled Errors Occurred.", errors));

		DarkstarUtils.saveNpcListSqlFile(configProperties, npcListSqlLines);
	}

	/**
	 * Logic to Update Npc Ids for a Single Zone
	 */
	private void updateNpcIdsForZone(final List<String> npcListSqlLines, final String polUtilsMobListFilePath, final int zoneId, final String zoneNameComment){
		// We find the zone by scanning for the Zone Comment in NPC_LIST Sql. The comment value mapping is configured in the batch properties
		final int zoneStartingLine = DarkstarUtils.findZoneInNpcListSql(zoneNameComment, npcListSqlLines);

		if(zoneStartingLine == -1){
			LOG.info(String.format("Unable to Find Zone Comment <%s> in Npc List SQL, Skipping...", zoneNameComment));
			return;
		}

		// Read in Relevant Pol Utils File to Match With
		final File polUtilsMobListFile = new File(polUtilsMobListFilePath);
		String polUtilsMobListString ="";

		try {
			polUtilsMobListString = FileUtils.readFileToString(polUtilsMobListFile, "UTF-8");
			polUtilsMobListString = DarkstarUtils.collapseAndFormatXmlString(polUtilsMobListString);
		} 
		catch (final IOException e) {
			LOG.error(String.format("Error Reading Mob List for Zone <%s>, Skipping...", zoneNameComment), e);
			errors++;
		}

		// Capture Hex Prefix for the Zone, Used in Matching Our Ids to POLUtils
		String npcNewValueHexString = Integer.toHexString((zoneId << 12) + 0x1000000);
		npcNewValueHexString = npcNewValueHexString.substring(0,npcNewValueHexString.length()-3);

		// Loop Through Npcs		
		for(int lineIndex = zoneStartingLine; lineIndex < npcListSqlLines.size(); lineIndex++) {
			try{
				String npcListSqlLine = npcListSqlLines.get(lineIndex);

				LOG.debug(String.format("Processing Line: %s", npcListSqlLine));

				if(npcListSqlLine.indexOf("-- -----")>=0){
					LOG.info(String.format("Finished <%s>: Zone Ended.",zoneNameComment));
					break;
				}
				else if("".equals(npcListSqlLine.trim())){
					continue;
				}

				if(npcListSqlLine.startsWith("--")){
					continue;
				}
				
				if(npcListSqlLine.indexOf(DarkstarUtils.NPC_LIST_INSERT_START)<0){
					continue;
				}

				final int npcId = DarkstarUtils.findCurrentNpcIdInNpcList(npcListSqlLine);
				String polUtilsName = DarkstarUtils.findPolUtilsNpcNameById(polUtilsMobListString, npcId);
				DarkstarUtils.replacePolUtilsName(npcListSqlLines, lineIndex, polUtilsName);
			}
			catch(Exception e){
				LOG.error(String.format("Exception Thrown While Processing Npc Ids In Zone <%s>", zoneNameComment), e);
				errors++;
			}
		}
	}
}

