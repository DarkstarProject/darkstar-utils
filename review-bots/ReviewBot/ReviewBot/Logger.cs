using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Text;
using System.IO;

namespace ReviewBot
{
    class Logger
    {
        static string FilePath;

        public Logger(string filePath)
        {
            FilePath = filePath;
        }

        public void Log(string str, params object[] args)
        {
            lock (FilePath)
            {
                var str2 = "[" + DateTime.Now.ToString() + "] " + String.Format(str, args);
                Console.WriteLine(str2);
                File.AppendAllText(FilePath, str2);
            }
        }
    }
}
