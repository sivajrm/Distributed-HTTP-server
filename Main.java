import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Date;

public class Main {

    public static void main(String[] args) {
        int i = -1;
        try {
            for (String val:args){
                i++;
                //System.out.println(val);
                if(val.contains("-port")){
                    Utils.PORT = Integer.parseInt(args[i+1]); ;
                }
                if(val.contains("-document_root")){
                    Utils.WEB_ROOT = new File(args[i+1]);
                }
            }

            ServerSocket serverConnect = new ServerSocket(Utils.PORT);

            System.out.println("Server started.\nListening for connections on port : " + Utils.PORT + " ...\n");
            System.out.println("Server serving from path " + Utils.WEB_ROOT.getPath());
            // we listen until user halts server execution
            while (true) {
                MultiThreadedHTTPServer myServer = new MultiThreadedHTTPServer(serverConnect.accept());
                System.out.println("Connecton opened. (" + new Date() + ")");
                // create dedicated thread to manage the client connection
                Thread thread = new Thread(myServer);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Server Connection error : " + e.getMessage());
        }
    }
}
