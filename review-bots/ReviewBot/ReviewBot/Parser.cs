using System;
using System.Collections.Generic;
using System.Data;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Xml;

namespace ReviewBot
{
    enum CppEnumType
    {
        Normal,
        Struct,
        Class
    }

    class EnumEntry
    {
        public string EnumName, EntryName, Comment;
        public long Id;
    }

    class EnumParser
    {
        public static readonly Dictionary<CppEnumType, string> EnumTypeStr = new Dictionary<CppEnumType, string>()
        {
            { CppEnumType.Normal, "" },
            { CppEnumType.Class, "class" },
            { CppEnumType.Struct, "struct" }
        };

        Dictionary<string, Dictionary<long, EnumEntry>> EnumDictionary = new Dictionary<string, Dictionary<long, EnumEntry>>();

        public EnumParser()
        {
            Reset();
        }

        public void Reset()
        {
            EnumDictionary.Clear();
            ParseEnum("darkstar/src/map/latent_effect.h", "LATENT_EFFECT");
            ParseEnum("darkstar/src/map/modifier.h", "Mod");
            ParseEnum("darkstar/src/map/items/item.h", "ITEM_FLAG");
            ParseEnum("darkstar/src/map/items/item.h", "ITEM_TYPE");
            ParseEnum("darkstar/src/map/entities/battleentity.h", "TARGETTYPE");
            ParseEnum("darkstar/src/map/entities/battleentity.h", "SLOTTYPE");
            ParseEnum("darkstar/src/map/entities/battleentity.h", "JOBTYPE");
        }

        bool ParseEnum(string fileName, string enumName)
        {
            if (File.Exists(fileName))
            {
                var fileStr = File.ReadAllText(fileName);
                int defStartPos = -1;
                defStartPos = fileStr.IndexOf("\nenum " + enumName);

                if (defStartPos < 0)
                {
                    for (var i = 1; i < EnumTypeStr.Count; ++i)
                    {
                        var typeStr = EnumTypeStr.ElementAt(i).Value;
                        if (defStartPos < 0)
                            defStartPos = fileStr.IndexOf("\nenum " + typeStr + enumName);

                        if (defStartPos >= 0)
                            break;
                    }
                }

                if (defStartPos >= 0)
                {
                    var defEndPos = fileStr.IndexOf(@"};", defStartPos);
                    var enumStr = fileStr.Substring(defStartPos, defEndPos - defStartPos);
                    var enumStrStart = fileStr.IndexOf("{", defStartPos, 1);

                    var entries = fileStr.Substring(enumStrStart, defEndPos).Replace("\r\n", "\n").Split('\n');

                    var regex = new Regex(@"(?:\s+|)(\w+)(?:\s+|)(?:\=(?:(?:\s+|)(\w+)|))(?:,|)(?:\s+|)(?:(\/\/.*)|)(?:\s+)");

                    foreach (var entry in entries)
                    {
                        EnumEntry enumEntry = new EnumEntry();

                        var entry2 = entry.Trim();
                        var matches = regex.Matches(entry2);

                        if (matches.Count > 0)
                        {
                            if (matches.Count > 2)
                                enumEntry.Comment = matches[2].Value;
                            if (matches.Count > 1)
                                enumEntry.Id = long.Parse(matches[1].Value, NumberStyles.AllowHexSpecifier);
                            enumEntry.EnumName = enumName;
                        }
                        EnumDictionary[enumName][enumEntry.Id] = enumEntry;
                    }

                }
            }
            return false;
        }
    }
}
