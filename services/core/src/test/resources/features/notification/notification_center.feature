# language: es
Característica: Centro de notificaciones de la empresa
  Como administrador u operador de la empresa
  Quiero ver y marcar los avisos operativos de gastos, incidencias y viajes
  Para revisar lo pendiente sin perder avisos y sin afectar la lectura de otros usuarios

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión
    Y que el administrador invitó a "Jorge Ruiz" con el correo "jorge@andes.pe" y el rol "OPERATIONS_MANAGER"
    Y que el operador "jorge@andes.pe" inició sesión

  Escenario: Ver el aviso de un gasto registrado y el contador de no leídas
    Cuando se registra el gasto "gasto-1" por "PEN 180.50" en el viaje "Lima-Arequipa"
    Y el usuario "ana@andes.pe" consulta sus notificaciones
    Entonces la respuesta tiene código 200
    Y la lista de notificaciones tiene 1 elementos
    Y la primera notificación tiene el tipo "EXPENSE_PENDING_REVIEW" y el título "Gasto pendiente de revisión"
    Y el mensaje de la primera notificación es "Se registró un gasto por PEN 180.50 para revisar."
    Cuando el usuario "ana@andes.pe" consulta el contador de no leídas
    Entonces el contador de no leídas es 1

  Escenario: Filtrar las notificaciones por tipo y por no leídas
    Dado que se registró el gasto "gasto-1" por "PEN 40.00" en el viaje "Lima-Trujillo"
    Y que se reportó la incidencia de tipo "ACCIDENTE" en el viaje "Lima-Trujillo"
    Cuando el usuario "ana@andes.pe" consulta sus notificaciones de tipo "INCIDENT_REPORTED"
    Entonces la respuesta tiene código 200
    Y la lista de notificaciones tiene 1 elementos
    Y la primera notificación tiene el tipo "INCIDENT_REPORTED" y el título "Incidencia reportada"
    Cuando el usuario "ana@andes.pe" consulta sus notificaciones no leídas de tipo "EXPENSE_PENDING_REVIEW"
    Entonces la lista de notificaciones tiene 1 elementos
    Y la primera notificación tiene el tipo "EXPENSE_PENDING_REVIEW" y el título "Gasto pendiente de revisión"

  Escenario: Marcar una notificación como leída sin afectar a otros usuarios
    Dado que se registró el gasto "gasto-1" por "PEN 40.00" en el viaje "Lima-Trujillo"
    Y que se registró el gasto "gasto-2" por "PEN 75.00" en el viaje "Lima-Piura"
    Cuando el usuario "ana@andes.pe" marca como leída la notificación del gasto "gasto-1"
    Entonces la respuesta tiene código 204
    Cuando el usuario "ana@andes.pe" consulta el contador de no leídas
    Entonces el contador de no leídas es 1
    Cuando el usuario "jorge@andes.pe" consulta sus notificaciones no leídas
    Entonces la lista de notificaciones tiene 2 elementos

  Escenario: Marcar todas las notificaciones como leídas
    Dado que se registró el gasto "gasto-1" por "PEN 40.00" en el viaje "Lima-Trujillo"
    Y que se reportó la incidencia de tipo "ACCIDENTE" en el viaje "Lima-Trujillo"
    Cuando el usuario "ana@andes.pe" marca todas las notificaciones como leídas
    Entonces la respuesta tiene código 204
    Cuando el usuario "ana@andes.pe" consulta el contador de no leídas
    Entonces el contador de no leídas es 0
    Cuando el usuario "jorge@andes.pe" consulta el contador de no leídas
    Entonces el contador de no leídas es 2

  Escenario: Un evento repetido no duplica la notificación
    Dado que se registró el gasto "gasto-1" por "PEN 40.00" en el viaje "Lima-Trujillo"
    Cuando llega nuevamente el mismo evento del gasto "gasto-1"
    Y el usuario "ana@andes.pe" consulta sus notificaciones
    Entonces la lista de notificaciones tiene 1 elementos

  Escenario: Error 404 al marcar como leída una notificación que no existe
    Cuando el usuario "ana@andes.pe" marca como leída una notificación inexistente
    Entonces la respuesta tiene código 404
    Y el mensaje de error es "La notificación no existe."

  Escenario: Error 400 al filtrar con un tipo de aviso inválido
    Cuando el usuario "ana@andes.pe" consulta sus notificaciones de tipo "CUALQUIERA"
    Entonces la respuesta tiene código 400
    Y el mensaje de error es "El parámetro type contiene un tipo de aviso inválido."
