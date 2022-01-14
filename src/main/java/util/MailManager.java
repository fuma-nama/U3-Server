package util;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

public class MailManager {
    private static final JavaMailSender mailSender = getJavaMailSender();

    public static void sendMail(String title, String context, String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("U3-Official");
        message.setTo(to);
        message.setSubject(title);
        message.setText(context);
        mailSender.send(message);
    }

    public static JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);

        mailSender.setUsername("xred379@gmail.com");
        mailSender.setPassword("1012410124Lol");
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }
}
