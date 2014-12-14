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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.darkstar.batch.utils.DarkstarUtils;

/**
 * Class to Find / Replace Updated Ids Across All Lua Scripts
 */
public class LuaScriptNpcIdUpdate {
	
	private static final Logger LOG = Logger.getLogger(LuaScriptNpcIdUpdate.class);
	private int errors = 0;
	
	/**
	 * LuaScriptIdUpdate - Batch Entry Point
	 */
	public static void main(String[] args) {
		try {
			final LuaScriptNpcIdUpdate luaScriptIdUpdate = new LuaScriptNpcIdUpdate();
			luaScriptIdUpdate.updateLuaScriptIds();
		}
		catch(final Exception e){
			LOG.error("An Unhandled Exception Occurred", e);
		}
	}
	
	private void updateLuaScriptIds(){
		final Properties configProperties = DarkstarUtils.loadBatchConfiguration();
		final Properties npcIdShiftProperties = DarkstarUtils.loadShiftProperties(configProperties);
		final String darkStarRoot = configProperties.getProperty("darkstar_root","");
		final String scriptsRoot = String.format("%s/%s", darkStarRoot, "scripts");
		final File scriptsDirectory = new File(scriptsRoot);
		final String[] extensions = new String[1];
		extensions[0] = "lua";
				
		if(!scriptsDirectory.exists() || !scriptsDirectory.isDirectory()){
			throw new RuntimeException(String.format("Cannot Find Scripts Directory! <%s>", scriptsRoot));
		}
		
		LOG.info("Preparing Shift Properties...");
		
		final Set<String> shiftKeysSet = npcIdShiftProperties.stringPropertyNames();
		final String[] shiftKeysArray = new String[shiftKeysSet.size()];
		shiftKeysSet.toArray(shiftKeysArray);
		final List<String> shiftKeys = Arrays.asList(shiftKeysArray);
		Collections.sort(shiftKeys, Collections.reverseOrder());
		
		if(shiftKeys.isEmpty()){
			throw new RuntimeException("Error: Empty Shift Properties Detected!");
		}
		
		LOG.info(String.format("Searching: %s", scriptsRoot));
				
		final Iterator<File> luaFiles = FileUtils.iterateFiles(scriptsDirectory, extensions, true);
				
		while(luaFiles.hasNext()){
			final File luaFile = luaFiles.next();
			updateFile(luaFile, shiftKeys, npcIdShiftProperties);
		}
		
		final StringBuilder elevatorSqlPathBuilder = new StringBuilder();
		elevatorSqlPathBuilder.append(configProperties.getProperty("darkstar_root",""));
		elevatorSqlPathBuilder.append("sql/elevators.sql");
		
		final File elevatorFile = new File(elevatorSqlPathBuilder.toString());
		updateFile(elevatorFile, shiftKeys, npcIdShiftProperties);
		
		final StringBuilder transportSqlPathBuilder = new StringBuilder();
		transportSqlPathBuilder.append(configProperties.getProperty("darkstar_root",""));
		transportSqlPathBuilder.append("sql/transport.sql");
		
		final File transportFile = new File(transportSqlPathBuilder.toString());
		updateFile(transportFile, shiftKeys, npcIdShiftProperties);
		
		LOG.info(String.format("Finished Updating Lua Scripts With <%d> Errors!", errors));
	}
	
	private void updateFile(final File luaFile, final List<String> shiftKeys, final Properties npcIdShiftProperties){
		LOG.info(String.format("Updating <%s>", luaFile.getAbsolutePath()));
		
		try {
			String luaFileContents = FileUtils.readFileToString(luaFile);
			
			for(final String shiftKey : shiftKeys){
				luaFileContents = luaFileContents.replaceAll(String.format("(%s)", shiftKey), 
						npcIdShiftProperties.getProperty(shiftKey));
			}
			
			FileUtils.writeStringToFile(luaFile, luaFileContents, false);
		} 
		catch (final IOException e) {
			errors++;
			LOG.error(String.format("Failed to Update <%s>", luaFile.getAbsolutePath()), e);				
		}
	}
}