import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.net.Socket;
import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;


// Each Client Connection will be managed in a dedicated Thread
public class MultiThreadedHTTPServer implements Runnable{

    boolean is500ErrorReturned = false;
    // verbose mode to turn on/off the comments
    static final boolean verbose = true;
    // Client Connection via Socket Class
    private Socket connect;
    private Integer httpType;
    // we manage our particular client connection
    BufferedReader in = null;
    PrintWriter out = null;
    BufferedOutputStream dataOut = null;
    String fileRequested = "";
    double timer = 60000; //init to 1 minute of expiration
    double startTime = System.currentTimeMillis();


    public MultiThreadedHTTPServer(Socket c) {
        connect = c;
    }

    @Override
    public void run() {
        boolean isBadRequest = false;
        while(true){

            if(System.currentTimeMillis() > (long)(startTime+timer)){
                System.out.println("Terminating socket due to timeout");
                break;
            }


            System.out.println("\n\nProcessing thread : "+ Thread.currentThread().getName());
            System.out.println("System:"+System.currentTimeMillis()+"\nExpiry:"+(long)(startTime+timer));
            try {
                // we read characters from the client via input stream on the socket
                in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
                // we get character output stream to client (for headers)
                out = new PrintWriter(connect.getOutputStream());
                // get binary output stream to client (for requested data)
                dataOut = new BufferedOutputStream(connect.getOutputStream());

                // get first line of the request from the client
                String input = "";
                StringBuilder command = new StringBuilder();
                String prev = "";
                int i = 0;
                while ((input = in.readLine()) != null) {
                    if (prev.equals(input) || i > 10 || input.equals("") || input.equals("^"))
                        break;
                    command.append(input);
                    command.append('\n');
                    prev = input;
                    i++;
                }
                System.out.println(command.toString());

                if (command.toString().isEmpty())
                    throw new NotImplementedException();
                // we parse the request with a string tokenizer
                StringTokenizer parse = new StringTokenizer(command.toString(), "\n");
                int lineNumber = 0;
                String method = "";
                httpType = -1;
                Boolean isKeepAlive = false;
                isBadRequest = false;
                while (parse.hasMoreTokens()) {
                    String line = parse.nextToken();
                    if (lineNumber == 0) {
                        if(!line.matches(Utils.requestRegex)){
                            if(verbose)
                                System.out.println(line);
                            isBadRequest = true;
                        }
                        method = Utils.getStringFromPattern(Utils.getMethodRegex ,line);

                        fileRequested = Utils.getStringFromPattern(Utils.getFilePathRegex ,line);
                        // GET method

                        httpType = Integer.parseInt(Utils.getStringFromPattern(Utils.getHttpModeRegex, line));
                        if(isBadRequest){
                            if (fileRequested != null && fileRequested.equals("/"))  {
                                fileRequested += Utils.DEFAULT_FILE;
                                fileRequested = Utils.getStringFromPattern(Utils.getFilePathRegex ,fileRequested);
                                if(method.equals("GET") && (httpType == 0 || httpType == 1))
                                    isBadRequest = false;
                            }
                            else{
                                fileRequested = "";
                                String[] tokens = line.split(" ");
                                if(tokens[1].equals("/")){
                                    fileRequested += '/'+Utils.DEFAULT_FILE;
                                    fileRequested = Utils.getStringFromPattern(Utils.getFilePathRegex ,fileRequested);
                                    if(method.equals("GET") && (httpType == 0 || httpType == 1))
                                        isBadRequest = false;
                                }
                            }
                        }


                        if (httpType == 1) {
                            isKeepAlive = true;
                        }

                        if(isBadRequest)
                            break;
                    }
                    lineNumber++;
                }

                if(isBadRequest){
                    this.badRequestTemplate("400", out, dataOut);
                    continue;
                }


                System.out.println("http: " + httpType + "keep:" + isKeepAlive);

                File file = new File(Utils.WEB_ROOT, fileRequested);
                String fileExtn ="";
                if(fileRequested.contains("."))
                        fileExtn = fileRequested.split("\\.")[1];
                else
                    this.badRequestTemplate("400",out, dataOut);

                if (!Utils.isSupportedFileExtension(fileExtn)){
                    this.unSupportedTemplate(fileExtn, out, dataOut);
                    continue;
                }


                String content = Utils.getContentType(fileExtn);

                if (method.equals("GET")) { // GET method so we return content

                    if (!file.exists())
                        throw new FileNotFoundException("File Not Found");

                    if(System.currentTimeMillis() > (long)(startTime+timer)){
                        System.out.println("Terminating socket due to timeout");
                        break;
                    }


                    if (!file.canRead())
                        throw new AccessDeniedException("Access Denied");



                    int fileLength = (int) file.length();
                    byte[] fileData = readFileData(file, fileLength);
                    // send HTTP Headers
                    out.println(Utils.getHttpHeaderFor(httpType, 200, "OK"));
                    this.sendContentHeader(fileLength,Utils.getContentType(fileExtn), (int) timer/1000);
                    out.println(); // blank line between headers and content, very important !
                    out.flush(); // flush character output stream buffer

                    dataOut.write(fileData, 0, fileLength);
                    dataOut.flush();
                    is500ErrorReturned = false;

                    if(httpType == 1)
                        this.updateTime(timer, System.currentTimeMillis());
                }

                if (verbose) {
                    System.out.println("File " + fileRequested + " of type " + content + " returned");
                }



            } catch (NullPointerException e) {
                if (verbose)
                    System.out.println("NPE:" + e.getCause());
                    continue;
            } catch (AccessDeniedException accessDeniedException) {
                try {
                    this.accessDeniedTemplate("GET", out, dataOut);
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException fnfe) {
                try {
                    fileNotFound(out, dataOut, fileRequested);
                    continue;
                } catch (IOException ioe) {
                    String reason = ioe.getMessage();
//                    try {
//                        this.internalServerErrorTemplate(reason);
//                        break;
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                    System.err.println("Error with file not found exception : " + ioe.getMessage());
                }

            } catch (Exception e) {
                try {
                    this.closeSocket();
                    break;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                System.err.println("Server error : " + e.getLocalizedMessage());
            } finally {
                try {
                    if (httpType == null || httpType != 1 || is500ErrorReturned) {
                        this.closeSocket();
                        break;
                        //we close socket connection
                    }
                } catch (Exception e) {
                    System.err.println("Error closing stream : " + e.getMessage());
                }
            }
        }
        try {
            this.closeSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeSocket() throws IOException {
        in.close();
        out.close();
        dataOut.close();
        if(connect.isConnected())
            connect.close();
        if (verbose) {
            System.out.println("Connection closed.\n");
        }
    }

    private byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }

        return fileData;
    }

    private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
        is500ErrorReturned = false;
        int fileLength = (int) Resources.notFound.length();

        byte[] fileData = Resources.notFound.getBytes();
        out.println(Utils.getHttpHeaderFor(httpType, 404, "File Not Found"));
        this.sendContentHeader(fileLength, (int) timer/1000);
        out.println(); // blank line between headers and content, very important !
        out.flush(); // flush character output stream buffer

        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();

        if (verbose) {
            System.out.println("File " + fileRequested + " not found");
        }

        if(httpType == 1)
            this.updateTime(timer, System.currentTimeMillis());
    }

    private void internalServerErrorTemplate(String reason) throws IOException {
        is500ErrorReturned = true;
        if (verbose) {
            System.out.println("500 Internal Server Error " + reason);
        }

        // we return the not supported file to the client
        int fileLength = (int) Resources.internalServerError.length();
        byte[] fileData = Resources.internalServerError.getBytes();

        //read content to return to client
        // we send HTTP Headers with data to client
        out.println(Utils.getHttpHeaderFor(httpType, 500, "Internal Server Error"));
        this.sendContentHeader(fileLength, (int) timer/1000);
        out.println(); // blank line between headers and content, very important !
        out.flush(); // flush character output stream buffer
        // file
        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();
        closeSocket();
    }

    private void accessDeniedTemplate(String method, PrintWriter out, BufferedOutputStream dataOut) throws IOException{
        is500ErrorReturned = false;
        if (verbose) {
            System.out.println("403 Access Denied : " + method + " method.");
        }

        // we return the not supported file to the client
        int fileLength = (int) Resources.forbidden.length();
        byte[] fileData = Resources.forbidden.getBytes();
        // we send HTTP Headers with data to client
        out.println(Utils.getHttpHeaderFor(httpType, 403, "Access Denied"));
        this.sendContentHeader(fileLength, (int) timer/1000);
        out.println(); // blank line between headers and content, very important !
        out.flush(); // flush character output stream buffer
        // file
        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();

        if(httpType == 1)
            this.updateTime(timer, System.currentTimeMillis());
    }

    private void unSupportedTemplate(String method, PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        is500ErrorReturned = false;
        if (verbose) {
            System.out.println("415 Unsupported Media Type : " + method + " method.");
        }

        // we return the not supported file to the client
        int fileLength = (int) Resources.unSupportedMediaType.length();
        byte[] fileData = Resources.unSupportedMediaType.getBytes();
        //read content to return to client

        // we send HTTP Headers with data to client
        out.println(Utils.getHttpHeaderFor(httpType, 415, "Unsupported Media Type"));
        this.sendContentHeader(fileLength, (int) timer/1000);
        out.println(); // blank line between headers and content, very important !
        out.flush(); // flush character output stream buffer
        // file
        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();

        if(httpType == 1)
            this.updateTime(timer, System.currentTimeMillis());
    }

    private void badRequestTemplate(String method, PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        is500ErrorReturned = false;
        if (verbose) {
            System.out.println("400 Bad Request"+method);
        }

        // we return the not supported file to the client
        int fileLength = (int) Resources.badRequest.length();
        byte[] fileData = Resources.badRequest.getBytes();
        //read content to return to client

        // we send HTTP Headers with data to client
        out.println(Utils.getHttpHeaderFor(httpType, 400, "Bad Request"));
        this.sendContentHeader(fileLength, (int) timer/1000);
        out.println(); // blank line between headers and content, very important !
        out.flush(); // flush character output stream buffer
        // file
        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();

        if(httpType == 1)
            this.updateTime(timer, System.currentTimeMillis());
    }

    private void sendContentHeader(int fileLength, int expiresIn) {
        String contentMimeType = "text/html";
        out.println("Date: " + new Date());
        out.println("Content-type: " + contentMimeType);
        out.println("Content-length: " + fileLength);
        if(httpType == 1)
            out.println("Expires-in: " + expiresIn + " seconds");
    }

    private void sendContentHeader(int fileLength, String contentMimeType, int expiresIn) {
        out.println("Date: " + new Date());
        out.println("Content-type: " + contentMimeType);
        out.println("Content-length: " + fileLength);
        if(httpType == 1)
            out.println("Expires-in: " + expiresIn + " seconds");
    }

    private void updateTime(double timer, long startTime){
        System.out.println("Before timer:"+timer);
        this.timer =  Utils.getTimeOutHeuristic(timer);
        System.out.println("After timer:"+timer);
        this.startTime = startTime;
        Instant instant = Instant.ofEpochMilli((long) (timer+startTime));
        LocalDateTime date = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        if(verbose)
            System.out.println("Updating expiration time to"+ date);
    }

}
