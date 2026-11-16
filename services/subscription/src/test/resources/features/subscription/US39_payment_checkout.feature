# language: es
Característica: US39 Checkout y webhook de pago
  Como administrador de una empresa
  Quiero pagar o reintentar mi suscripción mediante un checkout
  Para activar la vigencia solo después de recibir una confirmación válida

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión

  Escenario: Confirmar un pago activa la suscripción y deduplica el webhook
    Dado que existe el plan activo de prueba "Plan-Pago" con precio mensual "37.50" y límite 5
    Cuando el administrador selecciona el plan "Plan-Pago"
    Entonces la respuesta tiene código 201
    Cuando el administrador inicia el checkout simulado de la suscripción creada
    Entonces la respuesta tiene código 201
    Y el checkout es simulado y no expone datos de tarjeta
    Cuando la pasarela confirma el último pago simulado
    Entonces la respuesta tiene código 200
    Y la confirmación de pago activa la suscripción y asigna vigencia mensual
    Y hay un único comprobante de pago simulado para el administrador
    Cuando la pasarela repite el último evento de pago
    Entonces la respuesta tiene código 200
    Y el webhook repetido se reconoce sin extender la vigencia
    Y hay un único comprobante de pago simulado para el administrador

  Escenario: Un rechazo informa el motivo y permite reintentar el pago
    Dado que existe el plan activo de prueba "Plan-Reintento" con precio mensual "25.00" y límite 4
    Cuando el administrador selecciona el plan "Plan-Reintento"
    Entonces la respuesta tiene código 201
    Cuando el administrador inicia el checkout simulado de la suscripción creada
    Entonces la respuesta tiene código 201
    Cuando la pasarela rechaza el último pago simulado con motivo "Fondos insuficientes"
    Entonces la respuesta tiene código 200
    Y el pago rechazado deja la suscripción pendiente e informa el motivo "Fondos insuficientes"
    Cuando el administrador consulta la suscripción creada
    Entonces la respuesta tiene código 200
    Y la consulta de suscripción informa el último rechazo "Fondos insuficientes"
    Cuando el administrador reintenta el checkout simulado
    Entonces la respuesta tiene código 201
    Cuando la pasarela confirma el último pago simulado
    Entonces la respuesta tiene código 200
    Y la confirmación de pago activa la suscripción y asigna vigencia mensual

  Escenario: Rechazar un webhook con firma inválida
    Dado que existe el plan activo de prueba "Plan-Firma" con precio mensual "19.00" y límite 2
    Cuando el administrador selecciona el plan "Plan-Firma"
    Entonces la respuesta tiene código 201
    Cuando el administrador inicia el checkout simulado de la suscripción creada
    Entonces la respuesta tiene código 201
    Cuando la pasarela envía un pago con una firma inválida
    Entonces la respuesta tiene código 401
    Cuando el administrador consulta la suscripción creada
    Entonces la respuesta tiene código 200
    Y la consulta de suscripción informa el estado "PENDING_PAYMENT" y ningún pago confirmado
