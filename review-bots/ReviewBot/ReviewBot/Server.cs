using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewBot
{
    public class State
    {
        public Socket Sock = null;
        public byte[] Buf;
        public StringBuilder Sb;
    }

    class Server
    {
        string ListenIP;
        short ListenPort;
        ManualResetEvent Event;
        public bool Run = true;

        public Server(string ipAddr, short port)
        {
            ListenIP = ipAddr;
            ListenPort = port;
            Event = new ManualResetEvent(false);

            try
            {
                var ip = IPAddress.Parse(ListenIP);
                Socket s = new Socket(ip.AddressFamily, SocketType.Stream, ProtocolType.Tcp);
                var endPoint = new IPEndPoint(ip, ListenPort);
                s.Bind(endPoint);
                s.Listen(100);
                Console.WriteLine("Listening...");

                while (true)
                {
                    s.BeginAccept(new AsyncCallback(AcceptCallback), s);
                    Event.WaitOne();
                }
            }
            catch (Exception e)
            {
                Console.WriteLine(e.Message);
                Console.ReadLine();
                Run = false;
            }
        }

        void AcceptCallback(IAsyncResult asyncResult)
        {
            Event.Set();
            Socket listener = (Socket)asyncResult.AsyncState;
            Socket handler = listener.EndAccept(asyncResult);

            var state = new State();
            state.Buf = new byte[handler.ReceiveBufferSize];
            state.Sb = new StringBuilder();
            handler.BeginReceive(state.Buf, 0, state.Buf.Length, 0, new AsyncCallback(ReceiveCallback), state);

        }

        void ReceiveCallback(IAsyncResult asyncResult)
        {
            State state = (State)asyncResult.AsyncState;
            Socket handler = state.Sock;

            var payload = "";

            var bytesRead = handler.EndReceive(asyncResult);

            if (bytesRead > 0)
            {
                payload = state.Sb.Append(Encoding.ASCII.GetString(state.Buf, 0, bytesRead)).ToString();
                if (payload.Contains("<EOF>"))
                {
                    Program.Bot.Queue(payload);
                }
                else
                {
                    handler.BeginReceive(state.Buf, 0, state.Buf.Length, 0, new AsyncCallback(ReceiveCallback), state);
                }
            }
        }
    }
}