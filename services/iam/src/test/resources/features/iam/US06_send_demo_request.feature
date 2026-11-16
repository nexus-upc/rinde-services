# language: es
Característica: US06 Solicitar una demostración del producto
  Como responsable de una empresa de transporte interesado en RINDE
  Quiero dejar mis datos de contacto desde la landing page
  Para que el equipo se comunique conmigo y me muestre la plataforma

  Escenario: Envío correcto del formulario
    Dado que el visitante completa la solicitud de demostración con datos válidos
    Cuando el visitante envía la solicitud de demostración
    Entonces la respuesta tiene código 201
    Y la respuesta incluye el campo "status" con el valor "RECEIVED"
    Y la respuesta incluye un identificador y la fecha de recepción
    Y se registra una solicitud de demostración para el correo "ana.rojas@transportesandes.pe"

  Escenario: Envío con teléfono y mensaje opcionales
    Dado que el visitante completa la solicitud de demostración con datos válidos
    Y el campo "phone" tiene el valor "+51 999 888 777"
    Y el campo "message" tiene el valor "Queremos revisar la gestión de gastos"
    Cuando el visitante envía la solicitud de demostración
    Entonces la respuesta tiene código 201
    Y se registra la solicitud con el teléfono "+51 999 888 777"
