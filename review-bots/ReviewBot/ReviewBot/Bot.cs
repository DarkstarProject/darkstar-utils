using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewBot
{
    class Bot
    {
        ItemParser ItemParser;

        Queue<string> Payloads;
        string User;
        Octokit.GitHubClient Client;
        string Name = "DankStar";

        public Bot(string user)
        {
            Client = new Octokit.GitHubClient(new Octokit.ProductHeaderValue(Name));
            //var auth = Client.Authorization.GetOrCreateApplicationAuthentication(Name, "assquif", new Octokit.NewAuthorization()).Result;
            
            Payloads = new Queue<string>();
        }

        public void Queue(string str)
        {
            lock (Payloads)
                Payloads.Enqueue(str);
        }

        private void Run()
        {
            // todo: 
        }
    }
}
