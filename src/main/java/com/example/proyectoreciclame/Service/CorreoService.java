package com.example.proyectoreciclame.Service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class CorreoService {

    private final JavaMailSender mailSender;

    @Value("${app.url:http://localhost:8080}")
    private String appUrl;

    public CorreoService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCodigoRecuperacion(String destino, String codigo) {
        try {
            String htmlContent = cargarTemplate("templates/emails/codigo-recuperacion.html");
            htmlContent = htmlContent.replace("[[CODIGO]]", codigo);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.toString());
            helper.setTo(destino);
            helper.setSubject("Recíclame - Código de recuperación");
            helper.setText(htmlContent, true);

            // Embeber logo
            ClassPathResource logo = new ClassPathResource("static/images/logo-reciclame.png");
            helper.addInline("logo", logo);

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        }
    }

    public void enviarConfirmacionRegistro(String destino, String nombres, String rolSolicitado) {
        try {
            String htmlContent = cargarTemplate("templates/emails/confirmacion-registro.html");
            htmlContent = htmlContent.replace("[[NOMBRE]]", obtenerNombreSeguro(nombres));
            htmlContent = htmlContent.replace("[[ROL]]", rolSolicitado != null ? rolSolicitado : "usuario");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.toString());
            helper.setTo(destino);
            helper.setSubject("Recíclame - Registro recibido");
            helper.setText(htmlContent, true);

            // Embeber logo
            ClassPathResource logo = new ClassPathResource("static/images/logo-reciclame.png");
            helper.addInline("logo", logo);

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        }
    }

    public void enviarRegistroAprobado(String destino, String nombres) {
        try {
            String htmlContent = cargarTemplate("templates/emails/solicitud-aprobada.html");
            htmlContent = htmlContent.replace("[[NOMBRE]]", obtenerNombreSeguro(nombres));
            htmlContent = htmlContent.replace("[[CORREO]]", destino);
            htmlContent = htmlContent.replace("[[APP_URL]]", appUrl);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.toString());
            helper.setTo(destino);
            helper.setSubject("Recíclame - Solicitud aprobada");
            helper.setText(htmlContent, true);

            // Embeber logo
            ClassPathResource logo = new ClassPathResource("static/images/logo-reciclame.png");
            helper.addInline("logo", logo);

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        }
    }

    public void enviarRegistroDenegado(String destino, String nombres, String motivo) {
        try {
            String motivoFinal = (motivo != null && !motivo.isBlank())
                    ? motivo
                    : "No se especificó un motivo.";

            String htmlContent = cargarTemplate("templates/emails/solicitud-denegada.html");
            htmlContent = htmlContent.replace("[[NOMBRE]]", obtenerNombreSeguro(nombres));
            htmlContent = htmlContent.replace("[[MOTIVO]]", motivoFinal);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.toString());
            helper.setTo(destino);
            helper.setSubject("Recíclame - Solicitud denegada");
            helper.setText(htmlContent, true);

            // Embeber logo
            ClassPathResource logo = new ClassPathResource("static/images/logo-reciclame.png");
            helper.addInline("logo", logo);

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        }
    }

    private String obtenerNombreSeguro(String nombres) {
        return (nombres != null && !nombres.isBlank()) ? nombres : "usuario";
    }

    public void enviarCredencialesAdministrador(String destino, String nombres, String contrasenaTemp) {
        try {
            String htmlContent = cargarTemplate("templates/emails/credenciales-administrador.html");
            htmlContent = htmlContent.replace("[[NOMBRE]]", obtenerNombreSeguro(nombres));
            htmlContent = htmlContent.replace("[[CORREO]]", destino);
            htmlContent = htmlContent.replace("[[CONTRASENA]]", contrasenaTemp);
            htmlContent = htmlContent.replace("[[APP_URL]]", appUrl);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.toString());
            helper.setTo(destino);
            helper.setSubject("Recíclame - Tu cuenta de administrador ha sido creada");
            helper.setText(htmlContent, true);

            // Embeber logo
            ClassPathResource logo = new ClassPathResource("static/images/logo-reciclame.png");
            helper.addInline("logo", logo);

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Carga un template HTML desde el classpath
     */
    private String cargarTemplate(String rutaTemplate) throws IOException {
        ClassPathResource resource = new ClassPathResource(rutaTemplate);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}