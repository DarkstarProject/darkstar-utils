using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewBot
{

    class Item
    {
        public ushort ItemId;
        public string Name;
        public string Description;
        public ulong Flags;
        public byte StackSize;
        public ulong ValidTargets;
        public ushort Level;
        public ushort ItemLevel;
        public string Jobs;
        public ulong Races;
        public ulong Slots;
        public ushort SuperiorLevel;
        public ushort MaxCharges;
        public ushort ShieldSize;
        public ulong CastingTime;
        public ulong UseDelay;
        public ulong ReuseDelay;

        public string FFXIAHUrl;

        public string GetFFXIAHUrl()
        {
            if (string.IsNullOrEmpty(FFXIAHUrl))
                FFXIAHUrl = String.Format("https://ffxiah.com/item/{0}", ItemId);

            return FFXIAHUrl;
        }
    }
}
