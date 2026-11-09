# language: es
Característica: US06 Repetición de solicitudes de demostración
  Como equipo comercial de RINDE
  Quiero que un mismo correo no genere solicitudes duplicadas en poco tiempo
  Para atender cada interesado una sola vez mientras revisamos su solicitud

  Escenario: Segundo envío del mismo correo dentro de 10 minutos
    Dado que ya se envió una solicitud de demostración con el correo "ana.rojas@transportesandes.pe"
    Cuando el visitante envía otra solicitud de demostración con el correo "ana.rojas@transportesandes.pe"
    Entonces la respuesta tiene código 429
    Y el mensaje de error es "Ya recibimos tu solicitud. Te contactaremos pronto."
    Y se registra una sola solicitud de demostración

  Escenario: El mismo correo con mayúsculas cuenta como repetido
    Dado que ya se envió una solicitud de demostración con el correo "ana.rojas@transportesandes.pe"
    Cuando el visitante envía otra solicitud de demostración con el correo "Ana.Rojas@TransportesAndes.pe"
    Entonces la respuesta tiene código 429
    Y se registra una sola solicitud de demostración

  Escenario: Un correo distinto no se considera repetido
    Dado que ya se envió una solicitud de demostración con el correo "ana.rojas@transportesandes.pe"
    Cuando el visitante envía otra solicitud de demostración con el correo "luis.paz@ejemplo.pe"
    Entonces la respuesta tiene código 201
    Y se registran 2 solicitudes de demostración

  Escenario: Pasados 10 minutos, el mismo correo puede volver a enviar
    Dado que ya se envió una solicitud de demostración con el correo "ana.rojas@transportesandes.pe"
    Y que la solicitud se recibió hace más de 10 minutos
    Cuando el visitante envía otra solicitud de demostración con el correo "ana.rojas@transportesandes.pe"
    Entonces la respuesta tiene código 201
    Y se registran 2 solicitudes de demostración
