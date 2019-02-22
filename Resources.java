public class Resources{

    public static String notFound = "<!DOCTYPE html>\n"+
                                        "<html lang=\"en\">\n"+
                                        "<head>\n"+
                                        "    <meta charset=\"UTF-8\">\n"+
                                        "    <title>Title</title>\n"+
                                        "</head>\n"+
                                        "<body>\n"+
                                        "    <h1>Not Found</h1>\n"+
                                        "    <p>The requested URL was not found on this server.</p>\n"+
                                        "</body>\n"+
                                        "</html>\n\n";

    public static String unSupportedMediaType = "<html>\n"+
                                                  " <body>\n"+
                                                  "  <h1>Unsupported Media Type</h1>\n"+
                                                  "  <p>The requested media type is not supported in the webserver.</p>\n"+
                                                  "  <p>Supported media types are htm, html, txt, gif, jpg, jpeg, png</p>\n"+
                                                  " </body>\n"+
                                                  "</html>\n\n";

    public static String forbidden = "<html>\n"+
                                      " <body>\n"+
                                      "  <h1>Access denied</h1>\n"+
                                      "  <p>You are not allowed to access the resource in the requested URL.</p>\n"+
                                      " </body>\n"+
                                      "</html>\n\n";

    public static String badRequest = "<html>\n"+
                                        " <body>\n"+
                                        "  <h1>Bad Request</h1>\n"+
                                        "  <p>Your browser sent a request that this server could not understand.</p>\n"+
                                        " </body>\n"+
                                        "</html>\n\n";

    public static String internalServerError = "<html>\n"+
                                                " <body>\n"+
                                                "  <h1>Internal Server Error</h1>\n"+
                                                "  <p>The server encountered an internal error or misconfiguration and " +
                                                        "was unable to complete your request.</p>\n"+
                                                " </body>\n"+
                                                "</html>\n\n";
}

