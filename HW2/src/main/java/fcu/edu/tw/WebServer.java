package fcu.edu.tw;

import java.io.*;
import java.net.DatagramPacket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class WebServer {

    public static void main(String[] args) throws Exception{
        // Set the port number
        int portNumber = 6789;
        // Establish the listen socket
        ServerSocket serverSocket = new ServerSocket(portNumber);
        System.out.println("waiting for client connecting......\n");
        // create the client socket
        Socket clientSocket = new Socket();

        do {
            try {
                clientSocket = serverSocket.accept();
                System.out.println("Connect successfully IP : " + clientSocket.getInetAddress() +
                        " Port Number : " + clientSocket.getPort());
            } catch (IOException e) {
                System.out.println("ERROR : " + e);
            }

            HttpRequest httpRequest = new HttpRequest(clientSocket);
            Thread thread = new Thread(httpRequest);
            thread.start();
        } while (true);
    }
}

final class HttpRequest implements Runnable{
    final static String CRLF = "\r\n";
    Socket socket;
    public HttpRequest(Socket socket) throws Exception{
        this.socket = socket;
    }

    private void processRequest() throws Exception{
        // Get a reference to the socket's input and output streams.
        InputStream inputStream = socket.getInputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

        // Set up input stream filters
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        // Get the request line of the HTTP request message
        String requestLine = bufferedReader.readLine();
        System.out.println("Request Line : " + requestLine);

        // Get and display the header lines
        String headerLine = null;
        while((headerLine = bufferedReader.readLine()).length() != 0){
            System.out.println("Header Line : " + headerLine);
        }

        // Extract the filename from the request line
        StringTokenizer stringTokenizer = new StringTokenizer(requestLine);
        stringTokenizer.nextToken(); // skip over the method, which should be "Get"
        String fileName = stringTokenizer.nextToken();

        // Prepend a "." so that file request is within the current directory
        fileName = "." + fileName;
        // Open the requested file
        FileInputStream fileInputStream = null;
        boolean fileExists = true;
        try{
            fileInputStream = new FileInputStream(fileName);
        }catch (FileNotFoundException e){
            fileExists = false;
            System.out.println("ERROR : " + e);
        }

        // Construct the response message
        String statusLine = null;
        String contentTypeLine = null;
        String entityBody = null;

        if(fileExists){
            statusLine = "HTTP/1.1 200 OK" +CRLF;
            contentTypeLine = "Content-Type : " + contentType(fileName) + CRLF;
        }else{
            statusLine = "HTTP/1.1 404 Not Found" + CRLF;
            contentTypeLine = "Content-Type : text/html" + CRLF;
            entityBody = "<HTML><HEAD><TITLE>NOT FOUND</TITLE></HEAD><BODY>404 Not Found</BODY></HTML>";
        }

        // Send the status line.
        dataOutputStream.writeBytes(statusLine);
        // Send the content type line.
        dataOutputStream.writeBytes(contentTypeLine);
        // Send a blank line to indicate the end of the header lines.
        dataOutputStream.writeBytes(CRLF);

        // Send the entity body
        if(fileExists){
            sendBytes(fileInputStream, dataOutputStream);
            fileInputStream.close();
        }else{
            dataOutputStream.writeBytes(entityBody);
        }

        // CLose streams and socket
        dataOutputStream.close();
        bufferedReader.close();
        socket.close();
    }

    private static void sendBytes(FileInputStream fileInputStream, OutputStream outputStream) throws Exception{
        // Construct a 1K buffer to hold bytes on their way to the socket
        byte[] buffer = new byte[1024];
        int bytes = 0;

        // Copy requested file into the socket's output stream
        while((bytes = fileInputStream.read(buffer)) != -1){
            outputStream.write(buffer, 0, bytes);
        }
    }

    private static String contentType(String fileName){
        if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
            return "text/html";
        }
        if (fileName.endsWith(".css")) {
            return "text/css";
        }
        if (fileName.endsWith(".js")) {
            return "application/javascript";
        }
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        }

        return "application/octet-stream";
    }

    @Override
    public void run() {
        try{
            processRequest();

        } catch (Exception e) {
            System.out.println("ERROR : " + e);
        }
    }
}
