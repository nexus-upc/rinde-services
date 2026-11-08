# language: es
Característica: US40 Controlar el estado de la suscripción
  Como administrador de una empresa
  Quiero conocer el estado y la vigencia de mi suscripción
  Para anticipar el vencimiento y las restricciones aplicables

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión

  Escenario: Consultar el estado y la vigencia de la empresa
    Dado que existe una suscripción activa de prueba con vencimiento "HOY+20T00:00:00Z"
    Cuando el administrador consulta la suscripción de la empresa
    Entonces la respuesta tiene código 200
    Y la suscripción consultada tiene el estado "ACTIVE"
    Y la suscripción consultada vence en "HOY+20T00:00:00Z"

  Escenario: Una empresa no puede consultar la suscripción de otra empresa
    Dado que existe el plan activo de prueba "Plan-Consulta" con precio mensual "37.50" y límite 5
    Y que el administrador crea una suscripción pendiente con el plan "Plan-Consulta"
    Y que existe una empresa verificada con RUC "20987654321" y administrador "otro@otra.pe"
    Y que el administrador "otro@otra.pe" inició sesión
    Cuando un administrador de otra empresa consulta la suscripción creada
    Entonces la respuesta tiene código 404

  Escenario: Consultar una suscripción sin token requiere autenticación
    Dado que existe una suscripción activa de prueba con vencimiento "HOY+20T00:00:00Z"
    Cuando una persona sin token consulta la suscripción de la empresa
    Entonces la respuesta tiene código 401

  Escenario: Avisar cinco días antes del vencimiento
    Dado que la suscripción activa de la empresa vence en cinco días
    Cuando el proceso diario de suscripciones se ejecuta dos veces
    Y el administrador consulta la suscripción de la empresa
    Entonces la respuesta tiene código 200
    Y la consulta muestra el aviso de renovación y el estado "EXPIRING"
    Y se registra una sola notificación de vencimiento para el administrador

  Escenario: Suspender la suscripción vencida hace más de tres días
    Dado que existe una suscripción activa vencida hace más de tres días
    Cuando el proceso diario de suscripciones avanza los estados
    Entonces la suscripción queda suspendida y se publica el evento de suspensión
