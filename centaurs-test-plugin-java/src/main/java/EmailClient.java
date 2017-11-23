import com.sun.mail.util.MailSSLSocketFactory;

import java.security.GeneralSecurityException;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * Created by Feliciano on 11/22/2017.
 * Please add following to gradle dependencies
 * compile group: 'com.sun.mail', name: 'javax.mail', version: '1.6.0'
 */
public class EmailClient implements Plugin.IEmailClient {

    String username;
    String password;

    public EmailClient(String username, String password) {
        this.username = username;
        this.password = password;
    }

    static class MyAuthenticator extends Authenticator{
        String u = null;
        String p = null;
        public MyAuthenticator(String u, String p){
            this.u=u;
            this.p=p;
        }
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(u,p);
        }
    }

    public boolean send(String title, String content) {
        String to = "server@centaurstech.com";//change accordingly
        String from = "server@centaurstech.com";//change accordingly
        String host = "smtp.exmail.qq.com";//or IP address

        // Get the session object
        Properties properties = System.getProperties();

        // Set properties
        properties.setProperty("mail.transport.protocol", "smtp");
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", "465");

        // Enalbe Auth
        properties.setProperty("mail.smtp.auth", "true");

        // Enable SSL
        MailSSLSocketFactory sf = null;
        try {
            sf = new MailSSLSocketFactory();
            sf.setTrustAllHosts(true);
        } catch (GeneralSecurityException e1) {
            e1.printStackTrace();
        }

        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.ssl.socketFactory", sf);

        Session session = Session.getDefaultInstance(properties, new MyAuthenticator(username, password));

        //compose the message
        try{
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(title);
            message.setText(content);

            // Send message
            Transport.send(message);
            System.out.println("message sent successfully....");
        }catch (MessagingException mex) {
            mex.printStackTrace();
            return false;
        }
        return true;
    }

    public static void main(String [] args){
        EmailClient emailClient = new EmailClient("test@test.test", "testtest");
        emailClient.send("Ping", "test email");
    }
}
