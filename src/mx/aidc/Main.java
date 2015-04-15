package mx.aidc;

import com.github.jreddit.entity.Submission;

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

        List<Submission> submissionsUser = data.getTop100();

        if(submissionsUser == null){
            System.out.println("Error target");
            return;
        }

        System.out.println("Reading submissions...");

        for(Iterator iterator = submissionsUser.iterator(); iterator.hasNext();){

            Submission sub = (Submission)iterator.next();
            String url = sub.getURL();
            System.out.println(sub.getTitle()+" - "+url);

        }

    }
}
