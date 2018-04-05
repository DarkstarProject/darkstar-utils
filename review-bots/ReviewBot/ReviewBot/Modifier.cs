using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Text.RegularExpressions;

namespace ReviewBot
{
    class Modifier
    {
        public int ModifierId { get; set; }
        public string ModifierName { get; set; }
        public string ModifierValue { get; set; }
        public string[] ModifierValueRegex { get; set; }
        public string[] ModifierNameRegex { get; set; }
        public string ModifierConversion { get; set; }
        public string ErrorString { get; set; }
        public string ModifierComment { get; set; }

        public string LatentEffectId { get; set; }
        public string LatentEffectName { get; set; }

        public bool IsLatent { get; set; }

        public Modifier()
        {

        }
    }
}
