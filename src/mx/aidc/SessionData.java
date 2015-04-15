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

    public static SessionData getInstance() {
        return ourInstance;
    }

    private SessionData() {
    }

    public boolean authUser(String[] args){

        int index= 0;
        String username = "";
        String password = "";

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

            index++;

        }

        if(username.equals("") || password.equals("") || targetUser.equals("")){
            System.out.println("Some arguments are missing");
            return false;
        }

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

    public List<Submission> getTop100(){

        if(!isReady){
            System.out.println("You need to auth first");
            return null;
        }

        // Handle to Submissions, which offers the basic API submission functionality
        Submissions subms = new Submissions(restClient, selectedUser);

        List<Submission> submissionsUser = null;
        try {
            submissionsUser = subms.ofUser(targetUser, UserSubmissionsCategory.SUBMITTED, UserOverviewSort.NEW, -1, 10, null, null, true);
        }catch (RetrievalFailedException e){
            System.out.println("Could not find the target user");
        }

        return submissionsUser;
    }
}
