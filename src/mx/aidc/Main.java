package mx.aidc;

import com.github.jreddit.entity.Submission;
import com.mashape.unirest.http.Unirest;

import java.util.Iterator;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        SessionData data = SessionData.getInstance();

        boolean couldAuthuser = data.authUser(args);
        if(!couldAuthuser){
            System.out.println("Could not auth user");
            return;
        }

        List<Submission> submissionsUser;
        submissionsUser = data.getSubmissions(null);

        if(submissionsUser == null){
            System.out.println("Error target");
            return;
        }

        System.out.println("Reading submissions...");
        Submission last = null;

        while(submissionsUser.size() > 0){

            for(Iterator iterator = submissionsUser.iterator(); iterator.hasNext();){

                Submission sub = (Submission)iterator.next();
                data.processSubmission(sub);
                last = sub;
            }

            submissionsUser = data.getSubmissions(last);

        }

        try {
            Unirest.shutdown();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
