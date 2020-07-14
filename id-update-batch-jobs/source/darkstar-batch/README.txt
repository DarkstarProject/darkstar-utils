==========================================
= Darkstar Version Update Batch (Beta)
==========================================

Table of Contents:

A) System Requirements
B) Building the Project
C) Preparing to Run the Batch
D) Batch Configuration File
E) POLUtils Name Scraper
F) Npc Id Update Batch
G) Text Id Update Batch
H) Lua Script NPC Id Update Batch
I) License Notices

==========================================
= A) System Requirements
==========================================

Build Requirements:
1) Apache Maven: http://maven.apache.org/download.cgi
2) Java 8 JDK: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
3) Optional: Eclipse Neon (Includes Maven) http://www.eclipse.org/neon/

Runtime Requirements:
1) Java 8 JRE: http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html
2) POLUtils/MassExtractor: https://github.com/Windower/POLUtils/releases

==========================================
= B) Building the Project
==========================================

The Darkstar Batch Jobs are written in Java, and are built with Apache Maven, an industry standard
build system for Java projects. In order to build the project, you will need both Apache Maven and
the Java 8 JDK installed on your machine.

Building from the Command Line:

1) cd to <Darkstar_Root>/batch/source/darkstar-batch/
2) Run "mvn package"
3) The binary jar will be built and placed in the "target" directory.

Building from Eclipse:

1) Do File -> Import
2) Select Maven -> Existing Maven Projects
3) Point Eclipse to "<Darkstar_Root>/batch/source/darkstar-batch/"
4) Import the Project
5) Right Click and say "Run as Maven Build"
6) Type "package" as your goal
7) Click "Run".
8) The binary jar will be placed in the "target" directory within the project.

==========================================
= C) Preparing to Run the Batch
==========================================

In order to run the batch jobs, you must run the POLUtils Mass Extractor, and
point the batches to a complete dump. This guide will assume you have POLUtils
installed in the default location, and FFXI on the computer you are running on.
See system requirements for a link to POLUtils.

1) Click the Start Button and in the search window, type "cmd" and press enter.
2) The command prompt should open.
3) cd\Program Files (x86)\POLUtils
4) Run: MassExtractor.exe "<Darkstar_Root>/batch/binaries/polutils_dump/"
Fill in <Darkstar_Root> with the root path of your Darkstar Clone.
5) Ignore any warnings from the output. You should have a bunch of XML files
in your polutils_dump folder references above. If you do, you are ready to run
the batch jobs

NOTE: You may or may not need to run MassExtractor.exe as admin if you are a limited
user with UAC enabled on your computer. I'm not sure.

==========================================
= D) Batch Configuration File
==========================================

The section refers to the file "config.properties", in the binaries folder:
"<Darkstar_Root>/batch/binaries/polutils_dump/config.properties"

polutils_dump - Sets the path to the POLUtils Dump. 
darkstar_root - Path to the Root of your Darkstar Git Clone
minZoneId - Zone ID to Start Executing at
maxZoneId - Zone ID to End Execution at
npcIdMarkGuesses - Mark FIXME: on Guesses in the NPC Id Job
scanThreshold - How many ids ahead to scan for npc id matches before deciding to guess
textIdMarkGuesses - Mark FIXME: on Guesses in the Text Id Job
writeFilteredDialogTables - Set to true to write reg-ex filtered copies of the dialog tables. 
                            Use this to see what the text id job sees if you're having trouble 
                            with a comment due to bad chars in POLUtils

Section: # Npc Ids - Mob List Mappings - Comment Out Zones You Don't Want to Run
Maps Mob List Files from POLUtils Dump to a Zone Name as Spelled in the Comments 
in the "npc_list.sql" file (THIS MUST MATCH). To make the batch aware of new zones,
add mappings here. To stop the NPC ID batches from running for specific zones, 
comment them out with a # sign here.

Section: #Zone ID Mappings
Maps Zone IDs to their folder in Darkstar. Used by the Text ID Update Batch. To
stop this batch from running for specific zones, comment them out with a # sign
here.

==========================================
= E) POLUtils Name Scraper
==========================================

Batch Job to Update the POLUtils Name Column in npc_list w/ the latest data
 
For this to work properly in its entirety, we must be in a known good state.
 
This should be used when new npcs are added. This can also expediate manual fixing
of npc ids if the npc job is having difficulties. If the scraped name is de-synced
with the name in our file (other than in the case of internal _xxx names), then
the id in our file is wrong. Use how far away the right name is (use a diff client)
to determine where the shift de-sync started.

1) Run GenerateNpcMappingFile.bat. This will overwrite "npcid-mapping.properties"

2) See "darkstar-batch.log" for details of what happened. If you need more info, you
may enable debug logging by editing "log4j.properties" and changing "INFO" to "DEBUG" at
the top and rerunning the batch.

REMEMBER: If the wrong name is scraped that means the ID in OUR file is wrong. Fix the
ids then re-run the job. Unless something is out of sync, the only changes should be
new npcs that get filled in.

==========================================
= F) Npc Id Update Batch
==========================================

Batch Job to Update Npc Updates for Darkstar after a Version Update

This Job requires both a POLUtils Dump with names in sync (see POLUtils Name Scraper)

If scraped names are wrong the id will be updated wrong. Ensure scraped names are in sync first.
  
After such cases are manually fixed, the mapping file should be regenerated, and they should
then be auto-fixable in the future.

1) Run "NpcIdUpdate.bat". This will make updates to the "npc_list.sql" file in your SQL directory.

2) Review the results carefully with a diff tool.
      
3) After manually reviewing the diff changes and making fixes if necessary, the file may be committed.

4) See "darkstar-batch.log" for details of what happened. If you need more info, you
may enable debug logging by editing "log4j.properties" and changing "INFO" to "DEBUG" at
the top and rerunning the batch.

==========================================
= G) Text Id Update Batch
==========================================

Job to Perform a Batch Update of Text IDs after a Version Update

Requires a POLUtils Dump of dialog tables for reference. This job
makes comparisons between the comment in the TextID file and the
dialog tables in POLUtils to find the updated Text ID

Accuracy varies with two factors:

1) Accuracy of the comment in the TextID files. Typos will break the
comparison

2) State of bad chars & etc. in the dialog table dumps. This job will
try to strip them out of both sides for comparison, but that may leave
the sting without enough context to make it unique. In this case, its
possible the wrong ID would be selected.

Ids not concretely matched can be prepended with FIXME: for manual review.

Please check for these after the job and fix them before committing.

To run the batch:

1) Ensure you have a current POLUtils Dump

2) Run TextIdUpdate.bat

3) Check "darkstar-batch.log" - the last section will tell you any files with FIXMEs

4) Manually review and fix all the FIXMEs, comparing with POLUtils manually.

5) When fixing FIXMEs, update the comment in the LUA file with the correct POLUtils 
text if possible to make it auto-fix next time.

6) After all FIXMEs are fixed manually, the file can be committed

7) See "darkstar-batch.log" for details of what happened. If you need more info, you
may enable debug logging by editing "log4j.properties" and changing "INFO" to "DEBUG" at
the top and rerunning the batch.

IMPROVING THE BATCH:

Two main things can improve this batch easily:

1) Improving the comments in the LUA file to more closely match POLUtils.

2) Improve the Bad Char REGEX Filters in "badcharacters.dat" in the project source (or fix POLUtils :p)

==========================================
= H) Lua Script NPC Id Update Batch
==========================================

Job to Perform a Batch Update of NPC Ids in Lua Script Files

This job searches all lua files in the scripts directory and does
a find/replace with all collected data. Its sorts the shifts numerically
and then iterates in reverse order to avoid substituting the wrong values.

Requires a one time use properties file that is generated by the
npc id update job. In avoid to avoid accidentally messing things up,
this properties file currently only includes ids that were a direct
match, and not a guess. However, since this file is a generated as
a seperate step, the user can add the right values for guesses as
they are reviewing them if they wish, and then have this job also
auto replace the values.

Accuracy of this job will vary with accuracy of the NPC ID update
job. For entries that were mapped in that job using the mapping file,
the IDs will not get fixed properly if they are already broken, but
should not be made worse either (it should be left in the same state it
was prior to the version update).

IMPROVING THE BATCH:

Improve the NPC ID Job :-)

==========================================
= I) License Notices
==========================================

Dark Star Batch:
As noted in all source files, this batch project is distributed under the GNU GPL v3 (or higher), as
is the case with the core Dark Star Server.
http://www.gnu.org/copyleft/gpl.html

Apache Log4j, Apache Commons Lang, Apache Commons IO:
These dependencies are distributed under the Apache License V2.0
http://www.apache.org/licenses/LICENSE-2.0

Both GNU & the Apache Software Foundation consider Apache License 2.0 & GPL v3 Compatible w/ Each other,
so long as the final project is distrubted as GPL v3.

https://www.gnu.org/licenses/license-list.html
http://www.apache.org/licenses/GPL-compatibility.html
