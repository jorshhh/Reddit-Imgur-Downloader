package mx.aidc;

import com.github.jreddit.entity.Submission;
import com.github.jreddit.entity.User;
import com.github.jreddit.exception.RetrievalFailedException;
import com.github.jreddit.retrieval.Submissions;
import com.github.jreddit.retrieval.params.SubmissionSort;
import com.github.jreddit.retrieval.params.UserOverviewSort;
import com.github.jreddit.retrieval.params.UserSubmissionsCategory;
import com.github.jreddit.utils.restclient.HttpRestClient;
import com.github.jreddit.utils.restclient.RestClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Created by jorge on 4/15/15.
 */
public class SessionData {
    private static SessionData ourInstance = new SessionData();
    private User selectedUser;
    private RestClient restClient;
    private boolean isReady;
    private String targetUser;
    private String directory;

    public static SessionData getInstance() {
        return ourInstance;
    }

    private SessionData() {
    }

    public boolean authUser(String[] args){

        int index= 0;
        String username = "";
        String password = "";
        directory = "";

        for(String argument : args){

            if(argument.equals("-u") && args.length > index+1){
                username = args[index+1];
            }

            if(argument.equals("-p") && args.length > index+1){
                password = args[1+index];
            }

            if(argument.equals("-t") && args.length > index+1){
                targetUser = args[1+index];
            }

            if(argument.equals("-d") && args.length > index+1){
                directory = args[1+index];
            }

            index++;

        }

        if(username.equals("") || password.equals("") || targetUser.equals("")){
            System.out.println("Some arguments are missing");
            return false;
        }

        if(directory.equals(""))
            directory = "./";

        directory = directory+"/"+targetUser;
        System.out.println("Creating dir... "+directory);
        new File(directory).mkdirs();

        System.out.println("Authenticating user: "+username);

        // Initialize REST Client
        restClient = new HttpRestClient();
        restClient.setUserAgent("bot/1.0 by /u/jorshhh");

        // Connect the user
        User user = new User(restClient, username, password);
        selectedUser = user;
        try {
            user.connect();
        } catch (Exception e) {
            System.out.println("Auth Error: check your username and password. There might be no internet connection.");
            selectedUser = null;
            return false;
        }

        isReady = true;
        System.out.println("Connection succesful");
        return true;

    }

    public List<Submission> getSubmissions(Submission s){

        if(!isReady){
            System.out.println("You need to auth first");
            return null;
        }

        // Handle to Submissions, which offers the basic API submission functionality
        Submissions subms = new Submissions(restClient, selectedUser);

        List<Submission> submissionsUser = null;
        try {
            submissionsUser = subms.ofUser(targetUser, UserSubmissionsCategory.SUBMITTED, UserOverviewSort.NEW, -1, 100,s, null, true);
        }catch (RetrievalFailedException e){
            System.out.println("Could not find the target user");
        }

        return submissionsUser;
    }

    public void processSubmission(Submission submission){
        URL url;
        try {
            url = new URL(submission.getURL());

            if(url.getHost().equals("imgur.com")){

                if(url.getPath().contains("/a/")) {
                    this.downloadAlbum(url.getPath());
                }else{
                    this.downloadSingle(url.getPath());
                }
            }

            if(url.getHost().equals("i.imgur.com")){
                this.downloadFile("http://" + url.getAuthority() + url.getPath(), url.getPath());
            }

            if(url.getHost().equals("gfycat.com")){
                this.downloadVideo(url.getPath());
            }

        } catch (MalformedURLException e) {
            System.out.println("\n");
            e.printStackTrace();
        }

    }

    private void downloadVideo(String videoString){

        String baseURL = "http://gfycat.com/cajax/get";
        String target = baseURL+videoString;

        HttpResponse<JsonNode> jsonResponse;
        try {
            jsonResponse = Unirest.get(target)
                    .asJson();

            if(!jsonResponse.getBody().getObject().has("gfyItem"))
                return;

            JSONObject response = jsonResponse.getBody().getObject().getJSONObject("gfyItem");

            if(response.has("error")) {
                System.out.println(response.getString("error"));
                return;
            }

            String name = response.getString("gfyName");
            name = "/"+name+".mp4";
            String file = response.getString("mp4Url");
            this.downloadFile(file,name);

        } catch (UnirestException e) {
            e.printStackTrace();
        }


    }

    private void downloadFile(String url, String name){

        File f = new File(directory+name);
        if(f.exists() && !f.isDirectory()) {
            System.out.println("File already exists");
            return;
        }

        System.out.println("Downloading: " + url);

        try{
            URL downloadUrl = new URL(url);
            InputStream in = new BufferedInputStream(downloadUrl.openStream());
            OutputStream out = new BufferedOutputStream(new FileOutputStream(directory+name));

            for ( int i; (i = in.read()) != -1; ) {
                out.write(i);
            }
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void downloadSingle(String id){

        HttpResponse<JsonNode> jsonResponse;
        try {
            jsonResponse = Unirest.get("https://api.imgur.com/3/image" + id)
                    .header("Authorization", "Client-ID c33f3d201b7d01a")
                    .asJson();

            JSONObject response = jsonResponse.getBody().getObject().getJSONObject("data");

            if(!response.has("type"))
                return;

            String type = response.getString("type");
            String idItem = response.getString("id");

            if(type.equals("image/gif"))
                this.downloadFile(response.getString("mp4"), "/" + idItem + ".mp4");

        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }

    private void downloadAlbum(String album){

        String dirTemp = directory;

        album = album.replace("/a/","/");
        System.out.println(album);

        HttpResponse<JsonNode> jsonResponse;
        try {
            jsonResponse = Unirest.get("https://api.imgur.com/3/album" + album)
                    .header("Authorization", "Client-ID c33f3d201b7d01a")
                    .asJson();

            JSONObject response = jsonResponse.getBody().getObject().getJSONObject("data");

            if(response.has("error")) {
                System.out.println(response.getString("error"));
                return;
            }

            if(!response.has("id"))
                return;

            dirTemp = dirTemp + "/" + response.getString("id");
            System.out.println("Creating dir... "+dirTemp);
            new File(dirTemp).mkdir();

            JSONArray images = response.getJSONArray("images");
            int idItem = response.getInt("images_count");

            for(int i = 0; i < images.length(); i++){



                String urlFile = images.getJSONObject(i).getString("link");
                String name = images.getJSONObject(i).getString("id");

                if(!images.getJSONObject(i).has("type"))
                    continue;

                if(images.getJSONObject(i).getString("type").equals("image/jpeg")){
                    name = "/"+name+".jpg";
                }
                if(images.getJSONObject(i).getString("type").equals("image/png")){
                    name = "/"+name+".png";
                }

                this.downloadFile(urlFile,"/"+response.getString("id")+"/"+name);

            }


        } catch (UnirestException e) {
            e.printStackTrace();
        }

    }
}
