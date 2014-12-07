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
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.darkstar.batch.utils.DarkstarUtils;

/**
 * Batch Class Update Npc Updates for Darkstar after a Version Update
 * 
 * This Job requires both a POLUtils Dump and a Npc Id Mapping File for NPCs
 * where the naming between Darkstar SQL & POLUtils is different.
 * 
 * For these npcs, accuracy will depend on accuracy of the mapping file. If the mapping
 * file is generated right before the version update, it should keep things from getting worse,
 * but may not auto-fix everything if mapped entries are bad.
 * 
 * After such cases are manually fixed, the mapping file should be regenerated, and they should
 * then be auto-fixable in the future.
 * 
 */
public class NpcIdUpdate {

	private static final Logger LOG = Logger.getLogger(NpcIdUpdate.class);
		
	private Properties configProperties;
	private Properties npcIdShiftProperties;
	private int errors = 0;
	private int guesses = 0;	
	private int scanThreshold = 0;
	private boolean markGuesses;

	/**
	 * NpcIdUpdate - Batch Entry Point
	 */
	public static void main(String[] args) {
		final NpcIdUpdate npcIdUpdate = new NpcIdUpdate();
		npcIdUpdate.updateNpcIds();
	}

	/**
	 * Main Loop for Generating the Mapping File
	 */
	private void updateNpcIds(){	
		configProperties = DarkstarUtils.loadBatchConfiguration();
		markGuesses = Boolean.valueOf(configProperties.getProperty("npcIdMarkGuesses","false"));
		npcIdShiftProperties = new Properties();
		
		final int minZone = Integer.valueOf(configProperties.getProperty("minZoneId", "0"));
		final int maxZone = Integer.valueOf(configProperties.getProperty("maxZoneId", "255"));
		scanThreshold = Integer.valueOf(configProperties.getProperty("scanThreshold", "5"));

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
		DarkstarUtils.saveNpcIdShiftProperties(configProperties, npcIdShiftProperties);
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
		String polUtilsMobListString;

		try {
			polUtilsMobListString = FileUtils.readFileToString(polUtilsMobListFile, "UTF-8");
		} 
		catch (final IOException e) {
			LOG.error(String.format("Error Reading Mob List for Zone <%s>, Skipping...", zoneNameComment), e);
			errors++;
			return;
		}

		polUtilsMobListString = DarkstarUtils.collapseAndFormatXmlString(polUtilsMobListString);
		
		final Set<Integer> alreadyUsedIds = new HashSet<>();
		int highestUsedId = 0;
		int shiftTrend = 0;
		
		boolean atLeastOneFound = false;
		
		// Loop Through Npcs		
		for(int lineIndex = zoneStartingLine; lineIndex < npcListSqlLines.size(); lineIndex++) {
			try{
				String npcListSqlLine = npcListSqlLines.get(lineIndex);

				LOG.debug(String.format("Processing Line: %s", npcListSqlLine));

				if(npcListSqlLine.indexOf("-- -")>=0){
					LOG.info(String.format("Finished <%s>: Zone Ended.",zoneNameComment));
					break;
				}
				else if("".equals(npcListSqlLine.trim()) || npcListSqlLine.startsWith("--")){
					continue;
				}
				
				final int npcId = DarkstarUtils.findCurrentNpcIdInNpcList(npcListSqlLine);
				final String npcListPolUtilsName = DarkstarUtils.findCurrentNpcNameInNpcList(npcListSqlLine);
				
				String polUtilsName = null;
				
				int idToScan = npcId - 1;
				int idScanThreshold = 0;
								
				boolean found = false;
				
				if(atLeastOneFound && shiftTrend == 0){
					polUtilsName = DarkstarUtils.findPolUtilsNpcNameById(polUtilsMobListString, npcId);
					LOG.debug(String.format("Checking Current: %d (%s) -> %d (%s)", npcId, npcListPolUtilsName, npcId, polUtilsName));
					
					if(npcListPolUtilsName.equals(polUtilsName) && !alreadyUsedIds.contains(npcId)){
						found = true;
						break;
					}
				}
				
				for(idScanThreshold = 0; !found && !npcListPolUtilsName.equals("") && idScanThreshold < (scanThreshold-1); idScanThreshold++, idToScan--){
					polUtilsName = DarkstarUtils.findPolUtilsNpcNameById(polUtilsMobListString, idToScan);
					LOG.debug(String.format("Scanning: %d (%s) -> %d (%s)", npcId, npcListPolUtilsName, idToScan, polUtilsName));
					
					if(npcListPolUtilsName.equals(polUtilsName) && !alreadyUsedIds.contains(idToScan)){
						found = true;
						atLeastOneFound = true;
						break;
					}					
				}
				
				if(!found || npcListPolUtilsName.equals("")){
					idToScan = npcId;
				}
				
				for(idScanThreshold = 0; !npcListPolUtilsName.equals("") && !found && idScanThreshold < scanThreshold; idScanThreshold++, idToScan++){
					polUtilsName = DarkstarUtils.findPolUtilsNpcNameById(polUtilsMobListString, idToScan);
					LOG.debug(String.format("Scanning: %d (%s) -> %d (%s)", npcId, npcListPolUtilsName, idToScan, polUtilsName));
					
					if(npcListPolUtilsName.equals(polUtilsName) && !alreadyUsedIds.contains(idToScan)){
						found = true;
						atLeastOneFound = true;
						break;
					}
				}
				
				
				if(!found){
					guesses++;
					idToScan = npcId+shiftTrend;
					DarkstarUtils.replaceNpcId(npcListSqlLines, lineIndex, idToScan, (!found && markGuesses));
					LOG.info(String.format("Guessing By Trend: %d (%s) -> %d (%s) (Trend = %d)", npcId, npcListPolUtilsName, idToScan, polUtilsName, shiftTrend));
				}
				else if(idToScan == npcId){
					LOG.info(String.format("Already Matches: %d (%s) <-> %d (%s)", npcId, npcListPolUtilsName, idToScan, polUtilsName));
				}
				else {
					LOG.info(String.format("Shifting: %d (%s) -> %d (%s)", npcId, npcListPolUtilsName, idToScan, polUtilsName));
					npcIdShiftProperties.setProperty(String.valueOf(npcId),String.valueOf(idToScan));
					DarkstarUtils.replaceNpcId(npcListSqlLines, lineIndex, idToScan, (!found && markGuesses));					
				}
				
				alreadyUsedIds.add(idToScan);
				highestUsedId = idToScan;
				shiftTrend = idToScan - npcId;
				
				LOG.debug(String.format("Trend At: %d (%d - %d)", shiftTrend, idToScan, npcId));
			}
			catch(Exception e){
				LOG.error(String.format("Exception Thrown While Processing Npc Ids In Zone <%s>", zoneNameComment), e);
				errors++;
			}
		}
	}
}
