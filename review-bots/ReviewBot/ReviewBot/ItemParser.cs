using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml;
using System.IO;
using System.Globalization;

namespace ReviewBot
{
    class ItemParser
    {
        public string Directory { get; }

        Dictionary<string, Dictionary<int, Item>> Items;
        public ItemParser(string directory)
        {
            Items = new Dictionary<string, Dictionary<int, Item>>();
            Directory = directory;
            ParseItemXmls();
        }

        private void ParseItemXmls()
        {
            Items.Clear();

            foreach (var fileName in System.IO.Directory.EnumerateFiles(Directory, "item*.xml"))
            {
                Items.Add(fileName, ParseFile(fileName));
            }
        }

        private Dictionary<int, Item> ParseFile(string fileName)
        {
            var items = new Dictionary<int, Item>();
            if (!File.Exists(fileName))
            {
                Program.Logger.Log("[ItemParser] Unable to load {0}!", fileName);
                return null;
            }

            XmlDocument xml = new XmlDocument();
            xml.Load(fileName);

            Program.Logger.Log("[ItemParser] Loading {0}...", fileName);
            foreach (XmlNode thing in xml.GetElementsByTagName("thing"))
            {
                var item = new Item();
                bool skip = false;
                foreach (XmlNode child in thing.ChildNodes)
                {
                    string fieldName = child.Attributes.GetNamedItem("name").Value;

                    if (fieldName == "id")
                    {
                        item.ItemId = ushort.Parse(child.InnerText);
                        if (skip = items.ContainsKey(item.ItemId))
                            break;
                    }
                    else if (fieldName == "flags")
                        item.Flags = UInt32.Parse(child.InnerText, NumberStyles.AllowHexSpecifier);
                    else if (fieldName == "valid-targets")
                        item.ValidTargets = ulong.Parse(child.InnerText, NumberStyles.AllowHexSpecifier);
                    else if (fieldName == "name")
                        item.Name = child.InnerText.Replace(' ', '_').ToLower().Replace("\'", "\\'");
                    else if (fieldName == "description")
                        item.Description = child.InnerText;
                    else if (fieldName == "level")
                        item.Level = ushort.Parse(child.InnerText);
                    else if (fieldName == "iLevel")
                        item.ItemLevel = ushort.Parse(child.InnerText);
                    else if (fieldName == "slots")
                        item.Slots = ulong.Parse(child.InnerText);
                    else if (fieldName == "races")
                        item.Races = ulong.Parse(child.InnerText, NumberStyles.AllowHexSpecifier);
                    else if (fieldName == "jobs")
                        item.Jobs = child.InnerText;
                    else if (fieldName == "superior-level")
                        item.SuperiorLevel = ushort.Parse(child.InnerText);
                    else if (fieldName == "shield-size")
                        item.ShieldSize = ushort.Parse(child.InnerText);
                    else if (fieldName == "max-charges")
                        item.MaxCharges = ushort.Parse(child.InnerText);
                    else if (fieldName == "casting-time")
                        item.CastingTime = UInt32.Parse(child.InnerText);
                    else if (fieldName == "use-delay")
                        item.UseDelay = UInt32.Parse(child.InnerText);
                    else if (fieldName == "reuse-delay")
                        item.ReuseDelay = UInt32.Parse(child.InnerText);
                }
                if (!skip)
                    items.Add(item.ItemId, item);
            }

            return items;
        }
    }
}
