import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static File WEB_ROOT = new File(".");
    public static final String DEFAULT_FILE = "index.html";
    // port to listen connection
    static int PORT = 8080;
    public static String requestRegex = "[\\s]*(GET){1}[\\s]+((\\/([a-zA-Z0-9-_()]+)+)+\\.([a-z]+)){1}[\\s]+(HTTP\\/1\\.(0|1)){1}[\\s]*(\\n)*";
    public static String getMethodRegex = "[\\s]*(GET){1}[\\s]+";
    public static String getFilePathRegex = "(((\\/([a-zA-Z0-9-_()]+)+)+\\.([a-z]+)){1})";
    public static String getHttpModeRegex = "\\/1\\.(0|1)[\\s]*(\\n)*";

    public static String getContentType(String fileExtension){
        if(fileExtension.contains("htm"))
            return "text/html";
        if(fileExtension.equals("txt"))
            return "text/plain";
        return String.format("image/%s",fileExtension).toString();
    }

    public static boolean isSupportedFileExtension(String fileExtension){
        List<String> supportedExtensions = Arrays.asList("htm","html","txt","gif","jpg","jpeg","png");
        return supportedExtensions.contains(fileExtension);
    }

    public static String getStringFromPattern(String pattern, String input){
        Pattern p = Pattern.compile(pattern);   // the pattern to search for
        Matcher m = p.matcher(input);
        if(m.find()) {
            String s = m.group(1);
            return s;
        }
        return "";
    }

    public static String getHttpHeaderFor(int httpType, int code, String message){
        return String.format("HTTP/1.%d %d %s",httpType,code,message).toString();
    }

    public static double getTimeOutHeuristic(double currentTime){
        //double everytime until a max of 10 minutes.
        return  currentTime < 300000 ? currentTime * 2 : 600000;
    }
}
