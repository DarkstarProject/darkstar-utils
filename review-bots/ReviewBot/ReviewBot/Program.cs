using System;
using System.Threading;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Net;
using System.Net.Sockets;

using Octokit;

namespace ReviewBot
{
    class Program
    {
        static string User, Pass, Token, ListenIP, ItemXmlLocation;
        static short ListenPort;
        static ManualResetEvent Event;
        static bool Run = true;
        public static Bot Bot;
        public static Logger Logger;

        static void ParseArguments(string[] args)
        {
            for (int i = 0; i + 1 < args.Length; i += 2)
            {
                var arg = args[i];
                var val = args[i + 1];

                if (arg == "-User")
                    User = val;
                else if (arg == "-Pass")
                    Pass = val;
                else if (arg == "-Token")
                    Token = val;
                else if (arg == "-ListenPort")
                    ListenPort = Convert.ToInt16(val);
                else if (arg == "-ListenIP")
                    ListenIP = val;
                else if (arg == "-ItemXmlLocation")
                    ItemXmlLocation = val;
            }
        }

        static void Main(string[] args)
        {
            Bot = new Bot("DankStar");
            ParseArguments(args);

            short port = ListenPort != 0 ? ListenPort : (short)8765;

            try
            {
                Server s = new Server(ListenIP, port);
            }
            catch (Exception e)
            {
                Console.WriteLine(e.Message);
                Console.ReadLine();
            }
        }
    }
}
