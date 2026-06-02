package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.ChatMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMensajeRepository extends JpaRepository<ChatMensaje, Long> {

    /** Últimos N mensajes de una sesión, ordenados cronológicamente. */
    @Query("SELECT m FROM ChatMensaje m WHERE m.chat.idChat = :idChat ORDER BY m.fecha ASC")
    List<ChatMensaje> findBySesion(@Param("idChat") Long idChat);

    /** Últimos N mensajes para enviar como contexto a la IA. */
    @Query(value = "SELECT * FROM chat_mensaje WHERE id_chat = :idChat ORDER BY fecha DESC LIMIT :limite", nativeQuery = true)
    List<ChatMensaje> findUltimosMensajes(@Param("idChat") Long idChat, @Param("limite") int limite);

    /** Cuenta mensajes del usuario en una sesión (para rate limiting). */
    @Query("SELECT COUNT(m) FROM ChatMensaje m WHERE m.chat.idChat = :idChat AND m.emisor = 'USUARIO'")
    long contarMensajesUsuario(@Param("idChat") Long idChat);
}
