# language: es
@NF05 @NF06
Característica: Pago procesado una sola vez y sin datos de tarjeta
  # NF05: un webhook de pago repetido no duplica el pago ni extiende la vigencia.
  # NF06: el modelo de pago no guarda datos de tarjeta.
  Como administrador de una empresa
  Quiero que un mismo resultado de pago no se registre dos veces ni guarde datos de tarjeta
  Para que la vigencia de mi suscripción sea correcta y no se almacenen datos sensibles

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión

  Escenario: El mismo webhook de pago llega dos veces y no extiende la vigencia
    Dado que existe el plan activo de prueba "Plan-Idempotente" con precio mensual "42.00" y límite 5
    Cuando el administrador selecciona el plan "Plan-Idempotente"
    Entonces la respuesta tiene código 201
    Cuando el administrador inicia el checkout simulado de la suscripción creada
    Entonces la respuesta tiene código 201
    Cuando la pasarela confirma el último pago simulado
    Entonces la respuesta tiene código 200
    Y la confirmación de pago activa la suscripción y asigna vigencia mensual
    Cuando la pasarela repite el último evento de pago
    Entonces la respuesta tiene código 200
    Y el webhook repetido se reconoce sin extender la vigencia
    Y queda un único pago confirmado para el evento del proveedor del último webhook

  Escenario: Dos webhooks iguales seguidos dejan un solo registro de pago
    Dado que existe el plan activo de prueba "Plan-Doble" con precio mensual "30.00" y límite 3
    Cuando el administrador selecciona el plan "Plan-Doble"
    Entonces la respuesta tiene código 201
    Cuando el administrador inicia el checkout simulado de la suscripción creada
    Entonces la respuesta tiene código 201
    Cuando la pasarela envía dos veces el mismo webhook de pago confirmado
    Entonces la respuesta tiene código 200
    Y la primera entrega confirma el pago sin marcarlo como duplicado
    Y la segunda entrega se reconoce como duplicada
    Y la tabla de pagos tiene exactamente un registro para ese evento del proveedor

  Escenario: Un webhook con datos de tarjeta se procesa sin guardar esos datos
    Dado que existe el plan activo de prueba "Plan-Sin-Tarjeta" con precio mensual "28.00" y límite 2
    Cuando el administrador selecciona el plan "Plan-Sin-Tarjeta"
    Entonces la respuesta tiene código 201
    Cuando el administrador inicia el checkout simulado de la suscripción creada
    Entonces la respuesta tiene código 201
    Cuando la pasarela envía el pago confirmado con datos de tarjeta "4111111111111111" y cvv "123"
    Entonces la respuesta tiene código 200
    Y el pago guardado solo contiene la referencia del proveedor, el monto y las fechas
    Y ninguna columna de la tabla de pagos contiene el número de tarjeta ni el cvv
